package org.example.project.tyre3d.viewer

import android.util.Log
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.sceneview.Scene
import io.github.sceneview.math.Position
import io.github.sceneview.math.Rotation
import io.github.sceneview.math.Scale
import io.github.sceneview.node.ModelNode
import io.github.sceneview.node.Node
import io.github.sceneview.rememberEngine
import io.github.sceneview.rememberModelLoader
import io.github.sceneview.rememberNode
import io.github.sceneview.rememberNodes
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

private const val TAG = "TyreDefectViewer"

// ═══════════════════════════════════════════════════════════════════════════════
// THEME COLORS
// ═══════════════════════════════════════════════════════════════════════════════
private val PrimaryViolet = Color(0xFF7C3AED)
private val SecondaryPurple = Color(0xFF9333EA)
private val DangerRed = Color(0xFFEF4444)
private val WarningOrange = Color(0xFFF59E0B)
private val SuccessGreen = Color(0xFF10B981)
private val InfoBlue = Color(0xFF3B82F6)

// ═══════════════════════════════════════════════════════════════════════════════
// DEFECT DATA CLASSES
// ═══════════════════════════════════════════════════════════════════════════════

/**
 * Represents a defect or affected area on the tyre
 */
data class TyreDefect(
    val id: String,
    val type: DefectType,
    val severity: DefectSeverity,
    val position: DefectPosition,  // 3D position on tyre model
    val description: String,
    val recommendation: String
)

enum class DefectType(
    val displayName: String,
    val icon: ImageVector,
    val color: Color
) {
    CRACK("Crack", Icons.Default.BrokenImage, DangerRed),
    BULGE("Bulge", Icons.Default.Circle, DangerRed),
    WEAR("Tread Wear", Icons.Default.DoNotDisturbOn, WarningOrange),
    PUNCTURE("Puncture", Icons.Default.PushPin, DangerRed),
    CUT("Cut/Slash", Icons.Default.ContentCut, DangerRed),
    UNEVEN_WEAR("Uneven Wear", Icons.Default.SignalWifi4BarLock, WarningOrange),
    LOW_TREAD("Low Tread Depth", Icons.Default.Layers, WarningOrange),
    SIDEWALL_DAMAGE("Sidewall Damage", Icons.Default.Flare, DangerRed),
    FOREIGN_OBJECT("Foreign Object", Icons.Default.ReportProblem, InfoBlue),
    AGING("Age Cracking", Icons.Default.Timer, WarningOrange)
}

enum class DefectSeverity(
    val displayName: String,
    val color: Color,
    val priority: Int
) {
    CRITICAL("Critical", DangerRed, 1),
    HIGH("High", Color(0xFFDC2626), 2),
    MEDIUM("Medium", WarningOrange, 3),
    LOW("Low", InfoBlue, 4),
    INFO("Info", Color.Gray, 5)
}

/**
 * 3D position on the tyre model (normalized coordinates)
 * x: -1 to 1 (left to right)
 * y: -1 to 1 (bottom to top)
 * z: -1 to 1 (front to back)
 */
data class DefectPosition(
    val x: Float,
    val y: Float,
    val z: Float,
    val normalX: Float = 0f,  // Surface normal for marker orientation
    val normalY: Float = 1f,
    val normalZ: Float = 0f
)

// ═══════════════════════════════════════════════════════════════════════════════
// TYRE ANALYSIS DATA
// ═══════════════════════════════════════════════════════════════════════════════
data class TyreAnalysisResult(
    val overallScore: Int,  // 0-100
    val overallStatus: TyreHealthStatus,
    val pressure: Float,
    val temperature: Float,
    val treadDepth: Float,
    val defects: List<TyreDefect>,
    val analysisDate: String = "Just now"
)

enum class TyreHealthStatus(
    val displayName: String,
    val color: Color,
    val icon: ImageVector
) {
    EXCELLENT("Excellent", SuccessGreen, Icons.Default.CheckCircle),
    GOOD("Good", Color(0xFF22C55E), Icons.Default.Check),
    FAIR("Fair", WarningOrange, Icons.Default.Warning),
    POOR("Poor", Color(0xFFF97316), Icons.Default.ReportProblem),
    CRITICAL("Critical", DangerRed, Icons.Default.Error)
}

// ═══════════════════════════════════════════════════════════════════════════════
// MAIN COMPOSABLE - TYRE DEFECT 3D VIEWER
// ═══════════════════════════════════════════════════════════════════════════════

/**
 * Advanced 3D Tyre Viewer with Defect Visualization
 * 
 * Features:
 * - Renders 3D tyre model using SceneView (Filament engine)
 * - Shows defect markers at specific positions on the tyre
 * - Tap on markers to view defect details
 * - Zoom and rotate to inspect tyre from all angles
 * - Side panel shows all defects with severity colors
 * - AR mode available for real-world overlay
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TyreDefectViewer(
    modelPath: String,
    analysisResult: TyreAnalysisResult,
    modifier: Modifier = Modifier,
    onBackClick: () -> Unit = {},
    onArModeClick: () -> Unit = {},
    onDefectClick: (TyreDefect) -> Unit = {}
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    
    // UI State
    var isLoading by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var selectedDefect by remember { mutableStateOf<TyreDefect?>(null) }
    var showDefectPanel by remember { mutableStateOf(true) }
    var isAutoRotating by remember { mutableStateOf(false) }
    var showDefectMarkers by remember { mutableStateOf(true) }
    
    // Camera controls
    var rotationX by remember { mutableFloatStateOf(15f) }
    var rotationY by remember { mutableFloatStateOf(0f) }
    var zoom by remember { mutableFloatStateOf(1f) }
    
    // SceneView components
    val engine = rememberEngine()
    val modelLoader = rememberModelLoader(engine)
    
    // Model node
    var tyreModelNode by remember { mutableStateOf<ModelNode?>(null) }
    
    // Defect marker nodes (visual indicators on the 3D model)
    val defectMarkerNodes = remember { mutableStateListOf<Node>() }
    
    // Load model
    LaunchedEffect(modelPath) {
        isLoading = true
        errorMessage = null
        
        scope.launch {
            try {
                val loadedNode = withContext(Dispatchers.IO) {
                    val modelInstance = modelLoader.loadModelInstance(modelPath)
                    
                    modelInstance?.let {
                        ModelNode(
                            modelInstance = it,
                            autoAnimate = true,
                            scaleToUnits = 2.0f
                        ).apply {
                            position = Position(0f, 0f, 0f)
                        }
                    }
                }
                
                if (loadedNode != null) {
                    tyreModelNode = loadedNode
                    isLoading = false
                    Log.d(TAG, "Tyre model loaded successfully")
                } else {
                    throw Exception("Failed to load tyre model")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error loading model: ${e.message}", e)
                isLoading = false
                errorMessage = e.message ?: "Failed to load model"
            }
        }
    }
    
    // Auto-rotation effect
    LaunchedEffect(isAutoRotating) {
        while (isAutoRotating) {
            rotationY += 0.5f
            delay(16)
        }
    }
    
    // Apply transformations
    LaunchedEffect(rotationX, rotationY, zoom) {
        tyreModelNode?.let { node ->
            node.rotation = Rotation(x = rotationX, y = rotationY, z = 0f)
            node.scale = Scale(zoom)
        }
    }
    
    // Animated marker pulse
    val pulseAnimation = rememberInfiniteTransition(label = "pulse")
    val pulseScale by pulseAnimation.animateFloat(
        initialValue = 0.8f,
        targetValue = 1.2f,
        animationSpec = infiniteRepeatable(
            animation = tween(800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseScale"
    )
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            "Tyre Analysis",
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            "${analysisResult.defects.size} issues detected",
                            fontSize = 12.sp,
                            color = if (analysisResult.defects.isNotEmpty()) 
                                DangerRed else SuccessGreen
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    // Toggle markers
                    IconButton(onClick = { showDefectMarkers = !showDefectMarkers }) {
                        Icon(
                            if (showDefectMarkers) Icons.Default.Visibility 
                            else Icons.Default.VisibilityOff,
                            contentDescription = "Toggle markers",
                            tint = if (showDefectMarkers) PrimaryViolet else Color.Gray
                        )
                    }
                    // AR Mode
                    IconButton(onClick = onArModeClick) {
                        Icon(
                            Icons.Default.ViewInAr,
                            contentDescription = "AR Mode",
                            tint = PrimaryViolet
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.White
                )
            )
        }
    ) { paddingValues ->
        Box(
            modifier = modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // 3D Scene
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color(0xFF1A1A2E))
            ) {
                Scene(
                    modifier = Modifier.fillMaxSize(),
                    engine = engine,
                    modelLoader = modelLoader,
                    childNodes = listOfNotNull(tyreModelNode),
                    isOpaque = true
                )
                
                // Defect marker overlays (2D overlays positioned on 3D coordinates)
                if (showDefectMarkers && !isLoading) {
                    analysisResult.defects.forEach { defect ->
                        DefectMarkerOverlay(
                            defect = defect,
                            isSelected = selectedDefect?.id == defect.id,
                            pulseScale = pulseScale,
                            onClick = {
                                selectedDefect = defect
                                onDefectClick(defect)
                            },
                            // Position based on rotation and defect position
                            rotationY = rotationY,
                            zoom = zoom
                        )
                    }
                }
            }
            
            // Loading overlay
            AnimatedVisibility(
                visible = isLoading,
                enter = fadeIn(),
                exit = fadeOut()
            ) {
                LoadingOverlay()
            }
            
            // Error overlay
            errorMessage?.let { error ->
                ErrorOverlay(
                    message = error,
                    onRetry = { /* Retry loading */ }
                )
            }
            
            // Health score badge
            HealthScoreBadge(
                score = analysisResult.overallScore,
                status = analysisResult.overallStatus,
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(16.dp)
            )
            
            // Control buttons
            ControlButtons(
                isAutoRotating = isAutoRotating,
                onAutoRotateToggle = { isAutoRotating = !isAutoRotating },
                onZoomIn = { zoom = (zoom * 1.2f).coerceAtMost(3f) },
                onZoomOut = { zoom = (zoom / 1.2f).coerceAtLeast(0.3f) },
                onResetView = {
                    rotationX = 15f
                    rotationY = 0f
                    zoom = 1f
                    isAutoRotating = false
                },
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(16.dp)
            )
            
            // Defects panel toggle
            FloatingActionButton(
                onClick = { showDefectPanel = !showDefectPanel },
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(16.dp),
                containerColor = if (analysisResult.defects.isNotEmpty()) 
                    DangerRed else SuccessGreen
            ) {
                BadgedBox(
                    badge = {
                        if (analysisResult.defects.isNotEmpty()) {
                            Badge(containerColor = Color.White) {
                                Text(
                                    "${analysisResult.defects.size}",
                                    color = DangerRed,
                                    fontSize = 10.sp
                                )
                            }
                        }
                    }
                ) {
                    Icon(
                        Icons.Default.BugReport,
                        contentDescription = "View defects",
                        tint = Color.White
                    )
                }
            }
            
            // Defects panel (bottom sheet style)
            AnimatedVisibility(
                visible = showDefectPanel,
                enter = slideInVertically(initialOffsetY = { it }),
                exit = slideOutVertically(targetOffsetY = { it }),
                modifier = Modifier.align(Alignment.BottomCenter)
            ) {
                DefectsPanel(
                    defects = analysisResult.defects,
                    selectedDefect = selectedDefect,
                    analysisResult = analysisResult,
                    onDefectClick = { defect ->
                        selectedDefect = defect
                        // Rotate to show defect
                        rotationY = calculateRotationToDefect(defect.position)
                    },
                    onClose = { showDefectPanel = false }
                )
            }
            
            // Selected defect detail card
            AnimatedVisibility(
                visible = selectedDefect != null,
                enter = slideInHorizontally(initialOffsetX = { it }) + fadeIn(),
                exit = slideOutHorizontally(targetOffsetX = { it }) + fadeOut(),
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .padding(16.dp)
            ) {
                selectedDefect?.let { defect ->
                    DefectDetailCard(
                        defect = defect,
                        onDismiss = { selectedDefect = null }
                    )
                }
            }
        }
    }
}

// ═══════════════════════════════════════════════════════════════════════════════
// DEFECT MARKER OVERLAY
// ═══════════════════════════════════════════════════════════════════════════════
@Composable
private fun DefectMarkerOverlay(
    defect: TyreDefect,
    isSelected: Boolean,
    pulseScale: Float,
    onClick: () -> Unit,
    rotationY: Float,
    zoom: Float
) {
    // Calculate 2D screen position from 3D position and current rotation
    // This is a simplified projection - in production, use proper 3D to 2D projection
    val angleRad = Math.toRadians(rotationY.toDouble())
    val screenX = (defect.position.x * kotlin.math.cos(angleRad) - 
                   defect.position.z * kotlin.math.sin(angleRad)).toFloat()
    val visibility = (defect.position.x * kotlin.math.sin(angleRad) + 
                      defect.position.z * kotlin.math.cos(angleRad)).toFloat()
    
    // Only show marker if on visible side of tyre
    if (visibility > -0.3f) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(
                    start = (200 + screenX * 100 * zoom).dp,
                    top = (300 - defect.position.y * 100 * zoom).dp
                )
        ) {
            // Pulsing marker
            Box(
                modifier = Modifier
                    .size((24 * if (isSelected) pulseScale else 1f).dp)
                    .shadow(8.dp, CircleShape)
                    .background(
                        color = defect.type.color,
                        shape = CircleShape
                    )
                    .border(
                        width = 2.dp,
                        color = Color.White,
                        shape = CircleShape
                    )
                    .clickable { onClick() },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    defect.type.icon,
                    contentDescription = defect.type.displayName,
                    tint = Color.White,
                    modifier = Modifier.size(14.dp)
                )
            }
            
            // Severity indicator
            Box(
                modifier = Modifier
                    .offset(x = 16.dp, y = (-4).dp)
                    .size(12.dp)
                    .background(defect.severity.color, CircleShape)
                    .border(1.dp, Color.White, CircleShape)
            )
        }
    }
}

// ═══════════════════════════════════════════════════════════════════════════════
// HEALTH SCORE BADGE
// ═══════════════════════════════════════════════════════════════════════════════
@Composable
private fun HealthScoreBadge(
    score: Int,
    status: TyreHealthStatus,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.95f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Score circle
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .background(
                        brush = Brush.linearGradient(
                            colors = listOf(status.color, status.color.copy(alpha = 0.7f))
                        ),
                        shape = CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    "$score",
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp
                )
            }
            
            Column {
                Text(
                    "Health Score",
                    fontSize = 12.sp,
                    color = Color.Gray
                )
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(
                        status.icon,
                        contentDescription = null,
                        tint = status.color,
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        status.displayName,
                        fontWeight = FontWeight.SemiBold,
                        color = status.color
                    )
                }
            }
        }
    }
}

// ═══════════════════════════════════════════════════════════════════════════════
// CONTROL BUTTONS
// ═══════════════════════════════════════════════════════════════════════════════
@Composable
private fun ControlButtons(
    isAutoRotating: Boolean,
    onAutoRotateToggle: () -> Unit,
    onZoomIn: () -> Unit,
    onZoomOut: () -> Unit,
    onResetView: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.95f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
    ) {
        Column(
            modifier = Modifier.padding(8.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            ControlButton(
                icon = Icons.Default.Rotate90DegreesCcw,
                isActive = isAutoRotating,
                onClick = onAutoRotateToggle,
                contentDescription = "Auto rotate"
            )
            ControlButton(
                icon = Icons.Default.ZoomIn,
                onClick = onZoomIn,
                contentDescription = "Zoom in"
            )
            ControlButton(
                icon = Icons.Default.ZoomOut,
                onClick = onZoomOut,
                contentDescription = "Zoom out"
            )
            ControlButton(
                icon = Icons.Default.CenterFocusStrong,
                onClick = onResetView,
                contentDescription = "Reset view"
            )
        }
    }
}

@Composable
private fun ControlButton(
    icon: ImageVector,
    onClick: () -> Unit,
    contentDescription: String,
    isActive: Boolean = false
) {
    FilledIconButton(
        onClick = onClick,
        modifier = Modifier.size(40.dp),
        colors = IconButtonDefaults.filledIconButtonColors(
            containerColor = if (isActive) PrimaryViolet else Color.LightGray.copy(alpha = 0.3f)
        )
    ) {
        Icon(
            icon,
            contentDescription = contentDescription,
            tint = if (isActive) Color.White else Color.DarkGray,
            modifier = Modifier.size(20.dp)
        )
    }
}

// ═══════════════════════════════════════════════════════════════════════════════
// DEFECTS PANEL
// ═══════════════════════════════════════════════════════════════════════════════
@Composable
private fun DefectsPanel(
    defects: List<TyreDefect>,
    selectedDefect: TyreDefect?,
    analysisResult: TyreAnalysisResult,
    onDefectClick: (TyreDefect) -> Unit,
    onClose: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(max = 350.dp),
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 16.dp)
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            // Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        "Detected Issues",
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp
                    )
                    Text(
                        "${defects.size} problems found",
                        fontSize = 13.sp,
                        color = Color.Gray
                    )
                }
                
                // Stats chips
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    StatChip(
                        label = "PSI",
                        value = "%.1f".format(analysisResult.pressure),
                        color = if (analysisResult.pressure in 30f..35f) SuccessGreen else WarningOrange
                    )
                    StatChip(
                        label = "°C",
                        value = "%.0f".format(analysisResult.temperature),
                        color = if (analysisResult.temperature < 40f) SuccessGreen else DangerRed
                    )
                    StatChip(
                        label = "mm",
                        value = "%.1f".format(analysisResult.treadDepth),
                        color = when {
                            analysisResult.treadDepth >= 4f -> SuccessGreen
                            analysisResult.treadDepth >= 1.6f -> WarningOrange
                            else -> DangerRed
                        }
                    )
                }
                
                IconButton(onClick = onClose) {
                    Icon(Icons.Default.Close, contentDescription = "Close")
                }
            }
            
            HorizontalDivider()
            
            // Defects list
            if (defects.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            Icons.Default.CheckCircle,
                            contentDescription = null,
                            tint = SuccessGreen,
                            modifier = Modifier.size(48.dp)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            "No issues detected!",
                            fontWeight = FontWeight.SemiBold,
                            color = SuccessGreen
                        )
                        Text(
                            "Your tyre is in good condition",
                            fontSize = 13.sp,
                            color = Color.Gray
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxWidth(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(defects.sortedBy { it.severity.priority }) { defect ->
                        DefectListItem(
                            defect = defect,
                            isSelected = defect.id == selectedDefect?.id,
                            onClick = { onDefectClick(defect) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun StatChip(
    label: String,
    value: String,
    color: Color
) {
    Surface(
        color = color.copy(alpha = 0.1f),
        shape = RoundedCornerShape(8.dp)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                value,
                fontWeight = FontWeight.Bold,
                fontSize = 13.sp,
                color = color
            )
            Text(
                " $label",
                fontSize = 11.sp,
                color = color.copy(alpha = 0.7f)
            )
        }
    }
}

@Composable
private fun DefectListItem(
    defect: TyreDefect,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) 
                defect.type.color.copy(alpha = 0.1f) 
            else Color.Gray.copy(alpha = 0.05f)
        ),
        border = if (isSelected) 
            androidx.compose.foundation.BorderStroke(2.dp, defect.type.color) 
        else null
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Icon
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .background(defect.type.color.copy(alpha = 0.15f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    defect.type.icon,
                    contentDescription = null,
                    tint = defect.type.color,
                    modifier = Modifier.size(20.dp)
                )
            }
            
            Spacer(modifier = Modifier.width(12.dp))
            
            // Info
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    defect.type.displayName,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 14.sp
                )
                Text(
                    defect.description,
                    fontSize = 12.sp,
                    color = Color.Gray,
                    maxLines = 1
                )
            }
            
            // Severity badge
            Surface(
                color = defect.severity.color.copy(alpha = 0.15f),
                shape = RoundedCornerShape(8.dp)
            ) {
                Text(
                    defect.severity.displayName,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium,
                    color = defect.severity.color
                )
            }
        }
    }
}

// ═══════════════════════════════════════════════════════════════════════════════
// DEFECT DETAIL CARD
// ═══════════════════════════════════════════════════════════════════════════════
@Composable
private fun DefectDetailCard(
    defect: TyreDefect,
    onDismiss: () -> Unit
) {
    Card(
        modifier = Modifier.width(280.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 12.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .background(defect.type.color.copy(alpha = 0.15f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        defect.type.icon,
                        contentDescription = null,
                        tint = defect.type.color,
                        modifier = Modifier.size(24.dp)
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        defect.type.displayName,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp
                    )
                    Surface(
                        color = defect.severity.color.copy(alpha = 0.15f),
                        shape = RoundedCornerShape(4.dp)
                    ) {
                        Text(
                            defect.severity.displayName,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                            fontSize = 11.sp,
                            color = defect.severity.color,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
                IconButton(onClick = onDismiss, modifier = Modifier.size(32.dp)) {
                    Icon(
                        Icons.Default.Close,
                        contentDescription = "Close",
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Description
            Text(
                "Description",
                fontSize = 12.sp,
                color = Color.Gray,
                fontWeight = FontWeight.Medium
            )
            Text(
                defect.description,
                fontSize = 14.sp,
                modifier = Modifier.padding(top = 4.dp)
            )
            
            Spacer(modifier = Modifier.height(12.dp))
            
            // Recommendation
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = InfoBlue.copy(alpha = 0.1f)
                ),
                shape = RoundedCornerShape(8.dp)
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.Top
                ) {
                    Icon(
                        Icons.Default.Lightbulb,
                        contentDescription = null,
                        tint = InfoBlue,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text(
                            "Recommendation",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                            color = InfoBlue
                        )
                        Text(
                            defect.recommendation,
                            fontSize = 13.sp,
                            color = Color.DarkGray,
                            modifier = Modifier.padding(top = 4.dp)
                        )
                    }
                }
            }
        }
    }
}

// ═══════════════════════════════════════════════════════════════════════════════
// LOADING & ERROR OVERLAYS
// ═══════════════════════════════════════════════════════════════════════════════
@Composable
private fun LoadingOverlay() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.8f)),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            CircularProgressIndicator(
                modifier = Modifier.size(56.dp),
                color = PrimaryViolet,
                strokeWidth = 4.dp
            )
            Text(
                "Loading 3D Model...",
                color = Color.White,
                fontWeight = FontWeight.Medium
            )
            Text(
                "Analyzing tyre condition",
                color = Color.White.copy(alpha = 0.6f),
                fontSize = 14.sp
            )
        }
    }
}

@Composable
private fun ErrorOverlay(
    message: String,
    onRetry: () -> Unit
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
                Icons.Default.Error,
                contentDescription = null,
                tint = DangerRed,
                modifier = Modifier.size(56.dp)
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                "Failed to load model",
                color = Color.White,
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp
            )
            Text(
                message,
                color = Color.White.copy(alpha = 0.7f),
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 8.dp)
            )
            Spacer(modifier = Modifier.height(24.dp))
            Button(
                onClick = onRetry,
                colors = ButtonDefaults.buttonColors(containerColor = PrimaryViolet)
            ) {
                Text("Retry")
            }
        }
    }
}

// ═══════════════════════════════════════════════════════════════════════════════
// HELPER FUNCTIONS
// ═══════════════════════════════════════════════════════════════════════════════
private fun calculateRotationToDefect(position: DefectPosition): Float {
    // Calculate Y rotation to face the defect
    val angle = Math.toDegrees(kotlin.math.atan2(position.x.toDouble(), position.z.toDouble()))
    return angle.toFloat()
}

/**
 * Generate sample defects for testing
 */
fun generateSampleDefects(): List<TyreDefect> = listOf(
    TyreDefect(
        id = "1",
        type = DefectType.CRACK,
        severity = DefectSeverity.HIGH,
        position = DefectPosition(0.5f, 0.2f, 0.8f),
        description = "Visible crack on sidewall, approximately 2cm in length",
        recommendation = "Replace tyre immediately. Sidewall cracks can lead to sudden blowouts."
    ),
    TyreDefect(
        id = "2",
        type = DefectType.WEAR,
        severity = DefectSeverity.MEDIUM,
        position = DefectPosition(-0.3f, 0f, 0.9f),
        description = "Uneven tread wear detected on outer edge",
        recommendation = "Check wheel alignment. Rotate tyres and monitor wear pattern."
    ),
    TyreDefect(
        id = "3",
        type = DefectType.LOW_TREAD,
        severity = DefectSeverity.MEDIUM,
        position = DefectPosition(0f, -0.1f, 1f),
        description = "Tread depth at 3.2mm, approaching minimum safe level",
        recommendation = "Plan for tyre replacement within 5,000km or before monsoon season."
    ),
    TyreDefect(
        id = "4",
        type = DefectType.FOREIGN_OBJECT,
        severity = DefectSeverity.LOW,
        position = DefectPosition(-0.6f, 0.1f, 0.7f),
        description = "Small stone embedded in tread groove",
        recommendation = "Remove carefully with a blunt tool. No immediate concern."
    )
)

fun generateSampleAnalysis(): TyreAnalysisResult = TyreAnalysisResult(
    overallScore = 65,
    overallStatus = TyreHealthStatus.FAIR,
    pressure = 31.5f,
    temperature = 28f,
    treadDepth = 4.2f,
    defects = generateSampleDefects()
)
