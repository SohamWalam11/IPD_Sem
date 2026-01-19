package org.example.project

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import org.example.project.onboarding.EnhancedOnboardingScreen

/**
 * Android implementation with Lottie animations
 */
@Composable
actual fun PlatformOnboardingScreen(
    modifier: Modifier,
    onFinishOnboarding: () -> Unit
) {
    EnhancedOnboardingScreen(
        modifier = modifier,
        onFinishOnboarding = onFinishOnboarding
    )
}
