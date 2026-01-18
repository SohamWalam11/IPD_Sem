package org.example.project

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import org.example.project.analysis.TirePosition
import org.example.project.analysis.TireStatus
import org.example.project.analysis.TreadHealth
import org.example.project.analysis.TyreDetailScreen
import org.example.project.auth.SupabaseAuthService
import org.example.project.data.UserDataRepository

/**
 * Android-specific App implementation that uses the actual CameraScreen.
 * This overrides the common App function for Android.
 */
@Composable
fun AndroidApp() {
    MaterialTheme {
        val context = LocalContext.current
        var currentScreen by remember { mutableStateOf<AppScreen>(AppScreen.Login) }
        
        // Request notification permission on Android 13+
        val notificationPermissionLauncher = rememberLauncherForActivityResult(
            contract = ActivityResultContracts.RequestPermission()
        ) { isGranted ->
            // Permission result handled - user can proceed regardless
        }
        
        LaunchedEffect(Unit) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                val hasPermission = ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.POST_NOTIFICATIONS
                ) == android.content.pm.PackageManager.PERMISSION_GRANTED
                
                if (!hasPermission) {
                    notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                }
            }
        }

        val screen = currentScreen
        when (screen) {
            is AppScreen.Login -> {
                // Use Android-specific login with Credential Manager
                AndroidLoginScreen(
                    onLoginSuccess = {
                        currentScreen = AppScreen.Onboarding
                    }
                )
            }
            
            is AppScreen.Onboarding -> {
                OnboardingScreen(
                    onFinishOnboarding = {
                        currentScreen = AppScreen.Setup
                    }
                )
            }
            
            is AppScreen.Setup -> {
                SetupFlow(
                    onFinishedSetup = {
                        currentScreen = AppScreen.Dashboard
                    }
                )
            }
            
            is AppScreen.Dashboard -> {
                // Get user data from repository
                val userProfileData by UserDataRepository.userProfile.collectAsState()
                val authUser by SupabaseAuthService.currentUser.collectAsState()
                
                val displayUserName = userProfileData?.name?.ifBlank { null }
                    ?: userProfileData?.displayName?.ifBlank { null }
                    ?: authUser?.displayName
                    ?: "User"
                
                DashboardScreen(
                    userName = displayUserName,
                    onCameraClick = {
                        currentScreen = AppScreen.Camera
                    },
                    onTyreClick = { tyreData ->
                        // Convert TyreData to TireStatus for detail screen
                        val tireStatus = tyreData.toTireStatusAndroid()
                        currentScreen = AppScreen.TyreDetail(tireStatus)
                    },
                    onView3DClick = {
                        currentScreen = AppScreen.TyreView3D()
                    },
                    onServiceCenterClick = { center ->
                        // TODO: handle service center click
                    }
                )
            }
            
            is AppScreen.Profile -> {
                ProfileScreen(
                    userProfile = UserProfile(),
                    onBackClick = {
                        currentScreen = AppScreen.Dashboard
                    },
                    onEditProfile = {},
                    onNotificationsClick = {},
                    onLanguageClick = {},
                    onHelpClick = {},
                    onLogout = {
                        currentScreen = AppScreen.Login
                    }
                )
            }
            
            is AppScreen.Camera -> {
                // Use the actual Android CameraScreen
                CameraScreen(
                    onBackClick = {
                        currentScreen = AppScreen.Dashboard
                    },
                    onImageCaptured = { imagePath, detected ->
                        // Navigate to 3D viewer with captured image
                        currentScreen = AppScreen.TyreView3D(imagePath = imagePath)
                    },
                    onGalleryClick = {
                        // TODO: Open gallery picker
                    }
                )
            }
            
            is AppScreen.Analysis -> {
                org.example.project.analysis.AnalysisScreen(
                    onBackClick = {
                        currentScreen = AppScreen.Dashboard
                    }
                )
            }
            
            is AppScreen.Notifications -> {
                NotificationsScreen(
                    onBackClick = {
                        currentScreen = AppScreen.Dashboard
                    }
                )
            }
            
            is AppScreen.Settings -> {
                SettingsScreen(
                    onBackClick = {
                        currentScreen = AppScreen.Dashboard
                    }
                )
            }
            
            is AppScreen.TyreDetail -> {
                TyreDetailScreen(
                    tireStatus = screen.tireStatus,
                    onBack = {
                        currentScreen = AppScreen.Dashboard
                    },
                    onScheduleService = {
                        currentScreen = AppScreen.Dashboard
                    }
                )
            }
            
            is AppScreen.TyreView3D -> {
                Tyre3DViewerScreenPlaceholder(
                    imagePath = screen.imagePath,
                    modelPath = screen.modelPath,
                    onBackClick = {
                        currentScreen = AppScreen.Dashboard
                    },
                    onCaptureAnother = {
                        currentScreen = AppScreen.Camera
                    }
                )
            }
        }
    }
}

/**
 * Extension function to convert TyreData (Dashboard model) to TireStatus (Analysis model)
 */
private fun TyreData.toTireStatusAndroid(): TireStatus {
    val position = when (id) {
        "FL" -> TirePosition.FRONT_LEFT
        "FR" -> TirePosition.FRONT_RIGHT
        "RL" -> TirePosition.REAR_LEFT
        "RR" -> TirePosition.REAR_RIGHT
        else -> TirePosition.FRONT_LEFT
    }
    
    val treadHealth = when {
        treadDepth >= 6f -> TreadHealth.EXCELLENT
        treadDepth >= 4.5f -> TreadHealth.GOOD
        treadDepth >= 3f -> TreadHealth.FAIR
        treadDepth >= 1.6f -> TreadHealth.WORN
        else -> TreadHealth.CRITICAL
    }
    
    val defects = if (status == TyreStatus.CRITICAL) {
        listOf("Inspection Required")
    } else {
        emptyList()
    }
    
    return TireStatus(
        position = position,
        pressurePsi = psi,
        temperatureCelsius = temp.toFloat(),
        treadHealth = treadHealth,
        defects = defects
    )}