package org.example.project.auth

/**
 * Interface for platform-specific authentication storage.
 * Provides persistent storage for registered users and current session.
 */
interface AuthStorageProvider {
    /**
     * Check if user exists by email
     */
    fun userExists(email: String): Boolean
    
    /**
     * Get stored user credentials
     */
    fun getUser(email: String): StoredUser?
    
    /**
     * Register a new user
     */
    fun registerUser(
        email: String,
        password: String,
        userId: String,
        displayName: String?,
        phone: String?
    ): Boolean
    
    /**
     * Validate user credentials
     */
    fun validateCredentials(email: String, password: String): StoredUser?
    
    /**
     * Save current logged-in user
     */
    fun saveCurrentUser(user: AuthUser)
    
    /**
     * Get current logged-in user
     */
    fun getCurrentUser(): AuthUser?
    
    /**
     * Check if user is logged in
     */
    fun isLoggedIn(): Boolean
    
    /**
     * Clear current user session
     */
    fun clearCurrentUser()
}

/**
 * Stored user data
 */
data class StoredUser(
    val email: String,
    val password: String,
    val userId: String,
    val displayName: String?,
    val phone: String?,
    val createdAt: Long
) {
    fun toAuthUser(): AuthUser = AuthUser(
        id = userId,
        email = email,
        displayName = displayName,
        phone = phone,
        createdAt = createdAt
    )
}

/**
 * Platform-specific storage provider instance
 */
expect fun getAuthStorageProvider(): AuthStorageProvider
