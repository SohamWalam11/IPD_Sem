package org.example.project

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.Speed
import androidx.compose.material.icons.outlined.Thermostat
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.jetbrains.compose.ui.tooling.preview.Preview
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

// ============ CYBER-AUTOMOTIVE COLOR PALETTE ============

// Primary Brand
private val NeonViolet = Color(0xFF6200EA)
private val ElectricPurple = Color(0xFFBB86FC)
private val DeepViolet = Color(0xFF3700B3)

// Semantic Status Colors
private val SafeGreen = Color(0xFF00E676)
private val CautionAmber = Color(0xFFFFAB00)
private val DangerRed = Color(0xFFFF1744)
private val CriticalRed = Color(0xFFD50000)

// Glassmorphism
private val GlassWhite = Color.White.copy(alpha = 0.85f)
private val GlassBorder = Color.White.copy(alpha = 0.5f)
private val FrostedOverlay = Color.White.copy(alpha = 0.4f)

// Text & Background
private val DeepDark = Color(0xFF0D0D0D)
private val SoftGray = Color(0xFF6B6B6B)
private val CloudWhite = Color(0xFFF8F9FE)

// ============ DATA MODELS ============

data class TyreInfo(
    val id: String,
    val position: String,
    val pressure: Float,
    val temperature: Int,
    val treadDepth: Float,
    val healthScore: Int
) {
    val status: TyreHealthStatus
        get() = when {
            healthScore >= 80 -> TyreHealthStatus.OPTIMAL
            healthScore >= 50 -> TyreHealthStatus.CAUTION
            else -> TyreHealthStatus.CRITICAL
        }
}

enum class TyreHealthStatus(val label: String, val color: Color, val icon: @Composable () -> Unit) {
    OPTIMAL("Optimal", SafeGreen, { Icon(Icons.Default.CheckCircle, null, tint = SafeGreen) }),
    CAUTION("Attention", CautionAmber, { Icon(Icons.Default.Warning, null, tint = CautionAmber) }),
    CRITICAL("Critical", DangerRed, { Icon(Icons.Default.Error, null, tint = DangerRed) })
}

// Sample Data
private val tyreData = listOf(
    TyreInfo("FL", "Front Left", 32.5f, 28, 7.2f, 92),
    TyreInfo("FR", "Front Right", 32.0f, 29, 6.8f, 88),
    TyreInfo("RL", "Rear Left", 30.5f, 30, 5.1f, 65),
    TyreInfo("RR", "Rear Right", 26.0f, 38, 2.8f, 28)
)

// ============ MAIN PREMIUM DASHBOARD ============

@Composable
fun PremiumDashboardScreen(
    userName: String = "Soham",
    modifier: Modifier = Modifier,
    onCameraClick: () -> Unit = {},
    onTyreClick: (TyreInfo) -> Unit = {},
    onNotificationClick: () -> Unit = {}
) {
    // Calculate overall health from all tyres
    val overallHealth = remember(tyreData) {
        tyreData.map { it.healthScore }.average().toInt()
    }
    
    var selectedTyreIndex by remember { mutableStateOf(0) }
    val selectedTyre = tyreData[selectedTyreIndex]
    
    // SEMANTIC AMBIENT COLOR - Animates based on health
    val ambientColor by animateColorAsState(
        targetValue = when {
            overallHealth >= 75 -> SafeGreen
            overallHealth >= 45 -> CautionAmber
            else -> DangerRed
        },
        animationSpec = tween(1200, easing = EaseInOutCubic),
        label = "ambientColor"
    )
    
    // Secondary ambient for depth
    val secondaryAmbient by animateColorAsState(
        targetValue = when {
            overallHealth >= 75 -> Color(0xFF00BFA5)
            overallHealth >= 45 -> Color(0xFFFF6D00)
            else -> Color(0xFFFF5252)
        },
        animationSpec = tween(1500, easing = EaseInOutCubic),
        label = "secondaryAmbient"
    )

    // Breathing animation for ambient glow
    val infiniteTransition = rememberInfiniteTransition(label = "ambient_breathing")
    val breathingScale by infiniteTransition.animateFloat(
        initialValue = 0.9f,
        targetValue = 1.1f,
        animationSpec = infiniteRepeatable(
            animation = tween(4000, easing = EaseInOutSine),
            repeatMode = RepeatMode.Reverse
        ),
        label = "breathScale"
    )
    
    val glowAlpha by infiniteTransition.animateFloat(
        initialValue = 0.15f,
        targetValue = 0.35f,
        animationSpec = infiniteRepeatable(
            animation = tween(3000, easing = EaseInOutSine),
            repeatMode = RepeatMode.Reverse
        ),
        label = "glowAlpha"
    )

    Scaffold(
        containerColor = Color.Transparent,
        bottomBar = { 
            GlassmorphicBottomBar(
                onHomeClick = {},
                onAnalyticsClick = {},
                selectedTab = 0
            ) 
        }
    ) { innerPadding ->
        Box(
            modifier = modifier
                .fillMaxSize()
                .background(CloudWhite)
                .drawBehind {
                    // PRIMARY AMBIENT GLOW (Top)
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                ambientColor.copy(alpha = glowAlpha),
                                ambientColor.copy(alpha = glowAlpha * 0.5f),
                                Color.Transparent
                            ),
                            center = Offset(size.width * 0.3f, size.height * 0.15f),
                            radius = size.width * breathingScale
                        )
                    )
                    
                    // SECONDARY AMBIENT GLOW (Bottom Right)
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                secondaryAmbient.copy(alpha = glowAlpha * 0.6f),
                                Color.Transparent
                            ),
                            center = Offset(size.width * 0.85f, size.height * 0.7f),
                            radius = size.width * 0.6f * breathingScale
                        )
                    )
                    
                    // SUBTLE VIOLET ACCENT (Center)
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                ElectricPurple.copy(alpha = 0.08f),
                                Color.Transparent
                            ),
                            center = Offset(size.width * 0.5f, size.height * 0.5f),
                            radius = size.width * 0.4f
                        )
                    )
                }
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp)
            ) {
                Spacer(modifier = Modifier.height(16.dp))
                
                // 1. PREMIUM HEADER
                PremiumHeader(
                    userName = userName,
                    onNotificationClick = onNotificationClick
                )
                
                Spacer(modifier = Modifier.height(28.dp))

                // 2. HALO HEALTH RING
                Box(
                    modifier = Modifier.fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    HaloHealthRing(
                        score = overallHealth,
                        ambientColor = ambientColor
                    )
                }

                Spacer(modifier = Modifier.height(32.dp))

                // 3. VEHICLE STATUS SECTION
                SectionTitle(
                    title = "Vehicle Status",
                    subtitle = "Tap a tyre to inspect"
                )

                Spacer(modifier = Modifier.height(16.dp))

                // 4. INTERACTIVE CHASSIS SELECTOR
                GlassCard(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(280.dp)
                ) {
                    ChassisSelector(
                        tyres = tyreData,
                        selectedIndex = selectedTyreIndex,
                        onSelect = { selectedTyreIndex = it }
                    )
                }

                Spacer(modifier = Modifier.height(20.dp))

                // 5. TYRE DETAIL CARD (Morphs based on selection)
                AnimatedContent(
                    targetState = selectedTyre,
                    transitionSpec = {
                        (fadeIn(tween(300)) + scaleIn(initialScale = 0.95f))
                            .togetherWith(fadeOut(tween(200)))
                    },
                    label = "tyreDetail"
                ) { tyre ->
                    TyreDetailCard(tyre = tyre)
                }

                Spacer(modifier = Modifier.height(24.dp))

                // 6. QUICK ACTIONS
                QuickActionsRow()

                Spacer(modifier = Modifier.height(100.dp))
            }

            // FLOATING CAMERA BUTTON
            PulsingCameraFAB(
                onClick = onCameraClick,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .offset(y = (-50).dp)
            )
        }
    }
}

// ============ PREMIUM HEADER ============

@Composable
private fun PremiumHeader(
    userName: String,
    onNotificationClick: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column {
            Text(
                text = getGreeting(),
                color = SoftGray,
                fontSize = 14.sp
            )
            Text(
                text = userName,
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = DeepDark
            )
        }

        // Glass Notification Button
        GlassIconButton(
            onClick = onNotificationClick,
            hasBadge = true
        ) {
            Icon(
                Icons.Default.Notifications,
                contentDescription = "Notifications",
                tint = DeepDark
            )
        }
    }
}

private fun getGreeting(): String {
    // Simple time-based greeting
    return "Good Morning,"
}

@Composable
private fun GlassIconButton(
    onClick: () -> Unit,
    hasBadge: Boolean = false,
    content: @Composable () -> Unit
) {
    Box(
        modifier = Modifier
            .size(52.dp)
            .clip(CircleShape)
            .background(GlassWhite)
            .border(1.dp, GlassBorder, CircleShape)
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        content()
        
        if (hasBadge) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .offset(x = (-8).dp, y = 8.dp)
                    .size(10.dp)
                    .background(DangerRed, CircleShape)
                    .border(2.dp, Color.White, CircleShape)
            )
        }
    }
}

// ============ HALO HEALTH RING ============

@Composable
private fun HaloHealthRing(
    score: Int,
    ambientColor: Color
) {
    var animatedScore by remember { mutableStateOf(0f) }

    LaunchedEffect(score) {
        animate(
            initialValue = 0f,
            targetValue = score.toFloat(),
            animationSpec = tween(2000, easing = EaseOutExpo)
        ) { value, _ ->
            animatedScore = value
        }
    }

    // Outer glow pulse
    val infiniteTransition = rememberInfiniteTransition(label = "halo")
    val glowPulse by infiniteTransition.animateFloat(
        initialValue = 0.8f,
        targetValue = 1.2f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = EaseInOutSine),
            repeatMode = RepeatMode.Reverse
        ),
        label = "glowPulse"
    )

    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier.size(200.dp)
    ) {
        // OUTER GLOW AURA
        Canvas(modifier = Modifier.fillMaxSize()) {
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        ambientColor.copy(alpha = 0.4f * glowPulse),
                        ambientColor.copy(alpha = 0.1f),
                        Color.Transparent
                    ),
                    center = center,
                    radius = size.width / 1.3f
                ),
                radius = size.width / 1.5f
            )
        }

        // THE RING
        Canvas(modifier = Modifier.size(160.dp)) {
            val strokeWidth = 20.dp.toPx()
            val radius = (size.minDimension - strokeWidth) / 2
            
            // Track (Gray arc)
            drawArc(
                color = Color.LightGray.copy(alpha = 0.25f),
                startAngle = 135f,
                sweepAngle = 270f,
                useCenter = false,
                topLeft = Offset(strokeWidth / 2, strokeWidth / 2),
                size = Size(radius * 2, radius * 2),
                style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
            )

            // Progress Arc (Gradient)
            val sweepProgress = 270f * (animatedScore / 100f)
            drawArc(
                brush = Brush.sweepGradient(
                    colors = listOf(
                        ambientColor.copy(alpha = 0.6f),
                        ambientColor,
                        ambientColor.copy(alpha = 0.8f)
                    ),
                    center = center
                ),
                startAngle = 135f,
                sweepAngle = sweepProgress,
                useCenter = false,
                topLeft = Offset(strokeWidth / 2, strokeWidth / 2),
                size = Size(radius * 2, radius * 2),
                style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
            )
            
            // End cap glow
            if (sweepProgress > 0) {
                val endAngle = (135f + sweepProgress) * (PI / 180f).toFloat()
                val capX = center.x + cos(endAngle) * radius
                val capY = center.y + sin(endAngle) * radius
                
                drawCircle(
                    color = ambientColor,
                    radius = strokeWidth / 2 + 4.dp.toPx(),
                    center = Offset(capX, capY),
                    alpha = 0.5f
                )
            }
        }

        // CENTER CONTENT
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = "${animatedScore.toInt()}",
                style = MaterialTheme.typography.displayMedium,
                fontWeight = FontWeight.Bold,
                color = DeepDark
            )
            Text(
                text = "Health Score",
                color = SoftGray,
                fontSize = 13.sp
            )
        }
    }
}

// ============ SECTION TITLE ============

@Composable
private fun SectionTitle(
    title: String,
    subtitle: String? = null
) {
    Column {
        Text(
            text = title,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = DeepDark
        )
        if (subtitle != null) {
            Text(
                text = subtitle,
                color = SoftGray,
                fontSize = 13.sp
            )
        }
    }
}

// ============ GLASSMORPHISM CARD ============

@Composable
private fun GlassCard(
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit
) {
    Box(
        modifier = modifier
            .shadow(
                elevation = 20.dp,
                shape = RoundedCornerShape(28.dp),
                spotColor = Color.Black.copy(alpha = 0.1f)
            )
            .clip(RoundedCornerShape(28.dp))
            .background(GlassWhite)
            .border(1.5.dp, GlassBorder, RoundedCornerShape(28.dp))
    ) {
        // Inner highlight (top edge)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(1.dp)
                .background(
                    Brush.horizontalGradient(
                        colors = listOf(
                            Color.Transparent,
                            Color.White,
                            Color.Transparent
                        )
                    )
                )
        )
        content()
    }
}

// ============ CHASSIS SELECTOR ============

@Composable
private fun ChassisSelector(
    tyres: List<TyreInfo>,
    selectedIndex: Int,
    onSelect: (Int) -> Unit
) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        // CAR BODY OUTLINE
        Canvas(
            modifier = Modifier
                .width(140.dp)
                .height(220.dp)
        ) {
            val cornerRadius = 50f
            
            // Car body shadow
            drawRoundRect(
                color = Color.Black.copy(alpha = 0.05f),
                topLeft = Offset(4.dp.toPx(), 4.dp.toPx()),
                size = size,
                cornerRadius = CornerRadius(cornerRadius)
            )
            
            // Car body
            drawRoundRect(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFFE8E8E8),
                        Color(0xFFF5F5F5),
                        Color(0xFFE0E0E0)
                    )
                ),
                cornerRadius = CornerRadius(cornerRadius)
            )
            
            // Car body border
            drawRoundRect(
                color = Color.LightGray.copy(alpha = 0.5f),
                cornerRadius = CornerRadius(cornerRadius),
                style = Stroke(width = 2.dp.toPx())
            )
            
            // Center line (hood to trunk)
            drawLine(
                color = Color.LightGray.copy(alpha = 0.3f),
                start = Offset(size.width / 2, 30.dp.toPx()),
                end = Offset(size.width / 2, size.height - 30.dp.toPx()),
                strokeWidth = 2.dp.toPx(),
                cap = StrokeCap.Round
            )
            
            // Windshield indicator
            drawRoundRect(
                color = Color(0xFFB3E5FC).copy(alpha = 0.5f),
                topLeft = Offset(size.width * 0.25f, size.height * 0.15f),
                size = Size(size.width * 0.5f, size.height * 0.15f),
                cornerRadius = CornerRadius(20f)
            )
            
            // Rear window indicator
            drawRoundRect(
                color = Color(0xFFB3E5FC).copy(alpha = 0.4f),
                topLeft = Offset(size.width * 0.3f, size.height * 0.72f),
                size = Size(size.width * 0.4f, size.height * 0.1f),
                cornerRadius = CornerRadius(15f)
            )
        }

        // TYRE POSITIONS
        // Front Left
        TyreButton(
            tyre = tyres[0],
            isSelected = selectedIndex == 0,
            onClick = { onSelect(0) },
            modifier = Modifier
                .align(Alignment.TopStart)
                .offset(x = 24.dp, y = 30.dp)
        )

        // Front Right
        TyreButton(
            tyre = tyres[1],
            isSelected = selectedIndex == 1,
            onClick = { onSelect(1) },
            modifier = Modifier
                .align(Alignment.TopEnd)
                .offset(x = (-24).dp, y = 30.dp)
        )

        // Rear Left
        TyreButton(
            tyre = tyres[2],
            isSelected = selectedIndex == 2,
            onClick = { onSelect(2) },
            modifier = Modifier
                .align(Alignment.BottomStart)
                .offset(x = 24.dp, y = (-30).dp)
        )

        // Rear Right
        TyreButton(
            tyre = tyres[3],
            isSelected = selectedIndex == 3,
            onClick = { onSelect(3) },
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .offset(x = (-24).dp, y = (-30).dp)
        )

        // Connection lines from tyres to body
        Canvas(modifier = Modifier.fillMaxSize()) {
            val lineColor = Color.LightGray.copy(alpha = 0.4f)
            val strokeW = 2.dp.toPx()
            
            // These are approximate - connect tyre buttons to car body
            // FL
            drawLine(lineColor, Offset(85.dp.toPx(), 80.dp.toPx()), 
                    Offset(120.dp.toPx(), 95.dp.toPx()), strokeW)
            // FR
            drawLine(lineColor, Offset(size.width - 85.dp.toPx(), 80.dp.toPx()), 
                    Offset(size.width - 120.dp.toPx(), 95.dp.toPx()), strokeW)
            // RL
            drawLine(lineColor, Offset(85.dp.toPx(), size.height - 80.dp.toPx()), 
                    Offset(120.dp.toPx(), size.height - 95.dp.toPx()), strokeW)
            // RR
            drawLine(lineColor, Offset(size.width - 85.dp.toPx(), size.height - 80.dp.toPx()), 
                    Offset(size.width - 120.dp.toPx(), size.height - 95.dp.toPx()), strokeW)
        }
    }
}

@Composable
private fun TyreButton(
    tyre: TyreInfo,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    
    val scale by animateFloatAsState(
        targetValue = when {
            isPressed -> 0.9f
            isSelected -> 1.15f
            else -> 1f
        },
        animationSpec = spring(stiffness = Spring.StiffnessMedium),
        label = "tyreScale"
    )
    
    val shadowElevation by animateDpAsState(
        targetValue = if (isSelected) 12.dp else 4.dp,
        label = "tyreShadow"
    )
    
    val borderColor by animateColorAsState(
        targetValue = if (isSelected) tyre.status.color else Color.LightGray,
        label = "tyreBorder"
    )
    
    val bgColor by animateColorAsState(
        targetValue = if (isSelected) tyre.status.color else Color.White,
        label = "tyreBg"
    )

    // Pulse animation for critical tyres
    val infiniteTransition = rememberInfiniteTransition(label = "criticalPulse")
    val criticalPulse by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = if (tyre.status == TyreHealthStatus.CRITICAL) 1.1f else 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(500, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse"
    )

    Box(
        modifier = modifier
            .scale(scale * criticalPulse)
            .shadow(
                elevation = shadowElevation,
                shape = RoundedCornerShape(12.dp),
                spotColor = tyre.status.color.copy(alpha = 0.5f)
            )
            .clip(RoundedCornerShape(12.dp))
            .background(bgColor)
            .border(2.dp, borderColor, RoundedCornerShape(12.dp))
            .clickable(
                interactionSource = interactionSource,
                indication = null
            ) { onClick() }
            .size(width = 52.dp, height = 72.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Mini tyre icon
            Box(
                modifier = Modifier
                    .size(24.dp)
                    .background(
                        if (isSelected) Color.White.copy(alpha = 0.3f) else Color.LightGray.copy(alpha = 0.3f),
                        CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .size(12.dp)
                        .background(
                            if (isSelected) Color.White else tyre.status.color,
                            CircleShape
                        )
                )
            }
            
            Spacer(modifier = Modifier.height(6.dp))
            
            Text(
                text = tyre.id,
                color = if (isSelected) Color.White else DeepDark,
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp
            )
        }
    }
}

// ============ TYRE DETAIL CARD ============

@Composable
private fun TyreDetailCard(tyre: TyreInfo) {
    GlassCard(
        modifier = Modifier
            .fillMaxWidth()
            .height(180.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // LEFT SIDE - Primary Stats
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.Center
            ) {
                // Position Label
                Text(
                    text = tyre.position,
                    color = SoftGray,
                    fontSize = 13.sp
                )
                
                Spacer(modifier = Modifier.height(4.dp))
                
                // Pressure (Big Number)
                Row(
                    verticalAlignment = Alignment.Bottom
                ) {
                    Text(
                        text = tyre.pressure.toString(),
                        fontSize = 42.sp,
                        fontWeight = FontWeight.Bold,
                        color = DeepDark
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "PSI",
                        fontSize = 16.sp,
                        color = SoftGray,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                }
                
                Spacer(modifier = Modifier.height(12.dp))
                
                // Status Badge
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .background(
                            tyre.status.color.copy(alpha = 0.15f),
                            RoundedCornerShape(50)
                        )
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .background(tyre.status.color, CircleShape)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = tyre.status.label,
                        color = tyre.status.color,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 13.sp
                    )
                }
            }

            // DIVIDER
            Box(
                modifier = Modifier
                    .width(1.dp)
                    .height(80.dp)
                    .background(Color.LightGray.copy(alpha = 0.3f))
            )

            // RIGHT SIDE - Secondary Stats
            Column(
                modifier = Modifier
                    .weight(0.8f)
                    .padding(start = 20.dp),
                verticalArrangement = Arrangement.Center
            ) {
                // Temperature
                StatItem(
                    icon = Icons.Outlined.Thermostat,
                    label = "Temp",
                    value = "${tyre.temperature}°C",
                    isWarning = tyre.temperature > 32
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // Tread Depth
                StatItem(
                    icon = Icons.Outlined.Speed,
                    label = "Tread",
                    value = "${tyre.treadDepth}mm",
                    isWarning = tyre.treadDepth < 4f
                )
            }
        }
    }
}

@Composable
private fun StatItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    value: String,
    isWarning: Boolean = false
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(
            icon,
            contentDescription = null,
            tint = if (isWarning) DangerRed else SoftGray,
            modifier = Modifier.size(18.dp)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Column {
            Text(
                text = label,
                color = SoftGray,
                fontSize = 11.sp
            )
            Text(
                text = value,
                color = if (isWarning) DangerRed else DeepDark,
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp
            )
        }
    }
}

// ============ QUICK ACTIONS ============

@Composable
private fun QuickActionsRow() {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        QuickActionCard(
            icon = Icons.Default.ViewInAr,
            label = "3D View",
            color = NeonViolet,
            modifier = Modifier.weight(1f)
        )
        QuickActionCard(
            icon = Icons.Default.History,
            label = "History",
            color = Color(0xFF00BCD4),
            modifier = Modifier.weight(1f)
        )
        QuickActionCard(
            icon = Icons.Default.Build,
            label = "Service",
            color = Color(0xFFFF9800),
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun QuickActionCard(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    color: Color,
    modifier: Modifier = Modifier
) {
    GlassCard(modifier = modifier.height(80.dp)) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .clickable { }
                .padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                icon,
                contentDescription = null,
                tint = color,
                modifier = Modifier.size(28.dp)
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = label,
                color = DeepDark,
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

// ============ PULSING CAMERA FAB ============

@Composable
private fun PulsingCameraFAB(
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "fab_pulse")
    
    val pulse by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.08f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = EaseInOutSine),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse"
    )
    
    val glowAlpha by infiniteTransition.animateFloat(
        initialValue = 0.4f,
        targetValue = 0.8f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = EaseInOutSine),
            repeatMode = RepeatMode.Reverse
        ),
        label = "glow"
    )

    Box(
        modifier = modifier
            .scale(pulse)
            .size(76.dp)
            .shadow(
                elevation = 20.dp,
                shape = CircleShape,
                spotColor = NeonViolet.copy(alpha = glowAlpha)
            )
            .background(
                brush = Brush.linearGradient(
                    colors = listOf(NeonViolet, ElectricPurple)
                ),
                shape = CircleShape
            )
            .border(
                width = 3.dp,
                brush = Brush.linearGradient(
                    colors = listOf(
                        Color.White.copy(alpha = 0.4f),
                        Color.White.copy(alpha = 0.1f)
                    )
                ),
                shape = CircleShape
            )
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Icon(
            Icons.Default.CameraAlt,
            contentDescription = "Scan Tyre",
            tint = Color.White,
            modifier = Modifier.size(32.dp)
        )
    }
}

// ============ GLASSMORPHIC BOTTOM BAR ============

@Composable
private fun GlassmorphicBottomBar(
    onHomeClick: () -> Unit,
    onAnalyticsClick: () -> Unit,
    selectedTab: Int
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(85.dp)
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Color.Transparent,
                        Color.White.copy(alpha = 0.95f)
                    )
                )
            )
    ) {
        // Glass bar content
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter)
                .padding(horizontal = 40.dp, vertical = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Home
            BottomNavItem(
                icon = Icons.Default.Home,
                label = "Home",
                isSelected = selectedTab == 0,
                onClick = onHomeClick
            )

            // Spacer for FAB
            Spacer(modifier = Modifier.width(80.dp))

            // Analytics
            BottomNavItem(
                icon = Icons.Default.Analytics,
                label = "Analysis",
                isSelected = selectedTab == 1,
                onClick = onAnalyticsClick,
                hasBadge = true
            )
        }
    }
}

@Composable
private fun BottomNavItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    hasBadge: Boolean = false
) {
    val color by animateColorAsState(
        targetValue = if (isSelected) NeonViolet else SoftGray,
        label = "navColor"
    )
    
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.clickable { onClick() }
    ) {
        Box {
            Icon(
                icon,
                contentDescription = label,
                tint = color,
                modifier = Modifier.size(26.dp)
            )
            if (hasBadge) {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .offset(x = 4.dp, y = (-2).dp)
                        .size(10.dp)
                        .background(DangerRed, CircleShape)
                        .border(1.5.dp, Color.White, CircleShape)
                )
            }
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = label,
            color = color,
            fontSize = 11.sp,
            fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal
        )
    }
}

// ============ PREVIEW ============

@Composable
@Preview
private fun PremiumDashboardPreview() {
    MaterialTheme {
        PremiumDashboardScreen()
    }
}
