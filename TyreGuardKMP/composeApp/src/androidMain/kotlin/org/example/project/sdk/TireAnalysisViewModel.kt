package org.example.project.sdk

import android.content.Context
import android.graphics.Bitmap
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * ViewModel for Comprehensive Tire Analysis Screen.
 * Manages the analysis workflow and state for UI.
 */
class TireAnalysisViewModel : ViewModel() {

    private var analysisService: ComprehensiveTireAnalysisService? = null

    // UI State
    private val _uiState = MutableStateFlow(TireAnalysisUiState())
    val uiState: StateFlow<TireAnalysisUiState> = _uiState.asStateFlow()

    // Captured images
    private var treadImage: Bitmap? = null
    private var sidewallImage: Bitmap? = null

    /**
     * Initialize the analysis service.
     */
    fun initialize(context: Context) {
        if (analysisService != null) return

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isInitializing = true)

            analysisService = ComprehensiveTireAnalysisService(context)
            val result = analysisService?.initialize()

            _uiState.value = _uiState.value.copy(
                isInitializing = false,
                isReady = result?.isSuccess == true,
                error = result?.exceptionOrNull()?.message
            )
        }
    }

    /**
     * Set the captured tread image.
     */
    fun setTreadImage(bitmap: Bitmap) {
        treadImage = bitmap
        _uiState.value = _uiState.value.copy(
            hasTreadImage = true,
            canStartAnalysis = true
        )
    }

    /**
     * Set the captured sidewall image (optional).
     */
    fun setSidewallImage(bitmap: Bitmap) {
        sidewallImage = bitmap
        _uiState.value = _uiState.value.copy(hasSidewallImage = true)
    }

    /**
     * Clear captured images.
     */
    fun clearImages() {
        treadImage?.recycle()
        sidewallImage?.recycle()
        treadImage = null
        sidewallImage = null
        _uiState.value = _uiState.value.copy(
            hasTreadImage = false,
            hasSidewallImage = false,
            canStartAnalysis = false,
            analysisResult = null
        )
    }

    /**
     * Start comprehensive tire analysis.
     */
    fun startAnalysis() {
        val tread = treadImage ?: return
        val service = analysisService ?: return

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                isAnalyzing = true,
                analysisProgress = 0f,
                analysisStep = "Initializing analysis..."
            )

            try {
                // Update progress
                updateProgress(0.1f, "Measuring tread depth...")

                val result = service.analyzeTire(tread, sidewallImage)

                updateProgress(0.9f, "Generating recommendations...")

                if (result.isSuccess) {
                    _uiState.value = _uiState.value.copy(
                        isAnalyzing = false,
                        analysisProgress = 1f,
                        analysisStep = "Complete",
                        analysisResult = result.getOrNull(),
                        error = null
                    )
                } else {
                    _uiState.value = _uiState.value.copy(
                        isAnalyzing = false,
                        error = result.exceptionOrNull()?.message ?: "Analysis failed"
                    )
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isAnalyzing = false,
                    error = e.message ?: "Unknown error"
                )
            }
        }
    }

    /**
     * Quick defect scan (faster, less comprehensive).
     */
    fun quickScan() {
        val tread = treadImage ?: return
        val service = analysisService ?: return

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                isAnalyzing = true,
                analysisStep = "Scanning for defects..."
            )

            try {
                val defects = service.quickDefectScan(tread)

                _uiState.value = _uiState.value.copy(
                    isAnalyzing = false,
                    quickScanDefects = defects
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isAnalyzing = false,
                    error = e.message
                )
            }
        }
    }

    /**
     * Measure tread depth only.
     */
    fun measureTreadOnly() {
        val tread = treadImage ?: return
        val service = analysisService ?: return

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                isAnalyzing = true,
                analysisStep = "Measuring tread depth..."
            )

            try {
                val result = service.measureTreadDepth(tread)

                _uiState.value = _uiState.value.copy(
                    isAnalyzing = false,
                    treadDepthResult = result.getOrNull()
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isAnalyzing = false,
                    error = e.message
                )
            }
        }
    }

    /**
     * Generate heat map from last tread scan.
     */
    fun generateHeatMap(): TreadHeatMap? {
        val treadResult = _uiState.value.treadDepthResult
            ?: _uiState.value.analysisResult?.treadDepth
            ?: return null

        return analysisService?.generateTreadHeatMap(treadResult)
    }

    private fun updateProgress(progress: Float, step: String) {
        _uiState.value = _uiState.value.copy(
            analysisProgress = progress,
            analysisStep = step
        )
    }

    fun clearError() {
        _uiState.value = _uiState.value.copy(error = null)
    }

    override fun onCleared() {
        super.onCleared()
        analysisService?.release()
        treadImage?.recycle()
        sidewallImage?.recycle()
    }
}

/**
 * UI State for Tire Analysis Screen.
 */
data class TireAnalysisUiState(
    // Initialization
    val isInitializing: Boolean = false,
    val isReady: Boolean = false,

    // Image capture
    val hasTreadImage: Boolean = false,
    val hasSidewallImage: Boolean = false,
    val canStartAnalysis: Boolean = false,

    // Analysis progress
    val isAnalyzing: Boolean = false,
    val analysisProgress: Float = 0f,
    val analysisStep: String = "",

    // Results
    val analysisResult: ComprehensiveTireAnalysis? = null,
    val treadDepthResult: TreadDepthResult? = null,
    val quickScanDefects: List<TireDefect>? = null,

    // Error
    val error: String? = null
)

