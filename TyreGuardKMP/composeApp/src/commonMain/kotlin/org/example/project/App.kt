package org.example.project

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import org.example.project.analysis.AnalysisScreen
import org.example.project.analysis.TirePosition
import org.example.project.analysis.TireStatus
import org.example.project.analysis.TreadHealth
import org.example.project.analysis.TyreDetailScreen
import org.example.project.auth.SupabaseAuthService
import org.example.project.data.UserDataRepository
import org.jetbrains.compose.ui.tooling.preview.Preview

/**
 * Main navigation state for the app flow:
 * Login -> Onboarding -> Setup -> Dashboard -> Profile/Camera/Analysis/TyreDetail
 * 
 * 3D Viewer flow: Camera (capture 2D image) -> TyreView3D (convert to 3D and display)
 * Defect Analysis flow: TyreDetail -> TyreDefectAnalysis (3D/AR defect visualization)
 */
sealed class AppScreen {
    object Login : AppScreen()
    object ForgotPassword : AppScreen()
    object Onboarding : AppScreen()
    object Permissions : AppScreen()  // Request camera & location permissions
    object Setup : AppScreen()
    object Dashboard : AppScreen()
    object Profile : AppScreen()
    object Camera : AppScreen()
    object Analysis : AppScreen()
    object Notifications : AppScreen()
    object Settings : AppScreen()
    object ServiceCenter : AppScreen()  // Nearby service centers with Google Maps
    data class TyreDetail(val tireStatus: TireStatus) : AppScreen()
    
    // 3D Viewer screen - takes image path from Camera, converts to 3D
    data class TyreView3D(val imagePath: String? = null, val modelPath: String? = null) : AppScreen()
    
    // 3D/AR Defect Analysis - shows tyre with highlighted defects (SceneView/ArSceneView)
    data class TyreDefectAnalysis(
        val tireStatus: TireStatus,
        val startInArMode: Boolean = false
    ) : AppScreen()
}

@Composable
@Preview
fun App() {
    MaterialTheme {
        var currentScreen by remember { mutableStateOf<AppScreen>(AppScreen.Login) }
        
        // Get user data from repository
        val userProfileData by UserDataRepository.userProfile.collectAsState()
        val authUser by SupabaseAuthService.currentUser.collectAsState()
        
        val currentUserName = userProfileData?.name?.ifBlank { null }
            ?: userProfileData?.displayName?.ifBlank { null }
            ?: authUser?.displayName
            ?: "User"

        when (val screen = currentScreen) {
            is AppScreen.Login -> {
                LoginScreen(
                    onLoginSuccess = {
                        // After login, show onboarding
                        currentScreen = AppScreen.Onboarding
                    },
                    onForgotPassword = {
                        currentScreen = AppScreen.ForgotPassword
                    }
                )
            }
            
            is AppScreen.ForgotPassword -> {
                ForgotPasswordScreen(
                    onBackToLogin = {
                        currentScreen = AppScreen.Login
                    }
                )
            }
            
            is AppScreen.Onboarding -> {
                // Use platform-specific onboarding (Android has Lottie animations)
                PlatformOnboardingScreen(
                    onFinishOnboarding = {
                        // After onboarding, show setup flow
                        currentScreen = AppScreen.Setup
                    }
                )
            }
            
            is AppScreen.Setup -> {
                SetupFlow(
                    onFinishedSetup = {
                        // After setup, request permissions then go to dashboard
                        currentScreen = AppScreen.Permissions
                    }
                )
            }
            
            is AppScreen.Permissions -> {
                // Platform-specific permission screen
                // On Android, shows camera/location permission requests
                // On iOS, this is handled by the platform
                PermissionScreenPlaceholder(
                    onPermissionsGranted = {
                        currentScreen = AppScreen.Dashboard
                    },
                    onSkip = {
                        currentScreen = AppScreen.Dashboard
                    }
                )
            }
            
            is AppScreen.Dashboard -> {
                DashboardScreen(
                    userName = currentUserName,
                    onCameraClick = {
                        currentScreen = AppScreen.Camera
                    },
                    onTyreClick = { tyreData ->
                        // Convert TyreData to TireStatus for detail screen
                        val tireStatus = tyreData.toTireStatus()
                        currentScreen = AppScreen.TyreDetail(tireStatus)
                    },
                    onView3DClick = {
                        currentScreen = AppScreen.TyreView3D()
                    },
                    onServiceCenterClick = { center ->
                        // TODO: handle service center click
                    },
                    onAnalysisClick = {
                        currentScreen = AppScreen.Analysis
                    },
                    onFindServiceCenter = {
                        currentScreen = AppScreen.ServiceCenter
                    }
                )
            }
            
            is AppScreen.ServiceCenter -> {
                // Platform-specific service center screen with Google Maps
                ServiceCenterScreenPlaceholder(
                    onBackClick = {
                        currentScreen = AppScreen.Dashboard
                    }
                )
            }
            
            is AppScreen.Profile -> {
                ProfileScreen(
                    userProfile = UserProfile(),
                    onBackClick = {
                        currentScreen = AppScreen.Dashboard
                    },
                    onEditProfile = {
                        // TODO: open profile edit screen
                    },
                    onNotificationsClick = {
                        currentScreen = AppScreen.Notifications
                    },
                    onLanguageClick = {
                        // TODO: open language settings
                    },
                    onHelpClick = {
                        // TODO: open help screen
                    },
                    onLogout = {
                        // Return to login on logout
                        currentScreen = AppScreen.Login
                    }
                )
            }
            
            is AppScreen.Camera -> {
                // Camera screen - platform-specific implementation
                // On Android: Uses CameraX for real capture
                // On iOS: Shows placeholder
                CameraScreenPlaceholder(
                    onBackClick = {
                        currentScreen = AppScreen.Dashboard
                    },
                    onImageCaptured = { imagePath ->
                        // Navigate to 3D viewer with the captured image
                        currentScreen = AppScreen.TyreView3D(imagePath = imagePath)
                    },
                    onGalleryClick = {
                        // TODO: Open gallery picker
                    }
                )
            }
            
            is AppScreen.Analysis -> {
                AnalysisScreen(
                    onBackClick = {
                        currentScreen = AppScreen.Dashboard
                    },
                    onTyreClick = { tire ->
                        currentScreen = AppScreen.TyreDetail(tire)
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
                        // TODO: Navigate to service scheduling
                        currentScreen = AppScreen.Dashboard
                    },
                    onView3D = {
                        currentScreen = AppScreen.TyreDefectAnalysis(
                            tireStatus = screen.tireStatus,
                            startInArMode = false
                        )
                    },
                    onViewAR = {
                        currentScreen = AppScreen.TyreDefectAnalysis(
                            tireStatus = screen.tireStatus,
                            startInArMode = true
                        )
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
            
            // 3D Viewer - Android-specific implementation
            // Takes image path, converts to 3D, and displays the model
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
            
            // 3D/AR Defect Analysis - shows tyre with highlighted defects using SceneView/ArSceneView
            is AppScreen.TyreDefectAnalysis -> {
                TyreDefectAnalysisPlaceholder(
                    tireStatus = screen.tireStatus,
                    startInArMode = screen.startInArMode,
                    onBackClick = {
                        currentScreen = AppScreen.TyreDetail(screen.tireStatus)
                    }
                )
            }
        }
    }
}

/**
 * Extension function to convert TyreData (Dashboard model) to TireStatus (Analysis model)
 */
private fun TyreData.toTireStatus(): TireStatus {
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
    )
}

/**
 * Placeholder for 3D Viewer screen - will be replaced by actual implementation on Android
 * 
 * @param imagePath Path to captured 2D image (will trigger 3D conversion)
 * @param modelPath Path to existing 3D model (if already converted)
 */
@Composable
expect fun Tyre3DViewerScreenPlaceholder(
    imagePath: String?,
    modelPath: String?,
    onBackClick: () -> Unit,
    onCaptureAnother: () -> Unit
)

/**
 * Placeholder for Service Center screen with Google Maps
 * Android: Uses Maps SDK, Places SDK for nearby tyre service centers
 * iOS: Uses MapKit (to be implemented)
 */
@Composable
expect fun ServiceCenterScreenPlaceholder(
    onBackClick: () -> Unit
)

/**
 * Placeholder for 3D/AR Tyre Defect Analysis screen
 * Uses SceneView for 3D rendering and ArSceneView for AR overlay
 * Shows tyre model with highlighted defect areas
 * 
 * @param tireStatus The tire data including defects
 * @param startInArMode Whether to start in AR mode instead of 3D
 * @param onBackClick Callback when back button pressed
 */
@Composable
expect fun TyreDefectAnalysisPlaceholder(
    tireStatus: TireStatus,
    startInArMode: Boolean,
    onBackClick: () -> Unit
)