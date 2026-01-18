package org.example.project.ar

import android.util.Log
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Thermostat
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.ar.core.Config
import com.google.ar.core.Frame
import com.google.ar.core.Plane
import com.google.ar.core.TrackingFailureReason
import com.google.ar.core.TrackingState
import io.github.sceneview.ar.ARScene
import io.github.sceneview.ar.node.AnchorNode
import io.github.sceneview.ar.rememberARCameraNode
import io.github.sceneview.math.Position
import io.github.sceneview.node.ModelNode
import io.github.sceneview.rememberCollisionSystem
import io.github.sceneview.rememberEngine
import io.github.sceneview.rememberMaterialLoader
import io.github.sceneview.rememberModelLoader
import io.github.sceneview.rememberNodes
import io.github.sceneview.rememberView
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.io.File

private const val TAG = "ArTyreScreen"

// ═══════════════════════════════════════════════════════════════════════════════
// Premium AR Theme Colors
// ═══════════════════════════════════════════════════════════════════════════════
private val ArDarkBg = Color(0xCC121212)
private val ArCardBg = Color(0xE61E1E1E)
private val ArAccent = Color(0xFF2979FF)
private val ArGreen = Color(0xFF00E676)
private val ArOrange = Color(0xFFFFAB00)
private val ArRed = Color(0xFFE53935)
private val ArTextPrimary = Color(0xFFFFFFFF)
private val ArTextSecondary = Color(0xB3FFFFFF)

/**
 * ═══════════════════════════════════════════════════════════════════════════════
 * ArTyreScreen - Main AR Experience Composable
 * ═══════════════════════════════════════════════════════════════════════════════
 * 
 * This composable provides the full AR tyre visualization experience:
 * - ARCore-powered camera with plane detection
 * - 3D tyre model placement on detected surfaces
 * - Floating spatial UI panels showing tyre health data
 * - Touch gestures for model manipulation (rotate, scale)
 * 
 * @param modelPath Optional path to a custom GLB model file
 * @param onBackPressed Callback when user wants to exit AR
 */
@Composable
fun ArTyreScreen(
    modelPath: String? = null,
    onBackPressed: () -> Unit = {}
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    
    // ═══════════════════════════════════════════════════════════════════════════
    // AR State Management
    // ═══════════════════════════════════════════════════════════════════════════
    var arState by remember { mutableStateOf(ArState.SEARCHING) }
    var trackingFailureReason by remember { mutableStateOf<TrackingFailureReason?>(null) }
    var isModelPlaced by remember { mutableStateOf(false) }
    var isModelLoading by remember { mutableStateOf(false) }
    var showDataPanels by remember { mutableStateOf(false) }
    var placedAnchorNode by remember { mutableStateOf<AnchorNode?>(null) }
    
    // ═══════════════════════════════════════════════════════════════════════════
    // Dummy Tyre Health Data (replace with real sensor data in production)
    // ═══════════════════════════════════════════════════════════════════════════
    val tyreData = remember {
        TyreHealthData(
            pressure = "32 PSI",
            temperature = "45°C",
            status = TyreStatus.OPTIMAL,
            treadDepth = "6.2 mm",
            lastUpdated = "2 min ago"
        )
    }
    
    // ═══════════════════════════════════════════════════════════════════════════
    // SceneView/Filament Components
    // ═══════════════════════════════════════════════════════════════════════════
    val engine = rememberEngine()
    val modelLoader = rememberModelLoader(engine)
    val materialLoader = rememberMaterialLoader(engine)
    val view = rememberView(engine)
    val collisionSystem = rememberCollisionSystem(view)
    val cameraNode = rememberARCameraNode(engine)
    val childNodes = rememberNodes()
    
    // Show data panels after model is placed
    LaunchedEffect(isModelPlaced) {
        if (isModelPlaced) {
            delay(500) // Small delay for visual effect
            showDataPanels = true
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        // ═══════════════════════════════════════════════════════════════════════
        // AR Scene - The main AR camera view
        // ═══════════════════════════════════════════════════════════════════════
        ARScene(
            modifier = Modifier.fillMaxSize(),
            engine = engine,
            view = view,
            modelLoader = modelLoader,
            collisionSystem = collisionSystem,
            childNodes = childNodes,
            cameraNode = cameraNode,
            planeRenderer = true, // Show detected planes
            sessionConfiguration = { session, config ->
                // Configure ARCore session
                config.planeFindingMode = Config.PlaneFindingMode.HORIZONTAL_AND_VERTICAL
                config.depthMode = when {
                    session.isDepthModeSupported(Config.DepthMode.AUTOMATIC) -> 
                        Config.DepthMode.AUTOMATIC
                    else -> Config.DepthMode.DISABLED
                }
                config.lightEstimationMode = Config.LightEstimationMode.ENVIRONMENTAL_HDR
                config.instantPlacementMode = Config.InstantPlacementMode.LOCAL_Y_UP
            },
            onSessionUpdated = { session, frame ->
                // Update AR state based on tracking
                updateArState(frame) { newState, failureReason ->
                    arState = newState
                    trackingFailureReason = failureReason
                }
            },
            onGestureListener = rememberArGestureListener(
                onSingleTapConfirmed = { motionEvent, node ->
                    // Only handle tap if model not yet placed
                    if (!isModelPlaced && !isModelLoading && arState == ArState.PLANE_DETECTED) {
                        isModelLoading = true
                        
                        scope.launch {
                            try {
                                // Use the ARScene's built-in hit testing via session
                                val session = cameraNode.session ?: return@launch
                                val frame = session.update()
                                val hitResults = frame.hitTest(motionEvent)
                                
                                // Find first plane hit
                                val planeHit = hitResults.firstOrNull { hitResult ->
                                    (hitResult.trackable as? Plane)?.let { plane ->
                                        plane.isPoseInPolygon(hitResult.hitPose) &&
                                        plane.trackingState == TrackingState.TRACKING
                                    } ?: false
                                }
                                
                                if (planeHit != null) {
                                    // Create anchor at hit location
                                    val anchor = planeHit.createAnchor()
                                    
                                    val anchorNode = AnchorNode(
                                        engine = engine,
                                        anchor = anchor
                                    )
                                    
                                    // Load the tyre model
                                    val modelInstance = loadTyreModel(
                                        modelLoader = modelLoader,
                                        customPath = modelPath
                                    )
                                    
                                    if (modelInstance != null) {
                                        val modelNode = ModelNode(
                                            modelInstance = modelInstance,
                                            scaleToUnits = 0.5f // 50cm model
                                        ).apply {
                                            position = Position(0f, 0.1f, 0f) // Slightly above ground
                                            isEditable = true // Enable gesture manipulation
                                        }
                                        
                                        anchorNode.addChildNode(modelNode)
                                        childNodes += anchorNode
                                        placedAnchorNode = anchorNode
                                        isModelPlaced = true
                                        arState = ArState.MODEL_PLACED
                                        
                                        Log.d(TAG, "Model placed successfully")
                                    } else {
                                        Log.e(TAG, "Failed to load model")
                                        anchor.detach()
                                    }
                                }
                            } catch (e: Exception) {
                                Log.e(TAG, "Error placing model: ${e.message}", e)
                            } finally {
                                isModelLoading = false
                            }
                        }
                    }
                }
            )
        )
        
        // ═══════════════════════════════════════════════════════════════════════
        // Overlay UI - Status indicators and controls
        // ═══════════════════════════════════════════════════════════════════════
        Column(modifier = Modifier.fillMaxSize()) {
            // Top Bar
            ArTopBar(
                onBackPressed = onBackPressed,
                onResetPressed = {
                    // Reset the AR scene
                    placedAnchorNode?.let { anchor ->
                        childNodes -= anchor
                        anchor.destroy()
                    }
                    placedAnchorNode = null
                    isModelPlaced = false
                    showDataPanels = false
                    arState = ArState.SEARCHING
                },
                showReset = isModelPlaced
            )
            
            Spacer(modifier = Modifier.weight(1f))
            
            // Bottom Section - Status or Data Panels
            AnimatedVisibility(
                visible = showDataPanels && isModelPlaced,
                enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
                exit = slideOutVertically(targetOffsetY = { it }) + fadeOut()
            ) {
                SpatialDataPanels(tyreData = tyreData)
            }
            
            // AR Status Indicator (when not showing data panels)
            AnimatedVisibility(
                visible = !showDataPanels,
                enter = fadeIn(),
                exit = fadeOut()
            ) {
                ArStatusIndicator(
                    arState = arState,
                    isModelLoading = isModelLoading,
                    trackingFailureReason = trackingFailureReason
                )
            }
        }
    }
}

/**
 * AR State Machine
 */
enum class ArState {
    SEARCHING,      // Looking for surfaces
    PLANE_DETECTED, // Surface found, ready to place
    MODEL_PLACED,   // Model anchored in scene
    TRACKING_LOST   // Tracking temporarily lost
}

/**
 * Tyre Status Enum
 */
enum class TyreStatus(val displayText: String, val color: Color) {
    OPTIMAL("Optimal", ArGreen),
    WARNING("Warning", ArOrange),
    CRITICAL("Critical", ArRed)
}

/**
 * Tyre Health Data Container
 */
data class TyreHealthData(
    val pressure: String,
    val temperature: String,
    val status: TyreStatus,
    val treadDepth: String,
    val lastUpdated: String
)

// ═══════════════════════════════════════════════════════════════════════════════
// Helper Functions
// ═══════════════════════════════════════════════════════════════════════════════

/**
 * Update AR state based on current frame tracking
 */
private fun updateArState(
    frame: Frame,
    onStateChanged: (ArState, TrackingFailureReason?) -> Unit
) {
    val camera = frame.camera
    
    when (camera.trackingState) {
        TrackingState.TRACKING -> {
            // Check if any planes are detected
            val hasPlanes = frame.getUpdatedTrackables(Plane::class.java)
                .any { it.trackingState == TrackingState.TRACKING }
            
            if (hasPlanes) {
                onStateChanged(ArState.PLANE_DETECTED, null)
            } else {
                onStateChanged(ArState.SEARCHING, null)
            }
        }
        TrackingState.PAUSED -> {
            onStateChanged(ArState.TRACKING_LOST, camera.trackingFailureReason)
        }
        TrackingState.STOPPED -> {
            onStateChanged(ArState.TRACKING_LOST, camera.trackingFailureReason)
        }
    }
}

/**
 * Load the tyre GLB model
 * Priority: Custom path -> Local assets -> Remote Sketchfab model
 * 
 * The Rally Wheel model from Sketchfab by Kryox Shade:
 * https://sketchfab.com/3d-models/rally-wheel-c580152ebef94978920ad13c730e4eec
 */
private suspend fun loadTyreModel(
    modelLoader: io.github.sceneview.loaders.ModelLoader,
    customPath: String? = null
): io.github.sceneview.model.ModelInstance? {
    // List of model sources to try in order
    val modelSources = listOfNotNull(
        customPath?.takeIf { File(it).exists() },
        "models/rally_wheel.glb",  // Local asset - download from Sketchfab
        "models/tyre.glb",         // Fallback local asset
        // Remote GLB models for fallback (free tire/wheel models)
        "https://modelviewer.dev/shared-assets/models/Astronaut.glb" // Demo fallback
    )
    
    for (source in modelSources) {
        try {
            Log.d(TAG, "Attempting to load model from: $source")
            val model = modelLoader.loadModelInstance(source)
            if (model != null) {
                Log.d(TAG, "Successfully loaded model from: $source")
                return model
            }
        } catch (e: Exception) {
            Log.w(TAG, "Failed to load model from $source: ${e.message}")
            continue
        }
    }
    
    Log.e(TAG, "Failed to load any model")
    return null
}

/**
 * Create gesture listener for AR scene - using SimpleOnGestureListener pattern
 */
@Composable
private fun rememberArGestureListener(
    onSingleTapConfirmed: (android.view.MotionEvent, io.github.sceneview.node.Node?) -> Unit
): io.github.sceneview.gesture.GestureDetector.OnGestureListener {
    return remember {
        object : io.github.sceneview.gesture.GestureDetector.OnGestureListener {
            override fun onDown(e: android.view.MotionEvent, node: io.github.sceneview.node.Node?) {}
            override fun onShowPress(e: android.view.MotionEvent, node: io.github.sceneview.node.Node?) {}
            override fun onSingleTapUp(e: android.view.MotionEvent, node: io.github.sceneview.node.Node?) {}
            override fun onScroll(
                e1: android.view.MotionEvent?,
                e2: android.view.MotionEvent,
                node: io.github.sceneview.node.Node?,
                distance: dev.romainguy.kotlin.math.Float2
            ) {}
            override fun onLongPress(e: android.view.MotionEvent, node: io.github.sceneview.node.Node?) {}
            override fun onFling(
                e1: android.view.MotionEvent?,
                e2: android.view.MotionEvent,
                node: io.github.sceneview.node.Node?,
                velocity: dev.romainguy.kotlin.math.Float2
            ) {}
            override fun onSingleTapConfirmed(e: android.view.MotionEvent, node: io.github.sceneview.node.Node?) {
                onSingleTapConfirmed(e, node)
            }
            override fun onDoubleTap(e: android.view.MotionEvent, node: io.github.sceneview.node.Node?) {}
            override fun onDoubleTapEvent(e: android.view.MotionEvent, node: io.github.sceneview.node.Node?) {}
            override fun onContextClick(e: android.view.MotionEvent, node: io.github.sceneview.node.Node?) {}
            override fun onMoveBegin(detector: io.github.sceneview.gesture.MoveGestureDetector, e: android.view.MotionEvent, node: io.github.sceneview.node.Node?) {}
            override fun onMove(detector: io.github.sceneview.gesture.MoveGestureDetector, e: android.view.MotionEvent, node: io.github.sceneview.node.Node?) {}
            override fun onMoveEnd(detector: io.github.sceneview.gesture.MoveGestureDetector, e: android.view.MotionEvent, node: io.github.sceneview.node.Node?) {}
            override fun onRotateBegin(detector: io.github.sceneview.gesture.RotateGestureDetector, e: android.view.MotionEvent, node: io.github.sceneview.node.Node?) {}
            override fun onRotate(detector: io.github.sceneview.gesture.RotateGestureDetector, e: android.view.MotionEvent, node: io.github.sceneview.node.Node?) {}
            override fun onRotateEnd(detector: io.github.sceneview.gesture.RotateGestureDetector, e: android.view.MotionEvent, node: io.github.sceneview.node.Node?) {}
            override fun onScaleBegin(detector: io.github.sceneview.gesture.ScaleGestureDetector, e: android.view.MotionEvent, node: io.github.sceneview.node.Node?) {}
            override fun onScale(detector: io.github.sceneview.gesture.ScaleGestureDetector, e: android.view.MotionEvent, node: io.github.sceneview.node.Node?) {}
            override fun onScaleEnd(detector: io.github.sceneview.gesture.ScaleGestureDetector, e: android.view.MotionEvent, node: io.github.sceneview.node.Node?) {}
        }
    }
}

// ═══════════════════════════════════════════════════════════════════════════════
// UI Components
// ═══════════════════════════════════════════════════════════════════════════════

@Composable
private fun ArTopBar(
    onBackPressed: () -> Unit,
    onResetPressed: () -> Unit,
    showReset: Boolean
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Back Button
        FilledIconButton(
            onClick = onBackPressed,
            colors = IconButtonDefaults.filledIconButtonColors(
                containerColor = ArDarkBg,
                contentColor = ArTextPrimary
            )
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = "Back"
            )
        }
        
        // Title
        Text(
            text = "AR Tyre Viewer",
            color = ArTextPrimary,
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier
                .background(ArDarkBg, RoundedCornerShape(20.dp))
                .padding(horizontal = 16.dp, vertical = 8.dp)
        )
        
        // Reset Button
        AnimatedVisibility(visible = showReset) {
            FilledIconButton(
                onClick = onResetPressed,
                colors = IconButtonDefaults.filledIconButtonColors(
                    containerColor = ArDarkBg,
                    contentColor = ArTextPrimary
                )
            ) {
                Icon(
                    imageVector = Icons.Default.Refresh,
                    contentDescription = "Reset"
                )
            }
        }
        
        // Placeholder for alignment when reset is hidden
        if (!showReset) {
            Spacer(modifier = Modifier.size(48.dp))
        }
    }
}

@Composable
private fun ArStatusIndicator(
    arState: ArState,
    isModelLoading: Boolean,
    trackingFailureReason: TrackingFailureReason?
) {
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.6f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse_alpha"
    )
    
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = ArCardBg)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            when {
                isModelLoading -> {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        color = ArAccent,
                        strokeWidth = 2.dp
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = "Loading 3D Model...",
                        color = ArTextPrimary,
                        fontSize = 14.sp
                    )
                }
                arState == ArState.TRACKING_LOST -> {
                    Icon(
                        imageVector = Icons.Default.Warning,
                        contentDescription = null,
                        tint = ArOrange,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = getTrackingFailureMessage(trackingFailureReason),
                        color = ArOrange,
                        fontSize = 14.sp
                    )
                }
                arState == ArState.SEARCHING -> {
                    Box(
                        modifier = Modifier
                            .size(24.dp)
                            .alpha(pulseAlpha)
                            .background(ArAccent, CircleShape)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = "Move your phone to scan surfaces",
                        color = ArTextPrimary,
                        fontSize = 14.sp
                    )
                }
                arState == ArState.PLANE_DETECTED -> {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = null,
                        tint = ArGreen,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = "Tap on a surface to place the tyre",
                        color = ArGreen,
                        fontSize = 14.sp
                    )
                }
                else -> {}
            }
        }
    }
}

@Composable
private fun SpatialDataPanels(tyreData: TyreHealthData) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Main Status Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = ArCardBg)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Tyre Health Status",
                        color = ArTextSecondary,
                        fontSize = 12.sp
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = tyreData.status.displayText,
                        color = tyreData.status.color,
                        fontSize = 28.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
                
                Box(
                    modifier = Modifier
                        .size(60.dp)
                        .background(
                            brush = Brush.radialGradient(
                                colors = listOf(
                                    tyreData.status.color.copy(alpha = 0.3f),
                                    Color.Transparent
                                )
                            ),
                            shape = CircleShape
                        )
                        .border(2.dp, tyreData.status.color, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = null,
                        tint = tyreData.status.color,
                        modifier = Modifier.size(32.dp)
                    )
                }
            }
        }
        
        // Data Cards Row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            DataCard(
                modifier = Modifier.weight(1f),
                icon = Icons.Default.Speed,
                label = "Pressure",
                value = tyreData.pressure,
                accentColor = ArAccent
            )
            DataCard(
                modifier = Modifier.weight(1f),
                icon = Icons.Default.Thermostat,
                label = "Temperature",
                value = tyreData.temperature,
                accentColor = ArOrange
            )
        }
        
        // Tread Depth Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = ArCardBg)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Tread Depth",
                        color = ArTextSecondary,
                        fontSize = 12.sp
                    )
                    Text(
                        text = tyreData.treadDepth,
                        color = ArTextPrimary,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
                Text(
                    text = "Updated ${tyreData.lastUpdated}",
                    color = ArTextSecondary,
                    fontSize = 11.sp
                )
            }
        }
        
        // Instructions
        Text(
            text = "💡 Pinch to scale • Drag to rotate",
            color = ArTextSecondary,
            fontSize = 12.sp,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp)
        )
    }
}

@Composable
private fun DataCard(
    modifier: Modifier = Modifier,
    icon: ImageVector,
    label: String,
    value: String,
    accentColor: Color
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = ArCardBg)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .background(accentColor.copy(alpha = 0.2f), RoundedCornerShape(8.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = accentColor,
                        modifier = Modifier.size(18.dp)
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = label,
                    color = ArTextSecondary,
                    fontSize = 12.sp
                )
            }
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = value,
                color = ArTextPrimary,
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

/**
 * Get user-friendly message for tracking failure
 */
private fun getTrackingFailureMessage(reason: TrackingFailureReason?): String {
    return when (reason) {
        TrackingFailureReason.NONE -> "Tracking OK"
        TrackingFailureReason.BAD_STATE -> "AR session error"
        TrackingFailureReason.INSUFFICIENT_LIGHT -> "Need more light"
        TrackingFailureReason.EXCESSIVE_MOTION -> "Move slower"
        TrackingFailureReason.INSUFFICIENT_FEATURES -> "Point at textured surface"
        TrackingFailureReason.CAMERA_UNAVAILABLE -> "Camera unavailable"
        null -> "Tracking lost"
    }
}
