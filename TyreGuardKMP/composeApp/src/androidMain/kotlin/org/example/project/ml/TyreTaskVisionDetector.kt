package org.example.project.ml

import android.content.Context
import android.graphics.Bitmap
import android.graphics.RectF
import android.util.Log
import org.tensorflow.lite.support.image.TensorImage
import org.tensorflow.lite.task.vision.detector.Detection
import org.tensorflow.lite.task.vision.detector.ObjectDetector

private const val TAG = "TyreTaskVisionDetector"

// ──────────────────────────────────────────────────────────────────────────────
// Domain model returned to the UI layer
// ──────────────────────────────────────────────────────────────────────────────

/**
 * A single tyre defect found by the model.
 *
 * @param label       Defect name, e.g. "crack", "bulge", "bald_tread"
 * @param confidence  0–1 score from the model
 * @param boundingBox RectF in the **original image** coordinate space (pixels)
 */
data class TyreDefect(
    val label: String,
    val confidence: Float,
    val boundingBox: RectF
)

// ──────────────────────────────────────────────────────────────────────────────
// Health / RUL scoring
// ──────────────────────────────────────────────────────────────────────────────

data class TyreHealthReport(
    /** 0–100: 100 = perfect, 0 = scrap */
    val healthScore: Int,
    /** Estimated remaining useful life in arbitrary units (km buckets) */
    val estimatedRulKm: String,
    /** Short verdict string */
    val verdict: String,
    /** Detected defects (sorted by confidence desc) */
    val defects: List<TyreDefect>
)

// Per-defect penalty configurations ─ tune to your model's label set
private data class DefectRule(
    val baseScorePenalty: Int,    // subtracted from 100 per detection
    val highConfidenceThreshold: Float, // if confidence ≥ this, penalty ×1.5
)

private val DEFECT_RULES: Map<String, DefectRule> = mapOf(
    "bulge"      to DefectRule(baseScorePenalty = 40, highConfidenceThreshold = 0.70f),
    "crack"      to DefectRule(baseScorePenalty = 25, highConfidenceThreshold = 0.65f),
    "bald_tread" to DefectRule(baseScorePenalty = 20, highConfidenceThreshold = 0.60f),
    "cut"        to DefectRule(baseScorePenalty = 30, highConfidenceThreshold = 0.70f),
    "puncture"   to DefectRule(baseScorePenalty = 35, highConfidenceThreshold = 0.70f)
)

/** Calculates a [TyreHealthReport] from a list of detected defects. */
fun calculateHealthReport(defects: List<TyreDefect>): TyreHealthReport {
    if (defects.isEmpty()) {
        return TyreHealthReport(
            healthScore = 95,
            estimatedRulKm = "> 30,000 km",
            verdict = "No visible defects detected",
            defects = emptyList()
        )
    }

    var totalPenalty = 0
    for (defect in defects) {
        val rule = DEFECT_RULES[defect.label.lowercase()]
            ?: DefectRule(baseScorePenalty = 15, highConfidenceThreshold = 0.65f)
        val multiplier = if (defect.confidence >= rule.highConfidenceThreshold) 1.5f else 1.0f
        totalPenalty += (rule.baseScorePenalty * multiplier).toInt()
    }

    val score = (100 - totalPenalty).coerceIn(0, 100)

    val (rulEstimate, verdict) = when {
        score >= 80 -> Pair("> 20,000 km", "Good condition — routine monitoring advised")
        score >= 60 -> Pair("10,000–20,000 km", "Moderate wear — schedule inspection soon")
        score >= 40 -> Pair("5,000–10,000 km", "Significant damage — replace within weeks")
        score >= 20 -> Pair("< 5,000 km", "Severe damage — replace immediately")
        else        -> Pair("Replace NOW", "Critical — tyre is unsafe to drive on")
    }

    return TyreHealthReport(
        healthScore = score,
        estimatedRulKm = rulEstimate,
        verdict = verdict,
        defects = defects.sortedByDescending { it.confidence }
    )
}

// ──────────────────────────────────────────────────────────────────────────────
// The detector — wraps TFLite Task Vision ObjectDetector
// ──────────────────────────────────────────────────────────────────────────────

/**
 * Production-grade tyre defect detector using the TFLite Task Vision API.
 *
 * **Requirements on the .tflite model:**
 * The model file at `assets/models/best_int8.tflite` must contain embedded
 * TFLite Metadata (bounding-box detection format).  If training was done with
 * TensorFlow's Model Maker or if you annotated the model using the TFLite
 * Metadata Writer, the metadata will be present.
 *
 * If your model is a raw YOLO export WITHOUT metadata the `ObjectDetector`
 * constructor will throw `IllegalArgumentException`: in that case add metadata
 * with the [TFLite Metadata Writer](https://www.tensorflow.org/lite/models/modify/model_maker/object_detection)
 * before shipping.
 */
class TyreTaskVisionDetector(context: Context) {

    companion object {
        private const val MODEL_PATH = "models/best_int8.tflite"
        private const val CONFIDENCE_THRESHOLD = 0.45f
        private const val MAX_RESULTS = 10
    }

    private val objectDetector: ObjectDetector? = try {
        val options = ObjectDetector.ObjectDetectorOptions.builder()
            .setMaxResults(MAX_RESULTS)
            .setScoreThreshold(CONFIDENCE_THRESHOLD)
            .build()
        ObjectDetector.createFromFileAndOptions(context, MODEL_PATH, options)
    } catch (e: Exception) {
        Log.e(TAG, "Failed to load ObjectDetector: ${e.message}", e)
        null
    }

    val isReady: Boolean get() = objectDetector != null

    /**
     * Run inference on a **rotation-corrected** bitmap.
     *
     * Never pass a sideways image — rotate before calling this.
     *
     * @return List of [TyreDefect] in **bitmap pixel coordinates**.
     */
    fun detect(bitmap: Bitmap): List<TyreDefect> {
        val detector = objectDetector ?: run {
            Log.w(TAG, "Detector not ready — returning empty list")
            return emptyList()
        }

        return try {
            // TensorImage handles the correct pixel-format conversion internally
            val tensorImage = TensorImage.fromBitmap(bitmap)
            val detections: List<Detection> = detector.detect(tensorImage)

            detections.mapNotNull { detection ->
                val category = detection.categories.maxByOrNull { it.score } ?: return@mapNotNull null
                TyreDefect(
                    label = category.label ?: "unknown",
                    confidence = category.score,
                    boundingBox = RectF(detection.boundingBox)
                )
            }
        } catch (e: Exception) {
            Log.e(TAG, "Inference failed: ${e.message}", e)
            emptyList()
        }
    }

    /** Run the full analysis pipeline: detect → score → report. */
    fun analyzeAndScore(bitmap: Bitmap): TyreHealthReport {
        val defects = detect(bitmap)
        return calculateHealthReport(defects)
    }

    fun close() {
        try { objectDetector?.close() } catch (_: Exception) {}
    }
}
