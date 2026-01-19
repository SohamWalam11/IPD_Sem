package org.example.project.data.local

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.IOException

// ═══════════════════════════════════════════════════════════════════════════════
// DataStore Extension
// ═══════════════════════════════════════════════════════════════════════════════
private val Context.userDataStore: DataStore<Preferences> by preferencesDataStore(
    name = "user_preferences"
)

/**
 * User data that persists across app sessions
 */
@Serializable
data class CachedUserData(
    val id: String = "",
    val email: String = "",
    val name: String = "",
    val displayName: String = "",
    val photoUrl: String? = null,
    val authProvider: String = "email", // "email", "google", "biometric"
    val lastLoginTimestamp: Long = 0L,
    val vehicleType: String? = null,
    val vehicleModel: String? = null,
    val tyreSize: String? = null
)

/**
 * UserPreferencesManager - Jetpack DataStore implementation for persistent login
 * 
 * Features:
 * - Store login state (is_logged_in flag)
 * - Cache user profile data for quick dashboard load
 * - Store authentication tokens
 * - Thread-safe async operations with Kotlin Flow
 * - Auto-refresh on app restart
 */
class UserPreferencesManager(private val context: Context) {
    
    private val dataStore = context.userDataStore
    private val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }
    
    // ═══════════════════════════════════════════════════════════════════════════
    // Preference Keys
    // ═══════════════════════════════════════════════════════════════════════════
    private object PreferenceKeys {
        val IS_LOGGED_IN = booleanPreferencesKey("is_logged_in")
        val HAS_COMPLETED_ONBOARDING = booleanPreferencesKey("has_completed_onboarding")
        val HAS_COMPLETED_SETUP = booleanPreferencesKey("has_completed_setup")
        val USER_DATA_JSON = stringPreferencesKey("user_data_json")
        val AUTH_TOKEN = stringPreferencesKey("auth_token")
        val REFRESH_TOKEN = stringPreferencesKey("refresh_token")
        val TOKEN_EXPIRY = longPreferencesKey("token_expiry")
        val BIOMETRIC_ENABLED = booleanPreferencesKey("biometric_enabled")
        val LAST_SYNC_TIMESTAMP = longPreferencesKey("last_sync_timestamp")
        val SELECTED_THEME = stringPreferencesKey("selected_theme") // "light", "dark", "system"
        val NOTIFICATIONS_ENABLED = booleanPreferencesKey("notifications_enabled")
        val AUTO_SYNC_ENABLED = booleanPreferencesKey("auto_sync_enabled")
    }
    
    // ═══════════════════════════════════════════════════════════════════════════
    // LOGIN STATE
    // ═══════════════════════════════════════════════════════════════════════════
    
    /**
     * Observe login state as Flow - automatically updates when changed
     */
    val isLoggedIn: Flow<Boolean> = dataStore.data
        .catch { exception ->
            if (exception is IOException) {
                emit(emptyPreferences())
            } else {
                throw exception
            }
        }
        .map { preferences ->
            preferences[PreferenceKeys.IS_LOGGED_IN] ?: false
        }
    
    /**
     * Set login state
     */
    suspend fun setLoggedIn(isLoggedIn: Boolean) {
        dataStore.edit { preferences ->
            preferences[PreferenceKeys.IS_LOGGED_IN] = isLoggedIn
        }
    }
    
    // ═══════════════════════════════════════════════════════════════════════════
    // ONBOARDING & SETUP STATE
    // ═══════════════════════════════════════════════════════════════════════════
    
    val hasCompletedOnboarding: Flow<Boolean> = dataStore.data
        .catch { emit(emptyPreferences()) }
        .map { it[PreferenceKeys.HAS_COMPLETED_ONBOARDING] ?: false }
    
    suspend fun setOnboardingCompleted(completed: Boolean) {
        dataStore.edit { preferences ->
            preferences[PreferenceKeys.HAS_COMPLETED_ONBOARDING] = completed
        }
    }
    
    val hasCompletedSetup: Flow<Boolean> = dataStore.data
        .catch { emit(emptyPreferences()) }
        .map { it[PreferenceKeys.HAS_COMPLETED_SETUP] ?: false }
    
    suspend fun setSetupCompleted(completed: Boolean) {
        dataStore.edit { preferences ->
            preferences[PreferenceKeys.HAS_COMPLETED_SETUP] = completed
        }
    }
    
    // ═══════════════════════════════════════════════════════════════════════════
    // USER DATA CACHING
    // ═══════════════════════════════════════════════════════════════════════════
    
    /**
     * Get cached user data as Flow
     */
    val cachedUserData: Flow<CachedUserData?> = dataStore.data
        .catch { emit(emptyPreferences()) }
        .map { preferences ->
            preferences[PreferenceKeys.USER_DATA_JSON]?.let { jsonString ->
                try {
                    json.decodeFromString<CachedUserData>(jsonString)
                } catch (e: Exception) {
                    null
                }
            }
        }
    
    /**
     * Cache user data for quick dashboard loading
     */
    suspend fun cacheUserData(userData: CachedUserData) {
        dataStore.edit { preferences ->
            preferences[PreferenceKeys.USER_DATA_JSON] = json.encodeToString(userData)
        }
    }
    
    /**
     * Update specific user data fields
     */
    suspend fun updateUserData(transform: (CachedUserData) -> CachedUserData) {
        dataStore.edit { preferences ->
            val currentJson = preferences[PreferenceKeys.USER_DATA_JSON]
            val currentData = currentJson?.let {
                try { json.decodeFromString<CachedUserData>(it) } catch (e: Exception) { null }
            } ?: CachedUserData()
            
            val updatedData = transform(currentData)
            preferences[PreferenceKeys.USER_DATA_JSON] = json.encodeToString(updatedData)
        }
    }
    
    /**
     * Clear cached user data (on logout)
     */
    suspend fun clearUserData() {
        dataStore.edit { preferences ->
            preferences.remove(PreferenceKeys.USER_DATA_JSON)
        }
    }
    
    // ═══════════════════════════════════════════════════════════════════════════
    // AUTHENTICATION TOKENS
    // ═══════════════════════════════════════════════════════════════════════════
    
    /**
     * Save authentication tokens
     */
    suspend fun saveAuthTokens(
        accessToken: String,
        refreshToken: String?,
        expiryTimestamp: Long
    ) {
        dataStore.edit { preferences ->
            preferences[PreferenceKeys.AUTH_TOKEN] = accessToken
            refreshToken?.let { preferences[PreferenceKeys.REFRESH_TOKEN] = it }
            preferences[PreferenceKeys.TOKEN_EXPIRY] = expiryTimestamp
        }
    }
    
    val authToken: Flow<String?> = dataStore.data
        .catch { emit(emptyPreferences()) }
        .map { it[PreferenceKeys.AUTH_TOKEN] }
    
    val refreshToken: Flow<String?> = dataStore.data
        .catch { emit(emptyPreferences()) }
        .map { it[PreferenceKeys.REFRESH_TOKEN] }
    
    val tokenExpiry: Flow<Long> = dataStore.data
        .catch { emit(emptyPreferences()) }
        .map { it[PreferenceKeys.TOKEN_EXPIRY] ?: 0L }
    
    /**
     * Check if token is expired
     */
    fun isTokenExpired(expiryTimestamp: Long): Boolean {
        return System.currentTimeMillis() >= expiryTimestamp
    }
    
    suspend fun clearAuthTokens() {
        dataStore.edit { preferences ->
            preferences.remove(PreferenceKeys.AUTH_TOKEN)
            preferences.remove(PreferenceKeys.REFRESH_TOKEN)
            preferences.remove(PreferenceKeys.TOKEN_EXPIRY)
        }
    }
    
    // ═══════════════════════════════════════════════════════════════════════════
    // BIOMETRIC SETTINGS
    // ═══════════════════════════════════════════════════════════════════════════
    
    val isBiometricEnabled: Flow<Boolean> = dataStore.data
        .catch { emit(emptyPreferences()) }
        .map { it[PreferenceKeys.BIOMETRIC_ENABLED] ?: false }
    
    suspend fun setBiometricEnabled(enabled: Boolean) {
        dataStore.edit { preferences ->
            preferences[PreferenceKeys.BIOMETRIC_ENABLED] = enabled
        }
    }
    
    // ═══════════════════════════════════════════════════════════════════════════
    // APP SETTINGS
    // ═══════════════════════════════════════════════════════════════════════════
    
    val selectedTheme: Flow<String> = dataStore.data
        .catch { emit(emptyPreferences()) }
        .map { it[PreferenceKeys.SELECTED_THEME] ?: "system" }
    
    suspend fun setTheme(theme: String) {
        dataStore.edit { preferences ->
            preferences[PreferenceKeys.SELECTED_THEME] = theme
        }
    }
    
    val notificationsEnabled: Flow<Boolean> = dataStore.data
        .catch { emit(emptyPreferences()) }
        .map { it[PreferenceKeys.NOTIFICATIONS_ENABLED] ?: true }
    
    suspend fun setNotificationsEnabled(enabled: Boolean) {
        dataStore.edit { preferences ->
            preferences[PreferenceKeys.NOTIFICATIONS_ENABLED] = enabled
        }
    }
    
    val autoSyncEnabled: Flow<Boolean> = dataStore.data
        .catch { emit(emptyPreferences()) }
        .map { it[PreferenceKeys.AUTO_SYNC_ENABLED] ?: true }
    
    suspend fun setAutoSyncEnabled(enabled: Boolean) {
        dataStore.edit { preferences ->
            preferences[PreferenceKeys.AUTO_SYNC_ENABLED] = enabled
        }
    }
    
    val lastSyncTimestamp: Flow<Long> = dataStore.data
        .catch { emit(emptyPreferences()) }
        .map { it[PreferenceKeys.LAST_SYNC_TIMESTAMP] ?: 0L }
    
    suspend fun updateLastSyncTimestamp() {
        dataStore.edit { preferences ->
            preferences[PreferenceKeys.LAST_SYNC_TIMESTAMP] = System.currentTimeMillis()
        }
    }
    
    // ═══════════════════════════════════════════════════════════════════════════
    // COMPLETE LOGIN
    // ═══════════════════════════════════════════════════════════════════════════
    
    /**
     * Complete login process - saves all user data at once
     */
    suspend fun completeLogin(
        userData: CachedUserData,
        accessToken: String? = null,
        refreshToken: String? = null,
        tokenExpiry: Long = 0L
    ) {
        dataStore.edit { preferences ->
            preferences[PreferenceKeys.IS_LOGGED_IN] = true
            preferences[PreferenceKeys.USER_DATA_JSON] = json.encodeToString(
                userData.copy(lastLoginTimestamp = System.currentTimeMillis())
            )
            accessToken?.let { preferences[PreferenceKeys.AUTH_TOKEN] = it }
            refreshToken?.let { preferences[PreferenceKeys.REFRESH_TOKEN] = it }
            if (tokenExpiry > 0) preferences[PreferenceKeys.TOKEN_EXPIRY] = tokenExpiry
        }
    }
    
    // ═══════════════════════════════════════════════════════════════════════════
    // LOGOUT
    // ═══════════════════════════════════════════════════════════════════════════
    
    /**
     * Complete logout - clears all user data but preserves app settings
     */
    suspend fun logout() {
        dataStore.edit { preferences ->
            preferences[PreferenceKeys.IS_LOGGED_IN] = false
            preferences.remove(PreferenceKeys.USER_DATA_JSON)
            preferences.remove(PreferenceKeys.AUTH_TOKEN)
            preferences.remove(PreferenceKeys.REFRESH_TOKEN)
            preferences.remove(PreferenceKeys.TOKEN_EXPIRY)
            // Keep onboarding/setup completed flags
            // Keep theme and notification settings
        }
    }
    
    /**
     * Full reset - clears everything including settings
     */
    suspend fun clearAll() {
        dataStore.edit { preferences ->
            preferences.clear()
        }
    }
    
    companion object {
        @Volatile
        private var INSTANCE: UserPreferencesManager? = null
        
        fun getInstance(context: Context): UserPreferencesManager {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: UserPreferencesManager(context.applicationContext).also {
                    INSTANCE = it
                }
            }
        }
    }
}
