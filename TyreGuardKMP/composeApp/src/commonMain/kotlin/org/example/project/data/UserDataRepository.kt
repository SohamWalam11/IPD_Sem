package org.example.project.data

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * User Profile Data - Complete user information
 * Stored locally and synced to cloud
 */
data class UserProfileData(
    val userId: String = "",
    
    // Personal Info (from Sign In)
    val email: String = "",
    val displayName: String = "",
    val authProvider: String = "email", // email, google, phone
    
    // Personal Info (from Setup)
    val name: String = "",
    val dateOfBirth: String = "",
    
    // Cloud-synced fields (for notifications & ML)
    val mobileNumber: String = "",        // 1. Mobile Number (max 10 digits)
    val vehicleBrand: String = "",        // 2. Vehicle Brand
    val carModel: String = "",            // 3. Car Model
    val yearOfManufacture: Int? = null,   // 4. Year of Manufacturing
    val fuelVariant: String = "",         // 4. Fuel Variant
    
    // Plate Number (4 parts: MH XX XX XXXX)
    val plateState: String = "",          // e.g., "MH"
    val plateDistrict: String = "",       // e.g., "02" (numbers)
    val plateSeries: String = "",         // e.g., "AB" (letters)
    val plateNumber: String = "",         // e.g., "1234" (numbers)
    
    // Usage Profile (cloud-synced for ML)
    val odometerReading: String = "",     // 5. Current Odometer Reading
    val averageDailyDriveKm: Float = 0f,  // 6. Average Daily Drive
    
    // Timestamps
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
) {
    /**
     * Get full plate number formatted
     */
    val fullPlateNumber: String
        get() = if (plateState.isNotBlank()) {
            "$plateState $plateDistrict $plateSeries $plateNumber".trim()
        } else ""
    
    /**
     * Check if profile is complete
     */
    val isComplete: Boolean
        get() = name.isNotBlank() &&
                mobileNumber.length == 10 &&
                vehicleBrand.isNotBlank() &&
                carModel.isNotBlank() &&
                yearOfManufacture != null &&
                fuelVariant.isNotBlank() &&
                plateState.isNotBlank() &&
                plateNumber.isNotBlank() &&
                odometerReading.isNotBlank()
}

/**
 * Data to be synced to cloud for ML analysis and notifications
 */
data class CloudSyncData(
    val userId: String,
    val mobileNumber: String,      // For notifications
    val vehicleBrand: String,      // For ML
    val carModel: String,          // For ML
    val yearOfManufacture: Int?,   // For ML
    val fuelVariant: String,       // For ML
    val plateNumber: String,       // Full plate for reference
    val odometerReading: String,   // For ML wear prediction
    val averageDailyDriveKm: Float,// For ML usage patterns
    val updatedAt: Long = System.currentTimeMillis()
)

/**
 * Repository for managing user data
 * Handles local storage and cloud synchronization
 */
object UserDataRepository {
    
    private val _userProfile = MutableStateFlow<UserProfileData?>(null)
    val userProfile: StateFlow<UserProfileData?> = _userProfile.asStateFlow()
    
    private val _syncState = MutableStateFlow<SyncState>(SyncState.Idle)
    val syncState: StateFlow<SyncState> = _syncState.asStateFlow()
    
    // In-memory storage (replace with actual local storage implementation)
    private var localUserData: UserProfileData? = null
    
    /**
     * Initialize repository with user ID from authentication
     */
    fun initialize(userId: String, email: String, displayName: String, authProvider: String) {
        val existingData = localUserData
        if (existingData == null || existingData.userId != userId) {
            localUserData = UserProfileData(
                userId = userId,
                email = email,
                displayName = displayName,
                authProvider = authProvider
            )
        }
        _userProfile.value = localUserData
    }
    
    /**
     * Update personal info (Step 1)
     */
    fun updatePersonalInfo(
        name: String,
        dateOfBirth: String,
        mobileNumber: String
    ) {
        localUserData = localUserData?.copy(
            name = name,
            dateOfBirth = dateOfBirth,
            mobileNumber = mobileNumber.take(10), // Ensure max 10 digits
            updatedAt = System.currentTimeMillis()
        )
        _userProfile.value = localUserData
    }
    
    /**
     * Update vehicle info (Step 2)
     */
    fun updateVehicleInfo(
        vehicleBrand: String,
        carModel: String,
        yearOfManufacture: Int?,
        fuelVariant: String
    ) {
        localUserData = localUserData?.copy(
            vehicleBrand = vehicleBrand,
            carModel = carModel,
            yearOfManufacture = yearOfManufacture,
            fuelVariant = fuelVariant,
            updatedAt = System.currentTimeMillis()
        )
        _userProfile.value = localUserData
    }
    
    /**
     * Update usage profile (Step 3)
     */
    fun updateUsageProfile(
        plateState: String,
        plateDistrict: String,
        plateSeries: String,
        plateNumber: String,
        odometerReading: String,
        averageDailyDriveKm: Float
    ) {
        localUserData = localUserData?.copy(
            plateState = plateState.uppercase().take(2),
            plateDistrict = plateDistrict.take(2),
            plateSeries = plateSeries.uppercase().take(2),
            plateNumber = plateNumber.take(4),
            odometerReading = odometerReading,
            averageDailyDriveKm = averageDailyDriveKm,
            updatedAt = System.currentTimeMillis()
        )
        _userProfile.value = localUserData
    }
    
    /**
     * Save all data locally
     */
    suspend fun saveLocally(): Boolean {
        return try {
            // TODO: Implement actual local storage (SharedPreferences, DataStore, or SQLite)
            // For now, data is kept in memory
            true
        } catch (e: Exception) {
            false
        }
    }
    
    /**
     * Sync numbered data to cloud (for notifications and ML)
     */
    suspend fun syncToCloud(): SyncResult {
        _syncState.value = SyncState.Syncing
        
        return try {
            val userData = localUserData ?: return SyncResult.Error("No user data to sync")
            
            val cloudData = CloudSyncData(
                userId = userData.userId,
                mobileNumber = userData.mobileNumber,
                vehicleBrand = userData.vehicleBrand,
                carModel = userData.carModel,
                yearOfManufacture = userData.yearOfManufacture,
                fuelVariant = userData.fuelVariant,
                plateNumber = userData.fullPlateNumber,
                odometerReading = userData.odometerReading,
                averageDailyDriveKm = userData.averageDailyDriveKm
            )
            
            // TODO: Implement actual Supabase sync
            // For now, simulate network delay
            kotlinx.coroutines.delay(1000)
            
            _syncState.value = SyncState.Success
            SyncResult.Success
        } catch (e: Exception) {
            _syncState.value = SyncState.Error(e.message ?: "Sync failed")
            SyncResult.Error(e.message ?: "Sync failed")
        }
    }
    
    /**
     * Complete profile save - saves locally and syncs to cloud
     */
    suspend fun saveAndSync(): SyncResult {
        saveLocally()
        return syncToCloud()
    }
    
    /**
     * Load user data from local storage
     */
    suspend fun loadFromLocal(userId: String): UserProfileData? {
        // TODO: Implement actual local storage loading
        return localUserData?.takeIf { it.userId == userId }
    }
    
    /**
     * Clear all user data (on logout)
     */
    fun clear() {
        localUserData = null
        _userProfile.value = null
        _syncState.value = SyncState.Idle
    }
}

/**
 * Sync state for UI feedback
 */
sealed class SyncState {
    object Idle : SyncState()
    object Syncing : SyncState()
    object Success : SyncState()
    data class Error(val message: String) : SyncState()
}

/**
 * Result of sync operation
 */
sealed class SyncResult {
    object Success : SyncResult()
    data class Error(val message: String) : SyncResult()
}
