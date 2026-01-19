package org.example.project.tyre3d.viewer

import android.util.Log
import android.view.MotionEvent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTransformGestures
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CenterFocusStrong
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.Rotate90DegreesCcw
import androidx.compose.material.icons.filled.ViewInAr
import androidx.compose.material.icons.filled.ZoomIn
import androidx.compose.material.icons.filled.ZoomOut
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import io.github.sceneview.Scene
import io.github.sceneview.math.Position
import io.github.sceneview.math.Rotation
import io.github.sceneview.node.ModelNode
import io.github.sceneview.rememberCameraManipulator
import io.github.sceneview.rememberCameraNode
import io.github.sceneview.rememberEngine
import io.github.sceneview.rememberEnvironmentLoader
import io.github.sceneview.rememberModelLoader
import io.github.sceneview.rememberNode
import io.github.sceneview.rememberNodes
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

private const val TAG = "Interactive3DViewer"

/**
 * ═══════════════════════════════════════════════════════════════════════════════
 * Interactive3DViewer - Full-featured 3D model viewer with touch controls
 * ═══════════════════════════════════════════════════════════════════════════════
 * 
 * Features:
 * - Load GLB/GLTF models from local file paths or URLs
 * - Touch gestures: drag to rotate, pinch to zoom
 * - Auto-rotate toggle
 * - Light intensity control
 * - Reset view
 * - Smooth camera orbit controls
 */
@Composable
fun Interactive3DViewer(
    modelPath: String,
    modifier: Modifier = Modifier,
    showControls: Boolean = true,
    initialAutoRotate: Boolean = false,
    onModelLoaded: () -> Unit = {},
    onError: (String) -> Unit = {}
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    
    // States
    var isLoading by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var showControlPanel by remember { mutableStateOf(true) }
    var isAutoRotating by remember { mutableStateOf(initialAutoRotate) }
    var lightIntensity by remember { mutableFloatStateOf(1.0f) }
    
    // Camera control states
    var rotationX by remember { mutableFloatStateOf(0f) }
    var rotationY by remember { mutableFloatStateOf(0f) }
    var zoom by remember { mutableFloatStateOf(1f) }
    
    // SceneView components
    val engine = rememberEngine()
    val modelLoader = rememberModelLoader(engine)
    val environmentLoader = rememberEnvironmentLoader(engine)
    
    // Model node state
    var modelNode by remember { mutableStateOf<ModelNode?>(null) }
    
    // Load model
    LaunchedEffect(modelPath) {
        isLoading = true
        errorMessage = null
        
        scope.launch {
            try {
                val loadedNode = withContext(Dispatchers.IO) {
                    // Try loading from file path first
                    val file = File(modelPath)
                    
                    val modelInstance = if (file.exists()) {
                        Log.d(TAG, "Loading model from file: $modelPath")
                        modelLoader.loadModelInstance(modelPath)
                    } else if (modelPath.startsWith("http")) {
                        Log.d(TAG, "Loading model from URL: $modelPath")
                        modelLoader.loadModelInstance(modelPath)
                    } else {
                        // Try as asset path
                        Log.d(TAG, "Loading model from assets: $modelPath")
                        modelLoader.loadModelInstance(modelPath)
                    }
                    
                    if (modelInstance != null) {
                        ModelNode(
                            modelInstance = modelInstance,
                            autoAnimate = true,
                            scaleToUnits = 2.0f
                        ).apply {
                            position = Position(0f, 0f, 0f)
                        }
                    } else {
                        null
                    }
                }
                
                if (loadedNode != null) {
                    modelNode = loadedNode
                    isLoading = false
                    onModelLoaded()
                    Log.d(TAG, "Model loaded successfully")
                } else {
                    throw Exception("Failed to create model instance")
                }
                
            } catch (e: Exception) {
                Log.e(TAG, "Error loading model: ${e.message}", e)
                isLoading = false
                errorMessage = e.message ?: "Failed to load model"
                onError(errorMessage!!)
            }
        }
    }
    
    // Auto-rotation effect
    LaunchedEffect(isAutoRotating) {
        while (isAutoRotating) {
            rotationY += 1f
            delay(16) // ~60fps
        }
    }
    
    // Apply rotation to model
    LaunchedEffect(rotationX, rotationY, zoom) {
        modelNode?.let { node ->
            node.rotation = Rotation(x = rotationX, y = rotationY, z = 0f)
            node.scale = io.github.sceneview.math.Scale(zoom)
        }
    }
    
    Box(modifier = modifier) {
        // 3D Scene with gesture detection
        Box(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(Unit) {
                    detectTransformGestures { _, pan, gestureZoom, _ ->
                        // Disable gestures during auto-rotate
                        if (!isAutoRotating) {
                            // Rotate based on drag
                            rotationY += pan.x * 0.5f
                            rotationX -= pan.y * 0.5f
                            
                            // Clamp X rotation to prevent flipping
                            rotationX = rotationX.coerceIn(-80f, 80f)
                            
                            // Zoom based on pinch
                            zoom = (zoom * gestureZoom).coerceIn(0.3f, 3f)
                        }
                    }
                }
        ) {
            Scene(
                modifier = Modifier.fillMaxSize(),
                engine = engine,
                modelLoader = modelLoader,
                childNodes = listOfNotNull(modelNode),
                isOpaque = true
            )
        }
        
        // Loading overlay
        AnimatedVisibility(
            visible = isLoading,
            enter = fadeIn(),
            exit = fadeOut()
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.7f)),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(56.dp),
                        color = Color.White,
                        strokeWidth = 4.dp
                    )
                    Text(
                        "Loading 3D Model...",
                        color = Color.White,
                        style = MaterialTheme.typography.titleMedium
                    )
                    Text(
                        "This may take a moment",
                        color = Color.White.copy(alpha = 0.6f),
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        }
        
        // Error overlay
        AnimatedVisibility(
            visible = errorMessage != null && !isLoading,
            enter = fadeIn(),
            exit = fadeOut()
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.85f)),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.padding(32.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.ViewInAr,
                        contentDescription = null,
                        modifier = Modifier.size(72.dp),
                        tint = Color.Red.copy(alpha = 0.7f)
                    )
                    Spacer(modifier = Modifier.height(20.dp))
                    Text(
                        "Failed to Load Model",
                        color = Color.White,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        errorMessage ?: "Unknown error occurred",
                        color = Color.White.copy(alpha = 0.7f),
                        style = MaterialTheme.typography.bodyMedium,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
        
        // Control panel
        AnimatedVisibility(
            visible = showControls && showControlPanel && !isLoading && errorMessage == null,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier.align(Alignment.BottomCenter)
        ) {
            ControlPanel(
                isAutoRotating = isAutoRotating,
                onAutoRotateToggle = { isAutoRotating = !isAutoRotating },
                onResetView = {
                    rotationX = 0f
                    rotationY = 0f
                    zoom = 1f
                },
                onZoomIn = { zoom = (zoom * 1.2f).coerceAtMost(3f) },
                onZoomOut = { zoom = (zoom / 1.2f).coerceAtLeast(0.3f) },
                lightIntensity = lightIntensity,
                onLightIntensityChange = { lightIntensity = it }
            )
        }
        
        // Gesture hint (shown briefly)
        if (!isLoading && errorMessage == null && modelNode != null) {
            GestureHint(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 16.dp)
            )
        }
    }
}

@Composable
private fun ControlPanel(
    isAutoRotating: Boolean,
    onAutoRotateToggle: () -> Unit,
    onResetView: () -> Unit,
    onZoomIn: () -> Unit,
    onZoomOut: () -> Unit,
    lightIntensity: Float,
    onLightIntensityChange: (Float) -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color.Black.copy(alpha = 0.85f)
        )
    ) {
        Column(
            modifier = Modifier.padding(20.dp)
        ) {
            // Title
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    Icons.Default.ViewInAr,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.size(8.dp))
                Text(
                    "3D Controls",
                    style = MaterialTheme.typography.titleMedium,
                    color = Color.White,
                    fontWeight = FontWeight.Bold
                )
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Control buttons row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                ControlButton(
                    icon = Icons.Default.Rotate90DegreesCcw,
                    label = if (isAutoRotating) "Stop" else "Rotate",
                    isActive = isAutoRotating,
                    onClick = onAutoRotateToggle
                )
                
                ControlButton(
                    icon = Icons.Default.ZoomIn,
                    label = "Zoom +",
                    onClick = onZoomIn
                )
                
                ControlButton(
                    icon = Icons.Default.ZoomOut,
                    label = "Zoom -",
                    onClick = onZoomOut
                )
                
                ControlButton(
                    icon = Icons.Default.CenterFocusStrong,
                    label = "Reset",
                    onClick = onResetView
                )
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Light intensity slider
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(
                    Icons.Default.LightMode,
                    contentDescription = "Lighting",
                    tint = Color.White,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.size(12.dp))
                Slider(
                    value = lightIntensity,
                    onValueChange = onLightIntensityChange,
                    valueRange = 0.2f..2f,
                    modifier = Modifier.weight(1f),
                    colors = SliderDefaults.colors(
                        thumbColor = Color.White,
                        activeTrackColor = Color.White,
                        inactiveTrackColor = Color.White.copy(alpha = 0.3f)
                    )
                )
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            // Instructions
            Text(
                "Drag to rotate • Pinch to zoom",
                style = MaterialTheme.typography.bodySmall,
                color = Color.White.copy(alpha = 0.5f),
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
private fun ControlButton(
    icon: ImageVector,
    label: String,
    isActive: Boolean = false,
    onClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        FilledIconButton(
            onClick = onClick,
            modifier = Modifier.size(48.dp),
            colors = IconButtonDefaults.filledIconButtonColors(
                containerColor = if (isActive)
                    MaterialTheme.colorScheme.primary
                else
                    Color.White.copy(alpha = 0.15f),
                contentColor = Color.White
            )
        ) {
            Icon(icon, contentDescription = label, modifier = Modifier.size(24.dp))
        }
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            label,
            style = MaterialTheme.typography.labelSmall,
            color = Color.White.copy(alpha = 0.8f)
        )
    }
}

@Composable
private fun GestureHint(modifier: Modifier = Modifier) {
    var visible by remember { mutableStateOf(true) }
    
    LaunchedEffect(Unit) {
        delay(4000)
        visible = false
    }
    
    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(),
        exit = fadeOut(),
        modifier = modifier
    ) {
        Card(
            colors = CardDefaults.cardColors(
                containerColor = Color.Black.copy(alpha = 0.7f)
            ),
            shape = RoundedCornerShape(20.dp)
        ) {
            Text(
                "👆 Drag to rotate • 🤏 Pinch to zoom",
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                color = Color.White,
                style = MaterialTheme.typography.bodySmall
            )
        }
    }
}
