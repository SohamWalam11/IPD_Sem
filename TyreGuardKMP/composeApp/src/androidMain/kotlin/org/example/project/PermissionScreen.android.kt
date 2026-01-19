package org.example.project

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import org.example.project.permissions.PermissionRequestScreen

/**
 * Android implementation of permission screen
 * Uses Accompanist Permissions library for camera and location
 */
@Composable
actual fun PermissionScreenPlaceholder(
    modifier: Modifier,
    onPermissionsGranted: () -> Unit,
    onSkip: () -> Unit
) {
    PermissionRequestScreen(
        onAllPermissionsGranted = onPermissionsGranted,
        onSkip = onSkip,
        modifier = modifier
    )
}
