package org.example.project

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

/**
 * Camera screen placeholder declaration for commonMain.
 * The actual camera functionality is implemented in platform-specific modules.
 * 
 * - Android: Uses CameraX with real camera capture
 * - iOS: Shows placeholder message (camera not yet supported)
 * 
 * @param modifier Modifier for the composable
 * @param onBackClick Callback when back button is pressed
 * @param onImageCaptured Callback when an image is captured, provides the image path
 * @param onGalleryClick Callback when gallery button is clicked
 */
@Composable
expect fun CameraScreenPlaceholder(
    modifier: Modifier = Modifier,
    onBackClick: () -> Unit = {},
    onImageCaptured: (imagePath: String) -> Unit = {},
    onGalleryClick: () -> Unit = {}
)
