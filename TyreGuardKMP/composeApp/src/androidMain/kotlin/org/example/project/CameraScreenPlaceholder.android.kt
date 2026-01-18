package org.example.project

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

/**
 * Android actual implementation of CameraScreenPlaceholder
 * Uses the real CameraScreen with CameraX for tyre image capture
 */
@Composable
actual fun CameraScreenPlaceholder(
    modifier: Modifier,
    onBackClick: () -> Unit,
    onImageCaptured: (imagePath: String) -> Unit,
    onGalleryClick: () -> Unit
) {
    CameraScreen(
        modifier = modifier,
        onBackClick = onBackClick,
        onImageCaptured = { path, _ -> 
            // Pass the image path, ignoring the detection boolean
            onImageCaptured(path)
        },
        onGalleryClick = onGalleryClick
    )
}
