package org.example.project.ar

import android.content.Context
import android.content.Intent
import com.google.ar.core.ArCoreApk

/**
 * ═══════════════════════════════════════════════════════════════════════════════
 * ArLauncher - Utility for launching AR experiences
 * ═══════════════════════════════════════════════════════════════════════════════
 * 
 * Provides a clean API for starting the AR tyre viewer from anywhere in the app.
 * Handles AR availability checking before launching.
 */
object ArLauncher {
    
    /**
     * Check if AR is available on this device
     * @return true if AR is supported (may need installation)
     */
    fun isArAvailable(context: Context): Boolean {
        return try {
            when (ArCoreApk.getInstance().checkAvailability(context)) {
                ArCoreApk.Availability.SUPPORTED_INSTALLED,
                ArCoreApk.Availability.SUPPORTED_APK_TOO_OLD,
                ArCoreApk.Availability.SUPPORTED_NOT_INSTALLED -> true
                else -> false
            }
        } catch (e: Exception) {
            false
        }
    }
    
    /**
     * Check if AR is ready to use (installed and up-to-date)
     */
    fun isArReady(context: Context): Boolean {
        return try {
            ArCoreApk.getInstance().checkAvailability(context) == 
                ArCoreApk.Availability.SUPPORTED_INSTALLED
        } catch (e: Exception) {
            false
        }
    }
    
    /**
     * Launch the AR Tyre Viewer activity
     * @param context Android context
     * @param modelPath Optional path to a custom GLB model file
     */
    fun launchArViewer(context: Context, modelPath: String? = null) {
        val intent = Intent(context, TyreArActivity::class.java).apply {
            modelPath?.let { putExtra(TyreArActivity.EXTRA_MODEL_PATH, it) }
        }
        context.startActivity(intent)
    }
    
    /**
     * Create an intent for the AR Tyre Viewer
     * Useful when you need more control over how the activity is started
     */
    fun createArViewerIntent(context: Context, modelPath: String? = null): Intent {
        return Intent(context, TyreArActivity::class.java).apply {
            modelPath?.let { putExtra(TyreArActivity.EXTRA_MODEL_PATH, it) }
        }
    }
}
