package org.example.project

import android.Manifest
import android.content.ContentValues
import android.content.Context
import android.graphics.Rect
import android.os.Build
import android.provider.MediaStore
import android.util.Log
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import com.google.common.util.concurrent.ListenableFuture
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.example.project.data.TyreAnalysisRepository
import org.example.project.data.UserPreferencesDataStore

/**
 * Flash mode options for camera.
 */
enum class FlashMode {
    Off,
    On,
    Auto
}

/**
 * Camera Screen for TyreGuard - Captures tyre images with real-time AI assistance.
 * 
 * Features:
 * - CameraX Preview with flash control
 * - Real-time ML Kit object detection
 * - Distance warning overlay (Too Close / Too Far / Perfect)
 * - Image capture with automatic tyre detection
 * 
 * @param onBackClick Callback to navigate back.
 * @param onImageCaptured Callback when image is captured with detection result.
 * @param onGalleryClick Callback to open gallery.
 */
@Composable
fun CameraScreen(
    modifier: Modifier = Modifier,
    onBackClick: () -> Unit = {},
    onImageCaptured: (imagePath: String, detected: Boolean) -> Unit = { _, _ -> },
    onGalleryClick: () -> Unit = {}
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    // Check if camera permission is granted
    var hasCameraPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.CAMERA
            ) == android.content.pm.PackageManager.PERMISSION_GRANTED
        )
    }

    // Permission launcher
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = androidx.activity.result.contract.ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        hasCameraPermission = isGranted
    }

    // Request permission on first composition
    LaunchedEffect(Unit) {
        if (!hasCameraPermission) {
            permissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    // Show permission request UI if not granted
    if (!hasCameraPermission) {
        PermissionRequestScreen(
            onBackClick = onBackClick,
            onRequestPermission = {
                permissionLauncher.launch(Manifest.permission.CAMERA)
            }
        )
        return
    }

    // Camera state
    var flashMode by remember { mutableStateOf(FlashMode.Off) }
    var distanceState by remember { mutableStateOf(DistanceState.Unknown) }
    var boundingBox by remember { mutableStateOf<Rect?>(null) }
    var isCapturing by remember { mutableStateOf(false) }
    var defectResults by remember { mutableStateOf<List<DetectionResult>>(emptyList()) }

    // Data layer
    val repository = remember { TyreAnalysisRepository(context) }
    val preferencesDataStore = remember { UserPreferencesDataStore(context) }
    val coroutineScope = rememberCoroutineScope()

    // Camera components
    var imageCapture by remember { mutableStateOf<ImageCapture?>(null) }
    var camera by remember { mutableStateOf<Camera?>(null) }
    val cameraExecutor = remember { Executors.newSingleThreadExecutor() }

    // Cleanup on dispose
    DisposableEffect(Unit) {
        onDispose {
            cameraExecutor.shutdown()
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        // ─────────────────────────────────────────────────────────────────
        // Camera Preview (Full Screen)
        // ─────────────────────────────────────────────────────────────────
        CameraPreviewView(
            context = context,
            lifecycleOwner = lifecycleOwner,
            flashMode = flashMode,
            cameraExecutor = cameraExecutor,
            onDistanceStateChanged = { distanceState = it },
            onBoundingBoxDetected = { boundingBox = it },
            onDefectDetected = { defectResults = it },
            onCameraReady = { capturer, cam ->
                imageCapture = capturer
                camera = cam
            }
        )

        // ─────────────────────────────────────────────────────────────────
        // Top Section: Flash Control & Back Button
        // ─────────────────────────────────────────────────────────────────
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Back button
            FilledIconButton(
                onClick = onBackClick,
                colors = IconButtonDefaults.filledIconButtonColors(
                    containerColor = Color.Black.copy(alpha = 0.5f),
                    contentColor = Color.White
                )
            ) {
                Icon(
                    imageVector = Icons.Filled.ArrowBack,
                    contentDescription = "Back"
                )
            }

            // Flash toggle button
            FilledIconButton(
                onClick = {
                    flashMode = when (flashMode) {
                        FlashMode.Off -> FlashMode.On
                        FlashMode.On -> FlashMode.Auto
                        FlashMode.Auto -> FlashMode.Off
                    }
                },
                colors = IconButtonDefaults.filledIconButtonColors(
                    containerColor = Color.Black.copy(alpha = 0.5f),
                    contentColor = Color.White
                )
            ) {
                Icon(
                    imageVector = when (flashMode) {
                        FlashMode.Off -> Icons.Filled.FlashOff
                        FlashMode.On -> Icons.Filled.FlashOn
                        FlashMode.Auto -> Icons.Filled.FlashAuto
                    },
                    contentDescription = "Flash: ${flashMode.name}"
                )
            }
        }

        // ─────────────────────────────────────────────────────────────────
        // Center Section: Distance Warning Overlay
        // ─────────────────────────────────────────────────────────────────
        DistanceWarningOverlay(
            distanceState = distanceState,
            modifier = Modifier
                .align(Alignment.Center)
                .padding(horizontal = 32.dp)
        )

        // ─────────────────────────────────────────────────────────────────
        // Defect Detection Results Overlay (Top-Right)
        // ─────────────────────────────────────────────────────────────────
        if (defectResults.isNotEmpty()) {
            DefectResultsOverlay(
                defects = defectResults,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .statusBarsPadding()
                    .padding(top = 72.dp, end = 16.dp)
            )
        }

        // ─────────────────────────────────────────────────────────────────
        // Bottom Section: Controls (Gallery, Shutter, Info)
        // ─────────────────────────────────────────────────────────────────
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter)
                .background(Color.Black.copy(alpha = 0.7f))
                .navigationBarsPadding()
                .padding(horizontal = 32.dp, vertical = 24.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Gallery button (left)
            FilledIconButton(
                onClick = onGalleryClick,
                modifier = Modifier.size(56.dp),
                colors = IconButtonDefaults.filledIconButtonColors(
                    containerColor = Color.DarkGray,
                    contentColor = Color.White
                )
            ) {
                Icon(
                    imageVector = Icons.Filled.PhotoLibrary,
                    contentDescription = "Gallery"
                )
            }

            // Shutter button (center) - Large circular button
            Button(
                onClick = {
                    if (!isCapturing && defectResults.isNotEmpty()) {
                        isCapturing = true
                        // Get the best defect result to save
                        val bestDefect = defectResults.maxByOrNull { it.confidence }
                        
                        captureImageWithAnalysis(
                            context = context,
                            imageCapture = imageCapture,
                            flashMode = flashMode,
                            executor = cameraExecutor,
                            defectResult = bestDefect,
                            repository = repository,
                            coroutineScope = coroutineScope,
                            onImageCaptured = { path, detected ->
                                isCapturing = false
                                onImageCaptured(path, detected)
                                Toast.makeText(context, "Analysis saved!", Toast.LENGTH_SHORT).show()
                            },
                            onError = {
                                isCapturing = false
                                Toast.makeText(context, "Capture failed", Toast.LENGTH_SHORT).show()
                            }
                        )
                    } else if (defectResults.isEmpty()) {
                        // No defect detected yet, just capture without saving analysis
                        isCapturing = true
                        captureImage(
                            context = context,
                            imageCapture = imageCapture,
                            flashMode = flashMode,
                            executor = cameraExecutor,
                            onImageCaptured = { path, detected ->
                                isCapturing = false
                                onImageCaptured(path, detected)
                            },
                            onError = {
                                isCapturing = false
                                Toast.makeText(context, "Capture failed", Toast.LENGTH_SHORT).show()
                            }
                        )
                    }
                },
                modifier = Modifier
                    .size(80.dp)
                    .clip(CircleShape),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color.White,
                    contentColor = Color.Black
                ),
                contentPadding = PaddingValues(0.dp),
                enabled = !isCapturing
            ) {
                if (isCapturing) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(32.dp),
                        color = Color.Black,
                        strokeWidth = 3.dp
                    )
                } else {
                    Box(
                        modifier = Modifier
                            .size(64.dp)
                            .clip(CircleShape)
                            .background(Color.White)
                    )
                }
            }

            // Placeholder for symmetry (right)
            Box(modifier = Modifier.size(56.dp))
        }
    }
}

/**
 * Camera preview using CameraX.
 */
@Composable
private fun CameraPreviewView(
    context: Context,
    lifecycleOwner: LifecycleOwner,
    flashMode: FlashMode,
    cameraExecutor: ExecutorService,
    onDistanceStateChanged: (DistanceState) -> Unit,
    onBoundingBoxDetected: (Rect?) -> Unit,
    onDefectDetected: (List<DetectionResult>) -> Unit,
    onCameraReady: (ImageCapture, Camera) -> Unit
) {
    val previewView = remember { PreviewView(context) }
    var cameraProviderFuture by remember { mutableStateOf<ListenableFuture<ProcessCameraProvider>?>(null) }
    var tyreAnalyzer by remember { mutableStateOf<TyreAnalyzer?>(null) }

    // Cleanup analyzer on dispose
    DisposableEffect(Unit) {
        onDispose {
            tyreAnalyzer?.shutdown()
        }
    }

    // Setup camera
    LaunchedEffect(Unit) {
        try {
            cameraProviderFuture = ProcessCameraProvider.getInstance(context)
        } catch (e: Exception) {
            Log.e("CameraScreen", "Failed to get camera provider", e)
        }
    }

    // Bind camera when provider is ready
    LaunchedEffect(cameraProviderFuture, flashMode) {
        val future = cameraProviderFuture ?: return@LaunchedEffect

        future.addListener({
            try {
                val cameraProvider = future.get()

                // Preview use case
                val preview = Preview.Builder()
                    .build()
                    .also {
                        it.surfaceProvider = previewView.surfaceProvider
                    }

                // Image capture use case
                val imageCaptureBuilder = ImageCapture.Builder()
                    .setCaptureMode(ImageCapture.CAPTURE_MODE_MAXIMIZE_QUALITY)
                
                when (flashMode) {
                    FlashMode.Off -> imageCaptureBuilder.setFlashMode(ImageCapture.FLASH_MODE_OFF)
                    FlashMode.On -> imageCaptureBuilder.setFlashMode(ImageCapture.FLASH_MODE_ON)
                    FlashMode.Auto -> imageCaptureBuilder.setFlashMode(ImageCapture.FLASH_MODE_AUTO)
                }
                
                val imageCapture = imageCaptureBuilder.build()

                // Select back camera
                val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

                // Unbind all first
                cameraProvider.unbindAll()
                
                // Create analyzer safely (can be null if TFLite fails)
                val analyzer = try {
                    TyreAnalyzer(
                        context = context,
                        onDistanceStateChanged = onDistanceStateChanged,
                        onBoundingBoxDetected = onBoundingBoxDetected,
                        onDefectDetected = onDefectDetected
                    ).also { tyreAnalyzer = it }
                } catch (e: Exception) {
                    Log.e("CameraScreen", "Failed to create TyreAnalyzer", e)
                    null
                }

                // Try binding with image analysis first
                if (analyzer != null) {
                    val imageAnalysis = ImageAnalysis.Builder()
                        .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                        .setOutputImageFormat(ImageAnalysis.OUTPUT_IMAGE_FORMAT_YUV_420_888)
                        .build()
                        .also {
                            it.setAnalyzer(cameraExecutor, analyzer)
                        }
                    
                    try {
                        val camera = cameraProvider.bindToLifecycle(
                            lifecycleOwner,
                            cameraSelector,
                            preview,
                            imageCapture,
                            imageAnalysis
                        )
                        onCameraReady(imageCapture, camera)
                        Log.d("CameraScreen", "Camera bound with analysis")
                        return@addListener
                    } catch (bindException: Exception) {
                        Log.w("CameraScreen", "Binding with analysis failed, trying without", bindException)
                        analyzer.shutdown()
                        tyreAnalyzer = null
                    }
                }
                
                // Fallback: bind without image analysis
                try {
                    val camera = cameraProvider.bindToLifecycle(
                        lifecycleOwner,
                        cameraSelector,
                        preview,
                        imageCapture
                    )
                    onCameraReady(imageCapture, camera)
                    Log.d("CameraScreen", "Camera bound without analysis (fallback)")
                } catch (fallbackException: Exception) {
                    Log.e("CameraScreen", "Fallback binding also failed", fallbackException)
                }

            } catch (e: Exception) {
                Log.e("CameraScreen", "Camera binding failed", e)
            } catch (e: IllegalStateException) {
                Log.e("CameraScreen", "Camera in illegal state (activity may be finishing)", e)
            }
        }, ContextCompat.getMainExecutor(context))
    }

    AndroidView(
        factory = { previewView },
        modifier = Modifier.fillMaxSize()
    )
}

/**
 * Distance warning overlay based on tyre detection state.
 * Floats over the camera preview with semi-transparent background.
 */
@Composable
private fun DistanceWarningOverlay(
    distanceState: DistanceState,
    modifier: Modifier = Modifier
) {
    val (backgroundColor, textColor, message, borderColor) = when (distanceState) {
        DistanceState.Unknown -> listOf(
            Color.Black.copy(alpha = 0.6f),
            Color.White,
            "📷 Point camera at tyre",
            Color.White.copy(alpha = 0.3f)
        )
        DistanceState.TooFar -> listOf(
            Color(0xFFFF5252).copy(alpha = 0.85f),
            Color.White,
            "📍 Too Far - Move Closer",
            Color.White.copy(alpha = 0.5f)
        )
        DistanceState.TooClose -> listOf(
            Color(0xFFFF5252).copy(alpha = 0.85f),
            Color.White,
            "⚠️ Too Close - Move Back",
            Color.White.copy(alpha = 0.5f)
        )
        DistanceState.Perfect -> listOf(
            Color(0xFF4CAF50).copy(alpha = 0.85f),
            Color.White,
            "✓ Perfect Distance",
            Color.White.copy(alpha = 0.5f)
        )
    }

    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(24.dp),
        color = backgroundColor as Color,
        shadowElevation = 8.dp,
        border = androidx.compose.foundation.BorderStroke(
            width = 1.dp,
            color = borderColor as Color
        )
    ) {
        Text(
            text = message as String,
            modifier = Modifier.padding(horizontal = 28.dp, vertical = 14.dp),
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
            color = textColor as Color,
            textAlign = TextAlign.Center
        )
    }
}

/**
 * Overlay to display tyre defect detection results.
 * Shows detected defects with color-coded severity.
 */
@Composable
private fun DefectResultsOverlay(
    defects: List<DetectionResult>,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        defects.take(3).forEach { defect ->
            val isGood = defect.label.equals("Good", ignoreCase = true)
            val backgroundColor = if (isGood) {
                Color(0xFF4CAF50).copy(alpha = 0.9f)
            } else {
                Color(0xFFE53935).copy(alpha = 0.9f)
            }
            val icon = if (isGood) "✓" else "⚠️"

            Surface(
                shape = RoundedCornerShape(16.dp),
                color = backgroundColor,
                shadowElevation = 4.dp
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = icon,
                        style = MaterialTheme.typography.titleMedium
                    )
                    Column {
                        Text(
                            text = defect.label,
                            style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                            color = Color.White
                        )
                        Text(
                            text = "${(defect.confidence * 100).toInt()}% confidence",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.White.copy(alpha = 0.8f)
                        )
                    }
                }
            }
        }
    }
}

/**
 * Capture image and save to gallery.
 */
private fun captureImage(
    context: Context,
    imageCapture: ImageCapture?,
    flashMode: FlashMode,
    executor: ExecutorService,
    onImageCaptured: (path: String, detected: Boolean) -> Unit,
    onError: () -> Unit
) {
    if (imageCapture == null) {
        onError()
        return
    }

    // Create file name
    val name = SimpleDateFormat("yyyy-MM-dd-HH-mm-ss-SSS", Locale.US)
        .format(System.currentTimeMillis())

    // Save to MediaStore (gallery)
    val contentValues = ContentValues().apply {
        put(MediaStore.MediaColumns.DISPLAY_NAME, "TyreGuard_$name")
        put(MediaStore.MediaColumns.MIME_TYPE, "image/jpeg")
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            put(MediaStore.Images.Media.RELATIVE_PATH, "Pictures/TyreGuard")
        }
    }

    val outputOptions = ImageCapture.OutputFileOptions.Builder(
        context.contentResolver,
        MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
        contentValues
    ).build()

    imageCapture.takePicture(
        outputOptions,
        executor,
        object : ImageCapture.OnImageSavedCallback {
            override fun onImageSaved(outputFileResults: ImageCapture.OutputFileResults) {
                val savedUri = outputFileResults.savedUri
                val path = savedUri?.toString() ?: ""
                
                // For now, assume detection is successful if image is saved
                // In production, you would analyze the saved image with ML Kit
                onImageCaptured(path, true)
            }

            override fun onError(exception: ImageCaptureException) {
                Log.e("CameraScreen", "Image capture failed: ${exception.message}", exception)
                onError()
            }
        }
    )
}

/**
 * Capture image and save to gallery with analysis results.
 * Saves both the image and analysis data to the repository.
 */
private fun captureImageWithAnalysis(
    context: Context,
    imageCapture: ImageCapture?,
    flashMode: FlashMode,
    executor: ExecutorService,
    defectResult: DetectionResult?,
    repository: TyreAnalysisRepository,
    coroutineScope: CoroutineScope,
    onImageCaptured: (path: String, detected: Boolean) -> Unit,
    onError: () -> Unit
) {
    if (imageCapture == null) {
        onError()
        return
    }

    // Create file name
    val name = SimpleDateFormat("yyyy-MM-dd-HH-mm-ss-SSS", Locale.US)
        .format(System.currentTimeMillis())

    // Save to MediaStore (gallery)
    val contentValues = ContentValues().apply {
        put(MediaStore.MediaColumns.DISPLAY_NAME, "TyreGuard_$name")
        put(MediaStore.MediaColumns.MIME_TYPE, "image/jpeg")
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            put(MediaStore.Images.Media.RELATIVE_PATH, "Pictures/TyreGuard")
        }
    }

    val outputOptions = ImageCapture.OutputFileOptions.Builder(
        context.contentResolver,
        MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
        contentValues
    ).build()

    imageCapture.takePicture(
        outputOptions,
        executor,
        object : ImageCapture.OnImageSavedCallback {
            override fun onImageSaved(outputFileResults: ImageCapture.OutputFileResults) {
                val savedUri = outputFileResults.savedUri
                val path = savedUri?.toString() ?: ""
                
                // Save analysis result to database
                if (defectResult != null && path.isNotEmpty()) {
                    coroutineScope.launch(Dispatchers.IO) {
                        try {
                            repository.saveAnalysisWithPath(
                                imagePath = path,
                                defectType = defectResult.label,
                                confidence = defectResult.confidence
                            )
                            Log.d("CameraScreen", "Analysis saved: ${defectResult.label} (${defectResult.confidence})")
                        } catch (e: Exception) {
                            Log.e("CameraScreen", "Failed to save analysis: ${e.message}", e)
                        }
                    }
                }
                
                onImageCaptured(path, true)
            }

            override fun onError(exception: ImageCaptureException) {
                Log.e("CameraScreen", "Image capture failed: ${exception.message}", exception)
                onError()
            }
        }
    )
}

/**
 * Screen shown when camera permission is not granted.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PermissionRequestScreen(
    onBackClick: () -> Unit,
    onRequestPermission: () -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Camera Permission Required") },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(
                            imageVector = Icons.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .padding(paddingValues)
                .fillMaxSize()
                .padding(32.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = Icons.Filled.CameraAlt,
                contentDescription = null,
                modifier = Modifier.size(80.dp),
                tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.6f)
            )
            
            Spacer(modifier = Modifier.height(24.dp))
            
            Text(
                text = "Camera Access Needed",
                style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
                textAlign = TextAlign.Center
            )
            
            Spacer(modifier = Modifier.height(12.dp))
            
            Text(
                text = "TyreGuard needs camera access to scan and analyze your tyres for defects.",
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f)
            )
            
            Spacer(modifier = Modifier.height(32.dp))
            
            Button(
                onClick = onRequestPermission,
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(
                    imageVector = Icons.Filled.CameraAlt,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("Grant Camera Permission")
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            TextButton(onClick = onBackClick) {
                Text("Go Back")
            }
        }
    }
}
