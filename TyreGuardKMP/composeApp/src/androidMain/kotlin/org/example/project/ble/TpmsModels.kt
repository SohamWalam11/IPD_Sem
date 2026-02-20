package org.example.project.ble

/**
 * Data class representing parsed TPMS sensor data from a JK Tyre BLE sensor.
 *
 * The raw BLE advertisement payload (manufacturer-specific data after 0xFF) is parsed
 * to extract pressure, temperature, battery, and alarm flags.
 *
 * Example raw advertisement (hex):
 *   0x0201040303E0FF17FFD96CC215056C64DB961C886258F3AB27F35A06C1CB8B
 *
 * Layout of manufacturer-specific data (after the 0xFF type byte):
 *   Bytes 0-5   : MAC address (D9:6C:C2:15:05:6C)
 *   Bytes 6-9   : Pressure in kPa × 100 (little-endian uint32)
 *   Bytes 10-13 : Temperature in °C × 100 (little-endian int32)
 *   Byte  14    : Battery percentage (0-100)
 *   Byte  15    : Alarm flags (bit 0=low pressure, bit 1=high temp, bit 2=rapid leak)
 */
data class TpmsSensorData(
    val macAddress: String,
    val pressureKpa: Float,
    val pressurePsi: Float,
    val temperatureCelsius: Float,
    val batteryPercent: Int,
    val isLowPressure: Boolean = false,
    val isHighTemperature: Boolean = false,
    val isRapidLeak: Boolean = false,
    val rssi: Int = 0,
    val timestamp: Long = System.currentTimeMillis()
) {
    val hasAlarm: Boolean get() = isLowPressure || isHighTemperature || isRapidLeak
}

/**
 * Which wheel this sensor is mounted on (configurable by user).
 */
enum class TyreWheelPosition(val label: String, val shortLabel: String) {
    FRONT_LEFT("Front Left", "FL"),
    FRONT_RIGHT("Front Right", "FR"),
    REAR_LEFT("Rear Left", "RL"),
    REAR_RIGHT("Rear Right", "RR"),
    SPARE("Spare", "SP"),
    UNASSIGNED("Unassigned", "--")
}

/**
 * A configured sensor that maps a BLE MAC address to a wheel position.
 */
data class TpmsSensorConfig(
    val macAddress: String,
    val wheelPosition: TyreWheelPosition,
    val label: String = ""
)

/**
 * Overall state of the TPMS system.
 */
data class TpmsState(
    val isScanning: Boolean = false,
    val isBluetoothEnabled: Boolean = false,
    val hasPermissions: Boolean = false,
    val sensors: Map<String, TpmsSensorData> = emptyMap(),
    val sensorConfigs: List<TpmsSensorConfig> = emptyList(),
    val lastUpdateTime: Long = 0L,
    val errorMessage: String? = null
) {
    /**
     * Get sensor data for a specific wheel position.
     */
    fun getDataForPosition(position: TyreWheelPosition): TpmsSensorData? {
        val config = sensorConfigs.find { it.wheelPosition == position } ?: return null
        return sensors[config.macAddress]
    }

    /**
     * Get all assigned sensor data mapped by position.
     */
    fun getAllPositionData(): Map<TyreWheelPosition, TpmsSensorData> {
        return sensorConfigs
            .filter { it.wheelPosition != TyreWheelPosition.UNASSIGNED }
            .mapNotNull { config ->
                sensors[config.macAddress]?.let { data ->
                    config.wheelPosition to data
                }
            }
            .toMap()
    }
}
