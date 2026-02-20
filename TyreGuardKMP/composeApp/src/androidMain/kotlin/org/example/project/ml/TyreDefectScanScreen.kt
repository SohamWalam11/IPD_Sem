package org.example.project.ml

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.RectF
import android.util.Log
import android.view.ViewGroup
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.draw.*
import androidx.compose.ui.geometry.*
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.*
import androidx.compose.ui.platform.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.*
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import kotlinx.coroutines.*
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.Executors
import kotlin.math.min

// ─────────────────────────────────────────────────────────────────────────────
// Theme tokens
// ─────────────────────────────────────────────────────────────────────────────
private val ScreenBg      = Color(0xFF0D0D1A)
private val SurfaceDark   = Color(0xFF161625)
private val CardDark      = Color(0xFF1E1E30)
private val AccentPurple  = Color(0xFF7C3AED)
private val AccentPurple2 = Color(0xFF5B21B6)
private val GoodGreen     = Color(0xFF10B981)
private val WarningAmber  = Color(0xFFF59E0B)
private val DangerRed     = Color(0xFFEF4444)
private val ScanRed       = Color(0xFFFF2020)   // TireChecker-style bright red reticle
private val TextPrimary   = Color(0xFFECECFF)
private val TextSecondary = Color(0xFF9CA3AF)

// Per-label box tint
private fun defectColor(label: String) = when (label.lowercase()) {
    "bulge"       -> DangerRed
    "crack"       -> WarningAmber
    "bald_tread"  -> Color(0xFFFF6D00)
    else          -> AccentPurple
}

private fun healthColor(score: Int) = when {
    score >= 75 -> GoodGreen
    score >= 45 -> WarningAmber
    else        -> DangerRed
}

// ─────────────────────────────────────────────────────────────────────────────
// Tread zone analysis
// ─────────────────────────────────────────────────────────────────────────────

/**
 * Result for one of the three horizontal tread zones (Left / Centre / Right).
 */
private data class ZoneResult(
    val name: String,
    val passed: Boolean,
    val treadPct: Int   // 0–100 estimated tread depth percentage
)

/**
 * Splits detected defects into Left / Centre / Right zones based on horizontal
 * bounding-box centre and returns a pass/fail + tread % for each zone.
 */
private fun computeZoneResults(defects: List<TyreDefect>, bitmapWidth: Int): List<ZoneResult> {
    val zoneNames  = listOf("Left Tread", "Centre Tread", "Right Tread")
    val thirdW     = bitmapWidth / 3f

    return zoneNames.mapIndexed { idx, name ->
        val zoneDefs = defects.filter { d ->
            val cx = d.boundingBox.centerX()
            cx >= idx * thirdW && cx < (idx + 1) * thirdW
        }
        val penalty = zoneDefs.sumOf { d ->
            when (d.label.lowercase()) {
                "bulge"      -> (40 * d.confidence).toInt()
                "crack"      -> (25 * d.confidence).toInt()
                "bald_tread" -> (20 * d.confidence).toInt()
                else         -> (10 * d.confidence).toInt()
            }
        }
        val treadPct = (100 - penalty).coerceIn(0, 100)
        ZoneResult(name = name, passed = treadPct >= 60, treadPct = treadPct)
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Screen state machine
// ─────────────────────────────────────────────────────────────────────────────
private sealed interface ScanState {
    object Idle : ScanState
    object Capturing : ScanState
    object Analyzing : ScanState
    data class Result(val bitmap: Bitmap, val report: TyreHealthReport) : ScanState
    data class PermissionDenied(val message: String) : ScanState
}

private const val TAG = "TyreDefectScanScreen"

// ─────────────────────────────────────────────────────────────────────────────
// Entry-point composable
// ─────────────────────────────────────────────────────────────────────────────

/**
 * Production-ready tyre defect scan screen.
 *
 * Flow:
 *  1. Request camera permission (modern [ActivityResultContracts], no Accompanist).
 *  2. Live CameraX preview with real-time lightweight frame analysis.
 *  3. User taps Capture → full-res [ImageCapture] snapshot.
 *  4. Run [TyreTaskVisionDetector] on the captured bitmap.
 *  5. Display captured image + bounding-box overlay drawn in Compose [Canvas].
 *     * Scaling math maps model pixel-coords → Compose dp-coords correctly.
 *  6. Show [TyreHealthReport] cards beneath the image.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TyreDefectScanScreen(onBack: () -> Unit = {}) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val scope = rememberCoroutineScope()

    // ── Permission ──────────────────────────────────────────────────────────
    var hasCameraPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA)
                    == PackageManager.PERMISSION_GRANTED
        )
    }
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted -> hasCameraPermission = granted }

    LaunchedEffect(Unit) {
        if (!hasCameraPermission) permissionLauncher.launch(Manifest.permission.CAMERA)
    }

    // ── ML ──────────────────────────────────────────────────────────────────
    val detector = remember { TyreTaskVisionDetector(context) }
    DisposableEffect(Unit) { onDispose { detector.close() } }

    // ── CameraX refs ────────────────────────────────────────────────────────
    var imageCapture by remember { mutableStateOf<ImageCapture?>(null) }
    val cameraExecutor = remember { Executors.newSingleThreadExecutor() }
    DisposableEffect(Unit) { onDispose { cameraExecutor.shutdown() } }

    // ── Scan state ──────────────────────────────────────────────────────────
    var scanState by remember { mutableStateOf<ScanState>(ScanState.Idle) }

    // ── Live preview frame analysis (lightweight, every 8 frames) ───────────
    val liveAnalyzer = remember {
        TyreDefectCameraAnalyzer(
            detector = detector,
            analyzeEveryN = 8,
            onFrameResult = { _, _ -> /* live annotations removed for perf */ }
        )
    }

    // ────────────────────────────────────────────────────────────────────────
    // CameraX: bind Preview + ImageCapture + ImageAnalysis
    // ────────────────────────────────────────────────────────────────────────
    fun bindCamera(previewView: PreviewView) {
        val providerFuture = ProcessCameraProvider.getInstance(context)
        providerFuture.addListener({
            val provider = providerFuture.get()

            val preview = Preview.Builder().build()
                .also { it.surfaceProvider = previewView.surfaceProvider }

            val capture = ImageCapture.Builder()
                .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
                .build()
            imageCapture = capture

            val analysis = ImageAnalysis.Builder()
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .build()
                .also { it.setAnalyzer(cameraExecutor, liveAnalyzer) }

            try {
                provider.unbindAll()
                provider.bindToLifecycle(
                    lifecycleOwner,
                    CameraSelector.DEFAULT_BACK_CAMERA,
                    preview, capture, analysis
                )
            } catch (e: Exception) {
                Log.e(TAG, "Camera bind failed: ${e.message}", e)
            }
        }, ContextCompat.getMainExecutor(context))
    }

    // ────────────────────────────────────────────────────────────────────────
    // Capture + analyse action
    // ────────────────────────────────────────────────────────────────────────
    fun captureAndAnalyze() {
        val ic = imageCapture ?: return
        scanState = ScanState.Capturing

        ic.takePicture(cameraExecutor, object : ImageCapture.OnImageCapturedCallback() {
            override fun onCaptureSuccess(proxy: ImageProxy) {
                scope.launch {
                    scanState = ScanState.Analyzing
                    try {
                        // Convert ImageProxy → Bitmap with proper rotation
                        val rotationDegrees = proxy.imageInfo.rotationDegrees
                        val raw = imageProxyToBitmap(proxy)
                        proxy.close()

                        val rotated = if (rotationDegrees != 0) {
                            val m = android.graphics.Matrix().apply {
                                postRotate(rotationDegrees.toFloat())
                            }
                            Bitmap.createBitmap(raw, 0, 0, raw.width, raw.height, m, true)
                                .also { if (it !== raw) raw.recycle() }
                        } else raw

                        val report = withContext(Dispatchers.Default) {
                            detector.analyzeAndScore(rotated)
                        }
                        scanState = ScanState.Result(rotated, report)
                    } catch (e: Exception) {
                        Log.e(TAG, "Capture processing failed", e)
                        proxy.close()
                        scanState = ScanState.Idle
                    }
                }
            }

            override fun onError(exception: ImageCaptureException) {
                Log.e(TAG, "Capture error: ${exception.message}")
                scope.launch { scanState = ScanState.Idle }
            }
        })
    }

    // ────────────────────────────────────────────────────────────────────────
    // Root layout
    // ────────────────────────────────────────────────────────────────────────
    Box(modifier = Modifier.fillMaxSize().background(ScreenBg)) {
        when {
            !hasCameraPermission -> PermissionBlocker(
                onRequest = { permissionLauncher.launch(Manifest.permission.CAMERA) }
            )

            scanState is ScanState.Result -> {
                val s = scanState as ScanState.Result
                ResultsView(
                    bitmap  = s.bitmap,
                    report  = s.report,
                    onRetry = { scanState = ScanState.Idle },
                    onBack  = onBack
                )
            }

            else -> LiveViewfinderView(
                onBindCamera       = ::bindCamera,
                onCaptureClick     = ::captureAndAnalyze,
                scanState          = scanState,
                onBack             = onBack
            )
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Live viewfinder
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun LiveViewfinderView(
    onBindCamera: (PreviewView) -> Unit,
    onCaptureClick: () -> Unit,
    scanState: ScanState,
    onBack: () -> Unit
) {
    val isCapturing = scanState is ScanState.Capturing || scanState is ScanState.Analyzing

    Box(modifier = Modifier.fillMaxSize()) {
        // ── Camera preview ──────────────────────────────────────────────────
        AndroidView(
            factory = { ctx ->
                PreviewView(ctx).apply {
                    layoutParams = ViewGroup.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT
                    )
                    scaleType = PreviewView.ScaleType.FILL_CENTER
                    onBindCamera(this)
                }
            },
            modifier = Modifier.fillMaxSize()
        )

        // ── Top info bar (TireChecker-style HUD) ────────────────────────────
        val today     = remember { SimpleDateFormat("dd MMM yyyy", Locale.getDefault()).format(Date()) }
        val nextTest  = remember {
            val c = java.util.Calendar.getInstance().also { it.add(java.util.Calendar.MONTH, 3) }
            SimpleDateFormat("dd MMM yyyy", Locale.getDefault()).format(c.time)
        }
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color.Black.copy(alpha = 0.65f))
                .statusBarsPadding()
                .padding(horizontal = 14.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = onBack,
                modifier = Modifier.size(36.dp)
            ) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, null, tint = Color.White)
            }
            Column(modifier = Modifier.weight(1f).padding(start = 4.dp)) {
                Text(
                    "TYRE DIAGNOSIS",
                    color = ScanRed,
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 14.sp,
                    letterSpacing = 1.5.sp
                )
                Text(
                    "Last: $today  •  Next: $nextTest",
                    color = TextSecondary,
                    fontSize = 11.sp
                )
            }
            // Status badge
            Box(
                modifier = Modifier
                    .background(ScanRed.copy(alpha = 0.15f), RoundedCornerShape(6.dp))
                    .padding(horizontal = 8.dp, vertical = 3.dp)
            ) {
                Text(
                    if (isCapturing) "SCANNING…" else "READY",
                    color = if (isCapturing) WarningAmber else ScanRed,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
                )
            }
        }

        // ── TireChecker reticle: red L-brackets + 3 tread zone dividers ─────
        val pulse = rememberInfiniteTransition(label = "pulse")
        val pulseAlpha by pulse.animateFloat(
            initialValue = 0.7f, targetValue = 1.0f,
            animationSpec = infiniteRepeatable(tween(900, easing = EaseInOutSine), RepeatMode.Reverse),
            label = "pulseAlpha"
        )
        Canvas(modifier = Modifier.fillMaxSize()) {
            val w = size.width;  val h = size.height
            val cx = w / 2f;    val cy = h / 2f
            val rectW  = w * 0.88f;  val rectH  = rectW * 1.05f
            val left   = cx - rectW / 2f;  val top    = cy - rectH / 2f
            val right  = cx + rectW / 2f;  val bottom = cy + rectH / 2f
            val arm    = 44.dp.toPx()
            val sw     = 3.5f.dp.toPx()
            val rc     = ScanRed.copy(alpha = if (isCapturing) 1f else pulseAlpha)

            // ── L-bracket corners ──────────────────────────────────────────
            fun lBracket(ox: Float, oy: Float, dx: Float, dy: Float) {
                drawLine(rc, Offset(ox, oy), Offset(ox + dx * arm, oy),  strokeWidth = sw, cap = StrokeCap.Round)
                drawLine(rc, Offset(ox, oy), Offset(ox, oy + dy * arm),  strokeWidth = sw, cap = StrokeCap.Round)
            }
            lBracket(left,  top,    +1f, +1f)
            lBracket(right, top,    -1f, +1f)
            lBracket(left,  bottom, +1f, -1f)
            lBracket(right, bottom, -1f, -1f)

            // ── Outer frame (dim border) ───────────────────────────────────
            drawRect(
                color   = ScanRed.copy(alpha = 0.18f),
                topLeft = Offset(left, top),
                size    = Size(rectW, rectH),
                style   = Stroke(width = 1.dp.toPx())
            )

            // ── Tread zone dividers (2 vertical lines @ 1/3 and 2/3) ──────
            val zone1X = left + rectW / 3f
            val zone2X = left + rectW * 2f / 3f
            val divColor = Color.White.copy(alpha = 0.35f)
            val divSw    = 1.5f.dp.toPx()

            drawLine(divColor, Offset(zone1X, top + 8.dp.toPx()), Offset(zone1X, bottom - 8.dp.toPx()), strokeWidth = divSw)
            drawLine(divColor, Offset(zone2X, top + 8.dp.toPx()), Offset(zone2X, bottom - 8.dp.toPx()), strokeWidth = divSw)

            // ── Zone labels ────────────────────────────────────────────────
            val zoneLabelPaint = android.graphics.Paint().apply {
                textSize    = 9.5f.dp.toPx()
                color       = android.graphics.Color.argb(180, 255, 255, 255)
                isAntiAlias = true
                isFakeBoldText = true
                textAlign   = android.graphics.Paint.Align.CENTER
            }
            fun zoneLabel(label: String, centerX: Float) {
                drawContext.canvas.nativeCanvas.drawText(
                    label,
                    centerX,
                    top + 22.dp.toPx(),
                    zoneLabelPaint
                )
            }
            zoneLabel("LEFT",   left + rectW / 6f)
            zoneLabel("CENTRE", cx)
            zoneLabel("RIGHT",  right - rectW / 6f)
        }

        // ── Guidance label ──────────────────────────────────────────────────
        Box(modifier = Modifier.align(Alignment.BottomCenter).navigationBarsPadding().padding(bottom = 140.dp)) {
            Text(
                "Align tyre sidewall inside the frame",
                color = Color.White,
                fontSize = 12.sp,
                modifier = Modifier
                    .background(Color.Black.copy(alpha = 0.55f), RoundedCornerShape(20.dp))
                    .padding(horizontal = 14.dp, vertical = 6.dp)
            )
        }

        // ── Bottom action bar ────────────────────────────────────────────────
        Row(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .navigationBarsPadding()
                .background(Color.Black.copy(alpha = 0.70f))
                .padding(horizontal = 16.dp, vertical = 14.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Share button
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                IconButton(
                    onClick = { /* share */ },
                    modifier = Modifier
                        .size(46.dp)
                        .background(SurfaceDark, CircleShape)
                ) {
                    Icon(Icons.Filled.Share, null, tint = TextSecondary, modifier = Modifier.size(22.dp))
                }
                Text("Share", color = TextSecondary, fontSize = 11.sp)
            }

            // Main SCAN button
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                if (isCapturing) {
                    Box(
                        modifier = Modifier.size(72.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(color = ScanRed, modifier = Modifier.size(64.dp), strokeWidth = 3.dp)
                    }
                } else {
                    Box(
                        modifier = Modifier
                            .size(72.dp)
                            .background(ScanRed, CircleShape)
                            .border(3.dp, Color.White.copy(alpha = 0.5f), CircleShape)
                            .clickable(onClick = onCaptureClick),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Filled.Camera, null, tint = Color.White, modifier = Modifier.size(36.dp))
                    }
                }
                Text(if (isCapturing) "Scanning…" else "Scan", color = if (isCapturing) WarningAmber else ScanRed, fontSize = 11.sp, fontWeight = FontWeight.Bold)
            }

            // Help/Info button
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                IconButton(
                    onClick = { /* help */ },
                    modifier = Modifier
                        .size(46.dp)
                        .background(SurfaceDark, CircleShape)
                ) {
                    Icon(Icons.Filled.Info, null, tint = TextSecondary, modifier = Modifier.size(22.dp))
                }
                Text("Help", color = TextSecondary, fontSize = 11.sp)
            }
        }

        if (isCapturing) {
            // Dim overlay while processing
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.55f)),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    CircularProgressIndicator(color = ScanRed, modifier = Modifier.size(52.dp), strokeWidth = 3.dp)
                    Spacer(Modifier.height(16.dp))
                    Text(
                        if (scanState is ScanState.Capturing) "Capturing…" else "Analysing with AI…",
                        color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Medium
                    )
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Results screen
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun ResultsView(
    bitmap: Bitmap,
    report: TyreHealthReport,
    onRetry: () -> Unit,
    onBack: () -> Unit
) {
    val zones = remember(report) { computeZoneResults(report.defects, bitmap.width) }
    val today = remember { SimpleDateFormat("dd MMM yyyy", Locale.getDefault()).format(Date()) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
    ) {
        // ── Top bar ──────────────────────────────────────────────────────────
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(SurfaceDark)
                .statusBarsPadding()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, null, tint = TextPrimary)
            }
            Column(modifier = Modifier.weight(1f).padding(start = 4.dp)) {
                Text("Scan Results", color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                Text(today, color = TextSecondary, fontSize = 11.sp)
            }
            TextButton(onClick = onRetry) {
                Icon(Icons.Filled.Refresh, null, tint = ScanRed, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(4.dp))
                Text("Retry", color = ScanRed, fontWeight = FontWeight.SemiBold)
            }
        }

        // ── Image + Bounding-Box Canvas ──────────────────────────────────────
        AnnotatedTyreImage(
            bitmap  = bitmap,
            defects = report.defects,
            zones   = zones,
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(bitmap.width.toFloat() / bitmap.height.toFloat())
        )

        // ── Zone tread results (Left / Centre / Right) ───────────────────────
        Text(
            "TREAD ZONE ANALYSIS",
            color = TextSecondary,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.sp,
            modifier = Modifier.padding(start = 16.dp, top = 16.dp, bottom = 8.dp)
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            zones.forEach { zone ->
                ZoneResultCard(zone = zone, modifier = Modifier.weight(1f))
            }
        }

        // ── Health score card ─────────────────────────────────────────────────
        HealthScoreCard(report = report)

        // ── Defect list ───────────────────────────────────────────────────────
        if (report.defects.isEmpty()) {
            NoDefectsCard()
        } else {
            Text(
                "DETECTED DEFECTS",
                color = TextSecondary,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp,
                modifier = Modifier.padding(start = 16.dp, top = 8.dp, bottom = 4.dp)
            )
            report.defects.forEach { DefectItemCard(it) }
        }

        // ── Bottom action bar ─────────────────────────────────────────────────
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            OutlinedButton(
                onClick = { /* share */ },
                modifier = Modifier.weight(1f).height(48.dp),
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.dp, TextSecondary.copy(alpha = 0.4f))
            ) {
                Icon(Icons.Filled.Share, null, tint = TextSecondary, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text("Share", color = TextSecondary)
            }
            Button(
                onClick = onRetry,
                modifier = Modifier.weight(1f).height(48.dp),
                colors = ButtonDefaults.buttonColors(containerColor = ScanRed),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(Icons.Filled.Camera, null, tint = Color.White, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text("Scan Again", color = Color.White, fontWeight = FontWeight.Bold)
            }
        }

        Spacer(Modifier.height(32.dp))
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Zone result card (Left / Centre / Right)
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun ZoneResultCard(zone: ZoneResult, modifier: Modifier = Modifier) {
    val bg    = if (zone.passed) GoodGreen.copy(alpha = 0.12f) else DangerRed.copy(alpha = 0.12f)
    val color = if (zone.passed) GoodGreen else DangerRed
    val icon  = if (zone.passed) Icons.Filled.CheckCircle else Icons.Filled.Warning

    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = bg),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(icon, null, tint = color, modifier = Modifier.size(22.dp))
            Spacer(Modifier.height(4.dp))
            Text(
                zone.name,
                color = TextSecondary,
                fontSize = 10.sp,
                fontWeight = FontWeight.Medium,
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(2.dp))
            Text(
                if (zone.passed) "PASSED" else "FAILED",
                color = color,
                fontWeight = FontWeight.ExtraBold,
                fontSize = 12.sp,
                letterSpacing = 0.5.sp
            )
            Spacer(Modifier.height(4.dp))
            // Tread % mini-bar
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(4.dp)
                    .background(Color.White.copy(alpha = 0.08f), RoundedCornerShape(2.dp))
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(zone.treadPct / 100f)
                        .fillMaxHeight()
                        .background(color, RoundedCornerShape(2.dp))
                )
            }
            Spacer(Modifier.height(2.dp))
            Text("${zone.treadPct}%", color = color, fontSize = 11.sp, fontWeight = FontWeight.Bold)
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Annotated image with bounding-box + zone overlay canvas
// ─────────────────────────────────────────────────────────────────────────────

/**
 * Draws [bitmap] into a Compose [Canvas], overlays bounding boxes for each
 * [TyreDefect], and draws vertical zone divider lines with [ZoneResult] labels.
 */
@Composable
private fun AnnotatedTyreImage(
    bitmap: Bitmap,
    defects: List<TyreDefect>,
    zones: List<ZoneResult> = emptyList(),
    modifier: Modifier = Modifier
) {
    val imageBitmap = remember(bitmap) { bitmap.asImageBitmap() }

    Canvas(modifier = modifier) {
        // ── 1. Draw the captured photo ──────────────────────────────────────
        drawImage(image = imageBitmap, dstSize = IntSize(size.width.toInt(), size.height.toInt()))

        // ── 2. Scale factors: bitmap-space → canvas-space ───────────────────
        val scaleX = size.width  / bitmap.width.toFloat()
        val scaleY = size.height / bitmap.height.toFloat()

        // ── 3. Zone dividers + pass/fail labels ─────────────────────────────
        if (zones.isNotEmpty()) {
            val zone1X = size.width / 3f
            val zone2X = size.width * 2f / 3f
            val divColor = Color.White.copy(alpha = 0.50f)

            drawLine(divColor, Offset(zone1X, 0f), Offset(zone1X, size.height), strokeWidth = 2.dp.toPx())
            drawLine(divColor, Offset(zone2X, 0f), Offset(zone2X, size.height), strokeWidth = 2.dp.toPx())

            val zoneCentres = listOf(size.width / 6f, size.width / 2f, size.width * 5f / 6f)
            val labelPaint  = android.graphics.Paint().apply {
                textSize    = 11.dp.toPx()
                isAntiAlias = true
                isFakeBoldText = true
                textAlign   = android.graphics.Paint.Align.CENTER
            }
            zones.forEachIndexed { i, zone ->
                val cx = zoneCentres[i]
                val badgeColor  = if (zone.passed) GoodGreen else DangerRed
                val badgeText   = if (zone.passed) "✓ PASSED" else "✗ FAILED"

                // Pill background
                val pillW = 80.dp.toPx();  val pillH = 22.dp.toPx()
                drawRect(
                    color   = badgeColor.copy(alpha = 0.85f),
                    topLeft = Offset(cx - pillW / 2f, size.height - pillH - 8.dp.toPx()),
                    size    = Size(pillW, pillH)
                )
                labelPaint.color = android.graphics.Color.WHITE
                drawContext.canvas.nativeCanvas.drawText(
                    badgeText,
                    cx,
                    size.height - 14.dp.toPx(),
                    labelPaint
                )
            }
        }

        // ── 4. Bounding box + label for each defect ─────────────────────────
        defects.forEach { defect ->
            val color = defectColor(defect.label)
            val box   = defect.boundingBox

            val left   = box.left   * scaleX
            val top    = box.top    * scaleY
            val right  = box.right  * scaleX
            val bottom = box.bottom * scaleY

            drawRect(
                color   = color,
                topLeft = Offset(left, top),
                size    = Size(right - left, bottom - top),
                style   = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round)
            )

            val labelHeight = 22.dp.toPx()
            val labelWidth  = min((right - left), 160.dp.toPx())
            drawRect(
                color   = color.copy(alpha = 0.75f),
                topLeft = Offset(left, top - labelHeight),
                size    = Size(labelWidth, labelHeight)
            )

            val labelText = "${defect.label}  ${"%.0f".format(defect.confidence * 100)}%"
            drawContext.canvas.nativeCanvas.drawText(
                labelText,
                left + 4.dp.toPx(),
                top - 5.dp.toPx(),
                android.graphics.Paint().apply {
                    textSize       = 12.dp.toPx()
                    this.color     = android.graphics.Color.WHITE
                    isAntiAlias    = true
                    isFakeBoldText = true
                }
            )
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Health score card
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun HealthScoreCard(report: TyreHealthReport) {
    val scoreColor = healthColor(report.healthScore)
    val animated = remember { Animatable(0f) }
    LaunchedEffect(report.healthScore) {
        animated.animateTo(report.healthScore / 100f, tween(1000))
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        colors = CardDefaults.cardColors(containerColor = CardDark),
        shape = RoundedCornerShape(20.dp),
        elevation = CardDefaults.cardElevation(8.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(20.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Radial health gauge
            Box(contentAlignment = Alignment.Center, modifier = Modifier.size(90.dp)) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val stroke = Stroke(width = 10.dp.toPx(), cap = StrokeCap.Round)
                    // Track
                    drawArc(Color.White.copy(alpha = 0.08f), -210f, 240f, false, style = stroke)
                    // Progress arc
                    drawArc(scoreColor, -210f, 240f * animated.value, false, style = stroke)
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        "${report.healthScore}",
                        color = scoreColor,
                        fontSize = 26.sp,
                        fontWeight = FontWeight.ExtraBold,
                        lineHeight = 26.sp
                    )
                    Text("/100", color = TextSecondary, fontSize = 11.sp)
                }
            }

            Spacer(Modifier.width(20.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text("Health Score", color = TextSecondary, fontSize = 13.sp)
                Spacer(Modifier.height(4.dp))
                Text(
                    report.verdict,
                    color = TextPrimary,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 15.sp,
                    lineHeight = 20.sp
                )
                Spacer(Modifier.height(10.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Filled.DirectionsCar, null, tint = AccentPurple, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Est. RUL: ", color = TextSecondary, fontSize = 13.sp)
                    Text(report.estimatedRulKm, color = scoreColor, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Defect item card
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun DefectItemCard(defect: TyreDefect) {
    val color = defectColor(defect.label)
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp),
        colors = CardDefaults.cardColors(containerColor = CardDark),
        shape = RoundedCornerShape(14.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .background(color.copy(alpha = 0.15f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (color == DangerRed) Icons.Filled.Warning else Icons.Filled.Info,
                    contentDescription = null,
                    tint = color,
                    modifier = Modifier.size(22.dp)
                )
            }
            Spacer(Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = defect.label.replaceFirstChar { it.uppercase() }.replace("_", " "),
                    color = TextPrimary,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 15.sp
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    text = "Confidence: ${"%.1f".format(defect.confidence * 100)}%",
                    color = TextSecondary,
                    fontSize = 13.sp
                )
            }
            // Confidence badge
            Box(
                modifier = Modifier
                    .background(color.copy(alpha = 0.2f), RoundedCornerShape(8.dp))
                    .padding(horizontal = 10.dp, vertical = 4.dp)
            ) {
                Text(
                    "${"%.0f".format(defect.confidence * 100)}%",
                    color = color,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp
                )
            }
        }
    }
}

@Composable
private fun NoDefectsCard() {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        colors = CardDefaults.cardColors(containerColor = GoodGreen.copy(alpha = 0.12f)),
        shape = RoundedCornerShape(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(20.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Filled.CheckCircle, null, tint = GoodGreen, modifier = Modifier.size(32.dp))
            Spacer(Modifier.width(14.dp))
            Column {
                Text("No defects detected", color = GoodGreen, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                Text("Tyre appears to be in good condition.", color = TextSecondary, fontSize = 13.sp)
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Permission blocker
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun PermissionBlocker(onRequest: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize().padding(40.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(Icons.Filled.CameraAlt, null, tint = AccentPurple, modifier = Modifier.size(72.dp))
        Spacer(Modifier.height(24.dp))
        Text("Camera permission required", color = TextPrimary, fontSize = 20.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
        Spacer(Modifier.height(10.dp))
        Text("TyreGuard needs access to your camera to scan and analyse tyres.", color = TextSecondary, fontSize = 14.sp, textAlign = TextAlign.Center)
        Spacer(Modifier.height(32.dp))
        Button(
            onClick = onRequest,
            colors = ButtonDefaults.buttonColors(containerColor = AccentPurple),
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.fillMaxWidth().height(52.dp)
        ) {
            Text("Grant Camera Access", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// ImageProxy → Bitmap helper
// ─────────────────────────────────────────────────────────────────────────────

@androidx.annotation.OptIn(androidx.camera.core.ExperimentalGetImage::class)
private fun imageProxyToBitmap(proxy: ImageProxy): Bitmap {
    val buffer = proxy.planes[0].buffer
    val bytes = ByteArray(buffer.remaining())
    buffer.get(bytes)
    return android.graphics.BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
        ?: Bitmap.createBitmap(proxy.width, proxy.height, Bitmap.Config.ARGB_8888)
}
