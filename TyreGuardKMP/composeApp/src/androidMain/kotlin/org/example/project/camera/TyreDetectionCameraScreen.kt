package org.example.project.camera

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.util.Log
import android.view.ViewGroup
import android.widget.Toast
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.example.project.ml.*
import java.io.File
import java.util.concurrent.Executors

private const val TAG = "TyreDetectionCamera"

// Theme colors
private val PrimaryViolet = Color(0xFF7C3AED)
private val SuccessGreen = Color(0xFF10B981)
private val WarningOrange = Color(0xFFF59E0B)
private val DangerRed = Color(0xFFEF4444)

/**
 * Advanced Camera Screen with ML Kit Tyre Detection
 * 
 * Features:
 * - Real-time tyre detection using ML Kit Object Detection
 * - Bounding box overlay on detected tyres
 * - Guidance messages to help user frame tyre
 * - Auto-capture when tyre is properly framed
 * - Crop detected tyre for analysis
 * - Integration with TFLite health analyzer
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TyreDetectionCameraScreen(
    onBackClick: () -> Unit = {},
    onTyreCaptured: (String, TyreHealthResult) -> Unit = { _, _ -> },
    onError: (String) -> Unit = {}
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val scope = rememberCoroutineScope()
    
    // Camera state
    var hasCameraPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) ==
                PackageManager.PERMISSION_GRANTED
        )
    }
    var isFlashOn by remember { mutableStateOf(false) }
    var isCapturing by remember { mutableStateOf(false) }
    var isAnalyzing by remember { mutableStateOf(false) }
    
    // Detection state
    val tyreDetector = remember { TyreDetectionService.getInstance(context) }
    val tyreAnalyzer = remember { TyreHealthAnalyzer.getInstance(context) }
    
    var detectedTyres by remember { mutableStateOf<List<DetectedTyre>>(emptyList()) }
    var guidanceMessage by remember { mutableStateOf("Point camera at tyre") }
    var hasTyreInFrame by remember { mutableStateOf(false) }
    var consecutiveDetections by remember { mutableIntStateOf(0) }
    var isReadyToCapture by remember { mutableStateOf(false) }
    
    // Camera references
    var imageCapture by remember { mutableStateOf<ImageCapture?>(null) }
    var camera by remember { mutableStateOf<Camera?>(null) }
    
    // Initialize analyzer
    LaunchedEffect(Unit) {
        tyreAnalyzer.initialize()
    }
    
    // Cleanup on dispose
    DisposableEffect(Unit) {
        onDispose {
            // Cleanup if needed
        }
    }
    
    // Pulse animation for detection indicator
    val pulseAnimation = rememberInfiniteTransition(label = "pulse")
    val pulseScale by pulseAnimation.animateFloat(
        initialValue = 1f,
        targetValue = 1.2f,
        animationSpec = infiniteRepeatable(
            animation = tween(600),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseScale"
    )
    
    Box(modifier = Modifier.fillMaxSize()) {
        // Camera Preview
        AndroidView(
            factory = { ctx ->
                PreviewView(ctx).apply {
                    layoutParams = ViewGroup.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT
                    )
                    scaleType = PreviewView.ScaleType.FILL_CENTER
                    
                    // Setup camera
                    val cameraProviderFuture = ProcessCameraProvider.getInstance(ctx)
                    cameraProviderFuture.addListener({
                        val cameraProvider = cameraProviderFuture.get()
                        
                        // Preview use case
                        val preview = Preview.Builder()
                            .setTargetAspectRatio(AspectRatio.RATIO_16_9)
                            .build()
                            .also { it.setSurfaceProvider(surfaceProvider) }
                        
                        // Image capture use case
                        imageCapture = ImageCapture.Builder()
                            .setCaptureMode(ImageCapture.CAPTURE_MODE_MAXIMIZE_QUALITY)
                            .setTargetAspectRatio(AspectRatio.RATIO_16_9)
                            .build()
                        
                        // Image analysis for real-time detection
                        val imageAnalysis = ImageAnalysis.Builder()
                            .setTargetAspectRatio(AspectRatio.RATIO_16_9)
                            .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                            .build()
                        
                        val analyzer = TyreCameraAnalyzer(
                            context = ctx,
                            onTyreDetected = { tyre ->
                                consecutiveDetections++
                                if (consecutiveDetections >= 5) {
                                    isReadyToCapture = true
                                }
                            },
                            onFrameAnalyzed = { state ->
                                detectedTyres = state.detectedTyres
                                hasTyreInFrame = state.hasTyreInFrame
                                guidanceMessage = state.guidanceMessage
                                if (!state.hasTyreInFrame) {
                                    consecutiveDetections = 0
                                    isReadyToCapture = false
                                }
                            }
                        )
                        imageAnalysis.setAnalyzer(Executors.newSingleThreadExecutor(), analyzer)
                        
                        try {
                            cameraProvider.unbindAll()
                            camera = cameraProvider.bindToLifecycle(
                                lifecycleOwner,
                                CameraSelector.DEFAULT_BACK_CAMERA,
                                preview,
                                imageCapture,
                                imageAnalysis
                            )
                        } catch (e: Exception) {
                            Log.e(TAG, "Camera binding failed: ${e.message}", e)
                        }
                    }, ContextCompat.getMainExecutor(ctx))
                }
            },
            modifier = Modifier.fillMaxSize()
        )
        
        // Detection overlay - draw bounding boxes
        if (hasTyreInFrame && detectedTyres.isNotEmpty()) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                detectedTyres.forEach { tyre ->
                    val boxColor = when {
                        isReadyToCapture -> SuccessGreen
                        tyre.confidence > 0.5f -> PrimaryViolet
                        else -> WarningOrange
                    }
                    
                    // Scale bounding box to screen size
                    val scaleX = size.width / 1080f  // Approximate camera width
                    val scaleY = size.height / 1920f // Approximate camera height
                    
                    val left = tyre.boundingBox.left * scaleX
                    val top = tyre.boundingBox.top * scaleY
                    val width = tyre.boundingBox.width() * scaleX
                    val height = tyre.boundingBox.height() * scaleY
                    
                    // Draw bounding box
                    drawRect(
                        color = boxColor,
                        topLeft = Offset(left, top),
                        size = Size(width, height),
                        style = Stroke(width = 6f)
                    )
                    
                    // Draw corner markers
                    val cornerLength = 30f
                    val cornerStroke = 8f
                    
                    // Top-left corner
                    drawLine(boxColor, Offset(left, top), Offset(left + cornerLength, top), cornerStroke)
                    drawLine(boxColor, Offset(left, top), Offset(left, top + cornerLength), cornerStroke)
                    
                    // Top-right corner
                    drawLine(boxColor, Offset(left + width, top), Offset(left + width - cornerLength, top), cornerStroke)
                    drawLine(boxColor, Offset(left + width, top), Offset(left + width, top + cornerLength), cornerStroke)
                    
                    // Bottom-left corner
                    drawLine(boxColor, Offset(left, top + height), Offset(left + cornerLength, top + height), cornerStroke)
                    drawLine(boxColor, Offset(left, top + height), Offset(left, top + height - cornerLength), cornerStroke)
                    
                    // Bottom-right corner
                    drawLine(boxColor, Offset(left + width, top + height), Offset(left + width - cornerLength, top + height), cornerStroke)
                    drawLine(boxColor, Offset(left + width, top + height), Offset(left + width, top + height - cornerLength), cornerStroke)
                }
            }
        }
        
        // Scanning frame guide (when no tyre detected)
        if (!hasTyreInFrame) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                // Animated scanning frame
                Canvas(modifier = Modifier.size(280.dp)) {
                    val strokeWidth = 4f
                    val cornerLength = 50f
                    val color = PrimaryViolet.copy(alpha = 0.7f)
                    
                    // Draw corner brackets
                    // Top-left
                    drawLine(color, Offset(0f, 0f), Offset(cornerLength, 0f), strokeWidth)
                    drawLine(color, Offset(0f, 0f), Offset(0f, cornerLength), strokeWidth)
                    
                    // Top-right
                    drawLine(color, Offset(size.width, 0f), Offset(size.width - cornerLength, 0f), strokeWidth)
                    drawLine(color, Offset(size.width, 0f), Offset(size.width, cornerLength), strokeWidth)
                    
                    // Bottom-left
                    drawLine(color, Offset(0f, size.height), Offset(cornerLength, size.height), strokeWidth)
                    drawLine(color, Offset(0f, size.height), Offset(0f, size.height - cornerLength), strokeWidth)
                    
                    // Bottom-right
                    drawLine(color, Offset(size.width, size.height), Offset(size.width - cornerLength, size.height), strokeWidth)
                    drawLine(color, Offset(size.width, size.height), Offset(size.width, size.height - cornerLength), strokeWidth)
                }
            }
        }
        
        // Top bar with back button and flash
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = onBackClick,
                modifier = Modifier
                    .size(48.dp)
                    .background(Color.Black.copy(alpha = 0.5f), CircleShape)
            ) {
                Icon(
                    Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back",
                    tint = Color.White
                )
            }
            
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                // Flash toggle
                IconButton(
                    onClick = {
                        isFlashOn = !isFlashOn
                        camera?.cameraControl?.enableTorch(isFlashOn)
                    },
                    modifier = Modifier
                        .size(48.dp)
                        .background(
                            if (isFlashOn) PrimaryViolet else Color.Black.copy(alpha = 0.5f),
                            CircleShape
                        )
                ) {
                    Icon(
                        if (isFlashOn) Icons.Default.FlashOn else Icons.Default.FlashOff,
                        contentDescription = "Flash",
                        tint = Color.White
                    )
                }
            }
        }
        
        // Bottom panel with guidance and capture button
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter)
                .background(Color.Black.copy(alpha = 0.7f))
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Guidance message
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = when {
                        isReadyToCapture -> SuccessGreen.copy(alpha = 0.2f)
                        hasTyreInFrame -> PrimaryViolet.copy(alpha = 0.2f)
                        else -> Color.White.copy(alpha = 0.1f)
                    }
                ),
                shape = RoundedCornerShape(12.dp)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        when {
                            isReadyToCapture -> Icons.Default.CheckCircle
                            hasTyreInFrame -> Icons.Default.CropFree
                            else -> Icons.Default.Search
                        },
                        contentDescription = null,
                        tint = when {
                            isReadyToCapture -> SuccessGreen
                            hasTyreInFrame -> PrimaryViolet
                            else -> Color.White
                        },
                        modifier = Modifier.size(24.dp)
                    )
                    Text(
                        guidanceMessage,
                        color = Color.White,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(20.dp))
            
            // Capture button
            Box(contentAlignment = Alignment.Center) {
                // Outer ring (animated when ready)
                if (isReadyToCapture) {
                    Box(
                        modifier = Modifier
                            .size((80 * pulseScale).dp)
                            .border(4.dp, SuccessGreen, CircleShape)
                    )
                }
                
                // Main capture button
                IconButton(
                    onClick = {
                        if (!isCapturing) {
                            isCapturing = true
                            isAnalyzing = true
                            
                            scope.launch {
                                captureAndAnalyze(
                                    context = context,
                                    imageCapture = imageCapture,
                                    tyreDetector = tyreDetector,
                                    tyreAnalyzer = tyreAnalyzer,
                                    onSuccess = { imagePath, result ->
                                        isCapturing = false
                                        isAnalyzing = false
                                        onTyreCaptured(imagePath, result)
                                    },
                                    onError = { error ->
                                        isCapturing = false
                                        isAnalyzing = false
                                        onError(error)
                                        Toast.makeText(context, error, Toast.LENGTH_LONG).show()
                                    }
                                )
                            }
                        }
                    },
                    modifier = Modifier
                        .size(72.dp)
                        .background(
                            when {
                                isCapturing -> Color.Gray
                                isReadyToCapture -> SuccessGreen
                                else -> PrimaryViolet
                            },
                            CircleShape
                        ),
                    enabled = !isCapturing
                ) {
                    if (isCapturing) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(32.dp),
                            color = Color.White,
                            strokeWidth = 3.dp
                        )
                    } else {
                        Icon(
                            Icons.Default.CameraAlt,
                            contentDescription = "Capture",
                            tint = Color.White,
                            modifier = Modifier.size(32.dp)
                        )
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Text(
                if (isAnalyzing) "Analyzing tyre..." 
                else if (isReadyToCapture) "Tap to capture" 
                else "Position tyre in frame",
                color = Color.White.copy(alpha = 0.7f),
                fontSize = 14.sp
            )
        }
        
        // Analyzing overlay
        AnimatedVisibility(
            visible = isAnalyzing,
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
                        modifier = Modifier.size(64.dp),
                        color = PrimaryViolet,
                        strokeWidth = 4.dp
                    )
                    Text(
                        "Analyzing Tyre Health...",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp
                    )
                    Text(
                        "Using ML to detect defects",
                        color = Color.White.copy(alpha = 0.7f),
                        fontSize = 14.sp
                    )
                }
            }
        }
    }
}

/**
 * Capture image and run analysis
 */
private suspend fun captureAndAnalyze(
    context: android.content.Context,
    imageCapture: ImageCapture?,
    tyreDetector: TyreDetectionService,
    tyreAnalyzer: TyreHealthAnalyzer,
    onSuccess: (String, TyreHealthResult) -> Unit,
    onError: (String) -> Unit
) {
    if (imageCapture == null) {
        onError("Camera not ready")
        return
    }
    
    withContext(Dispatchers.IO) {
        try {
            // Create output file
            val outputDir = File(context.cacheDir, "tyre_captures")
            if (!outputDir.exists()) outputDir.mkdirs()
            val outputFile = File(outputDir, "tyre_${System.currentTimeMillis()}.jpg")
            
            val outputOptions = ImageCapture.OutputFileOptions.Builder(outputFile).build()
            
            // Capture image
            val saved = suspendCancellableCoroutine<Boolean> { cont ->
                imageCapture.takePicture(
                    outputOptions,
                    ContextCompat.getMainExecutor(context),
                    object : ImageCapture.OnImageSavedCallback {
                        override fun onImageSaved(outputFileResults: ImageCapture.OutputFileResults) {
                            cont.resume(true) {}
                        }
                        
                        override fun onError(exception: ImageCaptureException) {
                            Log.e(TAG, "Capture failed: ${exception.message}", exception)
                            cont.resume(false) {}
                        }
                    }
                )
            }
            
            if (!saved) {
                withContext(Dispatchers.Main) {
                    onError("Failed to capture image")
                }
                return@withContext
            }
            
            // Detect tyre in captured image
            when (val detection = tyreDetector.detectTyresInFile(outputFile.absolutePath)) {
                is TyreDetectionResult.Success -> {
                    val bestTyre = detection.tyres.maxByOrNull { it.confidence }
                    
                    if (bestTyre?.croppedBitmap != null) {
                        // Analyze the cropped tyre
                        val analysisResult = tyreAnalyzer.analyzeTyre(bestTyre.croppedBitmap!!)
                        
                        // Save cropped tyre
                        val croppedFile = File(outputDir, "tyre_cropped_${System.currentTimeMillis()}.jpg")
                        java.io.FileOutputStream(croppedFile).use { out ->
                            bestTyre.croppedBitmap!!.compress(Bitmap.CompressFormat.JPEG, 95, out)
                        }
                        
                        withContext(Dispatchers.Main) {
                            onSuccess(croppedFile.absolutePath, analysisResult)
                        }
                    } else {
                        // Analyze full image if no crop available
                        val analysisResult = tyreAnalyzer.analyzeTyreFromFile(outputFile.absolutePath)
                        
                        withContext(Dispatchers.Main) {
                            onSuccess(outputFile.absolutePath, analysisResult)
                        }
                    }
                }
                is TyreDetectionResult.NoTyreFound -> {
                    // Analyze anyway with lower confidence
                    val analysisResult = tyreAnalyzer.analyzeTyreFromFile(outputFile.absolutePath)
                    
                    withContext(Dispatchers.Main) {
                        onSuccess(outputFile.absolutePath, analysisResult)
                    }
                }
                is TyreDetectionResult.Error -> {
                    withContext(Dispatchers.Main) {
                        onError("Detection failed: ${detection.exception.message}")
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Analysis failed: ${e.message}", e)
            withContext(Dispatchers.Main) {
                onError("Analysis failed: ${e.message}")
            }
        }
    }
}

private suspend inline fun <T> suspendCancellableCoroutine(
    crossinline block: (kotlinx.coroutines.CancellableContinuation<T>) -> Unit
): T = kotlinx.coroutines.suspendCancellableCoroutine { cont -> block(cont) }
