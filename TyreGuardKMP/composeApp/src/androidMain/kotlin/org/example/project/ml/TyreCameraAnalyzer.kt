package org.example.project.ml

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.ImageFormat
import android.graphics.Matrix
import android.graphics.Rect
import android.graphics.YuvImage
import android.util.Log
import androidx.annotation.OptIn
import androidx.camera.core.ExperimentalGetImage
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import com.google.mlkit.vision.common.InputImage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.ByteArrayOutputStream
import java.nio.ByteBuffer
import java.util.concurrent.atomic.AtomicBoolean

private const val TAG = "TyreCameraAnalyzer"

/**
 * Real-time camera analysis state
 */
data class CameraAnalysisState(
    val isAnalyzing: Boolean = false,
    val detectedTyres: List<DetectedTyre> = emptyList(),
    val hasTyreInFrame: Boolean = false,
    val boundingBoxOverlay: Bitmap? = null,
    val lastFrameTimestamp: Long = 0L,
    val fps: Float = 0f,
    val guidanceMessage: String = "Point camera at tyre"
)

/**
 * TyreCameraAnalyzer - Real-time tyre detection for CameraX preview
 * 
 * Integrates with CameraX ImageAnalysis for live camera feed processing
 * Uses ML Kit for object detection and tracking
 * 
 * Usage:
 * ```
 * val analyzer = TyreCameraAnalyzer(context)
 * cameraProvider.bindToLifecycle(
 *     lifecycleOwner,
 *     cameraSelector,
 *     preview,
 *     imageAnalysis.apply { setAnalyzer(executor, analyzer) }
 * )
 * ```
 */
class TyreCameraAnalyzer(
    private val context: Context,
    private val onTyreDetected: ((DetectedTyre) -> Unit)? = null,
    private val onFrameAnalyzed: ((CameraAnalysisState) -> Unit)? = null
) : ImageAnalysis.Analyzer {
    
    private val tyreDetectionService = TyreDetectionService.getInstance(context)
    private val scope = CoroutineScope(Dispatchers.Default + SupervisorJob())
    
    // State management
    private val _analysisState = MutableStateFlow(CameraAnalysisState())
    val analysisState: StateFlow<CameraAnalysisState> = _analysisState.asStateFlow()
    
    // Processing control
    private val isProcessing = AtomicBoolean(false)
    private var frameCount = 0
    private var lastFpsTime = System.currentTimeMillis()
    private var framesSinceLastFps = 0
    
    // Detection settings
    var detectionEnabled = true
    var skipFrames = 2  // Process every Nth frame for performance
    var minConfidence = 0.3f
    
    // Tracking state
    private var lastDetectedTyre: DetectedTyre? = null
    private var consecutiveDetections = 0
    private val requiredConsecutiveDetections = 3  // Require stable detection
    
    @OptIn(ExperimentalGetImage::class)
    override fun analyze(imageProxy: ImageProxy) {
        frameCount++
        framesSinceLastFps++
        
        // Calculate FPS
        val currentTime = System.currentTimeMillis()
        if (currentTime - lastFpsTime >= 1000) {
            val fps = framesSinceLastFps.toFloat()
            _analysisState.value = _analysisState.value.copy(fps = fps)
            framesSinceLastFps = 0
            lastFpsTime = currentTime
        }
        
        // Skip frames for performance
        if (frameCount % skipFrames != 0) {
            imageProxy.close()
            return
        }
        
        // Skip if detection disabled or already processing
        if (!detectionEnabled || isProcessing.get()) {
            imageProxy.close()
            return
        }
        
        val mediaImage = imageProxy.image
        if (mediaImage == null) {
            imageProxy.close()
            return
        }
        
        isProcessing.set(true)
        
        scope.launch {
            try {
                // Create InputImage from camera frame
                val inputImage = InputImage.fromMediaImage(
                    mediaImage,
                    imageProxy.imageInfo.rotationDegrees
                )
                
                // Run detection
                when (val result = tyreDetectionService.detectTyresInFrame(inputImage)) {
                    is TyreDetectionResult.Success -> {
                        handleSuccessfulDetection(result.tyres)
                    }
                    is TyreDetectionResult.NoTyreFound -> {
                        handleNoDetection()
                    }
                    is TyreDetectionResult.Error -> {
                        Log.e(TAG, "Detection error: ${result.exception.message}")
                        handleNoDetection()
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Analysis error: ${e.message}", e)
            } finally {
                isProcessing.set(false)
                imageProxy.close()
            }
        }
    }
    
    private fun handleSuccessfulDetection(tyres: List<DetectedTyre>) {
        val bestTyre = tyres.maxByOrNull { it.confidence }
        
        if (bestTyre != null && bestTyre.confidence >= minConfidence) {
            consecutiveDetections++
            lastDetectedTyre = bestTyre
            
            // Update state
            val guidance = generateGuidanceMessage(bestTyre, tyres.size)
            _analysisState.value = CameraAnalysisState(
                isAnalyzing = true,
                detectedTyres = tyres,
                hasTyreInFrame = true,
                lastFrameTimestamp = System.currentTimeMillis(),
                fps = _analysisState.value.fps,
                guidanceMessage = guidance
            )
            
            // Callback for stable detection
            if (consecutiveDetections >= requiredConsecutiveDetections) {
                onTyreDetected?.invoke(bestTyre)
            }
            
            onFrameAnalyzed?.invoke(_analysisState.value)
        } else {
            handleNoDetection()
        }
    }
    
    private fun handleNoDetection() {
        consecutiveDetections = 0
        
        _analysisState.value = CameraAnalysisState(
            isAnalyzing = true,
            detectedTyres = emptyList(),
            hasTyreInFrame = false,
            lastFrameTimestamp = System.currentTimeMillis(),
            fps = _analysisState.value.fps,
            guidanceMessage = "Point camera at tyre"
        )
        
        onFrameAnalyzed?.invoke(_analysisState.value)
    }
    
    private fun generateGuidanceMessage(tyre: DetectedTyre, totalCount: Int): String {
        val box = tyre.boundingBox
        val centerX = box.centerX()
        val centerY = box.centerY()
        val size = (box.width() + box.height()) / 2
        
        return when {
            tyre.confidence < 0.4f -> "Move closer to tyre"
            size < 200 -> "Move closer for better detail"
            size > 800 -> "Move back slightly"
            centerX < 0.3f -> "Move camera right"
            centerX > 0.7f -> "Move camera left"
            centerY < 0.3f -> "Move camera down"
            centerY > 0.7f -> "Move camera up"
            consecutiveDetections < requiredConsecutiveDetections -> "Hold steady..."
            else -> "✓ Tyre detected - Ready to capture"
        }
    }
    
    /**
     * Capture current frame as bitmap for analysis
     */
    @OptIn(ExperimentalGetImage::class)
    fun captureFrame(imageProxy: ImageProxy): Bitmap? {
        return try {
            val image = imageProxy.image ?: return null
            
            // Convert YUV to RGB bitmap
            val yBuffer = image.planes[0].buffer
            val uBuffer = image.planes[1].buffer
            val vBuffer = image.planes[2].buffer
            
            val ySize = yBuffer.remaining()
            val uSize = uBuffer.remaining()
            val vSize = vBuffer.remaining()
            
            val nv21 = ByteArray(ySize + uSize + vSize)
            yBuffer.get(nv21, 0, ySize)
            vBuffer.get(nv21, ySize, vSize)
            uBuffer.get(nv21, ySize + vSize, uSize)
            
            val yuvImage = YuvImage(
                nv21,
                ImageFormat.NV21,
                image.width,
                image.height,
                null
            )
            
            val out = ByteArrayOutputStream()
            yuvImage.compressToJpeg(
                Rect(0, 0, image.width, image.height),
                90,
                out
            )
            
            val imageBytes = out.toByteArray()
            var bitmap = BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.size)
            
            // Apply rotation if needed
            val rotation = imageProxy.imageInfo.rotationDegrees
            if (rotation != 0) {
                val matrix = Matrix()
                matrix.postRotate(rotation.toFloat())
                bitmap = Bitmap.createBitmap(
                    bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true
                )
            }
            
            bitmap
        } catch (e: Exception) {
            Log.e(TAG, "Failed to capture frame: ${e.message}", e)
            null
        }
    }
    
    /**
     * Get the best detected tyre if stable detection achieved
     */
    fun getStableDetection(): DetectedTyre? {
        return if (consecutiveDetections >= requiredConsecutiveDetections) {
            lastDetectedTyre
        } else {
            null
        }
    }
    
    /**
     * Reset detection state
     */
    fun reset() {
        consecutiveDetections = 0
        lastDetectedTyre = null
        _analysisState.value = CameraAnalysisState()
    }
    
    /**
     * Pause/resume detection
     */
    fun pauseDetection() {
        detectionEnabled = false
        reset()
    }
    
    fun resumeDetection() {
        detectionEnabled = true
    }
}

/**
 * Extension function to create analyzer with default settings
 */
fun createTyreCameraAnalyzer(
    context: Context,
    onTyreDetected: ((DetectedTyre) -> Unit)? = null
): TyreCameraAnalyzer {
    return TyreCameraAnalyzer(context, onTyreDetected)
}
