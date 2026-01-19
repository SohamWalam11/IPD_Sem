package org.example.project

import androidx.compose.runtime.Composable
import org.example.project.locations.ServiceCenterScreen

/**
 * Android actual implementation for ServiceCenterScreen
 * Uses Google Maps SDK, Places SDK for nearby tyre service centers
 */
@Composable
actual fun ServiceCenterScreenPlaceholder(
    onBackClick: () -> Unit
) {
    ServiceCenterScreen(
        onBackClick = onBackClick,
        onNavigateToPlace = { /* Navigation handled in AndroidApp */ }
    )
}
