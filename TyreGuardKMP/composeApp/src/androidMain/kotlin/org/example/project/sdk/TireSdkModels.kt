package org.example.project.sdk

import android.graphics.Bitmap
import android.graphics.RectF

/**
 * Data models for Tire SDK integrations.
 * Shared across Anyline Tread SDK, Michelin API, and custom TFLite analysis.
 */

// ═══════════════════════════════════════════════════════════════════════════════
// Tread Depth Measurement Results (Anyline SDK)
// ═══════════════════════════════════════════════════════════════════════════════

/**
 * Comprehensive tread depth measurement from Anyline SDK.
 */
data class TreadDepthResult(
    val uuid: String,
    val measurementTimestamp: Long = System.currentTimeMillis(),

    // Depth measurements in mm
    val innerDepthMm: Float,
    val centerDepthMm: Float,
    val outerDepthMm: Float,
    val averageDepthMm: Float,
    val minimumDepthMm: Float,

    // Confidence and quality
    val confidence: Float, // 0.0 - 1.0
    val qualityScore: Int, // 0 - 100

    // Wear analysis
    val wearPercentage: Float, // 0-100% worn
    val estimatedRemainingLife: Int, // Estimated km remaining
    val wearPattern: TreadWearPattern,

    // Status
    val status: TreadStatus,
    val recommendations: List<String>
) {
    val isValid: Boolean get() = confidence >= AnylineTreadConfig.Quality.MIN_CONFIDENCE

    companion object {
        fun empty() = TreadDepthResult(
            uuid = "",
            innerDepthMm = 0f,
            centerDepthMm = 0f,
            outerDepthMm = 0f,
            averageDepthMm = 0f,
            minimumDepthMm = 0f,
            confidence = 0f,
            qualityScore = 0,
            wearPercentage = 0f,
            estimatedRemainingLife = 0,
            wearPattern = TreadWearPattern.UNKNOWN,
            status = TreadStatus.UNKNOWN,
            recommendations = emptyList()
        )
    }
}

/**
 * Tread depth status classification.
 */
enum class TreadStatus(val displayName: String, val colorHex: Long) {
    EXCELLENT("Excellent", 0xFF10B981),  // Green
    GOOD("Good", 0xFF22C55E),            // Light green
    FAIR("Fair", 0xFFF59E0B),            // Yellow/Amber
    LOW("Low - Replace Soon", 0xFFF97316), // Orange
    CRITICAL("Critical - Replace Now", 0xFFEF4444), // Red
    UNKNOWN("Unknown", 0xFF6B7280)       // Gray
}

/**
 * Tread wear pattern classification.
 */
enum class TreadWearPattern(val displayName: String, val cause: String) {
    EVEN("Even Wear", "Normal driving - tire is wearing correctly"),
    CENTER_WEAR("Center Wear", "Over-inflation - reduce pressure"),
    EDGE_WEAR("Edge Wear", "Under-inflation - increase pressure"),
    ONE_SIDE_WEAR("One-Side Wear", "Wheel alignment issue - check alignment"),
    CUPPING("Cupping/Scalloping", "Suspension problem - check shocks/struts"),
    FEATHERING("Feathering", "Toe alignment issue - needs adjustment"),
    DIAGONAL_WEAR("Diagonal Wear", "Multiple alignment issues"),
    FLAT_SPOT("Flat Spots", "Brake lock-up or extended parking"),
    UNKNOWN("Unknown Pattern", "Unable to determine wear pattern")
}

/**
 * Heat map data point for tread visualization.
 */
data class TreadHeatMapPoint(
    val x: Float,        // Normalized position 0-1 across tire width
    val y: Float,        // Normalized position 0-1 along tire circumference
    val depthMm: Float,  // Tread depth at this point
    val depthNormalized: Float // 0 = no tread, 1 = full tread
)

/**
 * Complete heat map for tire tread visualization.
 */
data class TreadHeatMap(
    val points: List<TreadHeatMapPoint>,
    val gridWidth: Int,
    val gridHeight: Int,
    val minDepthMm: Float,
    val maxDepthMm: Float
)

// ═══════════════════════════════════════════════════════════════════════════════
// Tire Size Recognition Results (Michelin API)
// ═══════════════════════════════════════════════════════════════════════════════

/**
 * Tire size and specification from sidewall image recognition.
 */
data class TireSizeInfo(
    val rawText: String,           // e.g., "225/45R17 94W"
    val width: Int,                // mm (e.g., 225)
    val aspectRatio: Int,          // % (e.g., 45)
    val construction: String,      // R = Radial, D = Diagonal
    val rimDiameter: Int,          // inches (e.g., 17)
    val loadIndex: Int,            // e.g., 94
    val speedRating: Char,         // e.g., 'W'
    val confidence: Float,
    val additionalMarks: List<String> = emptyList() // XL, RF, etc.
) {
    val formattedSize: String get() = "$width/${aspectRatio}${construction}$rimDiameter"
    val fullSpecification: String get() = "$formattedSize ${loadIndex}${speedRating}"

    // Calculate max speed from rating
    val maxSpeedKmh: Int get() = when (speedRating) {
        'L' -> 120
        'M' -> 130
        'N' -> 140
        'P' -> 150
        'Q' -> 160
        'R' -> 170
        'S' -> 180
        'T' -> 190
        'U' -> 200
        'H' -> 210
        'V' -> 240
        'W' -> 270
        'Y' -> 300
        'Z' -> 300 // 300+
        else -> 0
    }

    // Calculate max load from index
    val maxLoadKg: Int get() = LOAD_INDEX_TABLE.getOrDefault(loadIndex, 0)

    companion object {
        private val LOAD_INDEX_TABLE = mapOf(
            70 to 335, 71 to 345, 72 to 355, 73 to 365, 74 to 375,
            75 to 387, 76 to 400, 77 to 412, 78 to 425, 79 to 437,
            80 to 450, 81 to 462, 82 to 475, 83 to 487, 84 to 500,
            85 to 515, 86 to 530, 87 to 545, 88 to 560, 89 to 580,
            90 to 600, 91 to 615, 92 to 630, 93 to 650, 94 to 670,
            95 to 690, 96 to 710, 97 to 730, 98 to 750, 99 to 775,
            100 to 800, 101 to 825, 102 to 850, 103 to 875, 104 to 900
        )

        fun empty() = TireSizeInfo(
            rawText = "",
            width = 0,
            aspectRatio = 0,
            construction = "R",
            rimDiameter = 0,
            loadIndex = 0,
            speedRating = ' ',
            confidence = 0f
        )
    }
}

// ═══════════════════════════════════════════════════════════════════════════════
// DOT Code Recognition Results (Michelin API)
// ═══════════════════════════════════════════════════════════════════════════════

/**
 * DOT (Department of Transportation) code information.
 */
data class DotCodeInfo(
    val fullDotCode: String,       // Complete DOT code
    val plantCode: String,         // 2-3 character manufacturer plant code
    val tireSize: String,          // Manufacturer's size code
    val brandCharacteristics: String, // Optional brand info
    val manufactureWeek: Int,      // Week 01-52
    val manufactureYear: Int,      // Full year (e.g., 2024)
    val ageInMonths: Int,          // Calculated tire age
    val confidence: Float
) {
    val manufactureDate: String get() = "Week $manufactureWeek, $manufactureYear"

    val ageStatus: TireAgeStatus get() = when {
        ageInMonths < 36 -> TireAgeStatus.NEW
        ageInMonths < 60 -> TireAgeStatus.GOOD
        ageInMonths < 72 -> TireAgeStatus.AGING
        ageInMonths < 120 -> TireAgeStatus.OLD
        else -> TireAgeStatus.EXPIRED
    }

    val isExpired: Boolean get() = ageInMonths >= 120 // 10 years

    companion object {
        fun empty() = DotCodeInfo(
            fullDotCode = "",
            plantCode = "",
            tireSize = "",
            brandCharacteristics = "",
            manufactureWeek = 0,
            manufactureYear = 0,
            ageInMonths = 0,
            confidence = 0f
        )
    }
}

enum class TireAgeStatus(val displayName: String, val colorHex: Long) {
    NEW("New (< 3 years)", 0xFF10B981),
    GOOD("Good (3-5 years)", 0xFF22C55E),
    AGING("Aging (5-6 years)", 0xFFF59E0B),
    OLD("Old (6-10 years)", 0xFFF97316),
    EXPIRED("Expired (> 10 years)", 0xFFEF4444)
}

// ═══════════════════════════════════════════════════════════════════════════════
// Custom TFLite Defect Detection Results
// ═══════════════════════════════════════════════════════════════════════════════

/**
 * Defect detection result from custom TFLite model.
 */
data class TireDefect(
    val type: DefectType,
    val confidence: Float,
    val boundingBox: RectF?,
    val severity: DefectSeverity,
    val description: String
)

enum class DefectType(val displayName: String) {
    CRACK("Crack"),
    BULGE("Bulge/Bubble"),
    CUT("Cut/Slash"),
    PUNCTURE("Puncture"),
    WORN_TREAD("Worn Tread"),
    SIDEWALL_DAMAGE("Sidewall Damage"),
    FOREIGN_OBJECT("Foreign Object"),
    DRY_ROT("Dry Rot/Age Cracking"),
    BEAD_DAMAGE("Bead Damage"),
    GOOD("No Defect Detected")
}

enum class DefectSeverity(val displayName: String, val colorHex: Long, val priority: Int) {
    NONE("None", 0xFF22C55E, 0),
    LOW("Low", 0xFFF59E0B, 1),
    MEDIUM("Medium", 0xFFF97316, 2),
    HIGH("High", 0xFFEF4444, 3),
    CRITICAL("Critical", 0xFFDC2626, 4)
}

// ═══════════════════════════════════════════════════════════════════════════════
// Combined Analysis Result
// ═══════════════════════════════════════════════════════════════════════════════

/**
 * Comprehensive tire health analysis combining all data sources.
 */
data class ComprehensiveTireAnalysis(
    val id: String,
    val timestamp: Long = System.currentTimeMillis(),

    // Source data
    val treadDepth: TreadDepthResult?,
    val tireSizeInfo: TireSizeInfo?,
    val dotCodeInfo: DotCodeInfo?,
    val detectedDefects: List<TireDefect>,

    // Captured images
    val treadImagePath: String?,
    val sidewallImagePath: String?,

    // Overall assessment
    val overallHealthScore: Int, // 0-100
    val overallStatus: OverallTireStatus,
    val primaryConcerns: List<String>,
    val recommendations: List<TireRecommendation>,

    // Action required
    val actionRequired: ActionRequired,
    val estimatedCostRange: Pair<Int, Int>?, // Min-Max USD

    // PDF report
    val reportPdfPath: String? = null
)

enum class OverallTireStatus(val displayName: String, val colorHex: Long) {
    EXCELLENT("Excellent - No Issues", 0xFF10B981),
    GOOD("Good - Minor Wear", 0xFF22C55E),
    FAIR("Fair - Monitor Closely", 0xFFF59E0B),
    POOR("Poor - Service Soon", 0xFFF97316),
    CRITICAL("Critical - Unsafe", 0xFFEF4444)
}

enum class ActionRequired(val displayName: String, val urgency: Int) {
    NONE("No Action Needed", 0),
    MONITOR("Monitor & Re-check", 1),
    SERVICE_SOON("Service Within 30 Days", 2),
    SERVICE_NOW("Service Immediately", 3),
    REPLACE("Replace Tire", 4),
    DO_NOT_DRIVE("Do Not Drive - Unsafe", 5)
}

data class TireRecommendation(
    val title: String,
    val description: String,
    val priority: Int, // 1 = highest
    val estimatedCost: Int?, // USD
    val serviceType: ServiceType
)

enum class ServiceType {
    ROTATION,
    BALANCING,
    ALIGNMENT,
    REPAIR,
    REPLACEMENT,
    PRESSURE_ADJUSTMENT,
    INSPECTION
}

