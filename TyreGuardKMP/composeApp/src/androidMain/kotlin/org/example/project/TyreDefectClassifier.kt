package org.example.project

import android.content.Context
import android.graphics.Bitmap
import android.graphics.RectF
import android.util.Log
import org.tensorflow.lite.Interpreter
import org.tensorflow.lite.gpu.GpuDelegate
import org.tensorflow.lite.support.common.FileUtil
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.math.max
import kotlin.math.min

/**
 * Result from tyre defect detection.
 */
data class DetectionResult(
    val label: String,
    val confidence: Float,
    val boundingBox: RectF
)

/**
 * TFLite-based tyre defect detector using the custom trained YOLO model.
 * 
 * Handles:
 * - Model loading from assets (models/best_int8.tflite)
 * - Image preprocessing for YOLO input format
 * - Post-processing detection outputs (NMS)
 * - GPU acceleration when available
 */
class TyreDefectClassifier(
    private val context: Context,
    private val useGpu: Boolean = false // Disable GPU by default for stability
) {
    companion object {
        private const val TAG = "TyreDefectClassifier"
        private const val MODEL_PATH = "models/best_int8.tflite"
        
        // YOLO model parameters - adjust based on your model
        private const val INPUT_SIZE = 640  // Common YOLO input size
        private const val NUM_CHANNELS = 3
        private const val CONFIDENCE_THRESHOLD = 0.5f
        private const val IOU_THRESHOLD = 0.45f
        
        // Common tyre defect labels - adjust based on your model's training
        val LABELS = listOf(
            "Good",
            "Cracked", 
            "Worn",
            "Bulge",
            "Cut",
            "Flat",
            "Puncture",
            "Sidewall Damage"
        )
    }

    private var interpreter: Interpreter? = null
    private var gpuDelegate: GpuDelegate? = null
    private var inputBuffer: ByteBuffer? = null
    private var isModelLoaded = false
    private var isQuantized = false // Track if model uses int8 quantization
    
    // Model input/output shapes (will be determined at runtime)
    private var inputWidth = INPUT_SIZE
    private var inputHeight = INPUT_SIZE
    private var outputShape: IntArray? = null

    init {
        // Load model on background thread to avoid ANR
        try {
            loadModel()
            isModelLoaded = true
            Log.d(TAG, "Model loaded successfully, isQuantized=$isQuantized")
        } catch (e: Exception) {
            Log.e(TAG, "Model initialization failed: ${e.message}", e)
            isModelLoaded = false
        }
    }

    /**
     * Check if the model was loaded successfully.
     */
    fun isReady(): Boolean = isModelLoaded && interpreter != null

    /**
     * Load the TFLite model with optional GPU acceleration.
     */
    private fun loadModel() {
        try {
            // Check if model file exists in assets
            val assetManager = context.assets
            val modelFiles = assetManager.list("models") ?: emptyArray()
            if (!modelFiles.contains("best_int8.tflite")) {
                Log.w(TAG, "Model file not found in assets/models/")
                return
            }
            
            val modelBuffer = FileUtil.loadMappedFile(context, MODEL_PATH)
            
            val options = Interpreter.Options().apply {
                setNumThreads(2) // Reduced threads for stability
                
                // Only try GPU if explicitly enabled and on compatible device
                if (useGpu && android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.P) {
                    try {
                        gpuDelegate = GpuDelegate()
                        addDelegate(gpuDelegate)
                        Log.d(TAG, "GPU delegate enabled")
                    } catch (e: Exception) {
                        Log.w(TAG, "GPU delegate failed, falling back to CPU: ${e.message}")
                        gpuDelegate?.close()
                        gpuDelegate = null
                    } catch (e: UnsatisfiedLinkError) {
                        Log.w(TAG, "GPU native library not available: ${e.message}")
                        gpuDelegate = null
                    }
                }
            }
            
            interpreter = Interpreter(modelBuffer, options)
            
            // Get input tensor shape and type
            val inputTensor = interpreter!!.getInputTensor(0)
            val inputShape = inputTensor.shape()
            val inputType = inputTensor.dataType()
            Log.d(TAG, "Model input shape: ${inputShape.contentToString()}, type: $inputType")
            
            // Check if model is quantized (int8)
            isQuantized = inputType == org.tensorflow.lite.DataType.UINT8 || 
                          inputType == org.tensorflow.lite.DataType.INT8
            
            // Shape is typically [1, height, width, channels] or [1, channels, height, width]
            if (inputShape.size >= 4) {
                // NHWC format (most common for TFLite)
                inputHeight = inputShape[1]
                inputWidth = inputShape[2]
            }
            
            // Get output tensor shape
            val outputTensor = interpreter!!.getOutputTensor(0)
            outputShape = outputTensor.shape()
            Log.d(TAG, "Model output shape: ${outputShape?.contentToString()}")
            
            // Pre-allocate input buffer based on data type
            val bytesPerChannel = if (isQuantized) 1 else 4 // 1 byte for int8, 4 bytes for float32
            val inputSize = 1 * inputHeight * inputWidth * NUM_CHANNELS * bytesPerChannel
            inputBuffer = ByteBuffer.allocateDirect(inputSize).apply {
                order(ByteOrder.nativeOrder())
            }
            
            Log.d(TAG, "Model loaded successfully: ${inputWidth}x${inputHeight}, quantized=$isQuantized")
            
        } catch (e: Exception) {
            Log.e(TAG, "Failed to load model: ${e.message}", e)
            // Don't throw - just mark as not loaded and return gracefully
            safeClose()
        } catch (e: UnsatisfiedLinkError) {
            Log.e(TAG, "TFLite native library failed: ${e.message}", e)
            safeClose()
        }
    }
    
    /**
     * Safely close resources without throwing.
     */
    private fun safeClose() {
        try {
            interpreter?.close()
        } catch (ignored: Exception) {}
        try {
            gpuDelegate?.close()
        } catch (ignored: Exception) {}
        interpreter = null
        gpuDelegate = null
    }

    /**
     * Run defect detection on a bitmap.
     * 
     * @param bitmap Input image (will be resized internally)
     * @return List of detected defects with bounding boxes
     */
    fun detect(bitmap: Bitmap): List<DetectionResult> {
        if (!isModelLoaded) {
            Log.w(TAG, "Model not loaded, returning empty results")
            return emptyList()
        }
        
        val interp = interpreter ?: run {
            Log.e(TAG, "Interpreter not initialized")
            return emptyList()
        }
        
        try {
            // Preprocess: resize and normalize
            val resizedBitmap = Bitmap.createScaledBitmap(bitmap, inputWidth, inputHeight, true)
            preprocessImage(resizedBitmap)
            
            // Prepare output buffer based on model output shape
            val output = prepareOutputBuffer()
            
            // Run inference
            inputBuffer?.rewind()
            interp.run(inputBuffer, output)
            
            // Post-process and apply NMS
            val detections = postProcessOutput(output, bitmap.width, bitmap.height)
            
            return detections
            
        } catch (e: Exception) {
            Log.e(TAG, "Detection failed: ${e.message}", e)
            return emptyList()
        }
    }

    /**
     * Convert bitmap to normalized ByteBuffer for model input.
     * Handles both quantized (int8) and float32 models.
     */
    private fun preprocessImage(bitmap: Bitmap) {
        inputBuffer?.rewind()
        
        val pixels = IntArray(inputWidth * inputHeight)
        bitmap.getPixels(pixels, 0, inputWidth, 0, 0, inputWidth, inputHeight)
        
        for (pixel in pixels) {
            // Extract RGB values
            val r = (pixel shr 16) and 0xFF
            val g = (pixel shr 8) and 0xFF
            val b = pixel and 0xFF
            
            if (isQuantized) {
                // For int8 quantized model: use raw byte values (0-255)
                inputBuffer?.put(r.toByte())
                inputBuffer?.put(g.toByte())
                inputBuffer?.put(b.toByte())
            } else {
                // For float32 model: normalize to 0.0-1.0
                inputBuffer?.putFloat(r / 255.0f)
                inputBuffer?.putFloat(g / 255.0f)
                inputBuffer?.putFloat(b / 255.0f)
            }
        }
    }

    /**
     * Prepare output buffer based on model's output shape.
     */
    private fun prepareOutputBuffer(): Array<Array<FloatArray>> {
        val shape = outputShape ?: intArrayOf(1, 25200, 13) // Default YOLO-like shape
        
        // Create output array based on shape
        // Typical YOLO output: [1, num_detections, 5 + num_classes]
        // Where 5 = x, y, w, h, confidence
        val numDetections = if (shape.size >= 2) shape[1] else 25200
        val numOutputs = if (shape.size >= 3) shape[2] else 13
        
        return Array(1) { Array(numDetections) { FloatArray(numOutputs) } }
    }

    /**
     * Post-process YOLO output to get detection results.
     */
    private fun postProcessOutput(
        output: Array<Array<FloatArray>>,
        originalWidth: Int,
        originalHeight: Int
    ): List<DetectionResult> {
        val results = mutableListOf<DetectionResult>()
        val detections = output[0]
        
        for (detection in detections) {
            // YOLO format: [x_center, y_center, width, height, confidence, class_scores...]
            val confidence = detection[4]
            
            if (confidence < CONFIDENCE_THRESHOLD) continue
            
            // Find best class
            var maxClassScore = 0f
            var maxClassIdx = 0
            for (i in 5 until detection.size) {
                if (detection[i] > maxClassScore) {
                    maxClassScore = detection[i]
                    maxClassIdx = i - 5
                }
            }
            
            val finalConfidence = confidence * maxClassScore
            if (finalConfidence < CONFIDENCE_THRESHOLD) continue
            
            // Convert normalized coordinates to pixel coordinates
            val xCenter = detection[0] * originalWidth
            val yCenter = detection[1] * originalHeight
            val width = detection[2] * originalWidth
            val height = detection[3] * originalHeight
            
            val left = max(0f, xCenter - width / 2)
            val top = max(0f, yCenter - height / 2)
            val right = min(originalWidth.toFloat(), xCenter + width / 2)
            val bottom = min(originalHeight.toFloat(), yCenter + height / 2)
            
            val label = if (maxClassIdx < LABELS.size) LABELS[maxClassIdx] else "Defect $maxClassIdx"
            
            results.add(
                DetectionResult(
                    label = label,
                    confidence = finalConfidence,
                    boundingBox = RectF(left, top, right, bottom)
                )
            )
        }
        
        // Apply Non-Maximum Suppression
        return applyNMS(results)
    }

    /**
     * Apply Non-Maximum Suppression to remove overlapping detections.
     */
    private fun applyNMS(detections: List<DetectionResult>): List<DetectionResult> {
        if (detections.isEmpty()) return emptyList()
        
        val sorted = detections.sortedByDescending { it.confidence }.toMutableList()
        val selected = mutableListOf<DetectionResult>()
        
        while (sorted.isNotEmpty()) {
            val best = sorted.removeAt(0)
            selected.add(best)
            
            sorted.removeAll { detection ->
                calculateIoU(best.boundingBox, detection.boundingBox) > IOU_THRESHOLD
            }
        }
        
        return selected
    }

    /**
     * Calculate Intersection over Union for two bounding boxes.
     */
    private fun calculateIoU(box1: RectF, box2: RectF): Float {
        val intersectionLeft = max(box1.left, box2.left)
        val intersectionTop = max(box1.top, box2.top)
        val intersectionRight = min(box1.right, box2.right)
        val intersectionBottom = min(box1.bottom, box2.bottom)
        
        if (intersectionRight <= intersectionLeft || intersectionBottom <= intersectionTop) {
            return 0f
        }
        
        val intersectionArea = (intersectionRight - intersectionLeft) * (intersectionBottom - intersectionTop)
        val box1Area = (box1.right - box1.left) * (box1.bottom - box1.top)
        val box2Area = (box2.right - box2.left) * (box2.bottom - box2.top)
        val unionArea = box1Area + box2Area - intersectionArea
        
        return if (unionArea > 0) intersectionArea / unionArea else 0f
    }

    /**
     * Get the best detection result (highest confidence).
     */
    fun detectBest(bitmap: Bitmap): DetectionResult? {
        return detect(bitmap).maxByOrNull { it.confidence }
    }

    /**
     * Release resources.
     */
    fun close() {
        interpreter?.close()
        gpuDelegate?.close()
        interpreter = null
        gpuDelegate = null
        inputBuffer = null
        Log.d(TAG, "Classifier resources released")
    }
}
