package org.example.project.components

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.cos
import kotlin.math.sin

// ============ THEME COLORS ============
private val PrimaryViolet = Color(0xFF6200EA)
private val SecondaryPurple = Color(0xFFBB86FC)
private val DeepViolet = Color(0xFF3700B3)
private val GoodGreen = Color(0xFF4CAF50)
private val WarningYellow = Color(0xFFFFC107)
private val DangerRed = Color(0xFFE53935)
private val SoftGray = Color(0xFFF5F5F5)
private val DarkGray = Color(0xFF424242)

// ============ DATA MODELS ============

/**
 * Represents a single tyre's data
 */
data class TyrePressureData(
    val position: TyrePosition,
    val pressure: Float,           // PSI
    val temperature: Int,          // Celsius
    val treadDepth: Float,         // mm
    val status: TyreHealthStatus,
    val defects: List<String> = emptyList()
)

enum class TyrePosition(val label: String, val shortLabel: String) {
    FRONT_LEFT("Front Left", "FL"),
    FRONT_RIGHT("Front Right", "FR"),
    REAR_LEFT("Rear Left", "RL"),
    REAR_RIGHT("Rear Right", "RR")
}

enum class TyreHealthStatus(val label: String, val color: Color) {
    GOOD("Good", GoodGreen),
    WARNING("Attention", WarningYellow),
    CRITICAL("Critical", DangerRed)
}

/**
 * Car body types for different vehicle shapes
 */
enum class CarBodyType {
    SEDAN,
    SUV,
    HATCHBACK,
    COUPE,
    PICKUP
}

/**
 * Premium Tyre Pressure Overview Card
 * Shows a top-down view of the car with tyre pressures at each corner
 * Like the Rivian app design
 */
@Composable
fun TyrePressureOverviewCard(
    modifier: Modifier = Modifier,
    tyreData: List<TyrePressureData>,
    carBodyType: CarBodyType = CarBodyType.SEDAN,
    lastReading: String = "Just now",
    onTyreClick: (TyrePressureData) -> Unit = {}
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Tire pressure",
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.Bold
                        )
                    )
                    Text(
                        text = "Last reading: $lastReading",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.Gray
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(24.dp))
            
            // Car with tyre pressures
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(1.2f),
                contentAlignment = Alignment.Center
            ) {
                // Tyre positions around the car
                val frontLeft = tyreData.find { it.position == TyrePosition.FRONT_LEFT }
                val frontRight = tyreData.find { it.position == TyrePosition.FRONT_RIGHT }
                val rearLeft = tyreData.find { it.position == TyrePosition.REAR_LEFT }
                val rearRight = tyreData.find { it.position == TyrePosition.REAR_RIGHT }
                
                // Front Left Tyre (top-left)
                frontLeft?.let { tyre ->
                    TyrePressureChip(
                        tyre = tyre,
                        onClick = { onTyreClick(tyre) },
                        modifier = Modifier.align(Alignment.TopStart)
                    )
                }
                
                // Front Right Tyre (top-right)
                frontRight?.let { tyre ->
                    TyrePressureChip(
                        tyre = tyre,
                        onClick = { onTyreClick(tyre) },
                        modifier = Modifier.align(Alignment.TopEnd)
                    )
                }
                
                // Car body in center
                CarTopView(
                    bodyType = carBodyType,
                    modifier = Modifier
                        .fillMaxHeight(0.8f)
                        .aspectRatio(0.45f)
                )
                
                // Rear Left Tyre (bottom-left)
                rearLeft?.let { tyre ->
                    TyrePressureChip(
                        tyre = tyre,
                        onClick = { onTyreClick(tyre) },
                        modifier = Modifier.align(Alignment.BottomStart)
                    )
                }
                
                // Rear Right Tyre (bottom-right)
                rearRight?.let { tyre ->
                    TyrePressureChip(
                        tyre = tyre,
                        onClick = { onTyreClick(tyre) },
                        modifier = Modifier.align(Alignment.BottomEnd)
                    )
                }
            }
        }
    }
}

/**
 * Tyre pressure chip showing PSI value
 */
@Composable
private fun TyrePressureChip(
    tyre: TyrePressureData,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val borderColor = when (tyre.status) {
        TyreHealthStatus.GOOD -> Color.Transparent
        TyreHealthStatus.WARNING -> WarningYellow
        TyreHealthStatus.CRITICAL -> DangerRed
    }
    
    // Pulse animation for critical status
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(800),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse_alpha"
    )
    
    val displayBorderColor = if (tyre.status == TyreHealthStatus.CRITICAL) {
        borderColor.copy(alpha = pulseAlpha)
    } else {
        borderColor
    }
    
    Surface(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .clickable { onClick() }
            .then(
                if (borderColor != Color.Transparent) {
                    Modifier.border(2.dp, displayBorderColor, RoundedCornerShape(12.dp))
                } else Modifier
            ),
        shape = RoundedCornerShape(12.dp),
        color = SoftGray,
        tonalElevation = 2.dp
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.Bottom
        ) {
            Text(
                text = "${tyre.pressure.toInt()}",
                style = MaterialTheme.typography.headlineSmall.copy(
                    fontWeight = FontWeight.Bold
                ),
                color = Color.Black
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = "psi",
                style = MaterialTheme.typography.bodyMedium,
                color = Color.Gray,
                modifier = Modifier.padding(bottom = 2.dp)
            )
        }
    }
}

/**
 * Top-down car body view
 */
@Composable
private fun CarTopView(
    bodyType: CarBodyType,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier) {
        val width = size.width
        val height = size.height
        val strokeWidth = 3.dp.toPx()
        
        val carColor = Color(0xFF424242)
        val wheelColor = Color(0xFF616161)
        val windowColor = Color(0xFFE0E0E0)
        
        // Wheel dimensions
        val wheelWidth = width * 0.18f
        val wheelHeight = height * 0.12f
        val wheelCornerRadius = CornerRadius(8.dp.toPx())
        
        // Draw wheels
        // Front Left
        drawRoundRect(
            color = wheelColor,
            topLeft = Offset(-wheelWidth * 0.5f, height * 0.12f),
            size = Size(wheelWidth, wheelHeight),
            cornerRadius = wheelCornerRadius
        )
        
        // Front Right
        drawRoundRect(
            color = wheelColor,
            topLeft = Offset(width - wheelWidth * 0.5f, height * 0.12f),
            size = Size(wheelWidth, wheelHeight),
            cornerRadius = wheelCornerRadius
        )
        
        // Rear Left
        drawRoundRect(
            color = wheelColor,
            topLeft = Offset(-wheelWidth * 0.5f, height * 0.76f),
            size = Size(wheelWidth, wheelHeight),
            cornerRadius = wheelCornerRadius
        )
        
        // Rear Right
        drawRoundRect(
            color = wheelColor,
            topLeft = Offset(width - wheelWidth * 0.5f, height * 0.76f),
            size = Size(wheelWidth, wheelHeight),
            cornerRadius = wheelCornerRadius
        )
        
        // Car body outline based on type
        val bodyPath = Path().apply {
            val bodyPadding = width * 0.1f
            val bodyWidth = width - bodyPadding * 2
            val topRadius = when (bodyType) {
                CarBodyType.SUV -> height * 0.08f
                CarBodyType.HATCHBACK -> height * 0.1f
                CarBodyType.PICKUP -> height * 0.06f
                else -> height * 0.12f
            }
            
            // Start from top-left
            moveTo(bodyPadding + topRadius, height * 0.05f)
            
            // Top edge with curve at front
            lineTo(width - bodyPadding - topRadius, height * 0.05f)
            quadraticTo(
                width - bodyPadding, height * 0.05f,
                width - bodyPadding, height * 0.05f + topRadius
            )
            
            // Right side
            lineTo(width - bodyPadding, height * 0.95f - topRadius)
            quadraticTo(
                width - bodyPadding, height * 0.95f,
                width - bodyPadding - topRadius, height * 0.95f
            )
            
            // Bottom edge
            lineTo(bodyPadding + topRadius, height * 0.95f)
            quadraticTo(
                bodyPadding, height * 0.95f,
                bodyPadding, height * 0.95f - topRadius
            )
            
            // Left side
            lineTo(bodyPadding, height * 0.05f + topRadius)
            quadraticTo(
                bodyPadding, height * 0.05f,
                bodyPadding + topRadius, height * 0.05f
            )
            
            close()
        }
        
        // Fill car body
        drawPath(
            path = bodyPath,
            color = carColor
        )
        
        // Draw body outline
        drawPath(
            path = bodyPath,
            color = Color.Black.copy(alpha = 0.3f),
            style = Stroke(width = strokeWidth)
        )
        
        // Windshield (front window)
        val windshieldPath = Path().apply {
            val wPadding = width * 0.18f
            moveTo(wPadding, height * 0.15f)
            lineTo(width - wPadding, height * 0.15f)
            lineTo(width - wPadding - width * 0.05f, height * 0.28f)
            lineTo(wPadding + width * 0.05f, height * 0.28f)
            close()
        }
        drawPath(path = windshieldPath, color = windowColor)
        
        // Rear window
        val rearWindowPath = Path().apply {
            val wPadding = width * 0.18f
            moveTo(wPadding + width * 0.05f, height * 0.72f)
            lineTo(width - wPadding - width * 0.05f, height * 0.72f)
            lineTo(width - wPadding, height * 0.85f)
            lineTo(wPadding, height * 0.85f)
            close()
        }
        drawPath(path = rearWindowPath, color = windowColor)
        
        // Side mirrors
        val mirrorSize = Size(width * 0.06f, height * 0.03f)
        drawRoundRect(
            color = carColor,
            topLeft = Offset(-mirrorSize.width * 0.3f, height * 0.22f),
            size = mirrorSize,
            cornerRadius = CornerRadius(4.dp.toPx())
        )
        drawRoundRect(
            color = carColor,
            topLeft = Offset(width - mirrorSize.width * 0.7f, height * 0.22f),
            size = mirrorSize,
            cornerRadius = CornerRadius(4.dp.toPx())
        )
    }
}

/**
 * Detailed Tyre Health Bottom Sheet Content
 * Shows all tyre metrics with animated transitions
 */
@Composable
fun TyreDetailBottomSheet(
    tyre: TyrePressureData,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(24.dp)
    ) {
        // Handle bar
        Box(
            modifier = Modifier
                .align(Alignment.CenterHorizontally)
                .width(40.dp)
                .height(4.dp)
                .clip(RoundedCornerShape(2.dp))
                .background(Color.LightGray)
        )
        
        Spacer(modifier = Modifier.height(24.dp))
        
        // Title with position
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = tyre.position.label,
                    style = MaterialTheme.typography.headlineSmall.copy(
                        fontWeight = FontWeight.Bold
                    )
                )
                Text(
                    text = "Tyre Health Details",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.Gray
                )
            }
            
            // Status badge
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = tyre.status.color.copy(alpha = 0.15f)
            ) {
                Text(
                    text = tyre.status.label,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                    style = MaterialTheme.typography.labelLarge,
                    color = tyre.status.color,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
        
        Spacer(modifier = Modifier.height(32.dp))
        
        // Metrics grid with animations
        var visible by remember { mutableStateOf(false) }
        LaunchedEffect(Unit) { visible = true }
        
        Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
            // Row 1: Pressure & Temperature
            AnimatedVisibility(
                visible = visible,
                enter = fadeIn(tween(300)) + slideInVertically(tween(300))
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    MetricCard(
                        title = "Pressure",
                        value = "${tyre.pressure.toInt()}",
                        unit = "PSI",
                        icon = "⬆",
                        progress = tyre.pressure / 50f,
                        color = tyre.status.color,
                        modifier = Modifier.weight(1f)
                    )
                    MetricCard(
                        title = "Temperature",
                        value = "${tyre.temperature}",
                        unit = "°C",
                        icon = "🌡",
                        progress = tyre.temperature / 60f,
                        color = if (tyre.temperature > 40) DangerRed else GoodGreen,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
            
            // Row 2: Tread Depth & Status
            AnimatedVisibility(
                visible = visible,
                enter = fadeIn(tween(300, delayMillis = 150)) + slideInVertically(tween(300, delayMillis = 150))
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    MetricCard(
                        title = "Tread Depth",
                        value = String.format("%.1f", tyre.treadDepth),
                        unit = "mm",
                        icon = "📏",
                        progress = tyre.treadDepth / 8f,
                        color = when {
                            tyre.treadDepth >= 4f -> GoodGreen
                            tyre.treadDepth >= 2f -> WarningYellow
                            else -> DangerRed
                        },
                        modifier = Modifier.weight(1f)
                    )
                    MetricCard(
                        title = "Defects",
                        value = "${tyre.defects.size}",
                        unit = "found",
                        icon = "⚠",
                        progress = if (tyre.defects.isEmpty()) 1f else 0.3f,
                        color = if (tyre.defects.isEmpty()) GoodGreen else DangerRed,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
        
        // Defects list (if any)
        if (tyre.defects.isNotEmpty()) {
            Spacer(modifier = Modifier.height(24.dp))
            
            AnimatedVisibility(
                visible = visible,
                enter = fadeIn(tween(300, delayMillis = 300)) + expandVertically(tween(300, delayMillis = 300))
            ) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = DangerRed.copy(alpha = 0.1f)
                    ),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "Detected Issues",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.SemiBold
                            ),
                            color = DangerRed
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        tyre.defects.forEach { defect ->
                            Row(
                                modifier = Modifier.padding(vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("•", color = DangerRed)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = defect,
                                    style = MaterialTheme.typography.bodyMedium
                                )
                            }
                        }
                    }
                }
            }
        }
        
        Spacer(modifier = Modifier.height(32.dp))
        
        // Action button
        Button(
            onClick = onDismiss,
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp),
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = PrimaryViolet
            )
        ) {
            Text(
                text = "Close",
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

/**
 * Metric card with animated progress
 */
@Composable
private fun MetricCard(
    title: String,
    value: String,
    unit: String,
    icon: String,
    progress: Float,
    color: Color,
    modifier: Modifier = Modifier
) {
    val animatedProgress by animateFloatAsState(
        targetValue = progress.coerceIn(0f, 1f),
        animationSpec = tween(1000, easing = FastOutSlowInEasing),
        label = "progress"
    )
    
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = SoftGray)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.Gray
                )
                Text(text = icon, fontSize = 16.sp)
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Row(
                verticalAlignment = Alignment.Bottom
            ) {
                Text(
                    text = value,
                    style = MaterialTheme.typography.headlineMedium.copy(
                        fontWeight = FontWeight.Bold
                    )
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = unit,
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.Gray,
                    modifier = Modifier.padding(bottom = 4.dp)
                )
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            // Progress bar
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .clip(RoundedCornerShape(3.dp))
                    .background(Color.LightGray)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .fillMaxWidth(animatedProgress)
                        .clip(RoundedCornerShape(3.dp))
                        .background(color)
                )
            }
        }
    }
}
