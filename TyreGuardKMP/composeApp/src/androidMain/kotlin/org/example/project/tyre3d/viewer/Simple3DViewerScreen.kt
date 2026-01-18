package org.example.project.tyre3d.viewer

import android.content.Context
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
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
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Rotate90DegreesCcw
import androidx.compose.material.icons.filled.ViewInAr
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch
import org.example.project.tyre3d.ConversionProgress
import org.example.project.tyre3d.ConversionState
import org.example.project.tyre3d.ImageTo3DService

/**
 * ═══════════════════════════════════════════════════════════════════════════════
 * Simple3DViewerScreen - View 3D models from 2D image conversion
 * ═══════════════════════════════════════════════════════════════════════════════
 * 
 * This screen handles:
 * 1. Triggering 2D → 3D conversion when given an image path
 * 2. Showing conversion progress
 * 3. Displaying the 3D model once ready
 * 
 * Workflow:
 * - User captures image in CameraScreen
 * - Navigates here with imagePath parameter
 * - Conversion starts automatically
 * - 3D model is displayed when ready
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun Simple3DViewerScreen(
    imagePath: String?,
    modelPath: String? = null,  // If already converted, skip conversion
    onNavigateBack: () -> Unit,
    onCaptureAnother: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    
    // Service instance
    val service = remember { ImageTo3DService(context) }
    
    // State
    var currentModelPath by remember { mutableStateOf(modelPath) }
    var progressMessage by remember { mutableStateOf("") }
    var progressValue by remember { mutableStateOf(0f) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var isConverting by remember { mutableStateOf(false) }
    
    // Cleanup on dispose
    DisposableEffect(Unit) {
        onDispose {
            service.resetState()
        }
    }
    
    // Start conversion if we have an image path but no model
    LaunchedEffect(imagePath, modelPath) {
        if (imagePath != null && modelPath == null && currentModelPath == null) {
            isConverting = true
            errorMessage = null
            
            service.convertTo3D(imagePath).collect { progress ->
                when (progress) {
                    is ConversionProgress.Starting -> {
                        progressMessage = progress.message
                        progressValue = 0.05f
                    }
                    is ConversionProgress.Uploading -> {
                        progressMessage = progress.message
                        progressValue = 0.1f + (progress.progress * 0.2f)
                    }
                    is ConversionProgress.Processing -> {
                        progressMessage = progress.stage
                        progressValue = 0.3f + (progress.progress * 0.5f)
                    }
                    is ConversionProgress.Downloading -> {
                        progressMessage = progress.message
                        progressValue = 0.8f + (progress.progress * 0.2f)
                    }
                    is ConversionProgress.Complete -> {
                        currentModelPath = progress.modelPath
                        isConverting = false
                        progressValue = 1f
                    }
                    is ConversionProgress.Error -> {
                        errorMessage = progress.message
                        isConverting = false
                    }
                }
            }
        }
    }
    
    val animatedProgress by animateFloatAsState(
        targetValue = progressValue,
        label = "progress"
    )
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    Text(
                        "3D Tyre Model",
                        fontWeight = FontWeight.SemiBold
                    )
                },
                navigationIcon = {
                    FilledIconButton(
                        onClick = onNavigateBack,
                        colors = IconButtonDefaults.filledIconButtonColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant
                        )
                    ) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent
                )
            )
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            when {
                // Show 3D model if available
                currentModelPath != null -> {
                    Model3DViewer(
                        modelPath = currentModelPath!!,
                        modifier = Modifier.fillMaxSize()
                    )
                    
                    // Bottom controls overlay
                    Column(
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .fillMaxWidth()
                            .background(
                                Brush.verticalGradient(
                                    colors = listOf(
                                        Color.Transparent,
                                        Color.Black.copy(alpha = 0.7f)
                                    )
                                )
                            )
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            "🎉 3D Model Ready!",
                            style = MaterialTheme.typography.titleMedium,
                            color = Color.White,
                            fontWeight = FontWeight.Bold
                        )
                        
                        Text(
                            "Drag to rotate • Pinch to zoom",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.White.copy(alpha = 0.7f)
                        )
                        
                        Spacer(modifier = Modifier.height(16.dp))
                        
                        Button(
                            onClick = onCaptureAnother,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primary
                            )
                        ) {
                            Icon(Icons.Default.CameraAlt, null)
                            Spacer(modifier = Modifier.size(8.dp))
                            Text("Capture Another Tyre")
                        }
                    }
                }
                
                // Show error state
                errorMessage != null -> {
                    ErrorState(
                        message = errorMessage!!,
                        onRetry = {
                            if (imagePath != null) {
                                errorMessage = null
                                isConverting = true
                                scope.launch {
                                    service.convertTo3D(imagePath).collect { progress ->
                                        when (progress) {
                                            is ConversionProgress.Complete -> {
                                                currentModelPath = progress.modelPath
                                                isConverting = false
                                            }
                                            is ConversionProgress.Error -> {
                                                errorMessage = progress.message
                                                isConverting = false
                                            }
                                            else -> {}
                                        }
                                    }
                                }
                            }
                        },
                        onBack = onNavigateBack,
                        modifier = Modifier.align(Alignment.Center)
                    )
                }
                
                // Show conversion progress
                isConverting -> {
                    ConversionProgressView(
                        message = progressMessage,
                        progress = animatedProgress,
                        modifier = Modifier.align(Alignment.Center)
                    )
                }
                
                // No image provided
                else -> {
                    NoImageState(
                        onCapture = onCaptureAnother,
                        modifier = Modifier.align(Alignment.Center)
                    )
                }
            }
        }
    }
}

/**
 * 3D Model viewer using SceneView
 */
@Composable
private fun Model3DViewer(
    modelPath: String,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    
    // Use the TyreModelViewer composable
    TyreModelViewer(
        modelPath = modelPath,
        modifier = modifier,
        showControls = true,
        autoRotate = false
    )
}

/**
 * Progress view during 3D conversion
 */
@Composable
private fun ConversionProgressView(
    message: String,
    progress: Float,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .padding(32.dp)
            .fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(8.dp)
    ) {
        Column(
            modifier = Modifier
                .padding(32.dp)
                .fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Animated 3D icon
            Box(
                modifier = Modifier
                    .size(80.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Rotate90DegreesCcw,
                    contentDescription = null,
                    modifier = Modifier.size(40.dp),
                    tint = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
            
            Text(
                "Converting to 3D",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
            
            Text(
                message.ifEmpty { "Processing..." },
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
            
            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .clip(RoundedCornerShape(4.dp)),
            )
            
            Text(
                "${(progress * 100).toInt()}%",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary
            )
            
            Text(
                "This may take 2-5 minutes",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.outline
            )
        }
    }
}

/**
 * Error state view
 */
@Composable
private fun ErrorState(
    message: String,
    onRetry: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .padding(32.dp)
            .fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.errorContainer
        )
    ) {
        Column(
            modifier = Modifier
                .padding(32.dp)
                .fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Error,
                contentDescription = null,
                modifier = Modifier.size(64.dp),
                tint = MaterialTheme.colorScheme.error
            )
            
            Text(
                "Conversion Failed",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onErrorContainer
            )
            
            Text(
                message,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onErrorContainer,
                textAlign = TextAlign.Center
            )
            
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Button(
                    onClick = onBack,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant,
                        contentColor = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                ) {
                    Text("Go Back")
                }
                
                Button(
                    onClick = onRetry,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Icon(Icons.Default.Refresh, null)
                    Spacer(modifier = Modifier.size(8.dp))
                    Text("Retry")
                }
            }
        }
    }
}

/**
 * No image state view
 */
@Composable
private fun NoImageState(
    onCapture: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .padding(32.dp)
            .fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(
            modifier = Modifier
                .padding(32.dp)
                .fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Icon(
                imageVector = Icons.Default.ViewInAr,
                contentDescription = null,
                modifier = Modifier.size(64.dp),
                tint = MaterialTheme.colorScheme.primary
            )
            
            Text(
                "No Tyre Image",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
            
            Text(
                "Capture a photo of your tyre to generate a 3D model",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
            
            Button(
                onClick = onCapture,
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Default.CameraAlt, null)
                Spacer(modifier = Modifier.size(8.dp))
                Text("Capture Tyre Photo")
            }
        }
    }
}
