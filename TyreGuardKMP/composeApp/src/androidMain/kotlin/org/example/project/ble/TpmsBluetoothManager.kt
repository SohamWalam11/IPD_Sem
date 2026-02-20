package org.example.project.ble

import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.bluetooth.le.BluetoothLeScanner
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanFilter
import android.bluetooth.le.ScanResult
import android.bluetooth.le.ScanSettings
import android.content.Context
import android.os.Build
import android.os.ParcelUuid
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.UUID

/**
 * BLE TPMS Manager for JK Tyre pressure sensors.
 *
 * Handles scanning for BLE TPMS sensors, parsing advertisement data,
 * and providing real-time tyre pressure/temperature data.
 *
 * ## Supported Sensor
 * - JK Tyre TPMS sensor (Treel)
 * - MAC: D9:6C:C2:15:05:6C (configurable)
 * - Broadcasts manufacturer-specific data in BLE advertisements
 *
 * ## Advertisement Data Format
 * The raw advertisement hex:
 *   0x0201040303E0FF17FFD96CC215056C64DB961C886258F3AB27F35A06C1CB8B
 *
 * Breakdown:
 *   02 01 04          - AD Type: Flags (LE General Discoverable)
 *   03 03 E0 FF       - AD Type: Complete list of 16-bit service UUIDs (0xFFE0)
 *   17 FF             - AD Type: Manufacturer Specific Data, length 0x17 = 23 bytes
 *     D9 6C C2 15 05 6C - Sensor MAC address (6 bytes)
 *     64 DB 96 1C       - Pressure raw (4 bytes, little-endian) → kPa
 *     88 62 58 F3       - Temperature raw (4 bytes, little-endian) → °C
 *     AB                - Battery level (1 byte, 0-100%)
 *     27                - Alarm flags (1 byte)
 *     F3 5A 06 C1 CB 8B - Reserved / checksum (6 bytes)
 */
class TpmsBluetoothManager(
    private val context: Context
) {
    companion object {
        private const val TAG = "TpmsBluetoothManager"

        // ══════════════════════════════════════════════════════════════
        // Preconfigured JK Tyre Treel TPMS sensor MAC address
        // ══════════════════════════════════════════════════════════════
        const val DEFAULT_SENSOR_MAC = "D9:6C:C2:15:05:6C"

        // BLE service UUID for the TPMS sensor (0xFFE0)
        val TPMS_SERVICE_UUID: UUID = UUID.fromString("0000FFE0-0000-1000-8000-00805F9B34FB")

        // Scan interval
        private const val SCAN_PERIOD_MS = 30_000L // 30 seconds per scan cycle

        // Pressure conversion: raw value → kPa (divide by 100), then kPa → PSI
        private const val KPA_TO_PSI = 0.14503773f

        @Volatile
        private var instance: TpmsBluetoothManager? = null

        fun getInstance(context: Context): TpmsBluetoothManager {
            return instance ?: synchronized(this) {
                instance ?: TpmsBluetoothManager(context.applicationContext).also {
                    instance = it
                }
            }
        }
    }

    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())

    private val bluetoothManager: BluetoothManager? by lazy {
        context.getSystemService(Context.BLUETOOTH_SERVICE) as? BluetoothManager
    }
    private val bluetoothAdapter: BluetoothAdapter? by lazy {
        bluetoothManager?.adapter
    }
    private val bleScanner: BluetoothLeScanner? by lazy {
        bluetoothAdapter?.bluetoothLeScanner
    }

    // ══════════════════════════════════════════════════════════════
    // Public state
    // ══════════════════════════════════════════════════════════════

    private val _state = MutableStateFlow(
        TpmsState(
            sensorConfigs = listOf(
                // Preconfigure with the user's known sensor
                TpmsSensorConfig(
                    macAddress = DEFAULT_SENSOR_MAC,
                    wheelPosition = TyreWheelPosition.FRONT_LEFT,
                    label = "JK Tyre Treel TPMS"
                )
            )
        )
    )
    val state: StateFlow<TpmsState> = _state.asStateFlow()

    // Track all discovered TPMS devices (for pairing screen)
    private val _discoveredSensors = MutableStateFlow<List<TpmsSensorData>>(emptyList())
    val discoveredSensors: StateFlow<List<TpmsSensorData>> = _discoveredSensors.asStateFlow()

    // ══════════════════════════════════════════════════════════════
    // BLE Scan Callback
    // ══════════════════════════════════════════════════════════════

    private val scanCallback = object : ScanCallback() {
        override fun onScanResult(callbackType: Int, result: ScanResult) {
            processScanResult(result)
        }

        override fun onBatchScanResults(results: MutableList<ScanResult>) {
            results.forEach { processScanResult(it) }
        }

        override fun onScanFailed(errorCode: Int) {
            Log.e(TAG, "BLE Scan failed with error code: $errorCode")
            _state.value = _state.value.copy(
                isScanning = false,
                errorMessage = "Scan failed (error $errorCode)"
            )
        }
    }

    // ══════════════════════════════════════════════════════════════
    // Public API
    // ══════════════════════════════════════════════════════════════

    /**
     * Check if Bluetooth is available and enabled.
     */
    fun isBluetoothEnabled(): Boolean {
        return bluetoothAdapter?.isEnabled == true
    }

    /**
     * Start scanning for TPMS sensors.
     */
    @SuppressLint("MissingPermission")
    fun startScanning() {
        if (!isBluetoothEnabled()) {
            _state.value = _state.value.copy(
                isBluetoothEnabled = false,
                errorMessage = "Bluetooth is not enabled"
            )
            return
        }

        val scanner = bleScanner
        if (scanner == null) {
            _state.value = _state.value.copy(
                errorMessage = "BLE scanner not available"
            )
            return
        }

        Log.d(TAG, "Starting TPMS BLE scan...")

        // Build scan filters - look for the TPMS service UUID and/or known MAC
        val filters = mutableListOf<ScanFilter>()

        // Filter by service UUID (0xFFE0 - common for TPMS sensors)
        filters.add(
            ScanFilter.Builder()
                .setServiceUuid(ParcelUuid(TPMS_SERVICE_UUID))
                .build()
        )

        // Also filter by known MAC address for direct discovery
        _state.value.sensorConfigs.forEach { config ->
            filters.add(
                ScanFilter.Builder()
                    .setDeviceAddress(config.macAddress)
                    .build()
            )
        }

        val settings = ScanSettings.Builder()
            .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
            .setReportDelay(0)
            .build()

        try {
            scanner.startScan(filters, settings, scanCallback)
            _state.value = _state.value.copy(
                isScanning = true,
                isBluetoothEnabled = true,
                errorMessage = null
            )
        } catch (e: Exception) {
            Log.e(TAG, "Failed to start scan", e)
            // Fallback: scan without filters (catches all BLE devices)
            try {
                scanner.startScan(scanCallback)
                _state.value = _state.value.copy(
                    isScanning = true,
                    isBluetoothEnabled = true,
                    errorMessage = null
                )
            } catch (e2: Exception) {
                Log.e(TAG, "Fallback scan also failed", e2)
                _state.value = _state.value.copy(
                    isScanning = false,
                    errorMessage = "Failed to start scan: ${e2.message}"
                )
            }
        }
    }

    /**
     * Stop scanning for TPMS sensors.
     */
    @SuppressLint("MissingPermission")
    fun stopScanning() {
        try {
            bleScanner?.stopScan(scanCallback)
        } catch (e: Exception) {
            Log.e(TAG, "Error stopping scan", e)
        }
        _state.value = _state.value.copy(isScanning = false)
        Log.d(TAG, "TPMS BLE scan stopped")
    }

    /**
     * Assign a discovered sensor to a specific wheel position.
     */
    fun assignSensorToPosition(macAddress: String, position: TyreWheelPosition) {
        val currentConfigs = _state.value.sensorConfigs.toMutableList()

        // Remove any existing assignment for this MAC or position
        currentConfigs.removeAll { it.macAddress == macAddress || it.wheelPosition == position }

        // Add new assignment
        currentConfigs.add(
            TpmsSensorConfig(
                macAddress = macAddress,
                wheelPosition = position,
                label = "TPMS Sensor (${position.shortLabel})"
            )
        )

        _state.value = _state.value.copy(sensorConfigs = currentConfigs)
        Log.d(TAG, "Assigned sensor $macAddress to ${position.label}")
    }

    /**
     * Remove sensor assignment.
     */
    fun removeSensor(macAddress: String) {
        val currentConfigs = _state.value.sensorConfigs.toMutableList()
        currentConfigs.removeAll { it.macAddress == macAddress }
        _state.value = _state.value.copy(sensorConfigs = currentConfigs)

        val currentSensors = _state.value.sensors.toMutableMap()
        currentSensors.remove(macAddress)
        _state.value = _state.value.copy(sensors = currentSensors)
    }

    /**
     * Add a new sensor MAC for scanning.
     */
    fun addSensor(macAddress: String, position: TyreWheelPosition = TyreWheelPosition.UNASSIGNED) {
        val formatted = macAddress.uppercase().trim()
        assignSensorToPosition(formatted, position)
    }

    // ══════════════════════════════════════════════════════════════
    // Internal: process scan results
    // ══════════════════════════════════════════════════════════════

    @SuppressLint("MissingPermission")
    private fun processScanResult(result: ScanResult) {
        val device = result.device
        val mac = device.address ?: return
        val rssi = result.rssi
        val scanRecord = result.scanRecord ?: return

        Log.d(TAG, "BLE device found: $mac (RSSI: $rssi)")

        // Try to parse manufacturer-specific data from the advertisement
        val sensorData = parseAdvertisementData(mac, scanRecord.bytes, rssi)

        if (sensorData != null) {
            // Update sensor data map
            val updatedSensors = _state.value.sensors.toMutableMap()
            updatedSensors[mac] = sensorData
            _state.value = _state.value.copy(
                sensors = updatedSensors,
                lastUpdateTime = System.currentTimeMillis()
            )

            // Update discovered sensors list
            val discovered = _discoveredSensors.value.toMutableList()
            discovered.removeAll { it.macAddress == mac }
            discovered.add(sensorData)
            _discoveredSensors.value = discovered

            Log.d(TAG, "TPMS Data → MAC=$mac, Pressure=${sensorData.pressurePsi} PSI, " +
                    "Temp=${sensorData.temperatureCelsius}°C, Battery=${sensorData.batteryPercent}%")
        } else {
            // Even if we can't parse, record discovery if it matches our known MACs
            if (_state.value.sensorConfigs.any { it.macAddress == mac }) {
                Log.d(TAG, "Known sensor $mac found but could not parse advertisement data")
                // Create a placeholder entry so the UI shows the sensor is connected
                val placeholder = TpmsSensorData(
                    macAddress = mac,
                    pressureKpa = 0f,
                    pressurePsi = 0f,
                    temperatureCelsius = 0f,
                    batteryPercent = -1,
                    rssi = rssi
                )
                val updatedSensors = _state.value.sensors.toMutableMap()
                if (!updatedSensors.containsKey(mac)) {
                    updatedSensors[mac] = placeholder
                    _state.value = _state.value.copy(sensors = updatedSensors)
                }
            }
        }
    }

    /**
     * Parse the raw BLE advertisement bytes to extract TPMS data.
     *
     * The manufacturer-specific data block (AD type 0xFF) contains:
     *   [0..5]   MAC address (6 bytes)
     *   [6..9]   Pressure × 100 in kPa (uint32 LE)
     *   [10..13] Temperature × 100 in °C (int32 LE)
     *   [14]     Battery percent
     *   [15]     Alarm flags
     *   [16..22] Reserved / checksum
     *
     * We scan through the raw advertisement for the 0xFF type marker,
     * then extract the payload.
     */
    private fun parseAdvertisementData(mac: String, rawBytes: ByteArray?, rssi: Int): TpmsSensorData? {
        if (rawBytes == null || rawBytes.isEmpty()) return null

        try {
            // Walk through AD structures to find manufacturer-specific data (type 0xFF)
            var offset = 0
            while (offset < rawBytes.size - 1) {
                val length = rawBytes[offset].toInt() and 0xFF
                if (length == 0) break
                if (offset + length >= rawBytes.size) break

                val type = rawBytes[offset + 1].toInt() and 0xFF

                if (type == 0xFF && length >= 16) {
                    // Found manufacturer-specific data
                    val dataStart = offset + 2  // Skip length + type bytes
                    val dataBytes = rawBytes.copyOfRange(dataStart, minOf(dataStart + length - 1, rawBytes.size))

                    return parseTpmsPayload(mac, dataBytes, rssi)
                }

                offset += length + 1
            }

            // Fallback: try to parse the entire raw bytes if the sensor format
            // doesn't strictly follow BLE AD structure
            if (rawBytes.size >= 20) {
                return parseTpmsPayload(mac, rawBytes, rssi)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error parsing advertisement data for $mac", e)
        }

        return null
    }

    /**
     * Parse the TPMS payload bytes after locating them in the advertisement.
     */
    private fun parseTpmsPayload(mac: String, data: ByteArray, rssi: Int): TpmsSensorData? {
        try {
            // We need at least 16 bytes (6 MAC + 4 pressure + 4 temp + 1 battery + 1 alarm)
            if (data.size < 16) return null

            // Check if the first 6 bytes match the MAC (optional validation)
            // Some sensors embed their MAC in the payload

            // Extract pressure (bytes 6-9, little-endian, unsigned, ×100 kPa)
            val pressureRaw = ((data[6].toInt() and 0xFF)) or
                    ((data[7].toInt() and 0xFF) shl 8) or
                    ((data[8].toInt() and 0xFF) shl 16) or
                    ((data[9].toInt() and 0xFF) shl 24)
            val pressureKpa = pressureRaw / 100f

            // Extract temperature (bytes 10-13, little-endian, signed, ×100 °C)
            val tempRaw = ((data[10].toInt() and 0xFF)) or
                    ((data[11].toInt() and 0xFF) shl 8) or
                    ((data[12].toInt() and 0xFF) shl 16) or
                    ((data[13].toInt() and 0xFF) shl 24)
            val tempCelsius = tempRaw / 100f

            // Battery (byte 14)
            val battery = data[14].toInt() and 0xFF

            // Alarm flags (byte 15)
            val alarmFlags = if (data.size > 15) (data[15].toInt() and 0xFF) else 0
            val lowPressure = (alarmFlags and 0x01) != 0
            val highTemp = (alarmFlags and 0x02) != 0
            val rapidLeak = (alarmFlags and 0x04) != 0

            // Sanity checks - pressure should be roughly 0-500 kPa (0-72 PSI)
            // Temperature should be -40°C to 120°C
            val validPressure = pressureKpa in 0f..600f
            val validTemp = tempCelsius in -50f..150f

            if (!validPressure && !validTemp) {
                // Try alternate parsing: some Treel sensors use different byte order
                return parseTreelAlternateFormat(mac, data, rssi)
            }

            val pressurePsi = if (validPressure) pressureKpa * KPA_TO_PSI else 0f
            val finalTemp = if (validTemp) tempCelsius else 0f

            return TpmsSensorData(
                macAddress = mac,
                pressureKpa = if (validPressure) pressureKpa else 0f,
                pressurePsi = pressurePsi,
                temperatureCelsius = finalTemp,
                batteryPercent = if (battery in 0..100) battery else -1,
                isLowPressure = lowPressure,
                isHighTemperature = highTemp,
                isRapidLeak = rapidLeak,
                rssi = rssi
            )
        } catch (e: Exception) {
            Log.e(TAG, "Error parsing TPMS payload", e)
            return null
        }
    }

    /**
     * Alternate parsing for Treel sensors that may use a different data layout.
     * Some Treel sensors send: [ID(4)] [Pressure(2)] [Temp(2)] [Battery(1)] [Status(1)]
     */
    private fun parseTreelAlternateFormat(mac: String, data: ByteArray, rssi: Int): TpmsSensorData? {
        try {
            if (data.size < 10) return null

            // Skip first 4 bytes (sensor ID), then:
            // Pressure: 2 bytes (big-endian), value in kPa × 10
            val pressureRaw = ((data[4].toInt() and 0xFF) shl 8) or (data[5].toInt() and 0xFF)
            val pressureKpa = pressureRaw / 10f

            // Temperature: 2 bytes (big-endian), value in °C × 10, offset by 40
            val tempRaw = ((data[6].toInt() and 0xFF) shl 8) or (data[7].toInt() and 0xFF)
            val tempCelsius = (tempRaw / 10f) - 40f

            // Battery: 1 byte (percentage)
            val battery = data[8].toInt() and 0xFF

            // Status/alarm: 1 byte
            val status = if (data.size > 9) (data[9].toInt() and 0xFF) else 0

            val validPressure = pressureKpa in 0f..600f
            val validTemp = tempCelsius in -50f..150f

            if (!validPressure && !validTemp) return null

            return TpmsSensorData(
                macAddress = mac,
                pressureKpa = if (validPressure) pressureKpa else 0f,
                pressurePsi = if (validPressure) pressureKpa * KPA_TO_PSI else 0f,
                temperatureCelsius = if (validTemp) tempCelsius else 0f,
                batteryPercent = if (battery in 0..100) battery else -1,
                isLowPressure = (status and 0x01) != 0,
                isHighTemperature = (status and 0x02) != 0,
                isRapidLeak = (status and 0x04) != 0,
                rssi = rssi
            )
        } catch (e: Exception) {
            Log.e(TAG, "Error parsing Treel alternate format", e)
            return null
        }
    }

    /**
     * Clean up resources.
     */
    fun destroy() {
        stopScanning()
        instance = null
    }
}
