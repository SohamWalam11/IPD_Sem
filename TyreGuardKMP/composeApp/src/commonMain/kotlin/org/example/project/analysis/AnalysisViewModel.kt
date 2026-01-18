package org.example.project.analysis

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlin.random.Random

/**
 * ViewModel for the Analysis screen tabs (Report, History, Telemetry).
 * Manages tire status data and live telemetry simulation.
 */
class AnalysisViewModel : ViewModel() {
    
    // Report Tab State
    private val _reportState = MutableStateFlow(ReportState())
    val reportState: StateFlow<ReportState> = _reportState.asStateFlow()
    
    // Telemetry Tab State
    private val _telemetryState = MutableStateFlow(TelemetryState())
    val telemetryState: StateFlow<TelemetryState> = _telemetryState.asStateFlow()
    
    // Live simulation job
    private var simulationJob: Job? = null
    
    init {
        // Start with some initial data points
        _telemetryState.value = _telemetryState.value.copy(
            pressureHistory = listOf(32.1f, 32.3f, 32.2f, 32.4f, 32.3f)
        )
    }
    
    /**
     * Start live telemetry simulation.
     */
    fun startLiveSimulation() {
        if (simulationJob?.isActive == true) return
        
        simulationJob = viewModelScope.launch {
            _telemetryState.value = _telemetryState.value.copy(isLive = true)
            
            while (true) {
                delay(500L)
                
                val currentHistory = _telemetryState.value.pressureHistory.toMutableList()
                // Generate random pressure between 32.0 and 32.5 PSI
                val newPressure = 32.0f + Random.nextFloat() * 0.5f
                
                // Keep only last 20 data points
                if (currentHistory.size >= 20) {
                    currentHistory.removeAt(0)
                }
                currentHistory.add(newPressure)
                
                _telemetryState.value = _telemetryState.value.copy(
                    pressureHistory = currentHistory.toList()
                )
            }
        }
    }
    
    /**
     * Stop live telemetry simulation.
     */
    fun stopLiveSimulation() {
        simulationJob?.cancel()
        simulationJob = null
        _telemetryState.value = _telemetryState.value.copy(isLive = false)
    }
    
    /**
     * Select a tire for telemetry display.
     */
    fun selectTire(position: TirePosition) {
        _telemetryState.value = _telemetryState.value.copy(
            selectedTire = position,
            pressureHistory = listOf(32.1f, 32.3f, 32.2f, 32.4f, 32.3f) // Reset history
        )
    }
    
    /**
     * Refresh report data (simulates fetching from sensors).
     */
    fun refreshReport() {
        viewModelScope.launch {
            _reportState.value = _reportState.value.copy(isLoading = true)
            delay(1000L) // Simulate network/sensor delay
            
            // Generate slightly varied data
            _reportState.value = ReportState(
                tires = listOf(
                    TireStatus(
                        TirePosition.FRONT_LEFT,
                        32.0f + Random.nextFloat() * 1f,
                        27.0f + Random.nextFloat() * 3f,
                        TreadHealth.GOOD
                    ),
                    TireStatus(
                        TirePosition.FRONT_RIGHT,
                        31.5f + Random.nextFloat() * 1f,
                        27.0f + Random.nextFloat() * 3f,
                        TreadHealth.EXCELLENT
                    ),
                    TireStatus(
                        TirePosition.REAR_LEFT,
                        32.5f + Random.nextFloat() * 1f,
                        28.0f + Random.nextFloat() * 3f,
                        TreadHealth.FAIR
                    ),
                    TireStatus(
                        TirePosition.REAR_RIGHT,
                        26.0f + Random.nextFloat() * 1f,
                        30.0f + Random.nextFloat() * 3f,
                        TreadHealth.WORN,
                        listOf("Crack Detected", "Bulge")
                    )
                ),
                isLoading = false,
                lastUpdated = System.currentTimeMillis()
            )
        }
    }
    
    override fun onCleared() {
        super.onCleared()
        stopLiveSimulation()
    }
}
