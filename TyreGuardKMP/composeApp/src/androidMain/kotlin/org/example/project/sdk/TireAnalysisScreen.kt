package org.example.project.sdk

import android.graphics.Bitmap
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel

/**
 * Comprehensive Tire Analysis Screen
 *
 * Provides a complete tire health assessment using:
 * - Anyline Tire Tread SDK for depth measurement
 * - Michelin API for tire size/DOT recognition
 * - Custom TFLite model for defect detection
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TireAnalysisScreen(
    onNavigateBack: () -> Unit,
    onCaptureImage: (ImageType, (Bitmap) -> Unit) -> Unit,
    viewModel: TireAnalysisViewModel = viewModel()
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsState()

    // Initialize service
    LaunchedEffect(Unit) {
        viewModel.initialize(context)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Tire Health Analysis") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
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
                uiState.isInitializing -> {
                    InitializingView()
                }
                uiState.analysisResult != null -> {
                    AnalysisResultView(
                        result = uiState.analysisResult!!,
                        onNewAnalysis = { viewModel.clearImages() }
                    )
                }
                uiState.isAnalyzing -> {
                    AnalyzingView(
                        progress = uiState.analysisProgress,
                        step = uiState.analysisStep
                    )
                }
                else -> {
                    CaptureView(
                        uiState = uiState,
                        onCaptureTread = {
                            onCaptureImage(ImageType.TREAD) { bitmap ->
                                viewModel.setTreadImage(bitmap)
                            }
                        },
                        onCaptureSidewall = {
                            onCaptureImage(ImageType.SIDEWALL) { bitmap ->
                                viewModel.setSidewallImage(bitmap)
                            }
                        },
                        onStartAnalysis = { viewModel.startAnalysis() },
                        onQuickScan = { viewModel.quickScan() }
                    )
                }
            }

            // Error snackbar
            uiState.error?.let { error ->
                Snackbar(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(16.dp),
                    action = {
                        TextButton(onClick = { viewModel.clearError() }) {
                            Text("Dismiss")
                        }
                    }
                ) {
                    Text(error)
                }
            }
        }
    }
}

enum class ImageType {
    TREAD, SIDEWALL
}

@Composable
private fun InitializingView() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            CircularProgressIndicator(
                modifier = Modifier.size(64.dp),
                strokeWidth = 4.dp
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                "Initializing Analysis Engine...",
                style = MaterialTheme.typography.bodyLarge
            )
        }
    }
}

@Composable
private fun CaptureView(
    uiState: TireAnalysisUiState,
    onCaptureTread: () -> Unit,
    onCaptureSidewall: () -> Unit,
    onStartAnalysis: () -> Unit,
    onQuickScan: () -> Unit
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Header
        item {
            Text(
                "Capture Tire Images",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
            Text(
                "Take photos of your tire for comprehensive health analysis",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        // Tread Image Card
        item {
            CaptureCard(
                title = "Tread Image",
                description = "Required - Photo of tire tread surface for depth measurement and defect detection",
                icon = Icons.Default.GridOn,
                isCaptured = uiState.hasTreadImage,
                isRequired = true,
                onCapture = onCaptureTread
            )
        }

        // Sidewall Image Card
        item {
            CaptureCard(
                title = "Sidewall Image",
                description = "Optional - Photo of tire sidewall for size and DOT code recognition",
                icon = Icons.Default.Info,
                isCaptured = uiState.hasSidewallImage,
                isRequired = false,
                onCapture = onCaptureSidewall
            )
        }

        // Analysis Options
        item {
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                "Analysis Options",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
        }

        // Full Analysis Button
        item {
            Button(
                onClick = onStartAnalysis,
                enabled = uiState.canStartAnalysis,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary
                )
            ) {
                Icon(Icons.Default.Analytics, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Full Comprehensive Analysis")
            }
        }

        // Quick Scan Button
        item {
            OutlinedButton(
                onClick = onQuickScan,
                enabled = uiState.canStartAnalysis,
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Default.FlashOn, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Quick Defect Scan")
            }
        }

        // Info Section
        item {
            Spacer(modifier = Modifier.height(16.dp))
            InfoCard()
        }
    }
}

@Composable
private fun CaptureCard(
    title: String,
    description: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    isCaptured: Boolean,
    isRequired: Boolean,
    onCapture: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (isCaptured)
                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
            else
                MaterialTheme.colorScheme.surface
        ),
        border = if (isCaptured) null else CardDefaults.outlinedCardBorder()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Icon
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .clip(CircleShape)
                    .background(
                        if (isCaptured)
                            Color(0xFF10B981)
                        else
                            MaterialTheme.colorScheme.primaryContainer
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (isCaptured) Icons.Default.Check else icon,
                    contentDescription = null,
                    tint = if (isCaptured) Color.White else MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier.size(28.dp)
                )
            }

            Spacer(modifier = Modifier.width(16.dp))

            // Text
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    if (isRequired) {
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            "*",
                            color = MaterialTheme.colorScheme.error,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
                Text(
                    description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // Button
            if (!isCaptured) {
                IconButton(onClick = onCapture) {
                    Icon(
                        Icons.Default.CameraAlt,
                        contentDescription = "Capture",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }
    }
}

@Composable
private fun AnalyzingView(
    progress: Float,
    step: String
) {
    val animatedProgress by animateFloatAsState(targetValue = progress, label = "progress")

    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(32.dp)
        ) {
            // Circular Progress
            Box(
                modifier = Modifier.size(160.dp),
                contentAlignment = Alignment.Center
            ) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    // Background circle
                    drawArc(
                        color = Color.LightGray.copy(alpha = 0.3f),
                        startAngle = -90f,
                        sweepAngle = 360f,
                        useCenter = false,
                        style = Stroke(width = 12.dp.toPx(), cap = StrokeCap.Round)
                    )
                    // Progress arc
                    drawArc(
                        color = Color(0xFF3B82F6),
                        startAngle = -90f,
                        sweepAngle = 360f * animatedProgress,
                        useCenter = false,
                        style = Stroke(width = 12.dp.toPx(), cap = StrokeCap.Round)
                    )
                }

                Text(
                    "${(animatedProgress * 100).toInt()}%",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                "Analyzing Tire...",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                step,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun AnalysisResultView(
    result: ComprehensiveTireAnalysis,
    onNewAnalysis: () -> Unit
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Health Score Card
        item {
            HealthScoreCard(
                score = result.overallHealthScore,
                status = result.overallStatus,
                action = result.actionRequired
            )
        }

        // Tread Depth Section
        result.treadDepth?.let { tread ->
            item {
                TreadDepthCard(treadResult = tread)
            }
        }

        // Defects Section
        if (result.detectedDefects.isNotEmpty()) {
            item {
                Text(
                    "Detected Issues",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
            }

            items(result.detectedDefects.filter { it.type != DefectType.GOOD }) { defect ->
                DefectCard(defect = defect)
            }
        }

        // Tire Info Section
        result.tireSizeInfo?.let { size ->
            item {
                TireSizeCard(sizeInfo = size)
            }
        }

        result.dotCodeInfo?.let { dot ->
            item {
                DotCodeCard(dotInfo = dot)
            }
        }

        // Recommendations Section
        if (result.recommendations.isNotEmpty()) {
            item {
                Text(
                    "Recommendations",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
            }

            items(result.recommendations) { recommendation ->
                RecommendationCard(recommendation = recommendation)
            }
        }

        // Cost Estimate
        result.estimatedCostRange?.let { (min, max) ->
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.tertiaryContainer
                    )
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.AttachMoney,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onTertiaryContainer
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                "Estimated Service Cost",
                                style = MaterialTheme.typography.titleSmall
                            )
                            Text(
                                "$$min - $$max USD",
                                style = MaterialTheme.typography.headlineSmall,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }

        // New Analysis Button
        item {
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedButton(
                onClick = onNewAnalysis,
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Default.Refresh, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Start New Analysis")
            }
        }
    }
}

@Composable
private fun HealthScoreCard(
    score: Int,
    status: OverallTireStatus,
    action: ActionRequired
) {
    val scoreColor = Color(status.colorHex)

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = scoreColor.copy(alpha = 0.1f)
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Score Circle
            Box(
                modifier = Modifier
                    .size(120.dp)
                    .border(8.dp, scoreColor, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        "$score",
                        style = MaterialTheme.typography.displayMedium,
                        fontWeight = FontWeight.Bold,
                        color = scoreColor
                    )
                    Text(
                        "/100",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                status.displayName,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = scoreColor
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Action Required Chip
            Surface(
                color = when (action.urgency) {
                    0 -> Color(0xFF10B981)
                    1, 2 -> Color(0xFFF59E0B)
                    else -> Color(0xFFEF4444)
                },
                shape = RoundedCornerShape(16.dp)
            ) {
                Text(
                    action.displayName,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp),
                    color = Color.White,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}

@Composable
private fun TreadDepthCard(treadResult: TreadDepthResult) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Default.Straighten,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    "Tread Depth",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Depth measurements
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                DepthMeasurement("Inner", treadResult.innerDepthMm)
                DepthMeasurement("Center", treadResult.centerDepthMm)
                DepthMeasurement("Outer", treadResult.outerDepthMm)
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Status and wear pattern
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text("Status", style = MaterialTheme.typography.labelSmall)
                    Text(
                        treadResult.status.displayName,
                        color = Color(treadResult.status.colorHex),
                        fontWeight = FontWeight.SemiBold
                    )
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text("Wear Pattern", style = MaterialTheme.typography.labelSmall)
                    Text(
                        treadResult.wearPattern.displayName,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            // Wear percentage bar
            Spacer(modifier = Modifier.height(12.dp))
            Text("Wear: ${treadResult.wearPercentage.toInt()}%", style = MaterialTheme.typography.labelSmall)
            LinearProgressIndicator(
                progress = { treadResult.wearPercentage / 100f },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .clip(RoundedCornerShape(4.dp)),
                color = Color(treadResult.status.colorHex),
                trackColor = Color.LightGray.copy(alpha = 0.3f)
            )
        }
    }
}

@Composable
private fun DepthMeasurement(label: String, depth: Float) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            String.format("%.1f", depth),
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold
        )
        Text(
            "mm",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun DefectCard(defect: TireDefect) {
    val severityColor = Color(defect.severity.colorHex)

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = severityColor.copy(alpha = 0.1f)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(severityColor)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    defect.type.displayName,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    defect.description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    defect.severity.displayName,
                    style = MaterialTheme.typography.labelMedium,
                    color = severityColor,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    "${(defect.confidence * 100).toInt()}%",
                    style = MaterialTheme.typography.labelSmall
                )
            }
        }
    }
}

@Composable
private fun TireSizeCard(sizeInfo: TireSizeInfo) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Default.Info,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    "Tire Size",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                sizeInfo.fullSpecification,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("Max Speed: ${sizeInfo.maxSpeedKmh} km/h")
                Text("Max Load: ${sizeInfo.maxLoadKg} kg")
            }
        }
    }
}

@Composable
private fun DotCodeCard(dotInfo: DotCodeInfo) {
    val ageColor = Color(dotInfo.ageStatus.colorHex)

    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Default.DateRange,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    "Manufacturing Date",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                dotInfo.manufactureDate,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(8.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Age: ${dotInfo.ageInMonths / 12} years ${dotInfo.ageInMonths % 12} months")
                Spacer(modifier = Modifier.width(12.dp))
                Surface(
                    color = ageColor,
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        dotInfo.ageStatus.displayName,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        color = Color.White,
                        style = MaterialTheme.typography.labelSmall
                    )
                }
            }
        }
    }
}

@Composable
private fun RecommendationCard(recommendation: TireRecommendation) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.Top
        ) {
            // Priority indicator
            Box(
                modifier = Modifier
                    .size(24.dp)
                    .clip(CircleShape)
                    .background(
                        when (recommendation.priority) {
                            1 -> Color(0xFFEF4444)
                            2 -> Color(0xFFF97316)
                            3 -> Color(0xFFF59E0B)
                            else -> Color(0xFF22C55E)
                        }
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    "${recommendation.priority}",
                    color = Color.White,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    recommendation.title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    recommendation.description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            recommendation.estimatedCost?.let { cost ->
                if (cost > 0) {
                    Text(
                        "~$$cost",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }
    }
}

@Composable
private fun InfoCard() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Default.Lightbulb,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    "Tips for Best Results",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            val tips = listOf(
                "Ensure good lighting when capturing images",
                "Hold camera steady and perpendicular to tire",
                "For tread: capture the full width of the tread surface",
                "For sidewall: include the full tire size marking and DOT code"
            )

            tips.forEach { tip ->
                Row(modifier = Modifier.padding(vertical = 4.dp)) {
                    Text("•", color = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        tip,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

