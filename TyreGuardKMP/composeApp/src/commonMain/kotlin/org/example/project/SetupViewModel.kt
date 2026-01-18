package org.example.project

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.example.project.data.SyncResult
import org.example.project.data.UserDataRepository

/**
 * Simple shared state holder for the three-step user setup flow.
 * This does not extend Android's ViewModel so it can live in commonMain.
 */
class SetupViewModel {

    // Step 1: Personal info
    var name by mutableStateOf("")
    var dateOfBirth by mutableStateOf("") // formatted as DD/MM/YYYY
    var countryCode by mutableStateOf("+91")
    var mobileNumber by mutableStateOf("") // Max 10 digits, validated

    // Step 2: Vehicle info
    var vehicleBrand by mutableStateOf("")
    var carModel by mutableStateOf("")
    var yearOfManufacture by mutableStateOf<Int?>(null)
    var fuelVariant by mutableStateOf<String?>(null) // Petrol / Diesel / CNG / EV

    // Step 3: Usage profile
    // Plate number split into 4 parts: MH 02 AB 1234
    var plateState by mutableStateOf("")      // State code (e.g., "MH")
    var plateDistrict by mutableStateOf("")   // District code (e.g., "02") - numbers
    var plateSeries by mutableStateOf("")     // Series (e.g., "AB") - letters
    var plateNumberDigits by mutableStateOf("")// Number (e.g., "1234") - numbers
    
    var odometerReading by mutableStateOf("")
    var averageDailyDriveKm by mutableStateOf(0f)

    // Legacy support - combined plate number
    val plateNumber: String
        get() = "$plateState $plateDistrict $plateSeries $plateNumberDigits".trim()

    /**
     * Validate mobile number - must be exactly 10 digits
     */
    fun validateMobileNumber(): Boolean {
        return mobileNumber.length == 10 && mobileNumber.all { it.isDigit() }
    }

    /**
     * Validate plate number - all parts must be filled
     */
    fun validatePlateNumber(): Boolean {
        return plateState.length == 2 &&
                plateDistrict.isNotBlank() &&
                plateSeries.length == 2 &&
                plateNumberDigits.isNotBlank()
    }

    /**
     * Save all data to local storage and sync to cloud
     */
    fun saveProfile(onComplete: (Boolean) -> Unit) {
        CoroutineScope(Dispatchers.Default).launch {
            try {
                // Update personal info in repository
                UserDataRepository.updatePersonalInfo(
                    name = name,
                    dateOfBirth = dateOfBirth,
                    mobileNumber = mobileNumber
                )

                // Update vehicle info in repository
                UserDataRepository.updateVehicleInfo(
                    vehicleBrand = vehicleBrand,
                    carModel = carModel,
                    yearOfManufacture = yearOfManufacture,
                    fuelVariant = fuelVariant ?: ""
                )

                // Update usage profile in repository
                UserDataRepository.updateUsageProfile(
                    plateState = plateState,
                    plateDistrict = plateDistrict,
                    plateSeries = plateSeries,
                    plateNumber = plateNumberDigits,
                    odometerReading = odometerReading,
                    averageDailyDriveKm = averageDailyDriveKm
                )

                // Save locally and sync to cloud
                val result = UserDataRepository.saveAndSync()
                
                when (result) {
                    is SyncResult.Success -> onComplete(true)
                    is SyncResult.Error -> onComplete(false)
                }
            } catch (e: Exception) {
                onComplete(false)
            }
        }
    }
}


