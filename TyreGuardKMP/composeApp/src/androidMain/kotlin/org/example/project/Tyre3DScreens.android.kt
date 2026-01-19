package org.example.project

import androidx.compose.runtime.Composable
import org.example.project.analysis.TireStatus
import org.example.project.tyre3d.viewer.Simple3DViewerScreen
import org.example.project.tyre3d.viewer.TyreAnalysisScreen
import org.example.project.tyre3d.viewer.ViewerMode

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

/**
 * Android actual implementation of the 3D/AR Tyre Defect Analysis Screen
 * 
 * Uses SceneView for 3D rendering and ArSceneView for AR overlay
 * Shows the tyre model with highlighted defect areas based on TireStatus
 * 
 * Features:
 * - SceneView: 3D model rendering with defect markers
 * - ArSceneView: AR mode to place tyre in real world
 * - Tap on defects to see details and recommendations
 * - Switch between 3D and AR modes
 */
@Composable
actual fun TyreDefectAnalysisPlaceholder(
    tireStatus: TireStatus,
    startInArMode: Boolean,
    onBackClick: () -> Unit
) {
    // Default model path - in production, use model from captured/converted image
    val defaultModelPath = "models/car_tyre.glb"
    
    TyreAnalysisScreen(
        tireStatus = tireStatus,
        modelPath = defaultModelPath,
        initialMode = if (startInArMode) ViewerMode.MODE_AR else ViewerMode.MODE_3D,
        onBackClick = onBackClick    )
}