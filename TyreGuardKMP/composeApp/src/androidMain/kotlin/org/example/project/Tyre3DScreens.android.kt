package org.example.project

import androidx.compose.runtime.Composable
import org.example.project.tyre3d.viewer.Simple3DViewerScreen

/**
 * Android actual implementation of the 3D Viewer Screen
 * 
 * Workflow:
 * 1. User captures 2D image in CameraScreen
 * 2. Image path is passed here
 * 3. ImageTo3DService converts image to 3D model using Meshy AI
 * 4. SceneView renders the 3D GLB model with interactive controls
 */
@Composable
actual fun Tyre3DViewerScreenPlaceholder(
    imagePath: String?,
    modelPath: String?,
    onBackClick: () -> Unit,
    onCaptureAnother: () -> Unit
) {
    Simple3DViewerScreen(
        imagePath = imagePath,
        modelPath = modelPath,
        onNavigateBack = onBackClick,
        onCaptureAnother = onCaptureAnother
    )
}
