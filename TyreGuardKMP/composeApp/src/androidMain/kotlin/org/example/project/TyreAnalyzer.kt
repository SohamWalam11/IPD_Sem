package org.example.project

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Rect
import android.graphics.RectF
import androidx.annotation.OptIn
import androidx.camera.core.ExperimentalGetImage
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.objects.ObjectDetection
import com.google.mlkit.vision.objects.ObjectDetector
import com.google.mlkit.vision.objects.defaults.ObjectDetectorOptions

/**
 * Distance state based on tyre bounding box size relative to frame.
 */
enum class DistanceState {
    Unknown,    // No object detected
    TooFar,     // Bounding box < 20% of screen
    TooClose,   // Bounding box > 80% of screen
    Perfect     // Bounding box 20-80% of screen
}

/**
 * Real-time tyre analyzer using ML Kit Object Detection + Custom TFLite Model.
 * 
 * Analyzes each camera frame to:
 * 1. Detect objects and calculate distance guidance (ML Kit)
 * 2. Classify tyre defects when at perfect distance (Custom TFLite)
 * 
 * @param context Android context for loading TFLite model.
 * @param onDistanceStateChanged Callback when distance state changes.
 * @param onBoundingBoxDetected Callback with bounding box for overlay drawing.
 * @param onDefectDetected Callback with tyre defect detection results.
 */
class TyreAnalyzer(
    context: Context,
    private val onDistanceStateChanged: (DistanceState) -> Unit,
    private val onBoundingBoxDetected: (Rect?) -> Unit = {},
    private val onDefectDetected: (List<DetectionResult>) -> Unit = {}
) : ImageAnalysis.Analyzer {

    // Configure ML Kit for single prominent object detection (for distance)
    private val mlKitOptions = ObjectDetectorOptions.Builder()
        .setDetectorMode(ObjectDetectorOptions.STREAM_MODE)
        .enableClassification()
        .build()

    private val objectDetector: ObjectDetector = ObjectDetection.getClient(mlKitOptions)
    
    // Custom TFLite classifier for tyre defect detection
    private val tyreClassifier: TyreDefectClassifier? = try {
        TyreDefectClassifier(context).takeIf { it.isReady() }
    } catch (e: Exception) {
        android.util.Log.e("TyreAnalyzer", "Failed to initialize TyreDefectClassifier: ${e.message}")
        null
    } catch (e: UnsatisfiedLinkError) {
        android.util.Log.e("TyreAnalyzer", "TFLite native library not available: ${e.message}")
        null
    }

    private var lastDistanceState: DistanceState = DistanceState.Unknown
    private var frameCounter = 0
    private val DEFECT_DETECTION_INTERVAL = 10 // Run defect detection every N frames when at perfect distance
    private var isShutdown = false

    @OptIn(ExperimentalGetImage::class)
    override fun analyze(imageProxy: ImageProxy) {
        // Early exit if shutdown
        if (isShutdown) {
            imageProxy.close()
            return
        }
        
        val mediaImage = imageProxy.image
        if (mediaImage == null) {
            imageProxy.close()
            return
        }

        try {
            val image = InputImage.fromMediaImage(
                mediaImage,
                imageProxy.imageInfo.rotationDegrees
            )

            val frameWidth = imageProxy.width
            val frameHeight = imageProxy.height
            val frameArea = frameWidth * frameHeight

        frameCounter++

        objectDetector.process(image)
            .addOnSuccessListener { detectedObjects ->
                if (detectedObjects.isNotEmpty()) {
                    // Get the most prominent object (first one)
                    val primaryObject = detectedObjects.first()
                    val boundingBox = primaryObject.boundingBox

                    // Calculate bounding box area ratio
                    val boxArea = boundingBox.width() * boundingBox.height()
                    val areaRatio = boxArea.toFloat() / frameArea.toFloat()

                    // Determine distance state based on area ratio
                    val newState = when {
                        areaRatio < 0.20f -> DistanceState.TooFar
                        areaRatio > 0.80f -> DistanceState.TooClose
                        else -> DistanceState.Perfect
                    }

                    // Only callback if state changed
                    if (newState != lastDistanceState) {
                        lastDistanceState = newState
                        onDistanceStateChanged(newState)
                    }

                    onBoundingBoxDetected(boundingBox)

                    // Run defect detection when at perfect distance (periodically)
                    if (newState == DistanceState.Perfect && 
                        frameCounter % DEFECT_DETECTION_INTERVAL == 0 &&
                        tyreClassifier != null) {
                        
                        // Convert ImageProxy to Bitmap for TFLite inference
                        val bitmap = imageProxyToBitmap(imageProxy)
                        if (bitmap != null) {
                            val defects = tyreClassifier.detect(bitmap)
                            onDefectDetected(defects)
                            bitmap.recycle()
                        }
                    }
                } else {
                    // No objects detected
                    if (lastDistanceState != DistanceState.Unknown) {
                        lastDistanceState = DistanceState.Unknown
                        onDistanceStateChanged(DistanceState.Unknown)
                    }
                    onBoundingBoxDetected(null)
                    onDefectDetected(emptyList())
                }
            }
            .addOnFailureListener { e ->
                // Detection failed, maintain previous state
                android.util.Log.w("TyreAnalyzer", "Object detection failed: ${e.message}")
                onBoundingBoxDetected(null)
            }
            .addOnCompleteListener {
                imageProxy.close()
            }
        } catch (e: Exception) {
            android.util.Log.e("TyreAnalyzer", "Analyze failed: ${e.message}")
            imageProxy.close()
        }
    }

    /**
     * Convert ImageProxy to Bitmap for TFLite processing.
     */
    @OptIn(ExperimentalGetImage::class)
    private fun imageProxyToBitmap(imageProxy: ImageProxy): Bitmap? {
        return try {
            val image = imageProxy.image ?: return null
            val planes = image.planes
            if (planes.size < 3) return null
            
            val yBuffer = planes[0].buffer
            val uBuffer = planes[1].buffer
            val vBuffer = planes[2].buffer

            val ySize = yBuffer.remaining()
            val uSize = uBuffer.remaining()
            val vSize = vBuffer.remaining()

            val nv21 = ByteArray(ySize + uSize + vSize)
            yBuffer.get(nv21, 0, ySize)
            vBuffer.get(nv21, ySize, vSize)
            uBuffer.get(nv21, ySize + vSize, uSize)

            val yuvImage = android.graphics.YuvImage(
                nv21,
                android.graphics.ImageFormat.NV21,
                imageProxy.width,
                imageProxy.height,
                null
            )
            val out = java.io.ByteArrayOutputStream()
            yuvImage.compressToJpeg(
                android.graphics.Rect(0, 0, imageProxy.width, imageProxy.height),
                90,
                out
            )
            val jpegBytes = out.toByteArray()
            android.graphics.BitmapFactory.decodeByteArray(jpegBytes, 0, jpegBytes.size)
        } catch (e: Exception) {
            android.util.Log.e("TyreAnalyzer", "Failed to convert ImageProxy to Bitmap: ${e.message}")
            null
        }
    }

    /**
     * Clean up resources.
     */
    fun shutdown() {
        isShutdown = true
        try {
            objectDetector.close()
        } catch (ignored: Exception) {}
        try {
            tyreClassifier?.close()
        } catch (ignored: Exception) {}
    }
}

/**
 * Calculate distance state from a captured image.
 * Used for final image processing after shutter click.
 * 
 * @param inputImage The captured image to analyze.
 * @param onResult Callback with detection result and bounding box.
 */
fun analyzeStaticImage(
    inputImage: InputImage,
    onResult: (detected: Boolean, boundingBox: Rect?, distanceState: DistanceState) -> Unit
) {
    val options = ObjectDetectorOptions.Builder()
        .setDetectorMode(ObjectDetectorOptions.SINGLE_IMAGE_MODE)
        .enableClassification()
        .build()

    val detector = ObjectDetection.getClient(options)

    detector.process(inputImage)
        .addOnSuccessListener { detectedObjects ->
            if (detectedObjects.isNotEmpty()) {
                val primaryObject = detectedObjects.first()
                val boundingBox = primaryObject.boundingBox
                
                // Calculate area ratio
                val imageArea = inputImage.width * inputImage.height
                val boxArea = boundingBox.width() * boundingBox.height()
                val areaRatio = boxArea.toFloat() / imageArea.toFloat()

                val state = when {
                    areaRatio < 0.20f -> DistanceState.TooFar
                    areaRatio > 0.80f -> DistanceState.TooClose
                    else -> DistanceState.Perfect
                }

                onResult(true, boundingBox, state)
            } else {
                onResult(false, null, DistanceState.Unknown)
            }
        }
        .addOnFailureListener {
            onResult(false, null, DistanceState.Unknown)
        }
        .addOnCompleteListener {
            detector.close()
        }
}
