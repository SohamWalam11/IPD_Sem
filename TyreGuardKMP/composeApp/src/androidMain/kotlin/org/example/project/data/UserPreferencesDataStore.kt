package org.example.project.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import java.io.IOException

// Extension to create DataStore instance
private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "user_preferences")

/**
 * User preferences stored using DataStore.
 */
data class UserPreferences(
    /** AI detection sensitivity level (0.0 = low, 1.0 = high) */
    val sensitivityLevel: Float = 0.5f,
    
    /** Enable flash for camera by default */
    val defaultFlashEnabled: Boolean = false,
    
    /** Auto-save captured images */
    val autoSaveImages: Boolean = true,
    
    /** Show confidence scores on detection results */
    val showConfidenceScores: Boolean = true,
    
    /** Enable haptic feedback on detection */
    val hapticFeedbackEnabled: Boolean = true,
    
    /** Theme preference: "system", "light", "dark" */
    val themePreference: String = "system",
    
    /** Enable high accuracy mode (slower but more accurate) */
    val highAccuracyMode: Boolean = false,
    
    /** Whether user has completed onboarding */
    val onboardingCompleted: Boolean = false
)

/**
 * DataStore-based manager for user preferences.
 * 
 * Provides reactive access to user preferences with type-safe keys.
 */
class UserPreferencesDataStore(private val context: Context) {
    
    private object PreferenceKeys {
        val SENSITIVITY_LEVEL = floatPreferencesKey("sensitivity_level")
        val DEFAULT_FLASH_ENABLED = booleanPreferencesKey("default_flash_enabled")
        val AUTO_SAVE_IMAGES = booleanPreferencesKey("auto_save_images")
        val SHOW_CONFIDENCE_SCORES = booleanPreferencesKey("show_confidence_scores")
        val HAPTIC_FEEDBACK_ENABLED = booleanPreferencesKey("haptic_feedback_enabled")
        val THEME_PREFERENCE = stringPreferencesKey("theme_preference")
        val HIGH_ACCURACY_MODE = booleanPreferencesKey("high_accuracy_mode")
        val ONBOARDING_COMPLETED = booleanPreferencesKey("onboarding_completed")
    }
    
    /**
     * Flow of user preferences. Emits whenever any preference changes.
     */
    val userPreferencesFlow: Flow<UserPreferences> = context.dataStore.data
        .catch { exception ->
            if (exception is IOException) {
                emit(emptyPreferences())
            } else {
                throw exception
            }
        }
        .map { preferences ->
            UserPreferences(
                sensitivityLevel = preferences[PreferenceKeys.SENSITIVITY_LEVEL] ?: 0.5f,
                defaultFlashEnabled = preferences[PreferenceKeys.DEFAULT_FLASH_ENABLED] ?: false,
                autoSaveImages = preferences[PreferenceKeys.AUTO_SAVE_IMAGES] ?: true,
                showConfidenceScores = preferences[PreferenceKeys.SHOW_CONFIDENCE_SCORES] ?: true,
                hapticFeedbackEnabled = preferences[PreferenceKeys.HAPTIC_FEEDBACK_ENABLED] ?: true,
                themePreference = preferences[PreferenceKeys.THEME_PREFERENCE] ?: "system",
                highAccuracyMode = preferences[PreferenceKeys.HIGH_ACCURACY_MODE] ?: false,
                onboardingCompleted = preferences[PreferenceKeys.ONBOARDING_COMPLETED] ?: false
            )
        }
    
    // ─────────────────────────────────────────────────────────────────────
    // Sensitivity Level (AI Detection)
    // ─────────────────────────────────────────────────────────────────────
    
    /**
     * Update AI sensitivity level.
     * @param level Value between 0.0 (low sensitivity) and 1.0 (high sensitivity)
     */
    suspend fun setSensitivityLevel(level: Float) {
        context.dataStore.edit { preferences ->
            preferences[PreferenceKeys.SENSITIVITY_LEVEL] = level.coerceIn(0f, 1f)
        }
    }
    
    /**
     * Get current sensitivity level as flow.
     */
    val sensitivityLevelFlow: Flow<Float> = context.dataStore.data
        .map { it[PreferenceKeys.SENSITIVITY_LEVEL] ?: 0.5f }
    
    // ─────────────────────────────────────────────────────────────────────
    // Flash Settings
    // ─────────────────────────────────────────────────────────────────────
    
    suspend fun setDefaultFlashEnabled(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[PreferenceKeys.DEFAULT_FLASH_ENABLED] = enabled
        }
    }
    
    // ─────────────────────────────────────────────────────────────────────
    // Auto-save Settings
    // ─────────────────────────────────────────────────────────────────────
    
    suspend fun setAutoSaveImages(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[PreferenceKeys.AUTO_SAVE_IMAGES] = enabled
        }
    }
    
    // ─────────────────────────────────────────────────────────────────────
    // Display Settings
    // ─────────────────────────────────────────────────────────────────────
    
    suspend fun setShowConfidenceScores(show: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[PreferenceKeys.SHOW_CONFIDENCE_SCORES] = show
        }
    }
    
    suspend fun setHapticFeedbackEnabled(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[PreferenceKeys.HAPTIC_FEEDBACK_ENABLED] = enabled
        }
    }
    
    // ─────────────────────────────────────────────────────────────────────
    // Theme Settings
    // ─────────────────────────────────────────────────────────────────────
    
    suspend fun setThemePreference(theme: String) {
        require(theme in listOf("system", "light", "dark")) {
            "Theme must be 'system', 'light', or 'dark'"
        }
        context.dataStore.edit { preferences ->
            preferences[PreferenceKeys.THEME_PREFERENCE] = theme
        }
    }
    
    // ─────────────────────────────────────────────────────────────────────
    // Accuracy Mode
    // ─────────────────────────────────────────────────────────────────────
    
    suspend fun setHighAccuracyMode(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[PreferenceKeys.HIGH_ACCURACY_MODE] = enabled
        }
    }
    
    // ─────────────────────────────────────────────────────────────────────
    // Onboarding
    // ─────────────────────────────────────────────────────────────────────
    
    suspend fun setOnboardingCompleted(completed: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[PreferenceKeys.ONBOARDING_COMPLETED] = completed
        }
    }
    
    // ─────────────────────────────────────────────────────────────────────
    // Reset All
    // ─────────────────────────────────────────────────────────────────────
    
    /**
     * Reset all preferences to defaults.
     */
    suspend fun resetToDefaults() {
        context.dataStore.edit { preferences ->
            preferences.clear()
        }
    }
}
