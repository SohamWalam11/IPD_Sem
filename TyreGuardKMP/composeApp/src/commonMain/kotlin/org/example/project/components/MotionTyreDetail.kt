package org.example.project.components

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.roundToInt

// Theme colors
private val PrimaryViolet = Color(0xFF6200EA)
private val DeepViolet = Color(0xFF3700B3)
private val GoodGreen = Color(0xFF4CAF50)
private val WarningYellow = Color(0xFFFFC107)
private val DangerRed = Color(0xFFE53935)

/**
 * Motion-like transition states for tyre detail
 */
enum class TyreDetailState {
    COLLAPSED,    // Just the chip visible
    EXPANDED,     // Full detail panel
    FULL_SCREEN   // Complete takeover
}

/**
 * MotionLayout-style expandable tyre detail view
 * Provides smooth transitions between collapsed, expanded, and full-screen states
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MotionTyreDetail(
    tyre: TyrePressureData,
    isVisible: Boolean,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    var currentState by remember { mutableStateOf(TyreDetailState.COLLAPSED) }
    
    // Animated transition progress (0 = collapsed, 1 = expanded, 2 = full screen)
    val transitionProgress by animateFloatAsState(
        targetValue = when (currentState) {
            TyreDetailState.COLLAPSED -> 0f
            TyreDetailState.EXPANDED -> 1f
            TyreDetailState.FULL_SCREEN -> 2f
        },
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "transition_progress"
    )
    
    // When visibility changes, update state
    LaunchedEffect(isVisible) {
        currentState = if (isVisible) TyreDetailState.EXPANDED else TyreDetailState.COLLAPSED
    }
    
    // Calculate interpolated values based on transition progress
    val cornerRadius = lerp(20f, 0f, (transitionProgress - 1f).coerceIn(0f, 1f))
    val backgroundAlpha = lerp(0f, 0.5f, transitionProgress.coerceIn(0f, 1f))
    val panelHeight = lerp(0.4f, 1f, (transitionProgress - 1f).coerceIn(0f, 1f))
    
    if (transitionProgress > 0.01f) {
        Box(
            modifier = modifier.fillMaxSize()
        ) {
            // Background scrim
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = backgroundAlpha))
                    .clickable(enabled = currentState != TyreDetailState.COLLAPSED) {
                        if (currentState == TyreDetailState.FULL_SCREEN) {
                            currentState = TyreDetailState.EXPANDED
                        } else {
                            currentState = TyreDetailState.COLLAPSED
                            onDismiss()
                        }
                    }
            )
            
            // Sliding panel
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight(panelHeight)
                    .align(Alignment.BottomCenter)
                    .graphicsLayer {
                        translationY = (1 - transitionProgress.coerceIn(0f, 1f)) * 500
                    }
                    .clip(RoundedCornerShape(topStart = cornerRadius.dp, topEnd = cornerRadius.dp))
                    .background(Color.White)
                    .pointerInput(Unit) {
                        detectDragGestures(
                            onDragEnd = {
                                // Snap to nearest state based on velocity
                            },
                            onDrag = { change, dragAmount ->
                                change.consume()
                                val dragY = dragAmount.y
                                
                                if (dragY > 50) {
                                    // Dragging down
                                    if (currentState == TyreDetailState.FULL_SCREEN) {
                                        currentState = TyreDetailState.EXPANDED
                                    } else if (currentState == TyreDetailState.EXPANDED) {
                                        currentState = TyreDetailState.COLLAPSED
                                        onDismiss()
                                    }
                                } else if (dragY < -50) {
                                    // Dragging up
                                    if (currentState == TyreDetailState.EXPANDED) {
                                        currentState = TyreDetailState.FULL_SCREEN
                                    }
                                }
                            }
                        )
                    }
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp)
                ) {
                    // Drag handle
                    Box(
                        modifier = Modifier
                            .align(Alignment.CenterHorizontally)
                            .width(40.dp)
                            .height(4.dp)
                            .clip(RoundedCornerShape(2.dp))
                            .background(Color.LightGray)
                            .clickable {
                                if (currentState == TyreDetailState.EXPANDED) {
                                    currentState = TyreDetailState.FULL_SCREEN
                                } else {
                                    currentState = TyreDetailState.EXPANDED
                                }
                            }
                    )
                    
                    Spacer(modifier = Modifier.height(20.dp))
                    
                    // Header
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
                        
                        Row {
                            // Expand/Collapse button
                            IconButton(
                                onClick = {
                                    currentState = if (currentState == TyreDetailState.FULL_SCREEN) {
                                        TyreDetailState.EXPANDED
                                    } else {
                                        TyreDetailState.FULL_SCREEN
                                    }
                                }
                            ) {
                                Icon(
                                    Icons.Default.ExpandLess,
                                    contentDescription = "Expand",
                                    modifier = Modifier.graphicsLayer {
                                        rotationZ = if (currentState == TyreDetailState.FULL_SCREEN) 180f else 0f
                                    }
                                )
                            }
                            
                            // Close button
                            IconButton(
                                onClick = {
                                    currentState = TyreDetailState.COLLAPSED
                                    onDismiss()
                                }
                            ) {
                                Icon(Icons.Default.Close, contentDescription = "Close")
                            }
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    // Status badge
                    Surface(
                        shape = RoundedCornerShape(20.dp),
                        color = tyre.status.color.copy(alpha = 0.15f),
                        modifier = Modifier.align(Alignment.Start)
                    ) {
                        Text(
                            text = tyre.status.label,
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                            style = MaterialTheme.typography.labelLarge,
                            color = tyre.status.color,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                    
                    Spacer(modifier = Modifier.height(24.dp))
                    
                    // Metrics with staggered animation
                    val metricsVisible = transitionProgress > 0.5f
                    
                    // Pressure Card
                    AnimatedVisibility(
                        visible = metricsVisible,
                        enter = fadeIn(tween(200)) + slideInHorizontally(tween(200)) { -it }
                    ) {
                        MotionMetricCard(
                            title = "Pressure",
                            value = "${tyre.pressure.toInt()} PSI",
                            description = "Recommended: 32-35 PSI",
                            progress = tyre.pressure / 50f,
                            color = tyre.status.color
                        )
                    }
                    
                    Spacer(modifier = Modifier.height(12.dp))
                    
                    // Temperature Card
                    AnimatedVisibility(
                        visible = metricsVisible,
                        enter = fadeIn(tween(200, delayMillis = 100)) + slideInHorizontally(tween(200, delayMillis = 100)) { -it }
                    ) {
                        MotionMetricCard(
                            title = "Temperature",
                            value = "${tyre.temperature}°C",
                            description = if (tyre.temperature > 40) "Running hot!" else "Normal operating temp",
                            progress = tyre.temperature / 60f,
                            color = if (tyre.temperature > 40) DangerRed else GoodGreen
                        )
                    }
                    
                    Spacer(modifier = Modifier.height(12.dp))
                    
                    // Tread Depth Card
                    AnimatedVisibility(
                        visible = metricsVisible,
                        enter = fadeIn(tween(200, delayMillis = 200)) + slideInHorizontally(tween(200, delayMillis = 200)) { -it }
                    ) {
                        MotionMetricCard(
                            title = "Tread Depth",
                            value = String.format("%.1f mm", tyre.treadDepth),
                            description = when {
                                tyre.treadDepth >= 4f -> "Excellent condition"
                                tyre.treadDepth >= 2f -> "Consider replacement soon"
                                else -> "Replace immediately!"
                            },
                            progress = tyre.treadDepth / 8f,
                            color = when {
                                tyre.treadDepth >= 4f -> GoodGreen
                                tyre.treadDepth >= 2f -> WarningYellow
                                else -> DangerRed
                            }
                        )
                    }
                    
                    Spacer(modifier = Modifier.height(12.dp))
                    
                    // Defects Card
                    AnimatedVisibility(
                        visible = metricsVisible,
                        enter = fadeIn(tween(200, delayMillis = 300)) + slideInHorizontally(tween(200, delayMillis = 300)) { -it }
                    ) {
                        MotionMetricCard(
                            title = "Defects Detected",
                            value = "${tyre.defects.size} issues",
                            description = if (tyre.defects.isEmpty()) "No defects found" else tyre.defects.joinToString(", "),
                            progress = if (tyre.defects.isEmpty()) 1f else 0.2f,
                            color = if (tyre.defects.isEmpty()) GoodGreen else DangerRed
                        )
                    }
                    
                    // Extra content when full screen
                    AnimatedVisibility(
                        visible = currentState == TyreDetailState.FULL_SCREEN,
                        enter = fadeIn(tween(300)) + expandVertically(tween(300))
                    ) {
                        Column {
                            Spacer(modifier = Modifier.height(24.dp))
                            
                            Text(
                                text = "Recommendations",
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.Bold
                                )
                            )
                            
                            Spacer(modifier = Modifier.height(12.dp))
                            
                            RecommendationItem(
                                text = "Check pressure weekly",
                                isComplete = true
                            )
                            RecommendationItem(
                                text = "Rotate tyres every 5,000 km",
                                isComplete = false
                            )
                            RecommendationItem(
                                text = "Schedule alignment check",
                                isComplete = false
                            )
                            
                            Spacer(modifier = Modifier.height(24.dp))
                            
                            // Action button
                            Button(
                                onClick = { /* Schedule service */ },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(52.dp),
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = PrimaryViolet
                                )
                            ) {
                                Text(
                                    text = "Schedule Service",
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun MotionMetricCard(
    title: String,
    value: String,
    description: String,
    progress: Float,
    color: Color,
    modifier: Modifier = Modifier
) {
    val animatedProgress by animateFloatAsState(
        targetValue = progress.coerceIn(0f, 1f),
        animationSpec = tween(800, easing = FastOutSlowInEasing),
        label = "progress"
    )
    
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFF5F5F5))
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.Gray
                )
                Text(
                    text = value,
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold
                    )
                )
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            // Progress bar
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(Color.LightGray)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .fillMaxWidth(animatedProgress)
                        .clip(RoundedCornerShape(4.dp))
                        .background(
                            Brush.horizontalGradient(
                                colors = listOf(color.copy(alpha = 0.7f), color)
                            )
                        )
                )
            }
            
            Spacer(modifier = Modifier.height(4.dp))
            
            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall,
                color = Color.Gray
            )
        }
    }
}

@Composable
private fun RecommendationItem(
    text: String,
    isComplete: Boolean
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(24.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(if (isComplete) GoodGreen else Color.LightGray),
            contentAlignment = Alignment.Center
        ) {
            if (isComplete) {
                Text("✓", color = Color.White, fontWeight = FontWeight.Bold)
            }
        }
        
        Spacer(modifier = Modifier.width(12.dp))
        
        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium,
            color = if (isComplete) Color.Gray else Color.Black
        )
    }
}

// Helper function for linear interpolation
private fun lerp(start: Float, end: Float, fraction: Float): Float {
    return start + (end - start) * fraction
}
