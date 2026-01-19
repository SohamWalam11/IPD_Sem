package org.example.project

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import org.example.project.analysis.TirePosition
import org.example.project.analysis.TireStatus
import org.example.project.analysis.TreadHealth
import org.example.project.analysis.TyreDetailScreen
import org.example.project.auth.SupabaseAuthService
import org.example.project.data.UserDataRepository
import org.example.project.data.local.CachedUserData
import org.example.project.data.local.UserPreferencesManager

/**
 * Android-specific App implementation that uses the actual CameraScreen.
 * This overrides the common App function for Android.
 * 
 * Uses Jetpack DataStore for persistent login - users don't need to 
 * log in every time after initial authentication.
 */
@Composable
fun AndroidApp() {
    MaterialTheme {
        val context = LocalContext.current
        val scope = rememberCoroutineScope()
        
        // DataStore for persistent login
        val userPreferences = remember { UserPreferencesManager.getInstance(context) }
        
        // Loading state while checking saved login
        var isCheckingLogin by remember { mutableStateOf(true) }
        var currentScreen by remember { mutableStateOf<AppScreen>(AppScreen.Login) }
        
        // Collect persistent login state from DataStore
        val isLoggedIn by userPreferences.isLoggedIn.collectAsState(initial = false)
        val hasCompletedOnboarding by userPreferences.hasCompletedOnboarding.collectAsState(initial = false)
        val hasCompletedSetup by userPreferences.hasCompletedSetup.collectAsState(initial = false)
        val cachedUserData by userPreferences.cachedUserData.collectAsState(initial = null)
        
        // Determine initial screen based on saved state
        LaunchedEffect(isLoggedIn, hasCompletedOnboarding, hasCompletedSetup) {
            if (isCheckingLogin) {
                // Small delay to ensure DataStore has loaded
                kotlinx.coroutines.delay(100)
                
                currentScreen = when {
                    !isLoggedIn -> AppScreen.Login
                    !hasCompletedOnboarding -> AppScreen.Onboarding
                    !hasCompletedSetup -> AppScreen.Setup
                    else -> AppScreen.Dashboard
                }
                isCheckingLogin = false
            }
        }
        
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
        
        // Show loading while checking login state
        if (isCheckingLogin) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
            return@MaterialTheme
        }

        val screen = currentScreen
        when (screen) {
            is AppScreen.Login -> {
                // Use Android-specific login with Credential Manager
                AndroidLoginScreen(
                    onLoginSuccess = {
                        // Save login state to DataStore
                        scope.launch {
                            val authUser = SupabaseAuthService.currentUser.value
                            val userProfile = UserDataRepository.userProfile.value
                            val userData = CachedUserData(
                                id = authUser?.id ?: "",
                                email = authUser?.email ?: userProfile?.email ?: "",
                                name = userProfile?.name ?: authUser?.displayName ?: "",
                                displayName = authUser?.displayName ?: userProfile?.displayName ?: "",
                                photoUrl = null,  // Add later if profile has photo
                                authProvider = userProfile?.authProvider ?: "google",
                                lastLoginTimestamp = System.currentTimeMillis()
                            )
                            userPreferences.completeLogin(userData)
                        }
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
                // Use enhanced onboarding with Lottie animations
                org.example.project.onboarding.EnhancedOnboardingScreen(
                    onFinishOnboarding = {
                        // Save onboarding completion to DataStore
                        scope.launch {
                            userPreferences.setOnboardingCompleted(true)
                        }
                        currentScreen = AppScreen.Setup
                    }
                )
            }
            
            is AppScreen.Setup -> {
                SetupFlow(
                    onFinishedSetup = {
                        // Save setup completion to DataStore
                        scope.launch {
                            userPreferences.setSetupCompleted(true)
                        }
                        // After setup, request permissions
                        currentScreen = AppScreen.Permissions
                    }
                )
            }
            
            is AppScreen.Permissions -> {
                org.example.project.permissions.PermissionRequestScreen(
                    onAllPermissionsGranted = {
                        currentScreen = AppScreen.Dashboard
                    },
                    onSkip = {
                        currentScreen = AppScreen.Dashboard
                    }
                )
            }
            
            is AppScreen.Dashboard -> {
                // Get user data from repository or cached data
                val userProfileData by UserDataRepository.userProfile.collectAsState()
                val authUser by SupabaseAuthService.currentUser.collectAsState()
                
                // Use cached data if available (for quick load after restart)
                val displayUserName = userProfileData?.name?.ifBlank { null }
                    ?: userProfileData?.displayName?.ifBlank { null }
                    ?: cachedUserData?.name?.ifBlank { null }
                    ?: cachedUserData?.displayName?.ifBlank { null }
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
                // Use the actual Android Service Center screen with Google Maps
                org.example.project.locations.ServiceCenterScreen(
                    onBackClick = {
                        currentScreen = AppScreen.Dashboard
                    },
                    onNavigateToPlace = { place ->
                        // Open Google Maps navigation
                        val gmmIntentUri = android.net.Uri.parse(
                            "google.navigation:q=${place.latLng.latitude},${place.latLng.longitude}"
                        )
                        val mapIntent = android.content.Intent(android.content.Intent.ACTION_VIEW, gmmIntentUri)
                        mapIntent.setPackage("com.google.android.apps.maps")
                        context.startActivity(mapIntent)
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
                        // Clear DataStore on logout
                        scope.launch {
                            userPreferences.logout()
                            SupabaseAuthService.signOut()
                        }
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
            
            // 3D/AR Defect Analysis Screen - SceneView/ArSceneView
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