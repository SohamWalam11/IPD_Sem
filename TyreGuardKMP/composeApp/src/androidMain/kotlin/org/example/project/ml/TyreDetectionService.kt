package org.example.project.ml

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.RectF
import android.media.Image
import android.util.Log
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.objects.DetectedObject
import com.google.mlkit.vision.objects.ObjectDetection
import com.google.mlkit.vision.objects.ObjectDetector
import com.google.mlkit.vision.objects.defaults.ObjectDetectorOptions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

private const val TAG = "TyreDetectionService"

/**
 * Detected tyre result with bounding box and confidence
 */
data class DetectedTyre(
    val boundingBox: RectF,
    val trackingId: Int?,
    val confidence: Float,
    val labels: List<String>,
    val croppedBitmap: Bitmap? = null,
    val timestamp: Long = System.currentTimeMillis()
)

/**
 * Detection result wrapper
 */
sealed class TyreDetectionResult {
    data class Success(val tyres: List<DetectedTyre>) : TyreDetectionResult()
    data class NoTyreFound(val message: String = "No tyre detected in frame") : TyreDetectionResult()
    data class Error(val exception: Exception) : TyreDetectionResult()
}

/**
 * TyreDetectionService - ML Kit Object Detection for real-time tyre detection
 * 
 * Features:
 * - Real-time tyre detection from camera feed
 * - Object tracking for smooth bounding boxes
 * - Automatic tyre cropping for analysis
 * - Multiple tyre detection support
 * - Confidence scoring
 */
class TyreDetectionService(private val context: Context) {
    
    // ═══════════════════════════════════════════════════════════════════════════
    // ML Kit Object Detector - Stream mode for real-time detection
    // ═══════════════════════════════════════════════════════════════════════════
    private val streamDetectorOptions = ObjectDetectorOptions.Builder()
        .setDetectorMode(ObjectDetectorOptions.STREAM_MODE)
        .enableMultipleObjects()
        .enableClassification()
        .build()
    
    private val streamDetector: ObjectDetector = ObjectDetection.getClient(streamDetectorOptions)
    
    // ═══════════════════════════════════════════════════════════════════════════
    // Single image detector for high-quality static analysis
    // ═══════════════════════════════════════════════════════════════════════════
    private val singleImageOptions = ObjectDetectorOptions.Builder()
        .setDetectorMode(ObjectDetectorOptions.SINGLE_IMAGE_MODE)
        .enableMultipleObjects()
        .enableClassification()
        .build()
    
    private val singleImageDetector: ObjectDetector = ObjectDetection.getClient(singleImageOptions)
    
    // Paint for drawing bounding boxes
    private val boundingBoxPaint = Paint().apply {
        color = Color.parseColor("#7C3AED") // Purple
        style = Paint.Style.STROKE
        strokeWidth = 8f
    }
    
    private val labelPaint = Paint().apply {
        color = Color.WHITE
        textSize = 40f
        isAntiAlias = true
    }
    
    private val labelBackgroundPaint = Paint().apply {
        color = Color.parseColor("#7C3AED")
        style = Paint.Style.FILL
    }
    
    // ═══════════════════════════════════════════════════════════════════════════
    // REAL-TIME DETECTION (Camera Preview)
    // ═══════════════════════════════════════════════════════════════════════════
    
    /**
     * Detect tyres in a camera frame (real-time streaming mode)
     * Uses ML Kit's STREAM_MODE for fast, tracked detection
     */
    suspend fun detectTyresInFrame(
        image: InputImage
    ): TyreDetectionResult = suspendCancellableCoroutine { continuation ->
        streamDetector.process(image)
            .addOnSuccessListener { detectedObjects ->
                val tyres = filterTyreObjects(detectedObjects, null)
                if (tyres.isEmpty()) {
                    continuation.resume(TyreDetectionResult.NoTyreFound())
                } else {
                    continuation.resume(TyreDetectionResult.Success(tyres))
                }
            }
            .addOnFailureListener { exception ->
                Log.e(TAG, "Detection failed: ${exception.message}", exception)
                continuation.resume(TyreDetectionResult.Error(exception as Exception))
            }
    }
    
    /**
     * Detect tyres from Bitmap (for captured photos)
     */
    suspend fun detectTyresInBitmap(
        bitmap: Bitmap,
        rotationDegrees: Int = 0
    ): TyreDetectionResult = suspendCancellableCoroutine { continuation ->
        val inputImage = InputImage.fromBitmap(bitmap, rotationDegrees)
        
        singleImageDetector.process(inputImage)
            .addOnSuccessListener { detectedObjects ->
                val tyres = filterTyreObjects(detectedObjects, bitmap)
                if (tyres.isEmpty()) {
                    continuation.resume(TyreDetectionResult.NoTyreFound())
                } else {
                    continuation.resume(TyreDetectionResult.Success(tyres))
                }
            }
            .addOnFailureListener { exception ->
                Log.e(TAG, "Detection failed: ${exception.message}", exception)
                continuation.resume(TyreDetectionResult.Error(exception as Exception))
            }
    }
    
    /**
     * Detect tyres from file path
     */
    suspend fun detectTyresInFile(
        imagePath: String,
        rotationDegrees: Int = 0
    ): TyreDetectionResult = withContext(Dispatchers.IO) {
        try {
            val bitmap = BitmapFactory.decodeFile(imagePath)
                ?: return@withContext TyreDetectionResult.Error(Exception("Failed to decode image"))
            
            detectTyresInBitmap(bitmap, rotationDegrees)
        } catch (e: Exception) {
            TyreDetectionResult.Error(e)
        }
    }
    
    // ═══════════════════════════════════════════════════════════════════════════
    // TYRE FILTERING & CLASSIFICATION
    // ═══════════════════════════════════════════════════════════════════════════
    
    /**
     * Filter detected objects to identify tyres
     * Uses shape analysis and label matching
     */
    private fun filterTyreObjects(
        detectedObjects: List<DetectedObject>,
        sourceBitmap: Bitmap?
    ): List<DetectedTyre> {
        return detectedObjects.mapNotNull { obj ->
            val boundingBox = obj.boundingBox
            
            // Check if object shape is roughly circular/square (tyre-like)
            val aspectRatio = boundingBox.width().toFloat() / boundingBox.height().toFloat()
            val isTyreLikeShape = aspectRatio in 0.5f..2.0f
            
            // Check labels for vehicle/wheel related classifications
            val tyreRelatedLabels = listOf(
                "wheel", "tire", "tyre", "vehicle", "car", "automotive",
                "rubber", "circle", "round", "disk"
            )
            
            val matchingLabels = obj.labels.filter { label ->
                tyreRelatedLabels.any { 
                    label.text.lowercase().contains(it) 
                }
            }
            
            val hasRelevantLabel = matchingLabels.isNotEmpty() || obj.labels.isEmpty()
            
            // Calculate confidence based on shape and label match
            val shapeConfidence = if (isTyreLikeShape) 0.3f else 0f
            val labelConfidence = matchingLabels.maxOfOrNull { it.confidence } ?: 0.2f
            val overallConfidence = shapeConfidence + labelConfidence * 0.7f
            
            // Only include if it looks like a tyre (confidence > 0.3)
            if (overallConfidence > 0.25f || isTyreLikeShape) {
                val croppedBitmap = sourceBitmap?.let { 
                    cropTyreFromBitmap(it, boundingBox) 
                }
                
                DetectedTyre(
                    boundingBox = RectF(boundingBox),
                    trackingId = obj.trackingId,
                    confidence = overallConfidence.coerceIn(0f, 1f),
                    labels = obj.labels.map { it.text },
                    croppedBitmap = croppedBitmap
                )
            } else {
                null
            }
        }.sortedByDescending { it.confidence }
    }
    
    // ═══════════════════════════════════════════════════════════════════════════
    // TYRE CROPPING
    // ═══════════════════════════════════════════════════════════════════════════
    
    /**
     * Crop tyre region from source bitmap
     */
    private fun cropTyreFromBitmap(source: Bitmap, boundingBox: Rect): Bitmap {
        // Add padding around the detected area
        val padding = (boundingBox.width() * 0.1f).toInt()
        
        val left = (boundingBox.left - padding).coerceAtLeast(0)
        val top = (boundingBox.top - padding).coerceAtLeast(0)
        val right = (boundingBox.right + padding).coerceAtMost(source.width)
        val bottom = (boundingBox.bottom + padding).coerceAtMost(source.height)
        
        val width = right - left
        val height = bottom - top
        
        return Bitmap.createBitmap(source, left, top, width, height)
    }
    
    /**
     * Crop and save tyre to file for analysis
     */
    suspend fun cropAndSaveTyre(
        sourceBitmap: Bitmap,
        boundingBox: Rect,
        outputDir: File,
        filename: String = "tyre_${System.currentTimeMillis()}.jpg"
    ): File = withContext(Dispatchers.IO) {
        val cropped = cropTyreFromBitmap(sourceBitmap, boundingBox)
        
        if (!outputDir.exists()) {
            outputDir.mkdirs()
        }
        
        val outputFile = File(outputDir, filename)
        FileOutputStream(outputFile).use { out ->
            cropped.compress(Bitmap.CompressFormat.JPEG, 95, out)
        }
        
        outputFile
    }
    
    // ═══════════════════════════════════════════════════════════════════════════
    // VISUALIZATION
    // ═══════════════════════════════════════════════════════════════════════════
    
    /**
     * Draw bounding boxes on bitmap for preview
     */
    fun drawDetectionOverlay(
        source: Bitmap,
        detectedTyres: List<DetectedTyre>
    ): Bitmap {
        val result = source.copy(Bitmap.Config.ARGB_8888, true)
        val canvas = Canvas(result)
        
        detectedTyres.forEach { tyre ->
            // Draw bounding box
            canvas.drawRect(tyre.boundingBox, boundingBoxPaint)
            
            // Draw label background
            val labelText = "Tyre ${(tyre.confidence * 100).toInt()}%"
            val textWidth = labelPaint.measureText(labelText)
            val textHeight = 50f
            
            val labelRect = RectF(
                tyre.boundingBox.left,
                tyre.boundingBox.top - textHeight - 10,
                tyre.boundingBox.left + textWidth + 20,
                tyre.boundingBox.top
            )
            canvas.drawRoundRect(labelRect, 8f, 8f, labelBackgroundPaint)
            
            // Draw label text
            canvas.drawText(
                labelText,
                tyre.boundingBox.left + 10,
                tyre.boundingBox.top - 20,
                labelPaint
            )
            
            // Draw tracking ID if available
            tyre.trackingId?.let { id ->
                canvas.drawText(
                    "ID: $id",
                    tyre.boundingBox.left + 10,
                    tyre.boundingBox.bottom + 40,
                    labelPaint.apply { textSize = 30f }
                )
            }
        }
        
        return result
    }
    
    /**
     * Get the best (highest confidence) tyre detection
     */
    fun getBestTyre(tyres: List<DetectedTyre>): DetectedTyre? {
        return tyres.maxByOrNull { it.confidence }
    }
    
    // ═══════════════════════════════════════════════════════════════════════════
    // CLEANUP
    // ═══════════════════════════════════════════════════════════════════════════
    
    fun close() {
        streamDetector.close()
        singleImageDetector.close()
    }
    
    companion object {
        @Volatile
        private var INSTANCE: TyreDetectionService? = null
        
        fun getInstance(context: Context): TyreDetectionService {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: TyreDetectionService(context.applicationContext).also {
                    INSTANCE = it
                }
            }
        }
    }
}
