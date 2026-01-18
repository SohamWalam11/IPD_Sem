package org.example.project

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.launch

enum class SetupStep {
    PersonalInfo,
    VehicleInfo,
    UsageProfile
}

/**
 * Simple in-composable navigation graph for the three setup screens.
 * Call this after login; onFinishedSetup will typically navigate to the main dashboard.
 */
@Composable
fun SetupFlow(
    viewModel: SetupViewModel = remember { SetupViewModel() },
    onFinishedSetup: () -> Unit
 ) {
    var currentStep by remember { mutableStateOf(SetupStep.PersonalInfo) }
    var isSaving by remember { mutableStateOf(false) }
    val coroutineScope = rememberCoroutineScope()

    when (currentStep) {
        SetupStep.PersonalInfo -> PersonalInfoScreen(
            viewModel = viewModel,
            onNext = { currentStep = SetupStep.VehicleInfo }
        )

        SetupStep.VehicleInfo -> VehicleInfoScreen(
            viewModel = viewModel,
            onNext = { currentStep = SetupStep.UsageProfile }
        )

        SetupStep.UsageProfile -> UsageProfileScreen(
            viewModel = viewModel,
            onSaveProfile = {
                if (!isSaving) {
                    isSaving = true
                    // Save all profile data to local storage and cloud
                    viewModel.saveProfile { success ->
                        isSaving = false
                        // Proceed to dashboard regardless of sync status
                        // (data is saved locally even if cloud sync fails)
                        onFinishedSetup()
                    }
                }
            }
        )
    }
}

