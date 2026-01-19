package org.example.project

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

/**
 * iOS implementation - uses the default onboarding screen
 * (Lottie not available on iOS via this route, could use Lottie-ios separately)
 */
@Composable
actual fun PlatformOnboardingScreen(
    modifier: Modifier,
    onFinishOnboarding: () -> Unit
) {
    // Use the standard onboarding screen for iOS
    OnboardingScreen(
        modifier = modifier,
        onFinishOnboarding = onFinishOnboarding
    )
}
