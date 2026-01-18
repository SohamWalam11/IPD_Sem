package org.example.project.auth

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Authentication state sealed class
 */
sealed class AuthState {
    object Initial : AuthState()
    object Loading : AuthState()
    data class Authenticated(val user: AuthUser) : AuthState()
    object Unauthenticated : AuthState()
    data class Error(val message: String) : AuthState()
}

/**
 * Represents an authenticated user
 */
data class AuthUser(
    val id: String,
    val email: String,
    val displayName: String? = null,
    val phone: String? = null,
    val createdAt: Long = System.currentTimeMillis()
)

/**
 * Authentication result wrapper
 */
sealed class AuthResult {
    data class Success(val user: AuthUser) : AuthResult()
    data class UserAlreadyExists(val email: String) : AuthResult()
    data class InvalidCredentials(val message: String) : AuthResult()
    data class Error(val message: String) : AuthResult()
}

/**
 * Supabase Authentication Service
 * Handles user sign up, sign in, and session management
 * Uses platform-specific AuthStorageProvider for persistent storage
 */
object SupabaseAuthService {
    
    // TODO: Replace with your actual Supabase credentials
    private const val SUPABASE_URL = "https://nzxqctgvtopmmagklygy.supabase.co"
    private const val SUPABASE_KEY = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6Im56eHFjdGd2dG9wbW1hZ2tseWd5Iiwicm9sZSI6ImFub24iLCJpYXQiOjE3NjgzODYzNTgsImV4cCI6MjA4Mzk2MjM1OH0.JRKrnKSAxuZ3P_FCiAIv_CkLRg5ueVGZjiSetXDUYWw"
    
    private val _authState = MutableStateFlow<AuthState>(AuthState.Initial)
    val authState: StateFlow<AuthState> = _authState.asStateFlow()
    
    private val _currentUser = MutableStateFlow<AuthUser?>(null)
    val currentUser: StateFlow<AuthUser?> = _currentUser.asStateFlow()
    
    // Get platform-specific storage provider
    private val storage: AuthStorageProvider by lazy { getAuthStorageProvider() }
    
    /**
     * Initialize and restore any existing session
     */
    fun restoreSession() {
        val savedUser = storage.getCurrentUser()
        if (savedUser != null) {
            _currentUser.value = savedUser
            _authState.value = AuthState.Authenticated(savedUser)
        }
    }
    
    /**
     * Sign up with email and password
     * Returns error if user already exists
     * Persists user to storage
     */
    suspend fun signUp(
        email: String,
        password: String,
        displayName: String? = null
    ): AuthResult {
        _authState.value = AuthState.Loading
        
        return try {
            // Check if user already exists (using persistent storage)
            if (storage.userExists(email)) {
                _authState.value = AuthState.Error("User already exists")
                return AuthResult.UserAlreadyExists(email)
            }
            
            // Simulate network delay
            kotlinx.coroutines.delay(1500)
            
            val userId = generateUserId()
            val userName = displayName ?: email.substringBefore("@")
            
            // Register user in persistent storage
            storage.registerUser(
                email = email,
                password = password,
                userId = userId,
                displayName = userName,
                phone = null
            )
            
            // Create new user
            val newUser = AuthUser(
                id = userId,
                email = email,
                displayName = userName
            )
            
            // Save current session
            storage.saveCurrentUser(newUser)
            
            _currentUser.value = newUser
            _authState.value = AuthState.Authenticated(newUser)
            
            AuthResult.Success(newUser)
        } catch (e: Exception) {
            _authState.value = AuthState.Error(e.message ?: "Sign up failed")
            AuthResult.Error(e.message ?: "Sign up failed")
        }
    }
    
    /**
     * Sign in with email and password
     * Validates against persistent storage
     */
    suspend fun signIn(email: String, password: String): AuthResult {
        _authState.value = AuthState.Loading
        
        return try {
            // Simulate network delay
            kotlinx.coroutines.delay(1500)
            
            // Validate credentials using persistent storage
            val storedUser = storage.validateCredentials(email, password)
            
            if (storedUser == null) {
                // Check if user exists but password is wrong
                if (storage.userExists(email)) {
                    _authState.value = AuthState.Unauthenticated
                    return AuthResult.InvalidCredentials("Invalid password")
                }
                _authState.value = AuthState.Unauthenticated
                return AuthResult.InvalidCredentials("User not found. Please sign up first.")
            }
            
            val user = storedUser.toAuthUser()
            
            // Save current session
            storage.saveCurrentUser(user)
            
            _currentUser.value = user
            _authState.value = AuthState.Authenticated(user)
            
            AuthResult.Success(user)
        } catch (e: Exception) {
            _authState.value = AuthState.Error(e.message ?: "Sign in failed")
            AuthResult.Error(e.message ?: "Sign in failed")
        }
    }
    
    /**
     * Sign in with Google OAuth
     */
    suspend fun signInWithGoogle(
        email: String? = null,
        displayName: String? = null,
        googleId: String? = null
    ): AuthResult {
        _authState.value = AuthState.Loading
        
        return try {
            kotlinx.coroutines.delay(500)
            
            val userEmail = email ?: "user@gmail.com"
            val userName = displayName ?: "Google User"
            val userId = googleId ?: generateUserId()
            
            // Check if Google user already exists, if not register them
            if (!storage.userExists(userEmail)) {
                storage.registerUser(
                    email = userEmail,
                    password = "google_auth_${userId}", // Google users have special password
                    userId = userId,
                    displayName = userName,
                    phone = null
                )
            }
            
            val googleUser = AuthUser(
                id = userId,
                email = userEmail,
                displayName = userName
            )
            
            storage.saveCurrentUser(googleUser)
            _currentUser.value = googleUser
            _authState.value = AuthState.Authenticated(googleUser)
            
            AuthResult.Success(googleUser)
        } catch (e: Exception) {
            _authState.value = AuthState.Error(e.message ?: "Google sign in failed")
            AuthResult.Error(e.message ?: "Google sign in failed")
        }
    }
    
    /**
     * Sign out the current user
     */
    suspend fun signOut() {
        storage.clearCurrentUser()
        _currentUser.value = null
        _authState.value = AuthState.Unauthenticated
    }
    
    /**
     * Check if user is already logged in
     */
    fun isAuthenticated(): Boolean {
        return storage.isLoggedIn() || _currentUser.value != null
    }
    
    private fun generateUserId(): String {
        return "user_${System.currentTimeMillis()}_${(1000..9999).random()}"
    }
}
