package org.example.project.auth

/**
 * iOS implementation - in-memory storage (for now)
 * TODO: Implement using NSUserDefaults or Keychain for persistence
 */
class IosAuthStorage : AuthStorageProvider {
    private val registeredUsers = mutableMapOf<String, StoredUser>()
    private var currentUser: AuthUser? = null
    private var _isLoggedIn: Boolean = false
    
    override fun userExists(email: String): Boolean {
        return registeredUsers.containsKey(email.lowercase())
    }
    
    override fun getUser(email: String): StoredUser? {
        return registeredUsers[email.lowercase()]
    }
    
    override fun registerUser(
        email: String,
        password: String,
        userId: String,
        displayName: String?,
        phone: String?
    ): Boolean {
        val user = StoredUser(
            email = email.lowercase(),
            password = password,
            userId = userId,
            displayName = displayName,
            phone = phone,
            createdAt = System.currentTimeMillis()
        )
        registeredUsers[email.lowercase()] = user
        return true
    }
    
    override fun validateCredentials(email: String, password: String): StoredUser? {
        val user = registeredUsers[email.lowercase()]
        return if (user != null && user.password == password) user else null
    }
    
    override fun saveCurrentUser(user: AuthUser) {
        currentUser = user
        _isLoggedIn = true
    }
    
    override fun getCurrentUser(): AuthUser? {
        return if (_isLoggedIn) currentUser else null
    }
    
    override fun isLoggedIn(): Boolean = _isLoggedIn
    
    override fun clearCurrentUser() {
        currentUser = null
        _isLoggedIn = false
    }
}

// Singleton instance
private val iosAuthStorage = IosAuthStorage()

actual fun getAuthStorageProvider(): AuthStorageProvider = iosAuthStorage
