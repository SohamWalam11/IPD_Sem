package org.example.project.tyre3d.viewer

import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import org.example.project.analysis.TireStatus
import org.example.project.analysis.TreadHealth

/**
 * Unified Tyre Analysis Screen that handles both 3D and AR modes
 * 
 * Features:
 * - SceneView for 3D rendering with defect visualization
 * - ArSceneView for AR + 3D rendering with real-world overlay
 * - Seamless switching between modes
 * - Defect markers showing affected areas
 * - Tap on defects for details and recommendations
 */

enum class ViewerMode {
    MODE_3D,
    MODE_AR
}

/**
 * Main entry point for tyre analysis with 3D/AR visualization
 * 
 * @param tireStatus The tire status data containing defects and health info
 * @param modelPath Path to the 3D model file (GLB format)
 * @param initialMode Whether to start in 3D or AR mode
 * @param onBackClick Callback when back button is pressed
 */
@Composable
fun TyreAnalysisScreen(
    tireStatus: TireStatus,
    modelPath: String,
    initialMode: ViewerMode = ViewerMode.MODE_3D,
    modifier: Modifier = Modifier,
    onBackClick: () -> Unit = {}
) {
    var currentMode by remember { mutableStateOf(initialMode) }
    
    // Convert TireStatus to TyreAnalysisResult
    val analysisResult = remember(tireStatus) {
        convertToAnalysisResult(tireStatus)
    }
    
    when (currentMode) {
        ViewerMode.MODE_3D -> {
            TyreDefectViewer(
                modelPath = modelPath,
                analysisResult = analysisResult,
                modifier = modifier,
                onBackClick = onBackClick,
                onArModeClick = { currentMode = ViewerMode.MODE_AR },
                onDefectClick = { defect ->
                    // Handle defect click - could show detailed analysis
                }
            )
        }
        ViewerMode.MODE_AR -> {
            ArTyreDefectViewer(
                modelPath = modelPath,
                analysisResult = analysisResult,
                modifier = modifier,
                onBackClick = onBackClick,
                on3DModeClick = { currentMode = ViewerMode.MODE_3D },
                onDefectClick = { defect ->
                    // Handle defect click in AR mode
                }
            )
        }
    }
}

/**
 * Converts TireStatus from the app's data model to TyreAnalysisResult for the viewer
 */
private fun convertToAnalysisResult(tireStatus: TireStatus): TyreAnalysisResult {
    // Map string defects to typed defects
    val typedDefects = tireStatus.defects.mapIndexed { index, defectString ->
        parseDefect(defectString, index)
    }
    
    // Get tread health percentage
    val treadHealthPercent = when (tireStatus.treadHealth) {
        TreadHealth.EXCELLENT -> 95
        TreadHealth.GOOD -> 75
        TreadHealth.FAIR -> 50
        TreadHealth.WORN -> 25
        TreadHealth.CRITICAL -> 10
    }
    
    // Calculate overall health based on defects
    val overallScore = calculateHealthScore(tireStatus, typedDefects, treadHealthPercent)
    val overallStatus = when {
        overallScore >= 90 -> TyreHealthStatus.EXCELLENT
        overallScore >= 75 -> TyreHealthStatus.GOOD
        overallScore >= 50 -> TyreHealthStatus.FAIR
        overallScore >= 25 -> TyreHealthStatus.POOR
        else -> TyreHealthStatus.CRITICAL
    }
    
    return TyreAnalysisResult(
        overallScore = overallScore,
        overallStatus = overallStatus,
        pressure = tireStatus.pressurePsi,
        temperature = tireStatus.temperatureCelsius,
        treadDepth = estimateTreadDepth(treadHealthPercent),
        defects = typedDefects
    )
}

/**
 * Parse a defect string into a typed TyreDefect
 */
private fun parseDefect(defectString: String, index: Int): TyreDefect {
    val lowerDefect = defectString.lowercase()
    
    // Determine type from string
    val type = when {
        lowerDefect.contains("crack") -> DefectType.CRACK
        lowerDefect.contains("bulge") || lowerDefect.contains("bubble") -> DefectType.BULGE
        lowerDefect.contains("wear") && lowerDefect.contains("uneven") -> DefectType.UNEVEN_WEAR
        lowerDefect.contains("wear") || lowerDefect.contains("worn") -> DefectType.WEAR
        lowerDefect.contains("puncture") || lowerDefect.contains("nail") -> DefectType.PUNCTURE
        lowerDefect.contains("cut") || lowerDefect.contains("slash") -> DefectType.CUT
        lowerDefect.contains("tread") && lowerDefect.contains("low") -> DefectType.LOW_TREAD
        lowerDefect.contains("sidewall") -> DefectType.SIDEWALL_DAMAGE
        lowerDefect.contains("object") || lowerDefect.contains("foreign") || lowerDefect.contains("stone") -> DefectType.FOREIGN_OBJECT
        lowerDefect.contains("age") || lowerDefect.contains("old") || lowerDefect.contains("dry") -> DefectType.AGING
        else -> DefectType.WEAR // Default
    }
    
    // Determine severity from keywords
    val severity = when {
        lowerDefect.contains("critical") || lowerDefect.contains("severe") || lowerDefect.contains("dangerous") -> DefectSeverity.CRITICAL
        lowerDefect.contains("high") || lowerDefect.contains("major") || lowerDefect.contains("significant") -> DefectSeverity.HIGH
        lowerDefect.contains("medium") || lowerDefect.contains("moderate") -> DefectSeverity.MEDIUM
        lowerDefect.contains("low") || lowerDefect.contains("minor") || lowerDefect.contains("small") -> DefectSeverity.LOW
        else -> when (type) {
            DefectType.CRACK, DefectType.BULGE, DefectType.PUNCTURE, DefectType.CUT, DefectType.SIDEWALL_DAMAGE -> DefectSeverity.HIGH
            DefectType.WEAR, DefectType.UNEVEN_WEAR, DefectType.LOW_TREAD, DefectType.AGING -> DefectSeverity.MEDIUM
            DefectType.FOREIGN_OBJECT -> DefectSeverity.LOW
        }
    }
    
    // Generate position based on index (distribute around tyre)
    val angle = (index * 72f + 30f) * (Math.PI / 180.0)  // Spread at 72° intervals
    val position = DefectPosition(
        x = (kotlin.math.sin(angle) * 0.6f).toFloat(),
        y = ((index % 3) - 1) * 0.2f,  // Vary height
        z = (kotlin.math.cos(angle) * 0.6f).toFloat()
    )
    
    // Generate recommendation based on type and severity
    val recommendation = getRecommendation(type, severity)
    
    return TyreDefect(
        id = "defect_$index",
        type = type,
        severity = severity,
        position = position,
        description = defectString,
        recommendation = recommendation
    )
}

/**
 * Get recommendation based on defect type and severity
 */
private fun getRecommendation(type: DefectType, severity: DefectSeverity): String {
    return when (type) {
        DefectType.CRACK -> when (severity) {
            DefectSeverity.CRITICAL, DefectSeverity.HIGH -> "Replace tyre immediately. Cracks can cause sudden failure."
            else -> "Monitor crack progression. Consider replacement soon."
        }
        DefectType.BULGE -> "Replace tyre immediately! Bulges indicate internal damage and risk of blowout."
        DefectType.WEAR -> when (severity) {
            DefectSeverity.CRITICAL, DefectSeverity.HIGH -> "Tyre needs replacement. Worn treads reduce grip significantly."
            else -> "Monitor wear pattern. Rotate tyres regularly."
        }
        DefectType.PUNCTURE -> "Get tyre inspected by a professional. May need repair or replacement."
        DefectType.CUT -> when (severity) {
            DefectSeverity.CRITICAL, DefectSeverity.HIGH -> "Replace tyre. Deep cuts compromise structural integrity."
            else -> "Have cut inspected. Monitor for any changes."
        }
        DefectType.UNEVEN_WEAR -> "Check wheel alignment and suspension. Rotate tyres and rebalance."
        DefectType.LOW_TREAD -> when (severity) {
            DefectSeverity.CRITICAL -> "Replace immediately! Below legal minimum tread depth."
            DefectSeverity.HIGH -> "Plan for replacement within 1000km."
            else -> "Monitor tread depth. Replace before monsoon season."
        }
        DefectType.SIDEWALL_DAMAGE -> "Replace tyre. Sidewall damage cannot be safely repaired."
        DefectType.FOREIGN_OBJECT -> "Remove carefully if safe. If embedded deep, consult professional."
        DefectType.AGING -> when (severity) {
            DefectSeverity.CRITICAL, DefectSeverity.HIGH -> "Replace tyre. Age degradation affects safety."
            else -> "Monitor condition. Tyres over 5 years old need regular inspection."
        }
    }
}

/**
 * Calculate overall health score based on tire status and defects
 */
private fun calculateHealthScore(tireStatus: TireStatus, defects: List<TyreDefect>, treadHealthPercent: Int): Int {
    var score = 100
    
    // Deduct for defects based on severity
    defects.forEach { defect ->
        score -= when (defect.severity) {
            DefectSeverity.CRITICAL -> 25
            DefectSeverity.HIGH -> 15
            DefectSeverity.MEDIUM -> 8
            DefectSeverity.LOW -> 4
            DefectSeverity.INFO -> 1
        }
    }
    
    // Deduct for low tread health
    if (treadHealthPercent < 50) {
        score -= (50 - treadHealthPercent) / 5
    }
    
    // Deduct for abnormal pressure
    if (tireStatus.pressurePsi < 28 || tireStatus.pressurePsi > 38) {
        score -= 10
    }
    
    // Deduct for high temperature
    if (tireStatus.temperatureCelsius > 40) {
        score -= 10
    }
    
    return score.coerceIn(0, 100)
}

/**
 * Estimate tread depth from tread health percentage
 */
private fun estimateTreadDepth(treadHealth: Int): Float {
    // New tyre ~8mm, worn tyre ~1.6mm (legal min)
    val maxDepth = 8f
    val minDepth = 1.6f
    return minDepth + (maxDepth - minDepth) * (treadHealth / 100f)
}
