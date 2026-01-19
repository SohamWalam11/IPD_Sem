package org.example.project

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

/**
 * Common interface for permission request screen
 * Actual implementation is platform-specific
 */
@Composable
expect fun PermissionScreenPlaceholder(
    modifier: Modifier = Modifier,
    onPermissionsGranted: () -> Unit,
    onSkip: () -> Unit
)
