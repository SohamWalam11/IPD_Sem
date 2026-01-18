package org.example.project.auth

import android.content.Context
import android.os.Build
import android.util.Log
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricManager.Authenticators.BIOMETRIC_STRONG
import androidx.biometric.BiometricManager.Authenticators.BIOMETRIC_WEAK
import androidx.biometric.BiometricManager.Authenticators.DEVICE_CREDENTIAL
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Result sealed class for biometric authentication
 */
sealed class BiometricAuthResult {
    object Success : BiometricAuthResult()
    object NotAvailable : BiometricAuthResult()
    object NotEnrolled : BiometricAuthResult()
    object HardwareUnavailable : BiometricAuthResult()
    object SecurityUpdateRequired : BiometricAuthResult()
    data class Error(val errorCode: Int, val message: String) : BiometricAuthResult()
    object Cancelled : BiometricAuthResult()
    object Failed : BiometricAuthResult()
}

/**
 * Biometric capability status
 */
enum class BiometricCapability {
    AVAILABLE,
    NOT_AVAILABLE,
    NOT_ENROLLED,
    HARDWARE_UNAVAILABLE,
    SECURITY_UPDATE_REQUIRED
}

/**
 * Modern Biometric Authentication Manager using Jetpack Biometric library
 * 
 * Supports unified biometric credentials (face, fingerprint, iris) as per Android 2026 standards.
 * Uses BiometricPrompt which automatically shows the appropriate interface based on
 * what the user has enrolled in their device settings.
 * 
 * @param context Application context
 */
class BiometricAuthManager(private val context: Context) {
    
    private val biometricManager = BiometricManager.from(context)
    
    // Authentication result channel for coroutine-based API
    private val authResultChannel = Channel<BiometricAuthResult>(Channel.BUFFERED)
    
    // State flow for observing biometric availability
    private val _biometricState = MutableStateFlow(checkBiometricCapability())
    val biometricState: StateFlow<BiometricCapability> = _biometricState.asStateFlow()
    
    // Allowed authenticators - prefer strong biometrics, fallback to weak + device credential
    private val authenticators = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
        BIOMETRIC_STRONG or BIOMETRIC_WEAK or DEVICE_CREDENTIAL
    } else {
        BIOMETRIC_STRONG or BIOMETRIC_WEAK
    }
    
    companion object {
        private const val TAG = "BiometricAuthManager"
        
        // Shared preferences key for biometric enrollment status
        private const val PREFS_NAME = "tyreguard_biometric_prefs"
        private const val KEY_BIOMETRIC_ENABLED = "biometric_enabled"
        private const val KEY_USER_ID = "biometric_user_id"
    }
    
    /**
     * Check if biometric authentication is available on this device
     */
    fun checkBiometricCapability(): BiometricCapability {
        return when (biometricManager.canAuthenticate(authenticators)) {
            BiometricManager.BIOMETRIC_SUCCESS -> BiometricCapability.AVAILABLE
            BiometricManager.BIOMETRIC_ERROR_NO_HARDWARE -> BiometricCapability.NOT_AVAILABLE
            BiometricManager.BIOMETRIC_ERROR_HW_UNAVAILABLE -> BiometricCapability.HARDWARE_UNAVAILABLE
            BiometricManager.BIOMETRIC_ERROR_NONE_ENROLLED -> BiometricCapability.NOT_ENROLLED
            BiometricManager.BIOMETRIC_ERROR_SECURITY_UPDATE_REQUIRED -> BiometricCapability.SECURITY_UPDATE_REQUIRED
            else -> BiometricCapability.NOT_AVAILABLE
        }
    }
    
    /**
     * Check if biometric authentication is enabled for this user
     */
    fun isBiometricEnabled(): Boolean {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getBoolean(KEY_BIOMETRIC_ENABLED, false)
    }
    
    /**
     * Get the stored user ID for biometric authentication
     */
    fun getStoredUserId(): String? {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getString(KEY_USER_ID, null)
    }
    
    /**
     * Enable biometric authentication for the given user
     */
    fun enableBiometric(userId: String) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit()
            .putBoolean(KEY_BIOMETRIC_ENABLED, true)
            .putString(KEY_USER_ID, userId)
            .apply()
        Log.d(TAG, "Biometric enabled for user: $userId")
    }
    
    /**
     * Disable biometric authentication
     */
    fun disableBiometric() {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit()
            .putBoolean(KEY_BIOMETRIC_ENABLED, false)
            .remove(KEY_USER_ID)
            .apply()
        Log.d(TAG, "Biometric disabled")
    }
    
    /**
     * Check if we can attempt biometric sign-in
     * Returns true if biometric is available, enabled, and user ID is stored
     */
    fun canAttemptBiometricSignIn(): Boolean {
        return checkBiometricCapability() == BiometricCapability.AVAILABLE 
            && isBiometricEnabled() 
            && getStoredUserId() != null
    }
    
    /**
     * Authenticate using biometrics with modern callback-based API
     * 
     * @param activity FragmentActivity required for BiometricPrompt
     * @param title Title shown in the biometric prompt
     * @param subtitle Subtitle shown in the biometric prompt
     * @param description Optional description
     * @param negativeButtonText Text for the negative/cancel button
     * @param onResult Callback with authentication result
     */
    fun authenticate(
        activity: FragmentActivity,
        title: String = "Biometric Sign In",
        subtitle: String = "Use your biometric credential to sign in",
        description: String? = "TyreGuard uses your device's biometric security for quick and secure sign-in",
        negativeButtonText: String = "Use Password",
        onResult: (BiometricAuthResult) -> Unit
    ) {
        // Check capability first
        when (val capability = checkBiometricCapability()) {
            BiometricCapability.NOT_AVAILABLE -> {
                onResult(BiometricAuthResult.NotAvailable)
                return
            }
            BiometricCapability.NOT_ENROLLED -> {
                onResult(BiometricAuthResult.NotEnrolled)
                return
            }
            BiometricCapability.HARDWARE_UNAVAILABLE -> {
                onResult(BiometricAuthResult.HardwareUnavailable)
                return
            }
            BiometricCapability.SECURITY_UPDATE_REQUIRED -> {
                onResult(BiometricAuthResult.SecurityUpdateRequired)
                return
            }
            BiometricCapability.AVAILABLE -> {
                // Continue with authentication
            }
        }
        
        val executor = ContextCompat.getMainExecutor(context)
        
        val authCallback = object : BiometricPrompt.AuthenticationCallback() {
            override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                super.onAuthenticationSucceeded(result)
                Log.d(TAG, "Biometric authentication succeeded. Type: ${result.authenticationType}")
                onResult(BiometricAuthResult.Success)
            }
            
            override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                super.onAuthenticationError(errorCode, errString)
                Log.e(TAG, "Biometric authentication error: $errorCode - $errString")
                
                val result = when (errorCode) {
                    BiometricPrompt.ERROR_CANCELED,
                    BiometricPrompt.ERROR_USER_CANCELED,
                    BiometricPrompt.ERROR_NEGATIVE_BUTTON -> BiometricAuthResult.Cancelled
                    BiometricPrompt.ERROR_NO_BIOMETRICS -> BiometricAuthResult.NotEnrolled
                    BiometricPrompt.ERROR_HW_NOT_PRESENT,
                    BiometricPrompt.ERROR_HW_UNAVAILABLE -> BiometricAuthResult.HardwareUnavailable
                    BiometricPrompt.ERROR_LOCKOUT,
                    BiometricPrompt.ERROR_LOCKOUT_PERMANENT -> BiometricAuthResult.Error(
                        errorCode, 
                        "Too many attempts. Please try again later or use password."
                    )
                    else -> BiometricAuthResult.Error(errorCode, errString.toString())
                }
                onResult(result)
            }
            
            override fun onAuthenticationFailed() {
                super.onAuthenticationFailed()
                Log.w(TAG, "Biometric authentication failed (fingerprint/face not recognized)")
                // Don't call onResult here - the prompt stays open for retry
                // onResult is only called on success, error, or cancellation
            }
        }
        
        val biometricPrompt = BiometricPrompt(activity, executor, authCallback)
        
        // Build prompt info based on API level
        val promptInfoBuilder = BiometricPrompt.PromptInfo.Builder()
            .setTitle(title)
            .setSubtitle(subtitle)
            .setConfirmationRequired(false) // Don't require explicit confirmation after biometric
        
        description?.let { promptInfoBuilder.setDescription(it) }
        
        // Configure authenticators based on API level
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            // API 30+: Use DEVICE_CREDENTIAL as fallback, don't set negative button
            promptInfoBuilder.setAllowedAuthenticators(
                BIOMETRIC_STRONG or BIOMETRIC_WEAK or DEVICE_CREDENTIAL
            )
        } else {
            // Pre-API 30: Must set negative button when not using DEVICE_CREDENTIAL
            promptInfoBuilder
                .setAllowedAuthenticators(BIOMETRIC_STRONG or BIOMETRIC_WEAK)
                .setNegativeButtonText(negativeButtonText)
        }
        
        try {
            val promptInfo = promptInfoBuilder.build()
            biometricPrompt.authenticate(promptInfo)
            Log.d(TAG, "Biometric prompt shown")
        } catch (e: Exception) {
            Log.e(TAG, "Error showing biometric prompt", e)
            onResult(BiometricAuthResult.Error(-1, e.message ?: "Unknown error"))
        }
    }
    
    /**
     * Suspend function for authenticate - useful for coroutines
     */
    suspend fun authenticateSuspend(
        activity: FragmentActivity,
        title: String = "Biometric Sign In",
        subtitle: String = "Use your biometric credential to sign in",
        description: String? = null,
        negativeButtonText: String = "Use Password"
    ): BiometricAuthResult {
        val channel = Channel<BiometricAuthResult>(Channel.RENDEZVOUS)
        
        authenticate(
            activity = activity,
            title = title,
            subtitle = subtitle,
            description = description,
            negativeButtonText = negativeButtonText
        ) { result ->
            channel.trySend(result)
        }
        
        return channel.receive()
    }
    
    /**
     * Get a user-friendly description of the biometric capability
     */
    fun getBiometricDescription(): String {
        return when (checkBiometricCapability()) {
            BiometricCapability.AVAILABLE -> "Biometric authentication is available"
            BiometricCapability.NOT_AVAILABLE -> "Biometric authentication is not available on this device"
            BiometricCapability.NOT_ENROLLED -> "No biometric credentials enrolled. Please set up fingerprint or face unlock in device settings."
            BiometricCapability.HARDWARE_UNAVAILABLE -> "Biometric hardware is currently unavailable"
            BiometricCapability.SECURITY_UPDATE_REQUIRED -> "A security update is required to use biometric authentication"
        }
    }
}
