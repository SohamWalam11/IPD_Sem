package org.example.project.tyre3d.viewer

import android.util.Log
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
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
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import io.github.sceneview.Scene
import io.github.sceneview.math.Position
import io.github.sceneview.math.Rotation
import io.github.sceneview.node.ModelNode
import io.github.sceneview.rememberEngine
import io.github.sceneview.rememberModelLoader
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import java.io.File
import java.nio.ByteBuffer

/**
 * ═══════════════════════════════════════════════════════════════════════════════
 * TyreModelViewer - SceneView-based 3D GLB Renderer
 * ═══════════════════════════════════════════════════════════════════════════════
 * 
 * This composable renders GLB/GLTF 3D models using SceneView (Sceneform successor).
 * Features:
 * - Load GLB models from local file paths
 * - Interactive orbit controls (rotate, pan, zoom)
 * - Auto-rotation option
 * - Control overlay
 * 
 * SceneView uses Google's Filament rendering engine for high-quality graphics.
 */
@Composable
fun TyreModelViewer(
    modelPath: String,
    modifier: Modifier = Modifier,
    showControls: Boolean = true,
    autoRotate: Boolean = false,
    onModelLoaded: () -> Unit = {},
    onError: (String) -> Unit = {}
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    
    // States
    var isLoading by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var showControlPanel by remember { mutableStateOf(false) }
    var isAutoRotating by remember { mutableStateOf(autoRotate) }
    var lightIntensity by remember { mutableFloatStateOf(1.0f) }
    
    // SceneView components
    val engine = rememberEngine()
    val modelLoader = rememberModelLoader(engine)
    
    // Model node state
    var modelNode by remember { mutableStateOf<ModelNode?>(null) }
    
    // Load model
    LaunchedEffect(modelPath) {
        isLoading = true
        errorMessage = null
        
        try {
            withContext(Dispatchers.IO) {
                val file = File(modelPath)
                
                if (!file.exists()) {
                    throw Exception("Model file not found: $modelPath")
                }
                
                if (file.length() == 0L) {
                    throw Exception("Model file is empty")
                }
                
                // Read file into buffer
                val buffer = ByteBuffer.wrap(file.readBytes())
                
                // Load model
                val instance = modelLoader.createModelInstance(
                    buffer = buffer
                )
                
                if (instance != null) {
                    modelNode = ModelNode(
                        modelInstance = instance,
                        autoAnimate = true,
                        scaleToUnits = 2.0f
                    ).apply {
                        position = Position(0f, 0f, 0f)
                    }
                    
                    withContext(Dispatchers.Main) {
                        isLoading = false
                        onModelLoaded()
                    }
                } else {
                    throw Exception("Failed to load model instance")
                }
            }
        } catch (e: Exception) {
            Log.e("TyreModelViewer", "Load error: ${e.message}", e)
            withContext(Dispatchers.Main) {
                isLoading = false
                errorMessage = e.message ?: "Unknown error"
                onError(errorMessage!!)
            }
        }
    }
    
    // Auto-rotation
    LaunchedEffect(isAutoRotating, modelNode) {
        if (isAutoRotating) {
            while (isAutoRotating) {
                modelNode?.let { node ->
                    val current = node.rotation
                    node.rotation = Rotation(
                        x = current.x,
                        y = current.y + 0.5f,
                        z = current.z
                    )
                }
                delay(16) // ~60fps
            }
        }
    }
    
    Box(modifier = modifier) {
        // Main 3D Scene
        Scene(
            modifier = Modifier.fillMaxSize(),
            engine = engine,
            modelLoader = modelLoader,
            childNodes = listOfNotNull(modelNode),
            isOpaque = true,
            onTouchEvent = { _, _ ->
                if (showControls) {
                    showControlPanel = !showControlPanel
                }
                true
            }
        )
        
        // Loading overlay
        AnimatedVisibility(
            visible = isLoading,
            enter = fadeIn(),
            exit = fadeOut()
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.6f)),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(48.dp),
                        color = Color.White
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        "Loading 3D Model...",
                        color = Color.White,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
        }
        
        // Error overlay
        AnimatedVisibility(
            visible = errorMessage != null,
            enter = fadeIn(),
            exit = fadeOut()
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.8f)),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.padding(32.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.ViewInAr,
                        contentDescription = null,
                        modifier = Modifier.size(64.dp),
                        tint = Color.White.copy(alpha = 0.5f)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        "Failed to load model",
                        color = Color.White,
                        style = MaterialTheme.typography.titleMedium
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        errorMessage ?: "Unknown error",
                        color = Color.White.copy(alpha = 0.7f),
                        style = MaterialTheme.typography.bodySmall,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
        
        // Control panel
        AnimatedVisibility(
            visible = showControlPanel && showControls && !isLoading && errorMessage == null,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier.align(Alignment.BottomCenter)
        ) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = Color.Black.copy(alpha = 0.8f)
                )
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    Text(
                        "Controls",
                        style = MaterialTheme.typography.titleSmall,
                        color = Color.White,
                        fontWeight = FontWeight.Bold
                    )
                    
                    Spacer(modifier = Modifier.height(12.dp))
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        ControlButton(
                            icon = Icons.Default.Rotate90DegreesCcw,
                            label = if (isAutoRotating) "Stop" else "Rotate",
                            isActive = isAutoRotating,
                            onClick = { isAutoRotating = !isAutoRotating }
                        )
                        
                        ControlButton(
                            icon = Icons.Default.CenterFocusStrong,
                            label = "Reset",
                            onClick = {
                                modelNode?.rotation = Rotation(0f, 0f, 0f)
                            }
                        )
                    }
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    // Light intensity slider
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.LightMode,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                        Slider(
                            value = lightIntensity,
                            onValueChange = { lightIntensity = it },
                            valueRange = 0.2f..2f,
                            modifier = Modifier
                                .weight(1f)
                                .padding(horizontal = 12.dp)
                        )
                    }
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
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
                    Color.White.copy(alpha = 0.2f),
                contentColor = Color.White
            )
        ) {
            Icon(icon, contentDescription = label)
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            label,
            style = MaterialTheme.typography.labelSmall,
            color = Color.White.copy(alpha = 0.8f)
        )
    }
}
