package org.example.project.sdk

import android.content.Context
import android.graphics.Bitmap
import android.graphics.RectF
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.withContext
import org.example.project.DetectionResult
import org.example.project.TyreDefectClassifier
import java.util.UUID

/**
 * Comprehensive Tire Analysis Service
 *
 * Integrates multiple data sources for complete tire health assessment:
 * 1. Anyline Tire Tread SDK - Professional tread depth measurement
 * 2. Michelin Mobility Intelligence API - Tire size and DOT code recognition
 * 3. Custom TFLite Model (best_int8.tflite) - Defect detection and classification
 *
 * This service orchestrates all analysis components and provides a unified
 * result with health scoring, recommendations, and action items.
 */
class ComprehensiveTireAnalysisService(private val context: Context) {

    companion object {
        private const val TAG = "TireAnalysisService"

        // Weight factors for overall health score calculation
        private const val TREAD_WEIGHT = 0.35f
        private const val DEFECT_WEIGHT = 0.40f
        private const val AGE_WEIGHT = 0.25f
    }

    // SDK instances
    private val anylineScanner = AnylineTreadScanner(context)
    private val michelinApi = MichelinTireApi(context)
    private var tyreDefectClassifier: TyreDefectClassifier? = null

    private var isInitialized = false

    /**
     * Initialize all analysis components.
     * Call this before performing any analysis.
     */
    suspend fun initialize(): Result<Boolean> = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "Initializing Comprehensive Tire Analysis Service")

            // Initialize Anyline SDK
            val anylineResult = anylineScanner.initialize()
            if (anylineResult.isFailure) {
                Log.w(TAG, "Anyline SDK initialization warning: ${anylineResult.exceptionOrNull()?.message}")
            }

            // Initialize custom TFLite classifier
            try {
                tyreDefectClassifier = TyreDefectClassifier(context)
                if (tyreDefectClassifier?.isReady() != true) {
                    Log.w(TAG, "TFLite classifier not ready - defect detection will be limited")
                    tyreDefectClassifier = null
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to initialize TFLite classifier: ${e.message}")
                tyreDefectClassifier = null
            }

            isInitialized = true
            Log.d(TAG, "Service initialized successfully")
            Result.success(true)

        } catch (e: Exception) {
            Log.e(TAG, "Service initialization failed: ${e.message}", e)
            Result.failure(e)
        }
    }

    /**
     * Check if the service is ready.
     */
    fun isReady(): Boolean = isInitialized

    /**
     * Perform comprehensive tire analysis from captured images.
     *
     * @param treadImage Image of tire tread for depth measurement and defect detection
     * @param sidewallImage Optional image of sidewall for size and DOT code recognition
     * @return ComprehensiveTireAnalysis with all findings and recommendations
     */
    suspend fun analyzeTire(
        treadImage: Bitmap,
        sidewallImage: Bitmap? = null
    ): Result<ComprehensiveTireAnalysis> = withContext(Dispatchers.IO) {
        if (!isInitialized) {
            return@withContext Result.failure(IllegalStateException("Service not initialized"))
        }

        try {
            Log.d(TAG, "Starting comprehensive tire analysis")
            val analysisId = UUID.randomUUID().toString()

            // Run analyses in parallel for efficiency
            val treadDepthDeferred = async { analyzeThread(treadImage) }
            val defectsDeferred = async { detectDefects(treadImage) }
            val sidewallDeferred = sidewallImage?.let { async { analyzeSidewall(it) } }

            // Collect results
            val treadResult = treadDepthDeferred.await()
            val defects = defectsDeferred.await()
            val sidewallResult = sidewallDeferred?.await()

            val tireSizeInfo = sidewallResult?.first
            val dotCodeInfo = sidewallResult?.second

            // Calculate overall health score
            val healthScore = calculateHealthScore(treadResult, defects, dotCodeInfo)

            // Determine overall status
            val overallStatus = determineOverallStatus(healthScore, treadResult, defects)

            // Generate primary concerns
            val concerns = generateConcerns(treadResult, defects, dotCodeInfo)

            // Generate recommendations
            val recommendations = generateRecommendations(treadResult, defects, dotCodeInfo)

            // Determine required action
            val actionRequired = determineActionRequired(overallStatus, treadResult, defects)

            // Estimate cost
            val costRange = estimateCostRange(recommendations)

            val analysis = ComprehensiveTireAnalysis(
                id = analysisId,
                treadDepth = treadResult,
                tireSizeInfo = tireSizeInfo,
                dotCodeInfo = dotCodeInfo,
                detectedDefects = defects,
                treadImagePath = null, // Could save images to storage
                sidewallImagePath = null,
                overallHealthScore = healthScore,
                overallStatus = overallStatus,
                primaryConcerns = concerns,
                recommendations = recommendations,
                actionRequired = actionRequired,
                estimatedCostRange = costRange
            )

            Log.d(TAG, "Analysis complete: score=$healthScore, status=${overallStatus.displayName}")
            Result.success(analysis)

        } catch (e: Exception) {
            Log.e(TAG, "Analysis failed: ${e.message}", e)
            Result.failure(e)
        }
    }

    /**
     * Quick defect scan using only the TFLite model.
     * Faster than full analysis, suitable for real-time detection.
     */
    suspend fun quickDefectScan(image: Bitmap): List<TireDefect> = withContext(Dispatchers.IO) {
        detectDefects(image)
    }

    /**
     * Measure tread depth only.
     */
    suspend fun measureTreadDepth(image: Bitmap): Result<TreadDepthResult> = withContext(Dispatchers.IO) {
        if (!anylineScanner.isReady()) {
            anylineScanner.initialize()
        }
        anylineScanner.scanFromImage(image)
    }

    /**
     * Recognize tire size from sidewall image.
     */
    suspend fun recognizeTireSize(sidewallImage: Bitmap): Result<TireSizeInfo> {
        return michelinApi.recognizeTireSize(sidewallImage)
    }

    /**
     * Recognize DOT code from sidewall image.
     */
    suspend fun recognizeDotCode(sidewallImage: Bitmap): Result<DotCodeInfo> {
        return michelinApi.recognizeDotCode(sidewallImage)
    }

    /**
     * Generate heat map from tread scan result.
     */
    fun generateTreadHeatMap(result: TreadDepthResult): TreadHeatMap {
        return anylineScanner.generateHeatMap(result)
    }

    /**
     * Release all resources.
     */
    fun release() {
        anylineScanner.release()
        michelinApi.close()
        tyreDefectClassifier?.close()
        tyreDefectClassifier = null
        isInitialized = false
        Log.d(TAG, "Service resources released")
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // Private Analysis Methods
    // ═══════════════════════════════════════════════════════════════════════════

    private suspend fun analyzeThread(image: Bitmap): TreadDepthResult? {
        return try {
            val result = anylineScanner.scanFromImage(image)
            result.getOrNull()
        } catch (e: Exception) {
            Log.e(TAG, "Tread analysis failed: ${e.message}")
            null
        }
    }

    private suspend fun detectDefects(image: Bitmap): List<TireDefect> {
        val classifier = tyreDefectClassifier ?: return emptyList()

        return try {
            val detections = classifier.detect(image)
            detections.map { convertToTireDefect(it) }
        } catch (e: Exception) {
            Log.e(TAG, "Defect detection failed: ${e.message}")
            emptyList()
        }
    }

    private suspend fun analyzeSidewall(image: Bitmap): Pair<TireSizeInfo?, DotCodeInfo?> {
        val sizeResult = michelinApi.recognizeTireSize(image)
        val dotResult = michelinApi.recognizeDotCode(image)

        return Pair(sizeResult.getOrNull(), dotResult.getOrNull())
    }

    private fun convertToTireDefect(detection: DetectionResult): TireDefect {
        val defectType = mapLabelToDefectType(detection.label)
        val severity = calculateSeverity(defectType, detection.confidence)

        return TireDefect(
            type = defectType,
            confidence = detection.confidence,
            boundingBox = detection.boundingBox,
            severity = severity,
            description = generateDefectDescription(defectType, severity)
        )
    }

    private fun mapLabelToDefectType(label: String): DefectType {
        return when (label.lowercase()) {
            "crack", "cracked" -> DefectType.CRACK
            "bulge", "bubble" -> DefectType.BULGE
            "cut", "slash" -> DefectType.CUT
            "puncture" -> DefectType.PUNCTURE
            "worn", "wear", "worn_tread" -> DefectType.WORN_TREAD
            "sidewall", "sidewall_damage" -> DefectType.SIDEWALL_DAMAGE
            "foreign", "object", "foreign_object" -> DefectType.FOREIGN_OBJECT
            "dry_rot", "age", "aging" -> DefectType.DRY_ROT
            "bead", "bead_damage" -> DefectType.BEAD_DAMAGE
            "good", "ok", "normal" -> DefectType.GOOD
            else -> DefectType.GOOD
        }
    }

    private fun calculateSeverity(type: DefectType, confidence: Float): DefectSeverity {
        if (type == DefectType.GOOD) return DefectSeverity.NONE

        // Base severity from defect type
        val baseSeverity = when (type) {
            DefectType.BULGE, DefectType.SIDEWALL_DAMAGE, DefectType.BEAD_DAMAGE -> DefectSeverity.CRITICAL
            DefectType.CUT, DefectType.PUNCTURE -> DefectSeverity.HIGH
            DefectType.CRACK, DefectType.DRY_ROT -> DefectSeverity.MEDIUM
            DefectType.WORN_TREAD, DefectType.FOREIGN_OBJECT -> DefectSeverity.LOW
            else -> DefectSeverity.NONE
        }

        // Adjust based on confidence
        return if (confidence < 0.5f) {
            // Lower confidence = lower severity
            when (baseSeverity) {
                DefectSeverity.CRITICAL -> DefectSeverity.HIGH
                DefectSeverity.HIGH -> DefectSeverity.MEDIUM
                DefectSeverity.MEDIUM -> DefectSeverity.LOW
                else -> baseSeverity
            }
        } else {
            baseSeverity
        }
    }

    private fun generateDefectDescription(type: DefectType, severity: DefectSeverity): String {
        return when (type) {
            DefectType.CRACK -> "Surface cracks detected - may indicate age or heat damage"
            DefectType.BULGE -> "Bulge/bubble detected - internal structure damage, replace immediately"
            DefectType.CUT -> "Cut or slash damage - check for air leaks, may require replacement"
            DefectType.PUNCTURE -> "Puncture detected - repair or replace depending on location"
            DefectType.WORN_TREAD -> "Excessive tread wear - reduced traction, plan for replacement"
            DefectType.SIDEWALL_DAMAGE -> "Sidewall damage - structural integrity compromised"
            DefectType.FOREIGN_OBJECT -> "Foreign object embedded - remove and inspect for damage"
            DefectType.DRY_ROT -> "Dry rot/age cracking - tire deterioration from age or UV"
            DefectType.BEAD_DAMAGE -> "Bead damage - seal integrity may be compromised"
            DefectType.GOOD -> "No defects detected"
        }
    }

    private fun calculateHealthScore(
        treadResult: TreadDepthResult?,
        defects: List<TireDefect>,
        dotCodeInfo: DotCodeInfo?
    ): Int {
        var score = 100f

        // Tread depth component (35%)
        treadResult?.let {
            val treadScore = when (it.status) {
                TreadStatus.EXCELLENT -> 100f
                TreadStatus.GOOD -> 85f
                TreadStatus.FAIR -> 65f
                TreadStatus.LOW -> 35f
                TreadStatus.CRITICAL -> 10f
                TreadStatus.UNKNOWN -> 50f
            }
            score -= (100f - treadScore) * TREAD_WEIGHT
        }

        // Defect component (40%)
        if (defects.isNotEmpty()) {
            val maxSeverity = defects.maxOfOrNull { it.severity.priority } ?: 0
            val defectPenalty = when (maxSeverity) {
                4 -> 100f  // Critical
                3 -> 70f   // High
                2 -> 40f   // Medium
                1 -> 20f   // Low
                else -> 0f
            }
            score -= defectPenalty * DEFECT_WEIGHT
        }

        // Age component (25%)
        dotCodeInfo?.let {
            val ageScore = when (it.ageStatus) {
                TireAgeStatus.NEW -> 100f
                TireAgeStatus.GOOD -> 85f
                TireAgeStatus.AGING -> 60f
                TireAgeStatus.OLD -> 30f
                TireAgeStatus.EXPIRED -> 0f
            }
            score -= (100f - ageScore) * AGE_WEIGHT
        }

        return score.toInt().coerceIn(0, 100)
    }

    private fun determineOverallStatus(
        score: Int,
        treadResult: TreadDepthResult?,
        defects: List<TireDefect>
    ): OverallTireStatus {
        // Check for critical conditions first
        if (defects.any { it.severity == DefectSeverity.CRITICAL }) {
            return OverallTireStatus.CRITICAL
        }

        if (treadResult?.status == TreadStatus.CRITICAL) {
            return OverallTireStatus.CRITICAL
        }

        // Otherwise, base on score
        return when {
            score >= 85 -> OverallTireStatus.EXCELLENT
            score >= 70 -> OverallTireStatus.GOOD
            score >= 50 -> OverallTireStatus.FAIR
            score >= 25 -> OverallTireStatus.POOR
            else -> OverallTireStatus.CRITICAL
        }
    }

    private fun generateConcerns(
        treadResult: TreadDepthResult?,
        defects: List<TireDefect>,
        dotCodeInfo: DotCodeInfo?
    ): List<String> {
        val concerns = mutableListOf<String>()

        // Tread concerns
        treadResult?.let {
            when (it.status) {
                TreadStatus.CRITICAL -> concerns.add("⚠️ CRITICAL: Tread depth below legal minimum (${it.minimumDepthMm}mm)")
                TreadStatus.LOW -> concerns.add("⚠️ Low tread depth - replacement needed soon")
                TreadStatus.FAIR -> concerns.add("Tread wear approaching replacement threshold")
                else -> {}
            }

            when (it.wearPattern) {
                TreadWearPattern.CENTER_WEAR -> concerns.add("Center wear pattern indicates over-inflation")
                TreadWearPattern.EDGE_WEAR -> concerns.add("Edge wear pattern indicates under-inflation")
                TreadWearPattern.ONE_SIDE_WEAR -> concerns.add("Uneven wear suggests alignment issues")
                TreadWearPattern.CUPPING -> concerns.add("Cupping wear suggests suspension problems")
                else -> {}
            }
        }

        // Defect concerns
        defects.filter { it.type != DefectType.GOOD }.forEach { defect ->
            concerns.add("${defect.severity.displayName} severity: ${defect.type.displayName}")
        }

        // Age concerns
        dotCodeInfo?.let {
            when (it.ageStatus) {
                TireAgeStatus.EXPIRED -> concerns.add("⚠️ Tire exceeds 10-year age limit - replace immediately")
                TireAgeStatus.OLD -> concerns.add("Tire is ${it.ageInMonths / 12} years old - inspect carefully")
                TireAgeStatus.AGING -> concerns.add("Tire age approaching recommended replacement threshold")
                else -> {}
            }
        }

        return concerns
    }

    private fun generateRecommendations(
        treadResult: TreadDepthResult?,
        defects: List<TireDefect>,
        dotCodeInfo: DotCodeInfo?
    ): List<TireRecommendation> {
        val recommendations = mutableListOf<TireRecommendation>()

        // Critical defects - replacement
        if (defects.any { it.severity == DefectSeverity.CRITICAL }) {
            recommendations.add(TireRecommendation(
                title = "Immediate Tire Replacement",
                description = "Critical defect detected. Do not drive. Replace tire before use.",
                priority = 1,
                estimatedCost = 150,
                serviceType = ServiceType.REPLACEMENT
            ))
        }

        // Tread depth recommendations
        treadResult?.let { result ->
            when (result.status) {
                TreadStatus.CRITICAL -> recommendations.add(TireRecommendation(
                    title = "Replace Tire - Critical Tread",
                    description = "Tread depth ${result.minimumDepthMm}mm is below the 1.6mm legal minimum",
                    priority = 1,
                    estimatedCost = 150,
                    serviceType = ServiceType.REPLACEMENT
                ))
                TreadStatus.LOW -> recommendations.add(TireRecommendation(
                    title = "Plan Tire Replacement",
                    description = "Tread depth ${result.minimumDepthMm}mm approaching minimum. Replace within 5,000 km",
                    priority = 2,
                    estimatedCost = 150,
                    serviceType = ServiceType.REPLACEMENT
                ))
                else -> {}
            }

            // Wear pattern recommendations
            when (result.wearPattern) {
                TreadWearPattern.CENTER_WEAR -> recommendations.add(TireRecommendation(
                    title = "Reduce Tire Pressure",
                    description = "Center wear indicates over-inflation. Check and reduce pressure to recommended PSI",
                    priority = 3,
                    estimatedCost = 0,
                    serviceType = ServiceType.PRESSURE_ADJUSTMENT
                ))
                TreadWearPattern.EDGE_WEAR -> recommendations.add(TireRecommendation(
                    title = "Increase Tire Pressure",
                    description = "Edge wear indicates under-inflation. Inflate to recommended PSI",
                    priority = 3,
                    estimatedCost = 0,
                    serviceType = ServiceType.PRESSURE_ADJUSTMENT
                ))
                TreadWearPattern.ONE_SIDE_WEAR -> recommendations.add(TireRecommendation(
                    title = "Wheel Alignment Service",
                    description = "Uneven wear pattern suggests wheel alignment issues",
                    priority = 2,
                    estimatedCost = 80,
                    serviceType = ServiceType.ALIGNMENT
                ))
                TreadWearPattern.CUPPING -> recommendations.add(TireRecommendation(
                    title = "Suspension Inspection",
                    description = "Cupping wear pattern indicates suspension problems",
                    priority = 2,
                    estimatedCost = 100,
                    serviceType = ServiceType.INSPECTION
                ))
                else -> {}
            }
        }

        // Repairable defects
        defects.filter { it.severity == DefectSeverity.MEDIUM || it.severity == DefectSeverity.LOW }
            .forEach { defect ->
                if (defect.type == DefectType.PUNCTURE) {
                    recommendations.add(TireRecommendation(
                        title = "Puncture Repair",
                        description = "Puncture can likely be repaired if in tread area",
                        priority = 2,
                        estimatedCost = 25,
                        serviceType = ServiceType.REPAIR
                    ))
                }
                if (defect.type == DefectType.FOREIGN_OBJECT) {
                    recommendations.add(TireRecommendation(
                        title = "Remove Foreign Object",
                        description = "Have foreign object removed and inspect for damage",
                        priority = 3,
                        estimatedCost = 15,
                        serviceType = ServiceType.INSPECTION
                    ))
                }
            }

        // Age recommendations
        dotCodeInfo?.let {
            if (it.ageStatus == TireAgeStatus.EXPIRED || it.ageStatus == TireAgeStatus.OLD) {
                recommendations.add(TireRecommendation(
                    title = "Age-Related Replacement",
                    description = "Tire is ${it.ageInMonths / 12} years old. Rubber degrades with age regardless of tread.",
                    priority = if (it.ageStatus == TireAgeStatus.EXPIRED) 1 else 2,
                    estimatedCost = 150,
                    serviceType = ServiceType.REPLACEMENT
                ))
            }
        }

        // Routine maintenance if no major issues
        if (recommendations.isEmpty()) {
            recommendations.add(TireRecommendation(
                title = "Continue Regular Maintenance",
                description = "Tire is in good condition. Continue regular rotation and pressure checks.",
                priority = 5,
                estimatedCost = 0,
                serviceType = ServiceType.ROTATION
            ))
        }

        return recommendations.sortedBy { it.priority }
    }

    private fun determineActionRequired(
        status: OverallTireStatus,
        treadResult: TreadDepthResult?,
        defects: List<TireDefect>
    ): ActionRequired {
        // Check for critical conditions
        if (defects.any { it.severity == DefectSeverity.CRITICAL }) {
            return if (defects.any { it.type == DefectType.BULGE || it.type == DefectType.SIDEWALL_DAMAGE }) {
                ActionRequired.DO_NOT_DRIVE
            } else {
                ActionRequired.REPLACE
            }
        }

        if (treadResult?.status == TreadStatus.CRITICAL) {
            return ActionRequired.REPLACE
        }

        return when (status) {
            OverallTireStatus.CRITICAL -> ActionRequired.REPLACE
            OverallTireStatus.POOR -> ActionRequired.SERVICE_NOW
            OverallTireStatus.FAIR -> ActionRequired.SERVICE_SOON
            OverallTireStatus.GOOD -> ActionRequired.MONITOR
            OverallTireStatus.EXCELLENT -> ActionRequired.NONE
        }
    }

    private fun estimateCostRange(recommendations: List<TireRecommendation>): Pair<Int, Int>? {
        if (recommendations.isEmpty()) return null

        val costs = recommendations.mapNotNull { it.estimatedCost }.filter { it > 0 }
        if (costs.isEmpty()) return null

        val minCost = costs.minOrNull() ?: 0
        val maxCost = costs.sum()

        return Pair(minCost, maxCost)
    }
}

