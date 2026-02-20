package org.example.project.tyre3d

import android.content.Context
import android.net.Uri
import android.util.Log
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.defaultRequest
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.request.forms.MultiPartFormDataContent
import io.ktor.client.request.forms.formData
import io.ktor.client.statement.bodyAsChannel
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.Headers
import io.ktor.http.HttpHeaders
import io.ktor.http.contentType
import io.ktor.serialization.kotlinx.json.json
import io.ktor.utils.io.jvm.javaio.copyTo
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flow
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import org.example.project.BuildConfig
import java.io.File
import java.io.FileOutputStream

/**
 * ═══════════════════════════════════════════════════════════════════════════════
 * ImageTo3DService — Converts a tyre photo to a 3D GLB model via Tripo3D API
 * ═══════════════════════════════════════════════════════════════════════════════
 *
 * Workflow:
 *  1. Upload image   → POST /upload/sts   (multipart)          → image_token
 *  2. Create task    → POST /task         (image_to_model)     → task_id
 *  3. Poll status    → GET  /task/{id}                         → status / progress
 *  4. Download GLB   → GET  output.model  URL                  → *.glb file path
 *
 * API docs: https://platform.tripo3d.ai/docs
 * Auth:     Authorization: Bearer <TRIPO3D_API_KEY>
 *
 * SETUP: Add TRIPO3D_API_KEY to your .env file:
 *   TRIPO3D_API_KEY=tsk_your_key_here
 */
class ImageTo3DService(
    private val context: Context
) {
    companion object {
        private const val TAG               = "ImageTo3DService"
        private const val BASE_URL          = "https://api.tripo3d.ai/v2/openapi"
        private const val POLL_INTERVAL_MS  = 4000L
        private const val MAX_POLL_ATTEMPTS = 150
    }
    
    // Get API key from BuildConfig (loaded from .env)
    private val apiKey: String = BuildConfig.TRIPO3D_API_KEY
    
    // Output directory for downloaded models
    private val modelsDir: File by lazy {
        File(context.filesDir, "tyre_3d_models").also { it.mkdirs() }
    }
    
    // Current conversion state
    private val _conversionState = MutableStateFlow<ConversionState>(ConversionState.Idle)
    val conversionState: StateFlow<ConversionState> = _conversionState.asStateFlow()
    
    // HTTP client configured for Meshy API
    private val httpClient by lazy {
        HttpClient(OkHttp) {
            install(ContentNegotiation) {
                json(Json {
                    ignoreUnknownKeys = true
                    isLenient = true
                })
            }
            defaultRequest {
                header(HttpHeaders.Authorization, "Bearer $apiKey")
            }
        }
    }
    
    /**
     * Checks if the Tripo3D API is configured and available
     */
    fun isAvailable(): Boolean = apiKey.isNotBlank() && !apiKey.startsWith("your_")
    
    /**
     * Converts a 2D image to a 3D model via Tripo3D API
     *
     * @param imageUri Absolute file path or `content://` URI of the captured tyre image.
     * @return [Flow] of [ConversionProgress] updates.
     */
    fun convertTo3D(imageUri: String): Flow<ConversionProgress> = flow {
        _conversionState.value = ConversionState.Converting
        emit(ConversionProgress.Starting("Preparing image for 3D conversion…"))

        try {
            if (!isAvailable()) {
                val msg = "Tripo3D API key not configured. Add TRIPO3D_API_KEY to .env file."
                emit(ConversionProgress.Error(msg))
                _conversionState.value = ConversionState.Error(msg)
                return@flow
            }

            // Step 1: Read image bytes
            emit(ConversionProgress.Uploading(0.1f, "Reading image…"))
            val imageBytes = readImageBytes(imageUri)

            // Step 2: Upload to Tripo3D /upload/sts (multipart)
            emit(ConversionProgress.Uploading(0.4f, "Uploading image to Tripo3D…"))
            val fileToken = uploadImage(imageBytes)
            Log.i(TAG, "Image uploaded — file_token: $fileToken")

            // Step 3: Create image_to_model task
            emit(ConversionProgress.Uploading(0.9f, "Creating 3D generation task…"))
            val taskId = createTask(fileToken)
            Log.i(TAG, "Tripo3D task created: $taskId")
            emit(ConversionProgress.Uploading(1.0f, "Task submitted!"))

            // Step 4: Poll until success / failure
            var attempts = 0
            while (attempts < MAX_POLL_ATTEMPTS) {
                delay(POLL_INTERVAL_MS)
                attempts++

                val statusResp = getTaskStatus(taskId)
                val taskData   = statusResp.data
                Log.d(TAG, "Poll #$attempts — status=${taskData?.status} progress=${taskData?.progress}")

                when (taskData?.status) {
                    "success" -> {
                        emit(ConversionProgress.Processing(1.0f, "3D model ready!"))
                        val modelUrl = taskData.output?.model
                            ?: taskData.output?.pbrModel
                            ?: throw Exception("Tripo3D returned no model URL")
                        emit(ConversionProgress.Downloading(0.1f, "Downloading GLB model…"))
                        val modelPath = downloadModel(taskId, modelUrl)
                        emit(ConversionProgress.Complete(modelPath = modelPath))
                        _conversionState.value = ConversionState.Complete(modelPath)
                        return@flow
                    }
                    "failed", "banned", "expired", "cancelled", "unknown" -> {
                        val msg = "3D conversion ${taskData.status} (task: $taskId)"
                        emit(ConversionProgress.Error(msg))
                        _conversionState.value = ConversionState.Error(msg)
                        return@flow
                    }
                    else -> {
                        // queued | running | null — keep polling
                        val progress = (taskData?.progress ?: 0) / 100f
                        emit(ConversionProgress.Processing(
                            progress = progress.coerceIn(0f, 0.99f),
                            stage    = getStageDescription(progress)
                        ))
                    }
                }
            }

            val timeout = "Conversion timed out — please try again"
            emit(ConversionProgress.Error(timeout))
            _conversionState.value = ConversionState.Error(timeout)

        } catch (e: Exception) {
            Log.e(TAG, "Conversion error: ${e.message}", e)
            emit(ConversionProgress.Error(e.message ?: "Unknown error"))
            _conversionState.value = ConversionState.Error(e.message ?: "Unknown error")
        }
    }
    
    /**
     * Reads image bytes from a file path or content URI
     */
    private fun readImageBytes(imageUri: String): ByteArray {
        return try {
            // Try as file path first
            val file = File(imageUri)
            if (file.exists()) {
                Log.d(TAG, "Reading image from file: $imageUri")
                file.readBytes()
            } else {
                // Try as content URI
                val uri = Uri.parse(imageUri)
                Log.d(TAG, "Reading image from URI: $uri")
                context.contentResolver.openInputStream(uri)?.use { 
                    it.readBytes() 
                } ?: throw Exception("Cannot open input stream for $imageUri")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to read image: ${e.message}", e)
            throw Exception("Cannot read image: ${e.message}")
        }
    }
    
    /**
     * Uploads image bytes to Tripo3D via multipart POST /upload/sts.
     * Returns the `image_token` needed for task creation.
     */
    private suspend fun uploadImage(imageBytes: ByteArray): String {
        val response = httpClient.post("$BASE_URL/upload/sts") {
            setBody(
                MultiPartFormDataContent(
                    formData {
                        append(
                            key   = "file",
                            value = imageBytes,
                            headers = Headers.build {
                                append(HttpHeaders.ContentType,        "image/jpeg")
                                append(HttpHeaders.ContentDisposition, "filename=\"tyre.jpg\"")
                            }
                        )
                    }
                )
            )
        }
        if (response.status.value !in 200..299) {
            val bodyText = runCatching { response.bodyAsText() }.getOrElse { "" }
            val tripoCode = runCatching { Json.decodeFromString<TripoCodeResponse>(bodyText).code }.getOrElse { -1 }
            val msg = when (tripoCode) {
                2010 -> "Insufficient Tripo3D credits — top up at platform.tripo3d.ai/billing"
                2000 -> "Tripo3D rate limit exceeded — please wait and retry"
                2008 -> "Image rejected by content policy — try a different photo"
                else -> "Upload failed — HTTP ${response.status.value} (code $tripoCode)"
            }
            throw Exception(msg)
        }
        val body: TripoUploadResponse = response.body()
        if (body.code != 0) throw Exception("Upload error — Tripo3D code ${body.code}")
        return body.data?.imageToken
            ?: throw Exception("No image_token in Tripo3D upload response")
    }

    /**
     * Creates an `image_to_model` task on Tripo3D using [fileToken] from the upload step.
     */
    private suspend fun createTask(fileToken: String): String {
        val request = TripoCreateTaskRequest(
            type = "image_to_model",
            file = TripoFileRef(type = "jpg", fileToken = fileToken)
        )
        val response = httpClient.post("$BASE_URL/task") {
            contentType(ContentType.Application.Json)
            setBody(request)
        }
        if (response.status.value !in 200..299) {
            val bodyText = runCatching { response.bodyAsText() }.getOrElse { "" }
            val tripoCode = runCatching { Json.decodeFromString<TripoCodeResponse>(bodyText).code }.getOrElse { -1 }
            val msg = when (tripoCode) {
                2010 -> "Insufficient Tripo3D credits — top up at platform.tripo3d.ai/billing"
                2000 -> "Tripo3D rate limit exceeded — please wait and retry"
                2008 -> "Image rejected by content policy — try a different photo"
                2003 -> "Image could not be processed — please try again"
                else -> "Task creation failed — HTTP ${response.status.value} (code $tripoCode)"
            }
            throw Exception(msg)
        }
        val body: TripoCreateTaskResponse = response.body()
        if (body.code != 0) throw Exception("Task creation error — Tripo3D code ${body.code}")
        return body.data?.taskId
            ?: throw Exception("No task_id in Tripo3D task creation response")
    }

    /**
     * Polls `GET /task/{taskId}` for current status + progress.
     */
    private suspend fun getTaskStatus(taskId: String): TripoTaskStatusResponse {
        val response = httpClient.get("$BASE_URL/task/$taskId")
        if (response.status.value !in 200..299) {
            throw Exception("Status check failed — HTTP ${response.status.value}")
        }
        return response.body()
    }
    
    /**
     * Downloads the GLB model from Meshy
     */
    private suspend fun downloadModel(taskId: String, modelUrl: String): String {
        val outputFile = File(modelsDir, "tyre_${taskId}.glb")
        
        val response = httpClient.get(modelUrl)
        
        if (response.status.value !in 200..299) {
            throw Exception("Failed to download model: HTTP ${response.status.value}")
        }
        
        FileOutputStream(outputFile).use { outputStream ->
            response.bodyAsChannel().copyTo(outputStream)
        }
        
        Log.i(TAG, "Model downloaded to: ${outputFile.absolutePath}")
        return outputFile.absolutePath
    }
    
    private fun getStageDescription(progress: Float): String = when {
        progress < 0.20f -> "Analysing image structure…"
        progress < 0.40f -> "Building 3D geometry…"
        progress < 0.60f -> "Generating mesh topology…"
        progress < 0.80f -> "Applying PBR textures…"
        else             -> "Finalising 3D model…"
    }
    
    /**
     * Returns the path to a previously generated model, if it exists
     */
    fun getExistingModel(taskId: String): String? {
        val modelFile = File(modelsDir, "tyre_${taskId}.glb")
        return if (modelFile.exists()) modelFile.absolutePath else null
    }
    
    /**
     * Lists all generated 3D models
     */
    fun getAllModels(): List<File> {
        return modelsDir.listFiles { file -> file.extension == "glb" }?.toList() ?: emptyList()
    }
    
    /**
     * Deletes a generated model
     */
    fun deleteModel(modelPath: String): Boolean {
        return File(modelPath).delete()
    }
    
    /**
     * Resets the conversion state
     */
    fun resetState() {
        _conversionState.value = ConversionState.Idle
    }
}

// ═══════════════════════════════════════════════════════════════════════════════
// State Classes
// ═══════════════════════════════════════════════════════════════════════════════

/**
 * Represents the overall state of the 3D conversion service
 */
sealed class ConversionState {
    object Idle : ConversionState()
    object Converting : ConversionState()
    data class Complete(val modelPath: String) : ConversionState()
    data class Error(val message: String) : ConversionState()
}

/**
 * Progress updates during conversion
 */
sealed class ConversionProgress {
    data class Starting(val message: String) : ConversionProgress()
    data class Uploading(val progress: Float, val message: String) : ConversionProgress()
    data class Processing(val progress: Float, val stage: String) : ConversionProgress()
    data class Downloading(val progress: Float, val message: String) : ConversionProgress()
    data class Complete(val modelPath: String, val thumbnailUrl: String? = null) : ConversionProgress()
    data class Error(val message: String) : ConversionProgress()
}

// ═══════════════════════════════════════════════════════════════════════════════
// Tripo3D API serialisation data classes
// ═══════════════════════════════════════════════════════════════════════════════

// ── Upload ────────────────────────────────────────────────────────────────────

@Serializable
private data class TripoUploadResponse(
    val code: Int = 0,
    val data: TripoUploadData? = null
)

@Serializable
private data class TripoUploadData(
    @SerialName("image_token") val imageToken: String? = null
)

// ── Task creation ─────────────────────────────────────────────────────────────

@Serializable
private data class TripoCreateTaskRequest(
    val type: String,
    val file: TripoFileRef
)

@Serializable
private data class TripoFileRef(
    val type: String,
    @SerialName("file_token") val fileToken: String
)

@Serializable
private data class TripoCreateTaskResponse(
    val code: Int = 0,
    val data: TripoCreateTaskData? = null
)

@Serializable
private data class TripoCreateTaskData(
    @SerialName("task_id") val taskId: String? = null
)

// ── Task status polling ───────────────────────────────────────────────────────

@Serializable
private data class TripoTaskStatusResponse(
    val code: Int = 0,
    val data: TripoTaskData? = null
)

@Serializable
private data class TripoTaskData(
    @SerialName("task_id") val taskId: String = "",
    val type: String = "",
    val status: String = "",        // queued | running | success | failed | banned | expired | cancelled
    val progress: Int = 0,          // 0–100
    val output: TripoTaskOutput? = null
)

/** Minimal wrapper to extract the top-level `code` from any Tripo3D error response. */
@Serializable
private data class TripoCodeResponse(val code: Int = -1)

@Serializable
private data class TripoTaskOutput(
    @SerialName("model")          val model:         String? = null,   // primary GLB URL
    @SerialName("base_model")     val baseModel:     String? = null,
    @SerialName("pbr_model")      val pbrModel:      String? = null,
    @SerialName("rendered_image") val renderedImage: String? = null
)
