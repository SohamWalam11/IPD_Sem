package org.example.project.data

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.example.project.auth.AuthStorageProvider
import org.example.project.auth.AuthUser
import org.example.project.auth.StoredUser

/**
 * Persistent storage for authentication data using SharedPreferences.
 * This ensures users remain registered across app restarts.
 * 
 * Stores:
 * - Registered users (email -> hashed password + user data)
 * - Currently logged-in user session
 */
object AuthStorage : AuthStorageProvider {
    
    private const val TAG = "AuthStorage"
    private const val PREFS_NAME = "tyregard_auth_storage"
    private const val KEY_REGISTERED_USERS = "registered_users"
    private const val KEY_CURRENT_USER = "current_user"
    private const val KEY_IS_LOGGED_IN = "is_logged_in"
    
    private var prefs: SharedPreferences? = null
    private val json = Json { 
        ignoreUnknownKeys = true 
        encodeDefaults = true
    }
    
    /**
     * Initialize the storage with application context.
     * Call this from Application.onCreate() or before any auth operations.
     */
    fun initialize(context: Context) {
        if (prefs == null) {
            prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            Log.d(TAG, "AuthStorage initialized")
        }
    }
    
    /**
     * Check if storage is initialized
     */
    fun isInitialized(): Boolean = prefs != null
    
    // ═══════════════════════════════════════════════════════════════════════
    // Registered Users Management
    // ═══════════════════════════════════════════════════════════════════════
    
    /**
     * Serializable user entry for storage
     */
    @Serializable
    private data class StoredUserEntry(
        val email: String,
        val password: String,
        val userId: String,
        val displayName: String?,
        val phone: String?,
        val createdAt: Long
    ) {
        fun toStoredUser(): StoredUser = StoredUser(
            email = email,
            password = password,
            userId = userId,
            displayName = displayName,
            phone = phone,
            createdAt = createdAt
        )
    }
    
    /**
     * Get all registered users from storage
     */
    private fun getRegisteredUsers(): Map<String, StoredUserEntry> {
        val prefs = prefs ?: return emptyMap()
        return try {
            val jsonString = prefs.getString(KEY_REGISTERED_USERS, null) ?: return emptyMap()
            json.decodeFromString<Map<String, StoredUserEntry>>(jsonString)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to load registered users: ${e.message}")
            emptyMap()
        }
    }
    
    /**
     * Check if a user with the given email exists
     */
    override fun userExists(email: String): Boolean {
        return getRegisteredUsers().containsKey(email.lowercase())
    }
    
    /**
     * Get a registered user by email
     */
    override fun getUser(email: String): StoredUser? {
        return getRegisteredUsers()[email.lowercase()]?.toStoredUser()
    }
    
    /**
     * Register a new user (save to persistent storage)
     */
    override fun registerUser(
        email: String,
        password: String,
        userId: String,
        displayName: String?,
        phone: String?
    ): Boolean {
        val prefs = prefs ?: return false
        return try {
            val users = getRegisteredUsers().toMutableMap()
            val entry = StoredUserEntry(
                email = email.lowercase(),
                password = password,
                userId = userId,
                displayName = displayName,
                phone = phone,
                createdAt = System.currentTimeMillis()
            )
            users[email.lowercase()] = entry
            
            prefs.edit()
                .putString(KEY_REGISTERED_USERS, json.encodeToString(users))
                .apply()
            
            Log.d(TAG, "User registered: ${email.lowercase()}")
            true
        } catch (e: Exception) {
            Log.e(TAG, "Failed to register user: ${e.message}")
            false
        }
    }
    
    /**
     * Validate user credentials
     */
    override fun validateCredentials(email: String, password: String): StoredUser? {
        val user = getRegisteredUsers()[email.lowercase()]
        return if (user != null && user.password == password) {
            user.toStoredUser()
        } else {
            null
        }
    }
    
    // ═══════════════════════════════════════════════════════════════════════
    // Current Session Management
    // ═══════════════════════════════════════════════════════════════════════
    
    /**
     * Serializable current user for storage
     */
    @Serializable
    private data class StoredCurrentUser(
        val id: String,
        val email: String,
        val displayName: String?,
        val phone: String?,
        val createdAt: Long
    )
    
    /**
     * Save current logged-in user session
     */
    override fun saveCurrentUser(user: AuthUser) {
        val prefs = prefs ?: return
        try {
            val storedUser = StoredCurrentUser(
                id = user.id,
                email = user.email,
                displayName = user.displayName,
                phone = user.phone,
                createdAt = user.createdAt
            )
            prefs.edit()
                .putString(KEY_CURRENT_USER, json.encodeToString(storedUser))
                .putBoolean(KEY_IS_LOGGED_IN, true)
                .apply()
            Log.d(TAG, "Current user saved: ${user.email}")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to save current user: ${e.message}")
        }
    }
    
    /**
     * Get current logged-in user (if any)
     */
    override fun getCurrentUser(): AuthUser? {
        val prefs = prefs ?: return null
        return try {
            if (!prefs.getBoolean(KEY_IS_LOGGED_IN, false)) return null
            
            val jsonString = prefs.getString(KEY_CURRENT_USER, null) ?: return null
            val stored = json.decodeFromString<StoredCurrentUser>(jsonString)
            
            AuthUser(
                id = stored.id,
                email = stored.email,
                displayName = stored.displayName,
                phone = stored.phone,
                createdAt = stored.createdAt
            )
        } catch (e: Exception) {
            Log.e(TAG, "Failed to load current user: ${e.message}")
            null
        }
    }
    
    /**
     * Check if user is logged in
     */
    override fun isLoggedIn(): Boolean {
        val prefs = prefs ?: return false
        return prefs.getBoolean(KEY_IS_LOGGED_IN, false)
    }
    
    /**
     * Clear current user session (logout)
     */
    override fun clearCurrentUser() {
        val prefs = prefs ?: return
        prefs.edit()
            .remove(KEY_CURRENT_USER)
            .putBoolean(KEY_IS_LOGGED_IN, false)
            .apply()
        Log.d(TAG, "Current user cleared (logged out)")
    }
    
    /**
     * Clear all data (for debugging/testing)
     */
    fun clearAll() {
        val prefs = prefs ?: return
        prefs.edit().clear().apply()
        Log.d(TAG, "All auth storage cleared")
    }
}
