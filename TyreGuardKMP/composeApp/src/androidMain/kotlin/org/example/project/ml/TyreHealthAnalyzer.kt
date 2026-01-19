package org.example.project.ml

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.tensorflow.lite.Interpreter
import org.tensorflow.lite.gpu.CompatibilityList
import org.tensorflow.lite.gpu.GpuDelegate
import org.tensorflow.lite.support.common.FileUtil
import org.tensorflow.lite.support.common.ops.NormalizeOp
import org.tensorflow.lite.support.image.ImageProcessor
import org.tensorflow.lite.support.image.TensorImage
import org.tensorflow.lite.support.image.ops.ResizeOp
import org.tensorflow.lite.support.tensorbuffer.TensorBuffer
import java.io.File
import java.io.FileInputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.MappedByteBuffer
import java.nio.channels.FileChannel

private const val TAG = "TyreHealthAnalyzer"

/**
 * Defect classification result
 */
data class DefectClassification(
    val type: DefectType,
    val confidence: Float,
    val location: DefectLocation? = null
)

enum class DefectType(val displayName: String, val severity: Int) {
    CRACK("Crack", 4),
    BULGE("Bulge", 5),
    CUT("Cut/Slash", 4),
    PUNCTURE("Puncture", 3),
    WEAR("Tread Wear", 2),
    UNEVEN_WEAR("Uneven Wear", 3),
    LOW_TREAD("Low Tread Depth", 3),
    SIDEWALL_DAMAGE("Sidewall Damage", 5),
    FOREIGN_OBJECT("Foreign Object", 2),
    AGING("Age Cracking", 3),
    GOOD("No Defect", 0)
}

data class DefectLocation(
    val x: Float,  // Normalized 0-1
    val y: Float,  // Normalized 0-1
    val width: Float,
    val height: Float
)

/**
 * Complete tyre health analysis result
 */
data class TyreHealthResult(
    val overallScore: Int,  // 0-100
    val healthStatus: HealthStatus,
    val defects: List<DefectClassification>,
    val treadDepthEstimate: Float,  // mm
    val wearPattern: WearPattern,
    val recommendations: List<String>,
    val analysisTimestamp: Long = System.currentTimeMillis()
)

enum class HealthStatus(val displayName: String, val color: Long) {
    EXCELLENT("Excellent", 0xFF10B981),
    GOOD("Good", 0xFF22C55E),
    FAIR("Fair", 0xFFF59E0B),
    POOR("Poor", 0xFFF97316),
    CRITICAL("Critical", 0xFFEF4444)
}

enum class WearPattern(val displayName: String) {
    EVEN("Even Wear"),
    CENTER("Center Wear - Over-inflation"),
    EDGE("Edge Wear - Under-inflation"),
    ONE_SIDE("One-Side Wear - Alignment Issue"),
    CUPPING("Cupping - Suspension Issue"),
    FEATHERING("Feathering - Toe Alignment"),
    FLAT_SPOT("Flat Spot - Brake Lock"),
    UNKNOWN("Unknown Pattern")
}

/**
 * TyreHealthAnalyzer - TensorFlow Lite based defect detection and health assessment
 * 
 * Features:
 * - Defect classification using trained TFLite model
 * - Tread depth estimation
 * - Wear pattern analysis
 * - Health score calculation
 * - GPU acceleration support
 */
class TyreHealthAnalyzer(private val context: Context) {
    
    private var interpreter: Interpreter? = null
    private var gpuDelegate: GpuDelegate? = null
    private var isModelLoaded = false
    
    // Model configuration
    private val modelFileName = "tyre_defect_model.tflite"
    private val inputSize = 224  // Model input size
    private val numClasses = 11  // Number of defect classes
    
    // Image processor for preprocessing
    private val imageProcessor = ImageProcessor.Builder()
        .add(ResizeOp(inputSize, inputSize, ResizeOp.ResizeMethod.BILINEAR))
        .add(NormalizeOp(127.5f, 127.5f))  // Normalize to [-1, 1]
        .build()
    
    // Defect class labels (must match model training order)
    private val defectLabels = listOf(
        DefectType.GOOD,
        DefectType.CRACK,
        DefectType.BULGE,
        DefectType.CUT,
        DefectType.PUNCTURE,
        DefectType.WEAR,
        DefectType.UNEVEN_WEAR,
        DefectType.LOW_TREAD,
        DefectType.SIDEWALL_DAMAGE,
        DefectType.FOREIGN_OBJECT,
        DefectType.AGING
    )
    
    // ═══════════════════════════════════════════════════════════════════════════
    // MODEL LOADING
    // ═══════════════════════════════════════════════════════════════════════════
    
    /**
     * Initialize the TFLite interpreter with GPU acceleration if available
     */
    suspend fun initialize(): Boolean = withContext(Dispatchers.IO) {
        try {
            val options = Interpreter.Options()
            
            // Try to use GPU delegate if available
            try {
                val compatList = CompatibilityList()
                if (compatList.isDelegateSupportedOnThisDevice) {
                    gpuDelegate = GpuDelegate()
                    options.addDelegate(gpuDelegate)
                    Log.d(TAG, "GPU acceleration enabled")
                } else {
                    options.setNumThreads(4)
                    Log.d(TAG, "GPU not supported, using CPU with 4 threads")
                }
            } catch (e: Exception) {
                Log.w(TAG, "GPU delegate failed, using CPU: ${e.message}")
                options.setNumThreads(4)
            }
            
            // Load model from assets
            val modelBuffer = loadModelFile()
            if (modelBuffer != null) {
                interpreter = Interpreter(modelBuffer, options)
                isModelLoaded = true
                Log.d(TAG, "Model loaded successfully")
                true
            } else {
                // Model file not found - use fallback analysis
                Log.w(TAG, "Model file not found, using heuristic analysis")
                isModelLoaded = false
                true // Still return true to allow fallback
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to initialize model: ${e.message}", e)
            isModelLoaded = false
            true // Allow fallback analysis
        }
    }
    
    private fun loadModelFile(): MappedByteBuffer? {
        return try {
            val assetManager = context.assets
            val fileDescriptor = assetManager.openFd(modelFileName)
            val inputStream = FileInputStream(fileDescriptor.fileDescriptor)
            val fileChannel = inputStream.channel
            val startOffset = fileDescriptor.startOffset
            val declaredLength = fileDescriptor.declaredLength
            fileChannel.map(FileChannel.MapMode.READ_ONLY, startOffset, declaredLength)
        } catch (e: Exception) {
            Log.w(TAG, "Model file not in assets: ${e.message}")
            null
        }
    }
    
    // ═══════════════════════════════════════════════════════════════════════════
    // TYRE ANALYSIS
    // ═══════════════════════════════════════════════════════════════════════════
    
    /**
     * Analyze tyre health from bitmap
     */
    suspend fun analyzeTyre(bitmap: Bitmap): TyreHealthResult = withContext(Dispatchers.Default) {
        if (!isModelLoaded || interpreter == null) {
            // Use heuristic analysis when model is not available
            return@withContext heuristicAnalysis(bitmap)
        }
        
        try {
            // Preprocess image
            val tensorImage = TensorImage.fromBitmap(bitmap)
            val processedImage = imageProcessor.process(tensorImage)
            
            // Prepare output buffer
            val outputBuffer = TensorBuffer.createFixedSize(
                intArrayOf(1, numClasses),
                org.tensorflow.lite.DataType.FLOAT32
            )
            
            // Run inference
            interpreter?.run(processedImage.buffer, outputBuffer.buffer.rewind())
            
            // Parse results
            val probabilities = outputBuffer.floatArray
            val defects = parseDefectProbabilities(probabilities)
            
            // Calculate overall health
            val healthResult = calculateHealthScore(defects, bitmap)
            
            healthResult
        } catch (e: Exception) {
            Log.e(TAG, "Inference failed: ${e.message}", e)
            heuristicAnalysis(bitmap)
        }
    }
    
    /**
     * Analyze tyre from file path
     */
    suspend fun analyzeTyreFromFile(imagePath: String): TyreHealthResult = withContext(Dispatchers.IO) {
        val bitmap = BitmapFactory.decodeFile(imagePath)
            ?: return@withContext createErrorResult("Failed to load image")
        
        analyzeTyre(bitmap)
    }
    
    // ═══════════════════════════════════════════════════════════════════════════
    // RESULT PARSING
    // ═══════════════════════════════════════════════════════════════════════════
    
    private fun parseDefectProbabilities(probabilities: FloatArray): List<DefectClassification> {
        val threshold = 0.15f  // Minimum confidence threshold
        
        return probabilities.mapIndexed { index, probability ->
            if (index < defectLabels.size) {
                DefectClassification(
                    type = defectLabels[index],
                    confidence = probability
                )
            } else null
        }.filterNotNull()
            .filter { it.confidence >= threshold && it.type != DefectType.GOOD }
            .sortedByDescending { it.confidence }
    }
    
    private fun calculateHealthScore(
        defects: List<DefectClassification>,
        bitmap: Bitmap
    ): TyreHealthResult {
        // Base score starts at 100
        var score = 100
        
        // Deduct points based on defects
        defects.forEach { defect ->
            val severityPenalty = defect.type.severity * 10 * defect.confidence
            score -= severityPenalty.toInt()
        }
        
        score = score.coerceIn(0, 100)
        
        // Determine health status
        val healthStatus = when {
            score >= 90 -> HealthStatus.EXCELLENT
            score >= 75 -> HealthStatus.GOOD
            score >= 50 -> HealthStatus.FAIR
            score >= 25 -> HealthStatus.POOR
            else -> HealthStatus.CRITICAL
        }
        
        // Estimate tread depth (simplified - would need depth model)
        val treadDepth = estimateTreadDepth(defects, score)
        
        // Determine wear pattern
        val wearPattern = analyzeWearPattern(defects)
        
        // Generate recommendations
        val recommendations = generateRecommendations(defects, healthStatus, treadDepth)
        
        return TyreHealthResult(
            overallScore = score,
            healthStatus = healthStatus,
            defects = defects,
            treadDepthEstimate = treadDepth,
            wearPattern = wearPattern,
            recommendations = recommendations
        )
    }
    
    // ═══════════════════════════════════════════════════════════════════════════
    // HEURISTIC ANALYSIS (Fallback when model not available)
    // ═══════════════════════════════════════════════════════════════════════════
    
    private fun heuristicAnalysis(bitmap: Bitmap): TyreHealthResult {
        // Analyze image properties for basic assessment
        val avgBrightness = calculateAverageBrightness(bitmap)
        val edgeIntensity = estimateEdgeIntensity(bitmap)
        val colorVariance = calculateColorVariance(bitmap)
        
        val defects = mutableListOf<DefectClassification>()
        var score = 85  // Default good score
        
        // Low brightness might indicate dirt/wear
        if (avgBrightness < 60) {
            defects.add(DefectClassification(DefectType.WEAR, 0.4f))
            score -= 10
        }
        
        // High edge intensity might indicate cracks
        if (edgeIntensity > 150) {
            defects.add(DefectClassification(DefectType.CRACK, 0.3f))
            score -= 15
        }
        
        // High color variance might indicate damage
        if (colorVariance > 50) {
            defects.add(DefectClassification(DefectType.UNEVEN_WEAR, 0.35f))
            score -= 10
        }
        
        score = score.coerceIn(40, 100)
        
        val healthStatus = when {
            score >= 80 -> HealthStatus.GOOD
            score >= 60 -> HealthStatus.FAIR
            else -> HealthStatus.POOR
        }
        
        return TyreHealthResult(
            overallScore = score,
            healthStatus = healthStatus,
            defects = defects,
            treadDepthEstimate = 5.0f,  // Default estimate
            wearPattern = WearPattern.UNKNOWN,
            recommendations = listOf(
                "Visual inspection recommended",
                "Consider professional assessment for accurate diagnosis",
                "Monitor tyre condition regularly"
            )
        )
    }
    
    private fun calculateAverageBrightness(bitmap: Bitmap): Float {
        val scaled = Bitmap.createScaledBitmap(bitmap, 50, 50, true)
        var totalBrightness = 0f
        val pixels = IntArray(scaled.width * scaled.height)
        scaled.getPixels(pixels, 0, scaled.width, 0, 0, scaled.width, scaled.height)
        
        pixels.forEach { pixel ->
            val r = (pixel shr 16) and 0xFF
            val g = (pixel shr 8) and 0xFF
            val b = pixel and 0xFF
            totalBrightness += (r + g + b) / 3f
        }
        
        return totalBrightness / pixels.size
    }
    
    private fun estimateEdgeIntensity(bitmap: Bitmap): Float {
        // Simplified edge detection using pixel differences
        val scaled = Bitmap.createScaledBitmap(bitmap, 50, 50, true)
        var totalDiff = 0f
        
        for (y in 1 until scaled.height) {
            for (x in 1 until scaled.width) {
                val current = scaled.getPixel(x, y) and 0xFF
                val left = scaled.getPixel(x - 1, y) and 0xFF
                val top = scaled.getPixel(x, y - 1) and 0xFF
                totalDiff += kotlin.math.abs(current - left) + kotlin.math.abs(current - top)
            }
        }
        
        return totalDiff / (scaled.width * scaled.height)
    }
    
    private fun calculateColorVariance(bitmap: Bitmap): Float {
        val scaled = Bitmap.createScaledBitmap(bitmap, 50, 50, true)
        val pixels = IntArray(scaled.width * scaled.height)
        scaled.getPixels(pixels, 0, scaled.width, 0, 0, scaled.width, scaled.height)
        
        val values = pixels.map { (it and 0xFF).toFloat() }
        val mean = values.average().toFloat()
        val variance = values.map { (it - mean) * (it - mean) }.average().toFloat()
        
        return kotlin.math.sqrt(variance)
    }
    
    // ═══════════════════════════════════════════════════════════════════════════
    // ANALYSIS HELPERS
    // ═══════════════════════════════════════════════════════════════════════════
    
    private fun estimateTreadDepth(defects: List<DefectClassification>, score: Int): Float {
        // New tyre: ~8mm, Legal minimum: 1.6mm
        val baseDepth = when {
            score >= 90 -> 7.0f
            score >= 75 -> 5.5f
            score >= 50 -> 3.5f
            score >= 25 -> 2.0f
            else -> 1.5f
        }
        
        // Adjust based on wear defects
        val wearPenalty = defects
            .filter { it.type == DefectType.WEAR || it.type == DefectType.LOW_TREAD }
            .sumOf { (it.confidence * 2).toDouble() }
            .toFloat()
        
        return (baseDepth - wearPenalty).coerceIn(1.0f, 8.0f)
    }
    
    private fun analyzeWearPattern(defects: List<DefectClassification>): WearPattern {
        val hasUnevenWear = defects.any { it.type == DefectType.UNEVEN_WEAR }
        
        return if (hasUnevenWear) {
            WearPattern.EDGE  // Default to edge wear, would need location data for accurate
        } else {
            WearPattern.EVEN
        }
    }
    
    private fun generateRecommendations(
        defects: List<DefectClassification>,
        status: HealthStatus,
        treadDepth: Float
    ): List<String> {
        val recommendations = mutableListOf<String>()
        
        // Status-based recommendations
        when (status) {
            HealthStatus.CRITICAL -> {
                recommendations.add("⚠️ URGENT: Replace tyre immediately for safety")
                recommendations.add("Do not drive at high speeds")
            }
            HealthStatus.POOR -> {
                recommendations.add("Schedule tyre replacement soon")
                recommendations.add("Limit highway driving")
            }
            HealthStatus.FAIR -> {
                recommendations.add("Monitor tyre condition closely")
                recommendations.add("Consider replacement in next 3-6 months")
            }
            HealthStatus.GOOD, HealthStatus.EXCELLENT -> {
                recommendations.add("Continue regular maintenance")
                recommendations.add("Next inspection in 5,000 km")
            }
        }
        
        // Defect-specific recommendations
        defects.forEach { defect ->
            when (defect.type) {
                DefectType.CRACK -> recommendations.add("Have cracks inspected by professional")
                DefectType.BULGE -> recommendations.add("Bulge detected - replace immediately!")
                DefectType.PUNCTURE -> recommendations.add("Check for puncture repair possibility")
                DefectType.UNEVEN_WEAR -> recommendations.add("Check wheel alignment")
                DefectType.LOW_TREAD -> recommendations.add("Tread depth is low - plan replacement")
                else -> {}
            }
        }
        
        // Tread depth recommendation
        if (treadDepth < 3.0f) {
            recommendations.add("Tread depth approaching minimum limit (1.6mm)")
        }
        
        return recommendations.distinct().take(5)
    }
    
    private fun createErrorResult(message: String): TyreHealthResult {
        return TyreHealthResult(
            overallScore = 0,
            healthStatus = HealthStatus.CRITICAL,
            defects = emptyList(),
            treadDepthEstimate = 0f,
            wearPattern = WearPattern.UNKNOWN,
            recommendations = listOf("Error: $message", "Please try again")
        )
    }
    
    // ═══════════════════════════════════════════════════════════════════════════
    // CLEANUP
    // ═══════════════════════════════════════════════════════════════════════════
    
    fun close() {
        interpreter?.close()
        gpuDelegate?.close()
        interpreter = null
        gpuDelegate = null
        isModelLoaded = false
    }
    
    companion object {
        @Volatile
        private var INSTANCE: TyreHealthAnalyzer? = null
        
        fun getInstance(context: Context): TyreHealthAnalyzer {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: TyreHealthAnalyzer(context.applicationContext).also {
                    INSTANCE = it
                }
            }
        }
    }
}
