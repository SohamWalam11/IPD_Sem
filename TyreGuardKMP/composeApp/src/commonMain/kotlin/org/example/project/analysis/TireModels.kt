package org.example.project.analysis

/**
 * Represents the status of a single tire including sensor data and defects.
 */
data class TireStatus(
    val position: TirePosition,
    val pressurePsi: Float,
    val temperatureCelsius: Float,
    val treadHealth: TreadHealth,
    val defects: List<String> = emptyList(),
    val lastInspectionTime: Long = System.currentTimeMillis()
) {
    val isCritical: Boolean
        get() = pressurePsi < 28f || pressurePsi > 38f || defects.isNotEmpty() || treadHealth == TreadHealth.CRITICAL
}

/**
 * Tire position on the vehicle.
 */
enum class TirePosition(val displayName: String, val shortName: String) {
    FRONT_LEFT("Front Left", "FL"),
    FRONT_RIGHT("Front Right", "FR"),
    REAR_LEFT("Rear Left", "RL"),
    REAR_RIGHT("Rear Right", "RR")
}

/**
 * Tread health status.
 */
enum class TreadHealth(val displayText: String) {
    EXCELLENT("Excellent"),
    GOOD("Good"),
    FAIR("Fair"),
    WORN("Worn"),
    CRITICAL("Critical - Replace")
}

/**
 * State holder for the Report tab.
 */
data class ReportState(
    val tires: List<TireStatus> = listOf(
        TireStatus(TirePosition.FRONT_LEFT, 32.5f, 28.0f, TreadHealth.GOOD),
        TireStatus(TirePosition.FRONT_RIGHT, 31.8f, 27.5f, TreadHealth.EXCELLENT),
        TireStatus(TirePosition.REAR_LEFT, 33.0f, 29.0f, TreadHealth.FAIR),
        TireStatus(TirePosition.REAR_RIGHT, 26.5f, 31.0f, TreadHealth.WORN, listOf("Crack Detected", "Bulge"))
    ),
    val isLoading: Boolean = false,
    val lastUpdated: Long = System.currentTimeMillis()
)

/**
 * State holder for the Telemetry tab.
 */
data class TelemetryState(
    val selectedTire: TirePosition = TirePosition.FRONT_LEFT,
    val pressureHistory: List<Float> = emptyList(),
    val temperatureHistory: List<Float> = emptyList(),
    val isLive: Boolean = true
)
