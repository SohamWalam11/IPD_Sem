package org.example.project.analysis

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.min
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

// --- PREMIUM DARK THEME COLORS ---
private val TechBlue = Color(0xFF2979FF)
private val GoodGreen = Color(0xFF00E676)
private val AlertOrange = Color(0xFFFFAB00)
private val DarkBg = Color(0xFF121212)
private val CardSurface = Color(0xFF1E1E1E)
private val SurfaceLight = Color(0xFF2A2A2A)
private val TextPrimary = Color(0xFFFFFFFF)
private val TextSecondary = Color(0xFFB0B0B0)

/**
 * Premium Tyre Detail Screen with "Digital Twin" UX
 * Features:
 * - Holographic rotating wireframe tyre visualization
 * - Liquid wave animation for tread depth
 * - Sparkline graphs for metrics context
 * - Smart action cards for AI insights
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TyreDetailScreen(
    tireStatus: TireStatus,
    onBack: () -> Unit = {},
    onScheduleService: () -> Unit = {}
) {
    val isGood = !tireStatus.isCritical && tireStatus.treadHealth != TreadHealth.WORN
    val healthColor = if (isGood) GoodGreen else AlertOrange
    val scrollState = rememberScrollState()
    
    // Calculate tread percentage from health
    val treadPercent = when (tireStatus.treadHealth) {
        TreadHealth.EXCELLENT -> 0.95f
        TreadHealth.GOOD -> 0.75f
        TreadHealth.FAIR -> 0.55f
        TreadHealth.WORN -> 0.30f
        TreadHealth.CRITICAL -> 0.10f
    }

    Scaffold(
        containerColor = DarkBg,
        topBar = {
            TopAppBar(
                title = { 
                    Text(
                        "${tireStatus.position.displayName} Tyre",
                        color = TextPrimary, 
                        fontWeight = FontWeight.Bold
                    ) 
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack, 
                            "Back", 
                            tint = TextPrimary
                        )
                    }
                },
                actions = {
                    IconButton(onClick = {}) {
                        Icon(Icons.Default.History, "History", tint = TextSecondary)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = DarkBg)
            )
        }
    ) { innerPadding ->
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            val screenWidth = maxWidth
            val isCompactScreen = screenWidth < 360.dp
            val horizontalPadding = if (isCompactScreen) 12.dp else 20.dp
            
            // Calculate responsive tyre size (40-50% of screen width, max 220dp)
            val tyreSizeCalc = (screenWidth.value * 0.5f).dp
            val tyreSize = min(tyreSizeCalc, 220.dp)
            val glowSize = (tyreSize.value * 0.9f).dp
            
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(scrollState)
                    .padding(horizontal = horizontalPadding, vertical = 16.dp)
            ) {
                // 1. HOLOGRAPHIC TYRE HERO SECTION
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    // Background Glow Effect
                    Box(
                        modifier = Modifier
                            .size(glowSize)
                            .background(
                                brush = Brush.radialGradient(
                                    colors = listOf(healthColor.copy(alpha = 0.2f), Color.Transparent)
                                )
                            )
                    )
                    
                    // The Scanner Animation - responsive size
                    HolographicTyre(
                        color = healthColor,
                        tyreSize = tyreSize
                    )
                    
                    // Status Badge
                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .offset(y = 8.dp)
                            .background(healthColor.copy(alpha = 0.2f), RoundedCornerShape(50))
                            .border(1.dp, healthColor, RoundedCornerShape(50))
                            .padding(horizontal = 12.dp, vertical = 4.dp)
                    ) {
                        Text(
                            if (isGood) "OPTIMAL" else "ATTENTION",
                            color = healthColor,
                            fontWeight = FontWeight.Bold,
                            fontSize = if (isCompactScreen) 10.sp else 12.sp
                        )
                    }
                }

                Spacer(modifier = Modifier.height(if (isCompactScreen) 24.dp else 32.dp))

                // 2. SENSOR METRICS (With Sparklines)
                Text(
                    "Real-time Telemetry", 
                    color = TextSecondary, 
                    fontSize = if (isCompactScreen) 12.sp else 14.sp,
                    fontWeight = FontWeight.Medium
                )
                Spacer(modifier = Modifier.height(10.dp))
                
                Row(
                    modifier = Modifier.fillMaxWidth(), 
                    horizontalArrangement = Arrangement.spacedBy(if (isCompactScreen) 8.dp else 12.dp)
                ) {
                    // Pressure Card with Sparkline
                    MetricCard(
                        modifier = Modifier.weight(1f),
                        title = "Pressure",
                        value = "%.1f".format(tireStatus.pressurePsi),
                        unit = "PSI",
                        color = TechBlue,
                        graphData = listOf(32f, 32.1f, 32.2f, 31.9f, tireStatus.pressurePsi),
                        isCompact = isCompactScreen
                    )
                    // Temperature Card with Sparkline
                    MetricCard(
                        modifier = Modifier.weight(1f),
                        title = "Temp",
                        value = "%.0f".format(tireStatus.temperatureCelsius),
                        unit = "°C",
                        color = AlertOrange,
                        graphData = listOf(24f, 25f, 26f, 27f, tireStatus.temperatureCelsius),
                        isCompact = isCompactScreen
                    )
                }

                Spacer(modifier = Modifier.height(if (isCompactScreen) 16.dp else 20.dp))

                // 3. LIQUID TREAD DEPTH VISUALIZATION
                Text(
                    "Tread Wear Analysis", 
                    color = TextSecondary, 
                    fontSize = if (isCompactScreen) 12.sp else 14.sp,
                    fontWeight = FontWeight.Medium
                )
                Spacer(modifier = Modifier.height(10.dp))
                LiquidTreadCard(
                    percentage = treadPercent,
                    treadHealth = tireStatus.treadHealth,
                    isCompact = isCompactScreen
                )

                Spacer(modifier = Modifier.height(if (isCompactScreen) 16.dp else 20.dp))

                // 4. AI RECOMMENDATIONS / SMART ACTION CARDS
                Text(
                    "AI Insights", 
                    color = TextSecondary, 
                    fontSize = if (isCompactScreen) 12.sp else 14.sp,
                    fontWeight = FontWeight.Medium
                )
                Spacer(modifier = Modifier.height(10.dp))
                
                // Generate dynamic recommendations
                val recommendations = buildRecommendations(tireStatus)
                recommendations.forEach { recommendation ->
                    RecommendationItem(
                        text = recommendation.text,
                        icon = recommendation.icon,
                        color = recommendation.color,
                        isCompact = isCompactScreen
                    )
                    Spacer(modifier = Modifier.height(if (isCompactScreen) 6.dp else 8.dp))
                }
                
                // Schedule Service Button for critical tyres
                if (tireStatus.isCritical) {
                    Spacer(modifier = Modifier.height(12.dp))
                    Button(
                        onClick = onScheduleService,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(if (isCompactScreen) 46.dp else 52.dp),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = AlertOrange
                        )
                    ) {
                        Icon(
                            Icons.Rounded.Warning,
                            contentDescription = null,
                            modifier = Modifier.size(if (isCompactScreen) 18.dp else 20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            "Schedule Service Now",
                            fontWeight = FontWeight.Bold,
                            fontSize = if (isCompactScreen) 13.sp else 15.sp
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }
}

// --- PREMIUM COMPONENTS ---

/**
 * Holographic Tyre Visualization with rotating wireframe and scanner animation
 * @param tyreSize Responsive size based on screen width
 */
@Composable
private fun HolographicTyre(
    color: Color,
    tyreSize: Dp = 220.dp
) {
    val infiniteTransition = rememberInfiniteTransition(label = "tyre_animation")
    
    // Slow rotation for the wireframe spokes
    val rotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(8000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "rotation"
    )
    
    // Scanner line moving up and down - normalized to 0-1 range
    val scanLineProgress by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = LinearEasing), 
            repeatMode = RepeatMode.Reverse
        ),
        label = "scanner"
    )
    
    // Pulse effect for the glow
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 0.6f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse"
    )

    Box(
        modifier = Modifier.size(tyreSize), 
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val center = Offset(size.width / 2, size.height / 2)
            val radius = size.minDimension / 2.2f
            
            // Outer glow ring
            drawCircle(
                color = color.copy(alpha = pulseAlpha * 0.3f),
                radius = radius + 8.dp.toPx(),
                center = center,
                style = Stroke(width = 4.dp.toPx())
            )
            
            // Draw Wireframe Circles
            drawCircle(
                color = color, 
                radius = radius, 
                center = center,
                style = Stroke(width = 2.dp.toPx())
            )
            drawCircle(
                color = color.copy(alpha = 0.5f), 
                radius = radius * 0.7f, 
                center = center,
                style = Stroke(width = 1.5.dp.toPx())
            )
            drawCircle(
                color = color.copy(alpha = 0.3f), 
                radius = radius * 0.4f, 
                center = center,
                style = Stroke(width = 1.dp.toPx())
            )

            // Draw Rotating Spokes/Segments
            rotate(rotation, center) {
                for (i in 0 until 12) {
                    val angle = (i * 30.0) * (PI / 180)
                    val start = Offset(
                        x = (center.x + (radius * 0.4f) * cos(angle)).toFloat(),
                        y = (center.y + (radius * 0.4f) * sin(angle)).toFloat()
                    )
                    val end = Offset(
                        x = (center.x + radius * cos(angle)).toFloat(),
                        y = (center.y + radius * sin(angle)).toFloat()
                    )
                    drawLine(
                        color = color.copy(alpha = 0.6f),
                        start = start,
                        end = end,
                        strokeWidth = 2.dp.toPx(),
                        cap = StrokeCap.Round
                    )
                }
            }
            
            // Draw "Scanner" Line overlay
            val scanY = scanLineProgress * size.height
            drawLine(
                brush = Brush.linearGradient(
                    colors = listOf(Color.Transparent, Color.White.copy(alpha = 0.8f), Color.Transparent),
                    start = Offset(0f, scanY),
                    end = Offset(size.width, scanY)
                ),
                start = Offset(center.x - radius, scanY),
                end = Offset(center.x + radius, scanY),
                strokeWidth = 2.dp.toPx()
            )
            
            // Center hub glow
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(color.copy(alpha = 0.4f), Color.Transparent),
                    center = center,
                    radius = radius * 0.3f
                ),
                radius = radius * 0.3f,
                center = center
            )
        }
    }
}

/**
 * Metric Card with Sparkline graph background
 * @param isCompact Use compact sizing for small screens
 */
@Composable
private fun MetricCard(
    modifier: Modifier,
    title: String,
    value: String,
    unit: String,
    color: Color,
    graphData: List<Float>,
    isCompact: Boolean = false
) {
    val cardHeight = if (isCompact) 115.dp else 130.dp
    val valueFontSize = if (isCompact) 26.sp else 30.sp
    val unitFontSize = if (isCompact) 13.sp else 15.sp
    val padding = if (isCompact) 12.dp else 14.dp
    
    Card(
        modifier = modifier.height(cardHeight),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = CardSurface)
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            // Sparkline Graph Background
            Canvas(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(top = if (isCompact) 35.dp else 40.dp)
                    .alpha(0.25f)
            ) {
                if (graphData.isNotEmpty()) {
                    val maxVal = graphData.maxOrNull() ?: 1f
                    val minVal = graphData.minOrNull() ?: 0f
                    val range = (maxVal - minVal).coerceAtLeast(0.1f)
                    val stepX = size.width / (graphData.size - 1).coerceAtLeast(1)
                    
                    val path = Path()
                    graphData.forEachIndexed { index, dataPoint ->
                        val x = index * stepX
                        val y = size.height - ((dataPoint - minVal) / range * size.height)
                        if (index == 0) path.moveTo(x, y) else path.lineTo(x, y)
                    }
                    
                    // Draw the line
                    drawPath(
                        path = path,
                        color = color,
                        style = Stroke(width = 4.dp.toPx(), cap = StrokeCap.Round)
                    )
                    
                    // Fill underneath
                    val fillPath = Path()
                    fillPath.addPath(path)
                    fillPath.lineTo(size.width, size.height)
                    fillPath.lineTo(0f, size.height)
                    fillPath.close()
                    drawPath(
                        path = fillPath,
                        brush = Brush.verticalGradient(listOf(color, Color.Transparent))
                    )
                }
            }

            // Foreground Data
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(if (isCompact) 6.dp else 8.dp)
                            .background(color, CircleShape)
                    )
                    Spacer(modifier = Modifier.width(if (isCompact) 4.dp else 6.dp))
                    Text(
                        title, 
                        color = TextSecondary, 
                        fontSize = if (isCompact) 11.sp else 12.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                
                Row(
                    verticalAlignment = Alignment.Bottom,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        value, 
                        fontSize = valueFontSize, 
                        fontWeight = FontWeight.Bold, 
                        color = TextPrimary,
                        maxLines = 1
                    )
                    Spacer(modifier = Modifier.width(3.dp))
                    Text(
                        unit, 
                        fontSize = unitFontSize, 
                        color = TextSecondary, 
                        modifier = Modifier.padding(bottom = if (isCompact) 4.dp else 5.dp),
                        maxLines = 1
                    )
                }
            }
        }
    }
}

/**
 * Liquid Tread Depth Card with wave animation
 * @param isCompact Use compact sizing for small screens
 */
@Composable
private fun LiquidTreadCard(
    percentage: Float,
    treadHealth: TreadHealth,
    isCompact: Boolean = false
) {
    val infiniteTransition = rememberInfiniteTransition(label = "liquid")
    val waveOffset by infiniteTransition.animateFloat(
        initialValue = 0f, 
        targetValue = 2 * PI.toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "wave"
    )
    
    val healthColor = when (treadHealth) {
        TreadHealth.EXCELLENT, TreadHealth.GOOD -> GoodGreen
        TreadHealth.FAIR -> AlertOrange
        TreadHealth.WORN, TreadHealth.CRITICAL -> Color(0xFFE53935)
    }
    
    val treadDepthMm = (percentage * 8f).coerceIn(0.5f, 8f) // 8mm is new tyre depth
    val cardHeight = if (isCompact) 85.dp else 95.dp
    val liquidWidth = if (isCompact) 80.dp else 90.dp

    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = CardSurface),
        modifier = Modifier
            .fillMaxWidth()
            .height(cardHeight)
    ) {
        Row(
            modifier = Modifier.fillMaxSize(), 
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Liquid Indicator
            Box(
                modifier = Modifier
                    .width(liquidWidth)
                    .fillMaxHeight()
                    .clip(RoundedCornerShape(topStart = 16.dp, bottomStart = 16.dp))
                    .background(SurfaceLight)
            ) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val waterLevel = size.height * (1 - percentage)
                    
                    val path = Path()
                    path.moveTo(0f, waterLevel)
                    
                    // Draw Wave
                    for (x in 0..size.width.toInt()) {
                        val y = waterLevel + 8f * sin((x / 20f) + waveOffset)
                        path.lineTo(x.toFloat(), y)
                    }
                    path.lineTo(size.width, size.height)
                    path.lineTo(0f, size.height)
                    path.close()

                    drawPath(
                        path = path,
                        brush = Brush.verticalGradient(
                            listOf(TechBlue, Color(0xFF1A237E))
                        )
                    )
                }
                
                // Percentage overlay on liquid
                Box(
                    modifier = Modifier.fillMaxSize(), 
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        "${(percentage * 100).toInt()}%", 
                        fontWeight = FontWeight.Bold, 
                        color = TextPrimary,
                        fontSize = if (isCompact) 15.sp else 17.sp
                    )
                }
            }
            
            // Text Info
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = if (isCompact) 12.dp else 14.dp, vertical = if (isCompact) 10.dp else 12.dp)
            ) {
                Text(
                    "Tread Depth", 
                    fontWeight = FontWeight.Bold, 
                    color = TextPrimary,
                    fontSize = if (isCompact) 14.sp else 15.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    "%.1fmm remaining".format(treadDepthMm), 
                    color = TextSecondary, 
                    fontSize = if (isCompact) 11.sp else 12.sp,
                    maxLines = 1
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    treadHealth.displayText, 
                    color = healthColor, 
                    fontSize = if (isCompact) 11.sp else 12.sp, 
                    fontWeight = FontWeight.Bold,
                    maxLines = 1
                )
            }
        }
    }
}

/**
 * AI Recommendation Item - Smart Action Card
 * @param isCompact Use compact sizing for small screens
 */
@Composable
private fun RecommendationItem(
    text: String, 
    icon: ImageVector, 
    color: Color,
    isCompact: Boolean = false
) {
    val iconBoxSize = if (isCompact) 24.dp else 28.dp
    val iconSize = if (isCompact) 14.dp else 16.dp
    val padding = if (isCompact) 10.dp else 12.dp
    
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(CardSurface, RoundedCornerShape(10.dp))
            .border(
                width = 1.dp,
                color = color.copy(alpha = 0.3f),
                shape = RoundedCornerShape(10.dp)
            )
            .padding(padding),
        verticalAlignment = Alignment.Top
    ) {
        Box(
            modifier = Modifier
                .size(iconBoxSize)
                .background(color.copy(alpha = 0.15f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                icon, 
                contentDescription = null, 
                tint = color, 
                modifier = Modifier.size(iconSize)
            )
        }
        Spacer(modifier = Modifier.width(if (isCompact) 8.dp else 10.dp))
        Text(
            text = text, 
            color = Color(0xFFEEEEEE), 
            fontSize = if (isCompact) 12.sp else 13.sp, 
            lineHeight = if (isCompact) 17.sp else 19.sp,
            modifier = Modifier.weight(1f)
        )
    }
}

/**
 * Data class for recommendations
 */
private data class Recommendation(
    val text: String,
    val icon: ImageVector,
    val color: Color
)

/**
 * Build dynamic recommendations based on tire status
 */
private fun buildRecommendations(tireStatus: TireStatus): List<Recommendation> {
    return buildList {
        // Pressure recommendations
        when {
            tireStatus.pressurePsi < 28f -> add(
                Recommendation(
                    "Low pressure detected! Inflate to recommended 32-35 PSI immediately.",
                    Icons.Rounded.Warning,
                    AlertOrange
                )
            )
            tireStatus.pressurePsi > 38f -> add(
                Recommendation(
                    "High pressure warning! Release air to reach optimal 32-35 PSI.",
                    Icons.Rounded.Warning,
                    AlertOrange
                )
            )
            else -> add(
                Recommendation(
                    "Pressure is stable. Next check recommended in 15 days.",
                    Icons.Rounded.CheckCircle,
                    GoodGreen
                )
            )
        }
        
        // Temperature recommendations
        if (tireStatus.temperatureCelsius > 40f) {
            add(
                Recommendation(
                    "Temperature elevated. Allow tyres to cool before extended driving.",
                    Icons.Rounded.Warning,
                    AlertOrange
                )
            )
        }
        
        // Tread recommendations
        when (tireStatus.treadHealth) {
            TreadHealth.WORN, TreadHealth.CRITICAL -> add(
                Recommendation(
                    "Tread wear critical. Schedule replacement immediately for safety.",
                    Icons.Rounded.Warning,
                    Color(0xFFE53935)
                )
            )
            TreadHealth.FAIR -> add(
                Recommendation(
                    "Slight uneven wear detected on outer rim. Consider wheel alignment.",
                    Icons.Rounded.Warning,
                    AlertOrange
                )
            )
            else -> {}
        }
        
        // Defect recommendations
        if (tireStatus.defects.isNotEmpty()) {
            add(
                Recommendation(
                    "Defects detected: ${tireStatus.defects.joinToString(", ")}. Professional inspection required.",
                    Icons.Rounded.Warning,
                    Color(0xFFE53935)
                )
            )
        }
        
        // General positive recommendation if all is well
        if (size <= 1 && !tireStatus.isCritical) {
            add(
                Recommendation(
                    "All parameters within optimal range. Continue regular monthly checks.",
                    Icons.Rounded.CheckCircle,
                    GoodGreen
                )
            )
        }
    }
}
