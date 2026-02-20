package org.example.project.ble

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * ViewModel for TPMS BLE sensor management.
 * Bridges TpmsBluetoothManager data into the UI layer
 * and manages pressure history for live telemetry charts.
 */
class TpmsViewModel(
    private val tpmsManager: TpmsBluetoothManager
) : ViewModel() {

    val tpmsState: StateFlow<TpmsState> = tpmsManager.state
    val discoveredSensors = tpmsManager.discoveredSensors

    // Pressure history for each position (for telemetry charts)
    private val _pressureHistories = MutableStateFlow<Map<TyreWheelPosition, List<Float>>>(emptyMap())
    val pressureHistories: StateFlow<Map<TyreWheelPosition, List<Float>>> = _pressureHistories.asStateFlow()

    // Temperature history
    private val _temperatureHistories = MutableStateFlow<Map<TyreWheelPosition, List<Float>>>(emptyMap())
    val temperatureHistories: StateFlow<Map<TyreWheelPosition, List<Float>>> = _temperatureHistories.asStateFlow()

    private var historyJob: Job? = null

    init {
        startHistoryCollection()
    }

    /**
     * Periodically sample sensor data for history charts.
     */
    private fun startHistoryCollection() {
        historyJob?.cancel()
        historyJob = viewModelScope.launch {
            while (true) {
                delay(2000L) // Sample every 2 seconds
                val positionData = tpmsManager.state.value.getAllPositionData()
                if (positionData.isNotEmpty()) {
                    val pressures = _pressureHistories.value.toMutableMap()
                    val temps = _temperatureHistories.value.toMutableMap()

                    positionData.forEach { (position, data) ->
                        if (data.pressurePsi > 0) {
                            val pHistory = (pressures[position] ?: emptyList()).toMutableList()
                            pHistory.add(data.pressurePsi)
                            if (pHistory.size > 30) pHistory.removeAt(0) // Keep last 30 samples (60 sec)
                            pressures[position] = pHistory
                        }
                        if (data.temperatureCelsius > 0) {
                            val tHistory = (temps[position] ?: emptyList()).toMutableList()
                            tHistory.add(data.temperatureCelsius)
                            if (tHistory.size > 30) tHistory.removeAt(0)
                            temps[position] = tHistory
                        }
                    }

                    _pressureHistories.value = pressures
                    _temperatureHistories.value = temps
                }
            }
        }
    }

    fun startScanning() = tpmsManager.startScanning()
    fun stopScanning() = tpmsManager.stopScanning()
    fun isBluetoothEnabled() = tpmsManager.isBluetoothEnabled()
    fun assignSensor(mac: String, position: TyreWheelPosition) =
        tpmsManager.assignSensorToPosition(mac, position)
    fun removeSensor(mac: String) = tpmsManager.removeSensor(mac)
    fun addSensor(mac: String, position: TyreWheelPosition) = tpmsManager.addSensor(mac, position)

    override fun onCleared() {
        super.onCleared()
        historyJob?.cancel()
        tpmsManager.stopScanning()
    }

    /**
     * Factory for creating TpmsViewModel with context.
     */
    class Factory(private val context: Context) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return TpmsViewModel(TpmsBluetoothManager.getInstance(context)) as T
        }
    }
}
