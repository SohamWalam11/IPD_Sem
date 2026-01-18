package org.example.project.tyre3d

import android.content.Context
import android.net.Uri
import android.util.Base64
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
import io.ktor.client.statement.bodyAsChannel
import io.ktor.http.ContentType
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
 * ImageTo3DService - Single 2D Image to 3D Model Conversion
 * ═══════════════════════════════════════════════════════════════════════════════
 * 
 * This service takes a single 2D image captured from CameraScreen and converts
 * it to a 3D model using Meshy AI's Image-to-3D API.
 * 
 * Workflow:
 * 1. User captures image using existing CameraScreen
 * 2. Image is sent to Meshy AI for 3D conversion
 * 3. Service polls for completion and downloads GLB model
 * 4. 3D model is displayed in SceneView viewer
 * 
 * API Documentation: https://docs.meshy.ai/api-image-to-3d
 * 
 * SETUP: Add MESHY_API_KEY to your .env file:
 * MESHY_API_KEY = "your-api-key-here"
 */
class ImageTo3DService(
    private val context: Context
) {
    companion object {
        private const val TAG = "ImageTo3DService"
        private const val BASE_URL = "https://api.meshy.ai"
        private const val POLL_INTERVAL_MS = 3000L  // 3 seconds
        private const val MAX_POLL_ATTEMPTS = 200   // ~10 minutes max
    }
    
    // Get API key from BuildConfig (loaded from .env)
    private val apiKey: String = BuildConfig.MESHY_API_KEY
    
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
     * Checks if the Meshy API is configured and available
     */
    fun isAvailable(): Boolean {
        return apiKey.isNotBlank() && apiKey != "YOUR_MESHY_API_KEY"
    }
    
    /**
     * Converts a 2D image to a 3D model
     * 
     * @param imageUri The URI of the captured image (from CameraScreen)
     * @return Flow of conversion progress updates
     */
    fun convertTo3D(imageUri: String): Flow<ConversionProgress> = flow {
        _conversionState.value = ConversionState.Converting
        
        emit(ConversionProgress.Starting("Preparing image for 3D conversion..."))
        
        try {
            // Validate API key
            if (!isAvailable()) {
                emit(ConversionProgress.Error(
                    "Meshy API key not configured. Add MESHY_API_KEY to .env file."
                ))
                _conversionState.value = ConversionState.Error("API key missing")
                return@flow
            }
            
            // Step 1: Read image and convert to base64
            emit(ConversionProgress.Uploading(0.2f, "Reading image..."))
            val imageBytes = readImageBytes(imageUri)
            val base64Image = Base64.encodeToString(imageBytes, Base64.NO_WRAP)
            
            // Step 2: Create Image-to-3D task
            emit(ConversionProgress.Uploading(0.5f, "Uploading to Meshy AI..."))
            val taskId = createTask(base64Image)
            Log.i(TAG, "Created Meshy task: $taskId")
            
            emit(ConversionProgress.Uploading(1.0f, "Upload complete!"))
            
            // Step 3: Poll for completion
            var attempts = 0
            var lastProgress = 0f
            
            while (attempts < MAX_POLL_ATTEMPTS) {
                delay(POLL_INTERVAL_MS)
                attempts++
                
                val status = getTaskStatus(taskId)
                Log.d(TAG, "Task status: ${status.status}, progress: ${status.progress}")
                
                when (status.status) {
                    "SUCCEEDED" -> {
                        emit(ConversionProgress.Processing(1.0f, "3D model ready!"))
                        
                        // Step 4: Download the GLB model
                        emit(ConversionProgress.Downloading(0.1f, "Downloading 3D model..."))
                        
                        val modelUrl = status.modelUrls?.glb
                            ?: throw Exception("No GLB model URL in response")
                        
                        val modelPath = downloadModel(taskId, modelUrl)
                        
                        emit(ConversionProgress.Complete(
                            modelPath = modelPath,
                            thumbnailUrl = status.thumbnailUrl
                        ))
                        
                        _conversionState.value = ConversionState.Complete(modelPath)
                        return@flow
                    }
                    
                    "FAILED" -> {
                        val errorMsg = status.taskError?.message ?: "Conversion failed"
                        emit(ConversionProgress.Error(errorMsg))
                        _conversionState.value = ConversionState.Error(errorMsg)
                        return@flow
                    }
                    
                    "PENDING", "IN_PROGRESS" -> {
                        val progress = status.progress / 100f
                        if (progress > lastProgress) {
                            lastProgress = progress
                            emit(ConversionProgress.Processing(
                                progress = progress,
                                stage = getStageDescription(progress)
                            ))
                        }
                    }
                }
            }
            
            emit(ConversionProgress.Error("Conversion timeout - please try again"))
            _conversionState.value = ConversionState.Error("Timeout")
            
        } catch (e: Exception) {
            Log.e(TAG, "Conversion error: ${e.message}", e)
            emit(ConversionProgress.Error(e.message ?: "Unknown error"))
            _conversionState.value = ConversionState.Error(e.message ?: "Unknown error")
        }
    }
    
    /**
     * Reads image bytes from a content URI
     */
    private fun readImageBytes(imageUri: String): ByteArray {
        val uri = Uri.parse(imageUri)
        return context.contentResolver.openInputStream(uri)?.use { 
            it.readBytes() 
        } ?: throw Exception("Cannot read image from $imageUri")
    }
    
    /**
     * Creates an Image-to-3D task with Meshy AI
     */
    private suspend fun createTask(base64Image: String): String {
        val request = MeshyCreateRequest(
            imageUrl = "data:image/jpeg;base64,$base64Image",
            enablePbr = true,
            surfaceMode = "organic",  // Good for rubber/tyre materials
            aiModel = "meshy-4"       // Latest model version
        )
        
        val response = httpClient.post("$BASE_URL/v2/image-to-3d") {
            contentType(ContentType.Application.Json)
            setBody(request)
        }
        
        if (response.status.value !in 200..299) {
            throw Exception("Failed to create task: HTTP ${response.status.value}")
        }
        
        val result: MeshyCreateResponse = response.body()
        return result.result ?: throw Exception("No task ID in response")
    }
    
    /**
     * Gets the current status of a Meshy task
     */
    private suspend fun getTaskStatus(taskId: String): MeshyTaskStatus {
        val response = httpClient.get("$BASE_URL/v2/image-to-3d/$taskId")
        
        if (response.status.value !in 200..299) {
            throw Exception("Failed to get status: HTTP ${response.status.value}")
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
    
    /**
     * Returns a human-readable description of the current processing stage
     */
    private fun getStageDescription(progress: Float): String {
        return when {
            progress < 0.2f -> "Analyzing image..."
            progress < 0.4f -> "Detecting geometry..."
            progress < 0.6f -> "Building 3D mesh..."
            progress < 0.8f -> "Generating textures..."
            else -> "Finalizing model..."
        }
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
// API Data Classes
// ═══════════════════════════════════════════════════════════════════════════════

@Serializable
private data class MeshyCreateRequest(
    @SerialName("image_url") val imageUrl: String,
    @SerialName("enable_pbr") val enablePbr: Boolean = true,
    @SerialName("surface_mode") val surfaceMode: String = "organic",
    @SerialName("ai_model") val aiModel: String = "meshy-4"
)

@Serializable
private data class MeshyCreateResponse(
    val result: String? = null
)

@Serializable
private data class MeshyTaskStatus(
    val id: String = "",
    val status: String = "",
    val progress: Int = 0,
    @SerialName("model_urls") val modelUrls: MeshyModelUrls? = null,
    @SerialName("thumbnail_url") val thumbnailUrl: String? = null,
    @SerialName("task_error") val taskError: MeshyTaskError? = null
)

@Serializable
private data class MeshyModelUrls(
    val glb: String? = null,
    val gltf: String? = null,
    val obj: String? = null,
    val fbx: String? = null,
    val usdz: String? = null
)

@Serializable
private data class MeshyTaskError(
    val message: String? = null
)
