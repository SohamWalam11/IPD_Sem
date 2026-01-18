package org.example.project.ar

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import com.google.ar.core.ArCoreApk
import com.google.ar.core.exceptions.UnavailableException

/**
 * ═══════════════════════════════════════════════════════════════════════════════
 * TyreArActivity - AR-enabled Activity for Tyre Health Visualization
 * ═══════════════════════════════════════════════════════════════════════════════
 * 
 * This Activity hosts the AR experience for visualizing tyre health data in 3D.
 * Features:
 * - ARCore session management with proper lifecycle handling
 * - Plane detection for surface anchoring
 * - 3D tyre model placement
 * - Floating spatial UI panels for real-time data
 * 
 * Architecture:
 * - Activity handles ARCore availability check and installation prompts
 * - Compose handles all UI rendering via ARSceneView
 */
class TyreArActivity : ComponentActivity() {
    
    // Track if we need to request ARCore installation
    private var userRequestedInstall = true
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Get optional model path from intent
        val modelPath = intent.getStringExtra(EXTRA_MODEL_PATH)
        
        // Check ARCore availability before setting content
        if (!checkArCoreAvailability()) {
            return // Activity will finish if AR not supported
        }
        
        setContent {
            MaterialTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    ArTyreScreen(
                        modelPath = modelPath,
                        onBackPressed = { finish() }
                    )
                }
            }
        }
    }
    
    override fun onResume() {
        super.onResume()
        
        // Request ARCore installation if needed
        try {
            when (ArCoreApk.getInstance().requestInstall(this, userRequestedInstall)) {
                ArCoreApk.InstallStatus.INSTALLED -> {
                    // ARCore is ready
                }
                ArCoreApk.InstallStatus.INSTALL_REQUESTED -> {
                    // ARCore installation was requested, onResume will be called again
                    userRequestedInstall = false
                    return
                }
            }
        } catch (e: UnavailableException) {
            handleArCoreException(e)
        }
    }
    
    /**
     * Check if ARCore is available on this device
     */
    private fun checkArCoreAvailability(): Boolean {
        return try {
            when (ArCoreApk.getInstance().checkAvailability(this)) {
                ArCoreApk.Availability.SUPPORTED_INSTALLED -> true
                ArCoreApk.Availability.SUPPORTED_APK_TOO_OLD,
                ArCoreApk.Availability.SUPPORTED_NOT_INSTALLED -> {
                    // ARCore is supported but needs installation/update
                    true
                }
                ArCoreApk.Availability.UNSUPPORTED_DEVICE_NOT_CAPABLE -> {
                    Toast.makeText(
                        this,
                        "This device does not support AR",
                        Toast.LENGTH_LONG
                    ).show()
                    finish()
                    false
                }
                ArCoreApk.Availability.UNKNOWN_CHECKING,
                ArCoreApk.Availability.UNKNOWN_ERROR,
                ArCoreApk.Availability.UNKNOWN_TIMED_OUT -> {
                    Toast.makeText(
                        this,
                        "Unable to check AR availability",
                        Toast.LENGTH_SHORT
                    ).show()
                    true // Continue anyway, will fail gracefully if unsupported
                }
            }
        } catch (e: Exception) {
            Toast.makeText(this, "AR check failed: ${e.message}", Toast.LENGTH_SHORT).show()
            finish()
            false
        }
    }
    
    /**
     * Handle ARCore exceptions with user-friendly messages
     */
    private fun handleArCoreException(e: UnavailableException) {
        val message = when (e) {
            is com.google.ar.core.exceptions.UnavailableArcoreNotInstalledException ->
                "Please install ARCore from Play Store"
            is com.google.ar.core.exceptions.UnavailableApkTooOldException ->
                "Please update ARCore from Play Store"
            is com.google.ar.core.exceptions.UnavailableSdkTooOldException ->
                "Please update this app"
            is com.google.ar.core.exceptions.UnavailableDeviceNotCompatibleException ->
                "This device does not support AR"
            else -> "AR is not available: ${e.message}"
        }
        Toast.makeText(this, message, Toast.LENGTH_LONG).show()
        finish()
    }
    
    companion object {
        const val EXTRA_MODEL_PATH = "extra_model_path"
    }
}
