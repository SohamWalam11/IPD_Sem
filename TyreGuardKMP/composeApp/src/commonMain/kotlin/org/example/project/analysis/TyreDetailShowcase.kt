package org.example.project.analysis

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
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
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import kotlin.math.cos
import kotlin.math.sin

// Colors for tyre status
private val ExcellentColor = Color(0xFF4CAF50)
private val GoodColor = Color(0xFF8BC34A)
private val FairColor = Color(0xFFFFC107)
private val WornColor = Color(0xFFFF9800)
private val CriticalColor = Color(0xFFF44336)

/**
 * 3D Tyre Detail Showcase Screen
 * Shows detailed analysis of a specific tyre with animated 3D-like visualization
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TyreDetailShowcase(
    tireStatus: TireStatus,
    onBackClick: () -> Unit = {},
    onScheduleService: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val healthColor = when (tireStatus.treadHealth) {
        TreadHealth.EXCELLENT -> ExcellentColor
        TreadHealth.GOOD -> GoodColor
        TreadHealth.FAIR -> FairColor
        TreadHealth.WORN -> WornColor
        TreadHealth.CRITICAL -> CriticalColor
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("${tireStatus.position.displayName} Tyre") },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // 3D Tyre Visualization
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "3D Tyre Overview",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        
                        Spacer(modifier = Modifier.height(16.dp))
                        
                        // Animated 3D Tyre
                        Animated3DTyre(
                            healthPercent = getHealthPercent(tireStatus.treadHealth),
                            healthColor = healthColor,
                            hasDefects = tireStatus.defects.isNotEmpty(),
                            modifier = Modifier.size(220.dp)
                        )
                        
                        Spacer(modifier = Modifier.height(16.dp))
                        
                        // Status Badge
                        Surface(
                            shape = RoundedCornerShape(20.dp),
                            color = healthColor.copy(alpha = 0.15f)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = if (tireStatus.isCritical) Icons.Filled.Warning else Icons.Filled.CheckCircle,
                                    contentDescription = null,
                                    tint = healthColor,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = tireStatus.treadHealth.displayText,
                                    color = healthColor,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }
                    }
                }
            }
            
            // Sensor Data Cards
            item {
                Text(
                    text = "Sensor Data",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }
            
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    SensorDataCard(
                        icon = Icons.Filled.Speed,
                        title = "Pressure",
                        value = "%.1f PSI".format(tireStatus.pressurePsi),
                        status = when {
                            tireStatus.pressurePsi < 28f -> "Low"
                            tireStatus.pressurePsi > 36f -> "High"
                            else -> "Optimal"
                        },
                        statusColor = when {
                            tireStatus.pressurePsi < 28f || tireStatus.pressurePsi > 38f -> CriticalColor
                            tireStatus.pressurePsi < 30f || tireStatus.pressurePsi > 36f -> WornColor
                            else -> ExcellentColor
                        },
                        modifier = Modifier.weight(1f)
                    )
                    
                    SensorDataCard(
                        icon = Icons.Filled.Thermostat,
                        title = "Temperature",
                        value = "%.1f°C".format(tireStatus.temperatureCelsius),
                        status = when {
                            tireStatus.temperatureCelsius > 40f -> "Hot"
                            tireStatus.temperatureCelsius < 10f -> "Cold"
                            else -> "Normal"
                        },
                        statusColor = when {
                            tireStatus.temperatureCelsius > 45f -> CriticalColor
                            tireStatus.temperatureCelsius > 40f -> WornColor
                            else -> ExcellentColor
                        },
                        modifier = Modifier.weight(1f)
                    )
                }
            }
            
            // Tread Depth Visualization
            item {
                TreadDepthCard(tireStatus = tireStatus)
            }
            
            // Defects Section (if any)
            if (tireStatus.defects.isNotEmpty()) {
                item {
                    Text(
                        text = "Detected Issues",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = CriticalColor
                    )
                }
                
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = CriticalColor.copy(alpha = 0.1f)
                        ),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            tireStatus.defects.forEach { defect ->
                                Row(
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.Warning,
                                        contentDescription = null,
                                        tint = CriticalColor,
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Text(
                                        text = defect,
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.Medium
                                    )
                                }
                            }
                        }
                    }
                }
            }
            
            // Recommendations
            item {
                RecommendationsCard(
                    tireStatus = tireStatus,
                    onScheduleService = onScheduleService
                )
            }
            
            item {
                Spacer(modifier = Modifier.height(32.dp))
            }
        }
    }
}

/**
 * Animated 3D-like Tyre Visualization
 */
@Composable
fun Animated3DTyre(
    healthPercent: Float,
    healthColor: Color,
    hasDefects: Boolean,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition()
    
    // Slow rotation animation
    val rotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(10000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        )
    )
    
    // Pulse animation for defects
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.1f,
        animationSpec = infiniteRepeatable(
            animation = tween(800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        )
    )
    
    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val center = Offset(size.width / 2, size.height / 2)
            val outerRadius = size.minDimension / 2 - 10
            val innerRadius = outerRadius * 0.4f
            val treadWidth = (outerRadius - innerRadius) * 0.3f
            
            // Draw outer tyre ring (rubber)
            drawCircle(
                color = Color(0xFF2D2D2D),
                radius = outerRadius,
                center = center
            )
            
            // Draw tread pattern with rotation
            rotate(rotation, center) {
                drawTreadPattern(
                    center = center,
                    outerRadius = outerRadius,
                    innerRadius = outerRadius - treadWidth,
                    healthColor = healthColor,
                    healthPercent = healthPercent
                )
            }
            
            // Draw sidewall
            drawCircle(
                color = Color(0xFF3D3D3D),
                radius = outerRadius - treadWidth - 5,
                center = center
            )
            
            // Draw rim
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        Color(0xFFE0E0E0),
                        Color(0xFFBDBDBD),
                        Color(0xFF9E9E9E)
                    ),
                    center = center,
                    radius = innerRadius
                ),
                radius = innerRadius,
                center = center
            )
            
            // Draw rim details (spokes)
            rotate(rotation / 3, center) {
                for (i in 0 until 5) {
                    val angle = (i * 72f) * (Math.PI / 180f)
                    val startX = center.x + (innerRadius * 0.3f) * cos(angle).toFloat()
                    val startY = center.y + (innerRadius * 0.3f) * sin(angle).toFloat()
                    val endX = center.x + (innerRadius * 0.9f) * cos(angle).toFloat()
                    val endY = center.y + (innerRadius * 0.9f) * sin(angle).toFloat()
                    
                    drawLine(
                        color = Color(0xFF757575),
                        start = Offset(startX, startY),
                        end = Offset(endX, endY),
                        strokeWidth = 8f,
                        cap = StrokeCap.Round
                    )
                }
            }
            
            // Draw center cap
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        Color(0xFFBDBDBD),
                        Color(0xFF9E9E9E)
                    ),
                    center = center,
                    radius = innerRadius * 0.25f
                ),
                radius = innerRadius * 0.25f,
                center = center
            )
            
            // Defect indicator
            if (hasDefects) {
                val defectRadius = outerRadius * 0.15f * pulseScale
                val defectOffset = Offset(
                    center.x + outerRadius * 0.5f,
                    center.y - outerRadius * 0.5f
                )
                drawCircle(
                    color = CriticalColor.copy(alpha = 0.8f),
                    radius = defectRadius,
                    center = defectOffset
                )
                drawCircle(
                    color = Color.White,
                    radius = defectRadius * 0.4f,
                    center = defectOffset
                )
            }
        }
        
        // Health percentage in center
        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "${(healthPercent * 100).toInt()}%",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = healthColor
            )
            Text(
                text = "Health",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

private fun DrawScope.drawTreadPattern(
    center: Offset,
    outerRadius: Float,
    innerRadius: Float,
    healthColor: Color,
    healthPercent: Float
) {
    val segments = 24
    val gapAngle = 2f
    val segmentAngle = (360f / segments) - gapAngle
    
    for (i in 0 until segments) {
        val startAngle = i * (segmentAngle + gapAngle)
        val segmentHealth = if (i < segments * healthPercent) healthColor else Color(0xFF5D5D5D)
        
        drawArc(
            color = segmentHealth,
            startAngle = startAngle,
            sweepAngle = segmentAngle,
            useCenter = false,
            topLeft = Offset(center.x - outerRadius, center.y - outerRadius),
            size = androidx.compose.ui.geometry.Size(outerRadius * 2, outerRadius * 2),
            style = Stroke(width = outerRadius - innerRadius)
        )
    }
}

@Composable
private fun SensorDataCard(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    value: String,
    status: String,
    statusColor: Color,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(28.dp),
                tint = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = value,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(4.dp))
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = statusColor.copy(alpha = 0.15f)
            ) {
                Text(
                    text = status,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                    style = MaterialTheme.typography.labelSmall,
                    color = statusColor,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}

@Composable
private fun TreadDepthCard(
    tireStatus: TireStatus
) {
    val treadPercent = getHealthPercent(tireStatus.treadHealth)
    
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp)
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
                    text = "Tread Depth",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = "${(treadPercent * 100).toInt()}%",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = when (tireStatus.treadHealth) {
                        TreadHealth.EXCELLENT, TreadHealth.GOOD -> ExcellentColor
                        TreadHealth.FAIR -> FairColor
                        TreadHealth.WORN -> WornColor
                        TreadHealth.CRITICAL -> CriticalColor
                    }
                )
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            // Tread depth bar
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(12.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .fillMaxWidth(treadPercent)
                        .clip(RoundedCornerShape(6.dp))
                        .background(
                            brush = Brush.horizontalGradient(
                                colors = listOf(
                                    CriticalColor,
                                    WornColor,
                                    FairColor,
                                    GoodColor,
                                    ExcellentColor
                                )
                            )
                        )
                )
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Replace",
                    style = MaterialTheme.typography.labelSmall,
                    color = CriticalColor
                )
                Text(
                    text = "New",
                    style = MaterialTheme.typography.labelSmall,
                    color = ExcellentColor
                )
            }
        }
    }
}

@Composable
private fun RecommendationsCard(
    tireStatus: TireStatus,
    onScheduleService: () -> Unit
) {
    val recommendations = buildList {
        if (tireStatus.pressurePsi < 30f) {
            add("Inflate tyre to recommended 32-35 PSI")
        }
        if (tireStatus.pressurePsi > 36f) {
            add("Release air to reach optimal 32-35 PSI")
        }
        if (tireStatus.treadHealth == TreadHealth.WORN || tireStatus.treadHealth == TreadHealth.CRITICAL) {
            add("Schedule tyre replacement soon")
        }
        if (tireStatus.defects.isNotEmpty()) {
            add("Professional inspection recommended")
        }
        if (isEmpty()) {
            add("No immediate action required")
            add("Continue regular monthly checks")
        }
    }
    
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = "Recommendations",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            
            Spacer(modifier = Modifier.height(12.dp))
            
            recommendations.forEach { rec ->
                Row(
                    modifier = Modifier.padding(vertical = 4.dp),
                    verticalAlignment = Alignment.Top
                ) {
                    Icon(
                        imageVector = Icons.Filled.CheckCircle,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = rec,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
            
            if (tireStatus.isCritical) {
                Spacer(modifier = Modifier.height(16.dp))
                Button(
                    onClick = onScheduleService,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = CriticalColor
                    )
                ) {
                    Icon(
                        imageVector = Icons.Filled.Build,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Schedule Service Now")
                }
            }
        }
    }
}

private fun getHealthPercent(health: TreadHealth): Float = when (health) {
    TreadHealth.EXCELLENT -> 0.95f
    TreadHealth.GOOD -> 0.75f
    TreadHealth.FAIR -> 0.55f
    TreadHealth.WORN -> 0.30f
    TreadHealth.CRITICAL -> 0.10f
}
