package org.example.project.auth

import android.content.Context
import android.util.Log
import androidx.credentials.ClearCredentialStateRequest
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.GetCredentialResponse
import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.credentials.exceptions.GetCredentialException
import androidx.credentials.exceptions.NoCredentialException
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.android.libraries.identity.googleid.GoogleIdTokenParsingException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.example.project.BuildConfig
import org.example.project.data.UserDataRepository
import java.security.MessageDigest
import java.util.UUID

/**
 * ═══════════════════════════════════════════════════════════════════════════════
 * GoogleAuthManager - Production-ready Google Sign-In using Credential Manager
 * ═══════════════════════════════════════════════════════════════════════════════
 * 
 * This implementation uses Android's Credential Manager API (stable since Android 14)
 * which is the recommended replacement for the legacy Google Sign-In API.
 * 
 * Features:
 * - Automatic sign-in for returning users
 * - Bottom sheet UI via Credential Manager
 * - One Tap sign-in experience
 * - Secure token handling with nonce
 * 
 * SETUP REQUIRED:
 * 1. Create a project in Google Cloud Console
 * 2. Configure OAuth 2.0 credentials (Web client type for Android)
 * 3. Add SHA-1 fingerprint of your signing key
 * 4. Add WEB_CLIENT_ID to .env file in the project root
 */
class GoogleAuthManager(private val context: Context) {
    
    companion object {
        private const val TAG = "GoogleAuthManager"
        
        // WEB_CLIENT_ID is loaded from .env file via BuildConfig
        private val WEB_CLIENT_ID = BuildConfig.WEB_CLIENT_ID
    }
    
    // Credential Manager instance - the modern way to handle authentication
    private val credentialManager = CredentialManager.create(context)
    
    // Authentication state observable
    private val _authState = MutableStateFlow<GoogleAuthState>(GoogleAuthState.Idle)
    val authState: StateFlow<GoogleAuthState> = _authState.asStateFlow()
    
    // Current authenticated user
    private val _currentUser = MutableStateFlow<GoogleUser?>(null)
    val currentUser: StateFlow<GoogleUser?> = _currentUser.asStateFlow()
    
    /**
     * Attempt automatic sign-in for returning users.
     * This should be called when the app starts to check for saved credentials.
     * 
     * @param activityContext Must be an Activity context for the credential picker UI
     * @return GoogleAuthResult indicating success, error, or no saved credentials
     */
    suspend fun attemptAutoSignIn(activityContext: Context): GoogleAuthResult {
        _authState.value = GoogleAuthState.Loading
        
        return try {
            // Generate a nonce for security
            val rawNonce = UUID.randomUUID().toString()
            val hashedNonce = hashNonce(rawNonce)
            
            // Configure for auto sign-in - only show previously authorized accounts
            val googleIdOption = GetGoogleIdOption.Builder()
                .setFilterByAuthorizedAccounts(true)  // Only show saved accounts
                .setServerClientId(WEB_CLIENT_ID)
                .setAutoSelectEnabled(true)           // Enable automatic sign-in
                .setNonce(hashedNonce)
                .build()
            
            val request = GetCredentialRequest.Builder()
                .addCredentialOption(googleIdOption)
                .build()
            
            val result = credentialManager.getCredential(
                request = request,
                context = activityContext
            )
            
            handleSignInResponse(result)
        } catch (e: NoCredentialException) {
            // No saved credentials - user needs to sign in manually
            Log.d(TAG, "No saved credentials found for auto sign-in")
            _authState.value = GoogleAuthState.Idle
            GoogleAuthResult.NoSavedCredentials
        } catch (e: GetCredentialCancellationException) {
            Log.d(TAG, "Auto sign-in cancelled")
            _authState.value = GoogleAuthState.Idle
            GoogleAuthResult.Cancelled
        } catch (e: GetCredentialException) {
            Log.e(TAG, "Auto sign-in failed: ${e.message}", e)
            _authState.value = GoogleAuthState.Idle
            GoogleAuthResult.Error(e.message ?: "Auto sign-in failed")
        } catch (e: Exception) {
            Log.e(TAG, "Unexpected error during auto sign-in: ${e.message}", e)
            _authState.value = GoogleAuthState.Idle
            GoogleAuthResult.Error(e.message ?: "Unexpected error")
        }
    }
    
    /**
     * Initiates Google Sign-In flow with the bottom sheet UI.
     * Shows all Google accounts on device for user selection.
     * 
     * @param activityContext Must be an Activity context for the credential picker UI
     */
    suspend fun signIn(activityContext: Context): GoogleAuthResult {
        _authState.value = GoogleAuthState.Loading
        
        return try {
            // Generate a nonce for security - prevents replay attacks
            val rawNonce = UUID.randomUUID().toString()
            val hashedNonce = hashNonce(rawNonce)
            
            // Configure Google ID request - show all accounts (not filtered)
            val googleIdOption = GetGoogleIdOption.Builder()
                .setFilterByAuthorizedAccounts(false) // Show all accounts
                .setServerClientId(WEB_CLIENT_ID)
                .setAutoSelectEnabled(false)          // Let user choose
                .setNonce(hashedNonce)
                .build()
            
            // Build the credential request
            val request = GetCredentialRequest.Builder()
                .addCredentialOption(googleIdOption)
                .build()
            
            // Launch the Credential Manager bottom sheet
            val result = credentialManager.getCredential(
                request = request,
                context = activityContext
            )
            
            // Process the response
            handleSignInResponse(result)
        } catch (e: GetCredentialCancellationException) {
            Log.d(TAG, "Sign-in cancelled by user")
            _authState.value = GoogleAuthState.Idle
            GoogleAuthResult.Cancelled
        } catch (e: GetCredentialException) {
            Log.e(TAG, "Sign-in failed: ${e.message}", e)
            _authState.value = GoogleAuthState.Error(e.message ?: "Sign-in failed")
            GoogleAuthResult.Error(e.message ?: "Sign-in failed")
        } catch (e: Exception) {
            Log.e(TAG, "Unexpected error during sign-in: ${e.message}", e)
            _authState.value = GoogleAuthState.Error(e.message ?: "Unexpected error")
            GoogleAuthResult.Error(e.message ?: "Unexpected error")
        }
    }
    
    /**
     * Sign in with Google using the Sign In With Google button style.
     * This shows a more prominent sign-in UI.
     */
    suspend fun signInWithGoogleButton(activityContext: Context): GoogleAuthResult {
        _authState.value = GoogleAuthState.Loading
        
        return try {
            val rawNonce = UUID.randomUUID().toString()
            val hashedNonce = hashNonce(rawNonce)
            
            // Use GetSignInWithGoogleOption for the button-style sign-in
            val signInWithGoogleOption = GetSignInWithGoogleOption.Builder(WEB_CLIENT_ID)
                .setNonce(hashedNonce)
                .build()
            
            val request = GetCredentialRequest.Builder()
                .addCredentialOption(signInWithGoogleOption)
                .build()
            
            val result = credentialManager.getCredential(
                request = request,
                context = activityContext
            )
            
            handleSignInResponse(result)
        } catch (e: GetCredentialCancellationException) {
            Log.d(TAG, "Sign-in with Google button cancelled")
            _authState.value = GoogleAuthState.Idle
            GoogleAuthResult.Cancelled
        } catch (e: GetCredentialException) {
            Log.e(TAG, "Sign-in with Google button failed: ${e.message}", e)
            _authState.value = GoogleAuthState.Error(e.message ?: "Sign-in failed")
            GoogleAuthResult.Error(e.message ?: "Sign-in failed")
        } catch (e: Exception) {
            Log.e(TAG, "Unexpected error: ${e.message}", e)
            _authState.value = GoogleAuthState.Error(e.message ?: "Unexpected error")
            GoogleAuthResult.Error(e.message ?: "Unexpected error")
        }
    }
    
    /**
     * Handles the credential response and extracts user information
     */
    private suspend fun handleSignInResponse(result: GetCredentialResponse): GoogleAuthResult {
        val credential = result.credential
        
        return when (credential) {
            is CustomCredential -> {
                if (credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
                    try {
                        // Parse the Google ID token
                        val googleIdTokenCredential = GoogleIdTokenCredential.createFrom(credential.data)
                        
                        // Extract user information from the token
                        val user = GoogleUser(
                            id = googleIdTokenCredential.id,
                            email = googleIdTokenCredential.id, // ID is the email for Google
                            displayName = googleIdTokenCredential.displayName,
                            givenName = googleIdTokenCredential.givenName,
                            familyName = googleIdTokenCredential.familyName,
                            profilePictureUri = googleIdTokenCredential.profilePictureUri?.toString(),
                            idToken = googleIdTokenCredential.idToken
                        )
                        
                        _currentUser.value = user
                        _authState.value = GoogleAuthState.Authenticated(user)
                        
                        // Also persist to SupabaseAuthService for consistency
                        SupabaseAuthService.signInWithGoogle(
                            email = user.email,
                            displayName = user.displayName,
                            googleId = user.id
                        )
                        
                        // Initialize user data repository with auth info
                        UserDataRepository.initialize(
                            userId = user.id,
                            email = user.email,
                            displayName = user.displayName ?: "",
                            authProvider = "google"
                        )
                        
                        Log.i(TAG, "Successfully signed in: ${user.email}")
                        GoogleAuthResult.Success(user)
                        
                    } catch (e: GoogleIdTokenParsingException) {
                        Log.e(TAG, "Failed to parse Google ID token: ${e.message}", e)
                        _authState.value = GoogleAuthState.Error("Failed to parse credentials")
                        GoogleAuthResult.Error("Failed to parse credentials")
                    }
                } else {
                    Log.e(TAG, "Unexpected credential type: ${credential.type}")
                    _authState.value = GoogleAuthState.Error("Unexpected credential type")
                    GoogleAuthResult.Error("Unexpected credential type")
                }
            }
            else -> {
                Log.e(TAG, "Unsupported credential type")
                _authState.value = GoogleAuthState.Error("Unsupported credential type")
                GoogleAuthResult.Error("Unsupported credential type")
            }
        }
    }
    
    /**
     * Signs out the current user and clears credential state
     */
    suspend fun signOut(): Boolean {
        return try {
            // Clear the credential state from Credential Manager
            credentialManager.clearCredentialState(ClearCredentialStateRequest())
            
            _currentUser.value = null
            _authState.value = GoogleAuthState.Idle
            
            // Clear user data repository
            UserDataRepository.clear()
            
            Log.i(TAG, "Successfully signed out")
            true
        } catch (e: Exception) {
            Log.e(TAG, "Sign-out failed: ${e.message}", e)
            false
        }
    }
    
    /**
     * Checks if a user is currently authenticated
     */
    fun isAuthenticated(): Boolean = _currentUser.value != null
    
    /**
     * Hashes the nonce using SHA-256 for security
     * The nonce prevents replay attacks by ensuring each sign-in request is unique
     */
    private fun hashNonce(rawNonce: String): String {
        val bytes = rawNonce.toByteArray()
        val md = MessageDigest.getInstance("SHA-256")
        val digest = md.digest(bytes)
        return digest.fold("") { str, it -> str + "%02x".format(it) }
    }
}

/**
 * Represents the authentication state
 */
sealed class GoogleAuthState {
    object Idle : GoogleAuthState()
    object Loading : GoogleAuthState()
    data class Authenticated(val user: GoogleUser) : GoogleAuthState()
    data class Error(val message: String) : GoogleAuthState()
}

/**
 * Represents an authenticated Google user
 * 
 * @param id The user's unique Google ID (typically the email)
 * @param email The user's email address
 * @param displayName The user's full display name
 * @param givenName The user's first name
 * @param familyName The user's last name
 * @param profilePictureUri URL to the user's profile picture
 * @param idToken The Google ID token (can be used for backend verification)
 */
data class GoogleUser(
    val id: String,
    val email: String,
    val displayName: String?,
    val givenName: String?,
    val familyName: String?,
    val profilePictureUri: String?,
    val idToken: String
)

/**
 * Result wrapper for authentication operations
 */
sealed class GoogleAuthResult {
    data class Success(val user: GoogleUser) : GoogleAuthResult()
    data class Error(val message: String) : GoogleAuthResult()
    object Cancelled : GoogleAuthResult()
    object NoSavedCredentials : GoogleAuthResult()  // For auto sign-in when no credentials saved
}
