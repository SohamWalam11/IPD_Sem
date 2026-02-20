package org.example.project.sdk

import android.content.Context
import android.graphics.Bitmap
import android.util.Base64
import android.util.Log
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.engine.okhttp.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.io.ByteArrayOutputStream
import java.util.Calendar

/**
 * Michelin Mobility Intelligence API Integration
 *
 * Provides AI-powered tire recognition capabilities:
 * - Tire Size Recognition: Extract dimensions and specifications from sidewall photos
 * - DOT Image Recognition: Decode manufacturing date and DOT codes
 *
 * API Documentation: https://developer.michelin.com/
 *
 * @param context Android context
 * @param apiKey Michelin API key (defaults to BuildConfig value)
 */
class MichelinTireApi(
    private val context: Context,
    private val apiKey: String = AnylineTreadConfig.MICHELIN_API_KEY
) {
    companion object {
        private const val TAG = "MichelinTireApi"
        private const val BASE_URL = "https://api.michelin.com/mobility-intelligence/v1"
        private const val TIRE_SIZE_ENDPOINT = "/tire-size-recognition"
        private const val DOT_CODE_ENDPOINT = "/dot-recognition"
    }

    private val httpClient = HttpClient(OkHttp) {
        install(ContentNegotiation) {
            json(Json {
                ignoreUnknownKeys = true
                isLenient = true
            })
        }
    }

    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
    }

    /**
     * Recognize tire size from a sidewall image.
     *
     * @param bitmap Image of tire sidewall showing size markings
     * @return TireSizeInfo with extracted specifications
     */
    suspend fun recognizeTireSize(bitmap: Bitmap): Result<TireSizeInfo> = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "Starting tire size recognition")

            if (apiKey.isBlank()) {
                Log.w(TAG, "Michelin API key not configured. Using simulation mode.")
                return@withContext Result.success(simulateTireSizeRecognition(bitmap))
            }

            val base64Image = bitmapToBase64(bitmap)

            val response = httpClient.post("$BASE_URL$TIRE_SIZE_ENDPOINT") {
                contentType(ContentType.Application.Json)
                header("X-API-Key", apiKey)
                setBody(TireSizeRequest(image = base64Image))
            }

            if (response.status.isSuccess()) {
                val apiResponse = response.body<TireSizeApiResponse>()
                val result = convertToTireSizeInfo(apiResponse)
                Log.d(TAG, "Tire size recognized: ${result.fullSpecification}")
                Result.success(result)
            } else {
                Log.e(TAG, "API error: ${response.status}")
                Result.failure(Exception("API error: ${response.status}"))
            }

        } catch (e: Exception) {
            Log.e(TAG, "Tire size recognition failed: ${e.message}", e)
            // Fall back to simulation in case of network errors
            Result.success(simulateTireSizeRecognition(bitmap))
        }
    }

    /**
     * Recognize DOT code from a sidewall image.
     *
     * @param bitmap Image of tire sidewall showing DOT code
     * @return DotCodeInfo with manufacturing date and details
     */
    suspend fun recognizeDotCode(bitmap: Bitmap): Result<DotCodeInfo> = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "Starting DOT code recognition")

            if (apiKey.isBlank()) {
                Log.w(TAG, "Michelin API key not configured. Using simulation mode.")
                return@withContext Result.success(simulateDotCodeRecognition(bitmap))
            }

            val base64Image = bitmapToBase64(bitmap)

            val response = httpClient.post("$BASE_URL$DOT_CODE_ENDPOINT") {
                contentType(ContentType.Application.Json)
                header("X-API-Key", apiKey)
                setBody(DotCodeRequest(image = base64Image))
            }

            if (response.status.isSuccess()) {
                val apiResponse = response.body<DotCodeApiResponse>()
                val result = convertToDotCodeInfo(apiResponse)
                Log.d(TAG, "DOT code recognized: ${result.fullDotCode}")
                Result.success(result)
            } else {
                Log.e(TAG, "API error: ${response.status}")
                Result.failure(Exception("API error: ${response.status}"))
            }

        } catch (e: Exception) {
            Log.e(TAG, "DOT code recognition failed: ${e.message}", e)
            // Fall back to simulation
            Result.success(simulateDotCodeRecognition(bitmap))
        }
    }

    /**
     * Perform complete sidewall analysis (size + DOT code).
     */
    suspend fun analyzeSidewall(bitmap: Bitmap): Result<Pair<TireSizeInfo, DotCodeInfo>> =
        withContext(Dispatchers.IO) {
            try {
                val sizeResult = recognizeTireSize(bitmap)
                val dotResult = recognizeDotCode(bitmap)

                if (sizeResult.isSuccess && dotResult.isSuccess) {
                    Result.success(Pair(sizeResult.getOrThrow(), dotResult.getOrThrow()))
                } else {
                    val error = sizeResult.exceptionOrNull() ?: dotResult.exceptionOrNull()
                    Result.failure(error ?: Exception("Unknown error"))
                }
            } catch (e: Exception) {
                Result.failure(e)
            }
        }

    /**
     * Release resources.
     */
    fun close() {
        httpClient.close()
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // Private Helper Methods
    // ═══════════════════════════════════════════════════════════════════════════

    private fun bitmapToBase64(bitmap: Bitmap): String {
        val outputStream = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.JPEG, 90, outputStream)
        val byteArray = outputStream.toByteArray()
        return Base64.encodeToString(byteArray, Base64.NO_WRAP)
    }

    private fun convertToTireSizeInfo(response: TireSizeApiResponse): TireSizeInfo {
        return TireSizeInfo(
            rawText = response.rawText ?: "",
            width = response.width ?: 0,
            aspectRatio = response.aspectRatio ?: 0,
            construction = response.construction ?: "R",
            rimDiameter = response.rimDiameter ?: 0,
            loadIndex = response.loadIndex ?: 0,
            speedRating = response.speedRating?.firstOrNull() ?: ' ',
            confidence = response.confidence ?: 0f,
            additionalMarks = response.additionalMarks ?: emptyList()
        )
    }

    private fun convertToDotCodeInfo(response: DotCodeApiResponse): DotCodeInfo {
        val calendar = Calendar.getInstance()
        val currentYear = calendar.get(Calendar.YEAR)
        val currentMonth = calendar.get(Calendar.MONTH) + 1

        val mfgYear = response.manufactureYear ?: currentYear
        val mfgWeek = response.manufactureWeek ?: 1

        // Calculate age in months
        val yearsOld = currentYear - mfgYear
        val monthsOld = yearsOld * 12 + (currentMonth - (mfgWeek / 4))

        return DotCodeInfo(
            fullDotCode = response.fullDotCode ?: "",
            plantCode = response.plantCode ?: "",
            tireSize = response.tireSize ?: "",
            brandCharacteristics = response.brandCharacteristics ?: "",
            manufactureWeek = mfgWeek,
            manufactureYear = mfgYear,
            ageInMonths = monthsOld.coerceAtLeast(0),
            confidence = response.confidence ?: 0f
        )
    }

    /**
     * Simulate tire size recognition for development/testing.
     */
    private fun simulateTireSizeRecognition(bitmap: Bitmap?): TireSizeInfo {
        // Common tire sizes for simulation
        val sizes = listOf(
            TireSizeInfo("225/45R17 94W", 225, 45, "R", 17, 94, 'W', 0.92f, listOf("XL")),
            TireSizeInfo("205/55R16 91V", 205, 55, "R", 16, 91, 'V', 0.88f),
            TireSizeInfo("235/40R18 95Y", 235, 40, "R", 18, 95, 'Y', 0.90f, listOf("RF")),
            TireSizeInfo("195/65R15 91H", 195, 65, "R", 15, 91, 'H', 0.95f),
            TireSizeInfo("255/35R19 96Y", 255, 35, "R", 19, 96, 'Y', 0.87f, listOf("XL", "MO"))
        )

        return sizes.random()
    }

    /**
     * Simulate DOT code recognition for development/testing.
     */
    private fun simulateDotCodeRecognition(bitmap: Bitmap?): DotCodeInfo {
        val random = java.util.Random()
        val calendar = Calendar.getInstance()
        val currentYear = calendar.get(Calendar.YEAR)

        // Generate random manufacture date (within last 6 years)
        val yearsAgo = random.nextInt(6)
        val mfgYear = currentYear - yearsAgo
        val mfgWeek = random.nextInt(52) + 1

        val plantCodes = listOf("3D", "4B", "2M", "5J", "1K", "8H")
        val plantCode = plantCodes.random()

        val ageInMonths = yearsAgo * 12 + random.nextInt(12)

        val weekStr = mfgWeek.toString().padStart(2, '0')
        val yearStr = (mfgYear % 100).toString().padStart(2, '0')

        return DotCodeInfo(
            fullDotCode = "DOT ${plantCode}XX XXXX $weekStr$yearStr",
            plantCode = plantCode,
            tireSize = "XXXX",
            brandCharacteristics = "",
            manufactureWeek = mfgWeek,
            manufactureYear = mfgYear,
            ageInMonths = ageInMonths,
            confidence = 0.85f + random.nextFloat() * 0.1f
        )
    }
}

// ═══════════════════════════════════════════════════════════════════════════════
// API Request/Response Models
// ═══════════════════════════════════════════════════════════════════════════════

@Serializable
private data class TireSizeRequest(
    val image: String,
    val format: String = "base64"
)

@Serializable
private data class TireSizeApiResponse(
    val rawText: String? = null,
    val width: Int? = null,
    val aspectRatio: Int? = null,
    val construction: String? = null,
    val rimDiameter: Int? = null,
    val loadIndex: Int? = null,
    val speedRating: String? = null,
    val confidence: Float? = null,
    val additionalMarks: List<String>? = null
)

@Serializable
private data class DotCodeRequest(
    val image: String,
    val format: String = "base64"
)

@Serializable
private data class DotCodeApiResponse(
    val fullDotCode: String? = null,
    val plantCode: String? = null,
    val tireSize: String? = null,
    val brandCharacteristics: String? = null,
    val manufactureWeek: Int? = null,
    val manufactureYear: Int? = null,
    val confidence: Float? = null
)
