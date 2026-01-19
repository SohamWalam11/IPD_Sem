package org.example.project

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

/**
 * Common interface for enhanced onboarding screen
 * Android uses Lottie animations, iOS uses native animations
 */
@Composable
expect fun PlatformOnboardingScreen(
    modifier: Modifier = Modifier,
    onFinishOnboarding: () -> Unit
)
