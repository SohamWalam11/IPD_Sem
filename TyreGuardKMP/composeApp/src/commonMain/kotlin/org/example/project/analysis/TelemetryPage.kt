package org.example.project.analysis

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import kotlin.random.Random

/**
 * Telemetry Page - Tab 3 of Analysis Screen
 * Displays real-time sensor data visualization with animated line chart.
 */
@Composable
fun TelemetryPage(
    telemetryState: TelemetryState,
    onSelectTire: (TirePosition) -> Unit = {},
    onStartSimulation: () -> Unit = {},
    onStopSimulation: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // Header
        Text(
            text = "Live Telemetry",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold
        )
        
        Spacer(modifier = Modifier.height(8.dp))
        
        // Tire selector dropdown
        TireSelectorCard(
            selectedTire = telemetryState.selectedTire,
            onSelectTire = onSelectTire
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // Live indicator
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(12.dp)
                    .background(
                        color = if (telemetryState.isLive) Color(0xFF4CAF50) else Color.Gray,
                        shape = RoundedCornerShape(6.dp)
                    )
            )
            Text(
                text = if (telemetryState.isLive) "LIVE" else "PAUSED",
                style = MaterialTheme.typography.labelMedium,
                color = if (telemetryState.isLive) Color(0xFF4CAF50) else Color.Gray,
                fontWeight = FontWeight.Bold
            )
            
            Spacer(modifier = Modifier.weight(1f))
            
            // Toggle simulation button
            FilledTonalButton(
                onClick = {
                    if (telemetryState.isLive) onStopSimulation() else onStartSimulation()
                }
            ) {
                Icon(
                    imageVector = if (telemetryState.isLive) Icons.Default.Pause else Icons.Default.PlayArrow,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(if (telemetryState.isLive) "Pause" else "Start")
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // Pressure Chart Card
        PressureChartCard(
            title = "Live Pressure (PSI) - ${telemetryState.selectedTire.shortName}",
            dataPoints = telemetryState.pressureHistory,
            modifier = Modifier
                .fillMaxWidth()
                .height(250.dp)
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // Current value card
        CurrentValueCard(
            pressureHistory = telemetryState.pressureHistory
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TireSelectorCard(
    selectedTire: TirePosition,
    onSelectTire: (TirePosition) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    
    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = it }
    ) {
        OutlinedTextField(
            value = selectedTire.displayName,
            onValueChange = {},
            readOnly = true,
            label = { Text("Select Tire") },
            trailingIcon = {
                ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded)
            },
            modifier = Modifier
                .fillMaxWidth()
                .menuAnchor()
        )
        
        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            TirePosition.entries.forEach { position ->
                DropdownMenuItem(
                    text = { Text(position.displayName) },
                    onClick = {
                        onSelectTire(position)
                        expanded = false
                    },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.RadioButtonChecked,
                            contentDescription = null,
                            tint = if (position == selectedTire) 
                                MaterialTheme.colorScheme.primary 
                            else Color.Gray
                        )
                    }
                )
            }
        }
    }
}

@Composable
private fun PressureChartCard(
    title: String,
    dataPoints: List<Float>,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            if (dataPoints.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(180.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "No data available",
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                LivePressureChart(
                    dataPoints = dataPoints,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(180.dp)
                )
            }
        }
    }
}

/**
 * Animated line chart with gradient fill using Canvas.
 */
@Composable
private fun LivePressureChart(
    dataPoints: List<Float>,
    modifier: Modifier = Modifier
) {
    val primaryColor = MaterialTheme.colorScheme.primary
    val surfaceColor = MaterialTheme.colorScheme.surface
    
    Canvas(modifier = modifier) {
        if (dataPoints.isEmpty()) return@Canvas
        
        val width = size.width
        val height = size.height
        val padding = 20f
        
        // Calculate min/max for scaling
        val minValue = (dataPoints.minOrNull() ?: 30f) - 0.5f
        val maxValue = (dataPoints.maxOrNull() ?: 35f) + 0.5f
        val range = maxValue - minValue
        
        // Create path for the line
        val linePath = Path()
        val fillPath = Path()
        
        val stepX = (width - padding * 2) / (dataPoints.size - 1).coerceAtLeast(1)
        
        dataPoints.forEachIndexed { index, value ->
            val x = padding + index * stepX
            val y = height - padding - ((value - minValue) / range * (height - padding * 2))
            
            if (index == 0) {
                linePath.moveTo(x, y)
                fillPath.moveTo(x, height - padding)
                fillPath.lineTo(x, y)
            } else {
                // Smooth curve using quadratic bezier
                val prevX = padding + (index - 1) * stepX
                val prevY = height - padding - ((dataPoints[index - 1] - minValue) / range * (height - padding * 2))
                val midX = (prevX + x) / 2
                
                linePath.quadraticBezierTo(prevX, prevY, midX, (prevY + y) / 2)
                linePath.quadraticBezierTo(midX, (prevY + y) / 2, x, y)
                
                fillPath.quadraticBezierTo(prevX, prevY, midX, (prevY + y) / 2)
                fillPath.quadraticBezierTo(midX, (prevY + y) / 2, x, y)
            }
        }
        
        // Close fill path
        val lastX = padding + (dataPoints.size - 1) * stepX
        fillPath.lineTo(lastX, height - padding)
        fillPath.close()
        
        // Draw gradient fill
        drawPath(
            path = fillPath,
            brush = Brush.verticalGradient(
                colors = listOf(
                    primaryColor.copy(alpha = 0.4f),
                    primaryColor.copy(alpha = 0.1f),
                    Color.Transparent
                )
            )
        )
        
        // Draw the line
        drawPath(
            path = linePath,
            color = primaryColor,
            style = Stroke(
                width = 3f,
                cap = StrokeCap.Round,
                join = StrokeJoin.Round
            )
        )
        
        // Draw data points
        dataPoints.forEachIndexed { index, value ->
            val x = padding + index * stepX
            val y = height - padding - ((value - minValue) / range * (height - padding * 2))
            
            // Outer circle
            drawCircle(
                color = surfaceColor,
                radius = 6f,
                center = Offset(x, y)
            )
            // Inner circle
            drawCircle(
                color = primaryColor,
                radius = 4f,
                center = Offset(x, y)
            )
        }
    }
}

@Composable
private fun CurrentValueCard(
    pressureHistory: List<Float>
) {
    val currentValue = pressureHistory.lastOrNull() ?: 0f
    val previousValue = pressureHistory.getOrNull(pressureHistory.size - 2) ?: currentValue
    val trend = currentValue - previousValue
    
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "%.2f".format(currentValue),
                    style = MaterialTheme.typography.headlineLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = "Current PSI",
                    style = MaterialTheme.typography.labelMedium
                )
            }
            
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = when {
                            trend > 0.05f -> Icons.Default.TrendingUp
                            trend < -0.05f -> Icons.Default.TrendingDown
                            else -> Icons.Default.TrendingFlat
                        },
                        contentDescription = null,
                        tint = when {
                            trend > 0.05f -> Color(0xFF4CAF50)
                            trend < -0.05f -> Color(0xFFE53935)
                            else -> Color.Gray
                        },
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "%+.2f".format(trend),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = when {
                            trend > 0.05f -> Color(0xFF4CAF50)
                            trend < -0.05f -> Color(0xFFE53935)
                            else -> Color.Gray
                        }
                    )
                }
                Text(
                    text = "Trend",
                    style = MaterialTheme.typography.labelMedium
                )
            }
            
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "${pressureHistory.size}",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Data Points",
                    style = MaterialTheme.typography.labelMedium
                )
            }
        }
    }
}

/**
 * Preview with self-updating simulation.
 */
@Composable
fun TelemetryPagePreview() {
    var pressureHistory by remember { mutableStateOf(listOf(32.1f, 32.3f, 32.2f)) }
    var isLive by remember { mutableStateOf(true) }
    
    LaunchedEffect(isLive) {
        while (isLive) {
            delay(500L)
            val newList = pressureHistory.toMutableList()
            if (newList.size >= 20) newList.removeAt(0)
            newList.add(32.0f + Random.nextFloat() * 0.5f)
            pressureHistory = newList
        }
    }
    
    MaterialTheme {
        TelemetryPage(
            telemetryState = TelemetryState(
                selectedTire = TirePosition.FRONT_LEFT,
                pressureHistory = pressureHistory,
                isLive = isLive
            ),
            onStartSimulation = { isLive = true },
            onStopSimulation = { isLive = false }
        )
    }
}
