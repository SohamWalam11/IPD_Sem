package org.example.project

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.Circle
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.jetbrains.compose.ui.tooling.preview.Preview
import org.example.project.components.TyrePressureOverviewCard
import org.example.project.components.TyrePressureData
import org.example.project.components.TyrePosition
import org.example.project.components.TyreHealthStatus
import org.example.project.components.CarBodyType
import org.example.project.components.MotionTyreDetail
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

// ============ PREMIUM THEME COLORS ============
private val PrimaryViolet = Color(0xFF6200EA)
private val SecondaryPurple = Color(0xFFBB86FC)
private val DeepViolet = Color(0xFF3700B3)
private val DarkViolet = Color(0xFF4A148C)
private val LightLavender = Color(0xFFF3E5F5)

// Status Colors
private val GoodGreen = Color(0xFF4CAF50)
private val WarningYellow = Color(0xFFFFC107)
private val DangerRed = Color(0xFFE53935)

private val BrandGradient = Brush.horizontalGradient(
    colors = listOf(PrimaryViolet, DeepViolet)
)

private val BackgroundGradient = Brush.verticalGradient(
    colors = listOf(LightLavender, Color.White, Color(0xFFFAFAFA))
)

// ============ DATA MODELS ============

data class TyreData(
    val id: String,
    val name: String,
    val psi: Float,
    val temp: Int,
    val status: TyreStatus,
    val treadDepth: Float = 7.5f // mm
)

enum class TyreStatus(val label: String, val color: Color) {
    GOOD("Good", GoodGreen),
    WARNING("Attention", WarningYellow),
    CRITICAL("Check Required", DangerRed)
}

data class ServiceCenter(
    val name: String,
    val distance: String,
    val rating: Float
)

data class AnalysisReport(
    val date: String,
    val time: String,
    val title: String,
    val score: Int
)

// Fallback sample data (used only when no BLE sensor connected)
private val sampleTyres = listOf(
    TyreData("FL", "Front Left", 32.5f, 28, TyreStatus.GOOD),
    TyreData("FR", "Front Right", 32.0f, 29, TyreStatus.GOOD),
    TyreData("RL", "Rear Left", 31.5f, 28, TyreStatus.WARNING, treadDepth = 4.2f),
    TyreData("RR", "Rear Right", 26.0f, 35, TyreStatus.CRITICAL, treadDepth = 3.1f)
)

private val sampleCenters = listOf(
    ServiceCenter("Speedy Tyres", "2.5 km", 4.5f),
    ServiceCenter("Urban Wheel Care", "4.1 km", 4.2f),
    ServiceCenter("Highway Pit Stop", "5.8 km", 4.8f)
)

private val sampleReports = listOf(
    AnalysisReport("14 Jan", "10:00 AM", "Overall Report", 85)
)

// ============ MAIN DASHBOARD SCREEN ============

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun DashboardScreen(
    userName: String = "Soham",
    modifier: Modifier = Modifier,
    liveTyreData: List<TyreData>? = null,  // Dynamic BLE data — null = use fallback
    onCameraClick: () -> Unit = {},
    onTyreClick: (TyreData) -> Unit = {},
    onView3DClick: () -> Unit = {},
    onServiceCenterClick: (ServiceCenter) -> Unit = {},
    onAnalysisClick: () -> Unit = {},
    onFindServiceCenter: () -> Unit = {},
    onTpmsClick: () -> Unit = {},
    onTreadScanClick: () -> Unit = {}
) {
    val scope = rememberCoroutineScope()
    
    // Use live BLE data when available, otherwise fall back to static sample
    val activeTyres = liveTyreData ?: sampleTyres
    val isLive = liveTyreData != null
    
    // State for selected tyre detail (MotionLayout-style detail view)
    var selectedTyreForDetail by remember { mutableStateOf<TyrePressureData?>(null) }
    
    // Convert active tyres to TyrePressureData for the overview component
    val tyrePressureDataList = remember(activeTyres) {
        activeTyres.map { tyre ->
            TyrePressureData(
                position = when (tyre.id) {
                    "FL" -> TyrePosition.FRONT_LEFT
                    "FR" -> TyrePosition.FRONT_RIGHT
                    "RL" -> TyrePosition.REAR_LEFT
                    "RR" -> TyrePosition.REAR_RIGHT
                    else -> TyrePosition.FRONT_LEFT
                },
                pressure = tyre.psi,
                temperature = tyre.temp,
                treadDepth = tyre.treadDepth,
                status = when (tyre.status) {
                    TyreStatus.GOOD -> TyreHealthStatus.GOOD
                    TyreStatus.WARNING -> TyreHealthStatus.WARNING
                    TyreStatus.CRITICAL -> TyreHealthStatus.CRITICAL
                },
                defects = if (tyre.status == TyreStatus.CRITICAL) listOf("Low pressure", "Wear detected") else emptyList()
            )
        }
    }
    
    // Calculate overall health score dynamically
    val healthScore = remember(activeTyres) {
        if (activeTyres.isEmpty()) 0
        else {
            val goodCount = activeTyres.count { it.status == TyreStatus.GOOD }
            val warningCount = activeTyres.count { it.status == TyreStatus.WARNING }
            val criticalCount = activeTyres.count { it.status == TyreStatus.CRITICAL }
            ((goodCount * 100 + warningCount * 60 + criticalCount * 20) / activeTyres.size)
        }
    }
    
    val issueCount = activeTyres.count { it.status != TyreStatus.GOOD }

    Box(modifier = modifier.fillMaxSize()) {
        Scaffold(
            containerColor = Color.Transparent,
            bottomBar = { 
                PremiumBottomNavBar(
                    onCameraClick = onCameraClick,
                    onAnalysisClick = onAnalysisClick
                )
            }
        ) { innerPadding ->
            Box(modifier = Modifier.fillMaxSize()) {
                // Background
                DashboardBackground()

                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding),
                    contentPadding = PaddingValues(bottom = 24.dp)
                ) {
                    // 1. Header Section
                    item { 
                        DashboardHeader(
                            userName = userName,
                            notificationCount = issueCount
                        ) 
                    }

                    // 2. Animated Health Score Card
                    item { 
                        AnimatedHealthScoreCard(score = healthScore) 
                    }
                    
                    // 2.5. Tyre Pressure Overview (Rivian-style car view)
                    item {
                        Spacer(modifier = Modifier.height(16.dp))
                        TyrePressureOverviewCard(
                            tyreData = tyrePressureDataList,
                            carBodyType = CarBodyType.SUV,
                            lastReading = if (isLive) "Live" else "14:15",
                            onTyreClick = { tyre ->
                                selectedTyreForDetail = tyre
                            }
                        )
                    }

                    // 3. Tyre Overview Carousel
                    item { 
                        TyreOverviewSection(
                            tyres = activeTyres,
                            onTyreClick = onTyreClick,
                            onView3DClick = onView3DClick
                        ) 
                    }

                    // 3.5. TPMS Sensor Card
                    item {
                        TpmsSensorQuickCard(onTpmsClick = onTpmsClick, isLive = isLive)
                    }

                    // 3.6. Anyline Tread Depth Scan Card
                    item {
                        Spacer(modifier = Modifier.height(12.dp))
                        AnylineTreadScanCard(onTreadScanClick = onTreadScanClick)
                    }

                    // 4. Latest Analysis Section
                    item { 
                        SectionHeader(title = "Latest Analysis", action = "Show All >") 
                    }
                    item { 
                        LatestAnalysisCard(report = sampleReports.first()) 
                    }

                    // 5. Service Centers Section (Staggered Animation)
                    item { 
                        ServiceCentersSectionHeader(
                            onFindNearbyClick = onFindServiceCenter
                        ) 
                    }
                    itemsIndexed(sampleCenters) { index, center ->
                        StaggeredServiceCenterCard(
                            center = center,
                            index = index,
                            onClick = { onServiceCenterClick(center) }
                        )
                    }

                    // Extra space for bottom nav
                    item { Spacer(modifier = Modifier.height(32.dp)) }
                }
            }
        }
        
        // MotionLayout-style tyre detail overlay
        selectedTyreForDetail?.let { tyre ->
            MotionTyreDetail(
                tyre = tyre,
                isVisible = true,
                onDismiss = { selectedTyreForDetail = null }
            )
        }
    }
}

// ============ ANIMATED BACKGROUND ============

@Composable
private fun DashboardBackground() {
    val infiniteTransition = rememberInfiniteTransition(label = "bg_anim")
    
    val breathingAlpha by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 0.5f,
        animationSpec = infiniteRepeatable(
            animation = tween(4000, easing = EaseInOutSine),
            repeatMode = RepeatMode.Reverse
        ),
        label = "breathing"
    )

    Canvas(modifier = Modifier.fillMaxSize()) {
        // Base gradient
        drawRect(brush = BackgroundGradient)
        
        // Subtle accent blob top-right
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(
                    SecondaryPurple.copy(alpha = breathingAlpha * 0.15f),
                    Color.Transparent
                ),
                center = Offset(size.width * 0.9f, size.height * 0.1f),
                radius = size.width * 0.4f
            ),
            radius = size.width * 0.4f,
            center = Offset(size.width * 0.9f, size.height * 0.1f)
        )
        
        // Bottom-left accent
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(
                    PrimaryViolet.copy(alpha = breathingAlpha * 0.08f),
                    Color.Transparent
                ),
                center = Offset(size.width * 0.1f, size.height * 0.85f),
                radius = size.width * 0.5f
            ),
            radius = size.width * 0.5f,
            center = Offset(size.width * 0.1f, size.height * 0.85f)
        )
    }
}

// ============ HEADER ============

@Composable
private fun DashboardHeader(
    userName: String,
    notificationCount: Int
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 16.dp)
            .padding(top = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            // Profile Avatar with gradient border
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .background(
                        brush = BrandGradient,
                        shape = CircleShape
                    )
                    .padding(2.dp)
                    .background(Color.White, CircleShape)
                    .padding(2.dp)
                    .background(LightLavender, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = userName.first().toString(),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = PrimaryViolet
                )
            }
            
            Spacer(modifier = Modifier.width(12.dp))
            
            Column {
                Text(
                    "Hello,",
                    color = Color.Gray,
                    fontSize = 14.sp
                )
                Text(
                    userName,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = DarkViolet
                )
            }
        }

        // Notification Bell with Badge
        IconButton(
            onClick = { },
            modifier = Modifier
                .size(48.dp)
                .background(Color.White, RoundedCornerShape(12.dp))
                .shadow(2.dp, RoundedCornerShape(12.dp))
        ) {
            BadgedBox(
                badge = {
                    if (notificationCount > 0) {
                        Badge(
                            containerColor = DangerRed,
                            contentColor = Color.White
                        ) {
                            Text(notificationCount.toString())
                        }
                    }
                }
            ) {
                Icon(
                    Icons.Default.Notifications,
                    contentDescription = "Notifications",
                    tint = DarkViolet
                )
            }
        }
    }
}

// ============ ANIMATED HEALTH SCORE CARD ============

@Composable
private fun AnimatedHealthScoreCard(score: Int) {
    var animatedScore by remember { mutableStateOf(0f) }
    var isVisible by remember { mutableStateOf(false) }
    
    LaunchedEffect(Unit) {
        isVisible = true
        animate(
            initialValue = 0f,
            targetValue = score.toFloat(),
            animationSpec = tween(durationMillis = 1500, easing = FastOutSlowInEasing)
        ) { value, _ -> animatedScore = value }
    }

    val scoreColor = when {
        animatedScore >= 80 -> GoodGreen
        animatedScore >= 50 -> WarningYellow
        else -> DangerRed
    }

    AnimatedVisibility(
        visible = isVisible,
        enter = slideInVertically(initialOffsetY = { -50 }) + fadeIn()
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 12.dp),
            shape = RoundedCornerShape(24.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        "Overall Health Score",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = DarkViolet
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        "Live snapshot of your tyre health.",
                        color = Color.Gray,
                        fontSize = 14.sp
                    )
                }
                
                Spacer(modifier = Modifier.width(16.dp))

                // Circular Progress
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier.size(100.dp)
                ) {
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        val strokeWidth = 12.dp.toPx()
                        val radius = (size.minDimension - strokeWidth) / 2
                        val center = Offset(size.width / 2, size.height / 2)

                        // Background Circle
                        drawCircle(
                            color = Color(0xFFE0E0E0),
                            radius = radius,
                            center = center,
                            style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                        )

                        // Progress Arc
                        drawArc(
                            color = scoreColor,
                            startAngle = -90f,
                            sweepAngle = 360f * (animatedScore / 100f),
                            useCenter = false,
                            topLeft = Offset(center.x - radius, center.y - radius),
                            size = androidx.compose.ui.geometry.Size(radius * 2, radius * 2),
                            style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                        )
                    }
                    
                    Text(
                        text = "${animatedScore.toInt()}%",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        color = scoreColor
                    )
                }
            }
        }
    }
}

// ============ TYRE OVERVIEW SECTION ============

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun TyreOverviewSection(
    tyres: List<TyreData>,
    onTyreClick: (TyreData) -> Unit,
    onView3DClick: () -> Unit
) {
    val pagerState = rememberPagerState(pageCount = { tyres.size })
    val scope = rememberCoroutineScope()
    val issueCount = tyres.count { it.status != TyreStatus.GOOD }

    Column(modifier = Modifier.padding(vertical = 16.dp)) {
        // Section Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    "Tyre Overview",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = DarkViolet
                )
                
                if (issueCount > 0) {
                    Spacer(modifier = Modifier.width(8.dp))
                    Box(
                        modifier = Modifier
                            .size(22.dp)
                            .background(DangerRed, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            issueCount.toString(),
                            color = Color.White,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
            
            // 3D View Button
            OutlinedButton(
                onClick = onView3DClick,
                shape = RoundedCornerShape(50),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = PrimaryViolet),
                border = androidx.compose.foundation.BorderStroke(1.dp, PrimaryViolet.copy(alpha = 0.5f)),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
            ) {
                Icon(
                    Icons.Default.ViewInAr,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text("View 3D", fontSize = 12.sp, fontWeight = FontWeight.Medium)
            }
        }

        Text(
            "Swipe to explore • Tap for details",
            color = Color.Gray,
            fontSize = 12.sp,
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 4.dp)
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Tyre Carousel
        HorizontalPager(
            state = pagerState,
            contentPadding = PaddingValues(horizontal = 32.dp),
            pageSpacing = 16.dp,
            modifier = Modifier.fillMaxWidth()
        ) { page ->
            TyreDetailCard(
                tyre = tyres[page],
                isSelected = pagerState.currentPage == page,
                onClick = { onTyreClick(tyres[page]) }
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Page Indicators (Dots)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center
        ) {
            repeat(tyres.size) { index ->
                val isSelected = pagerState.currentPage == index
                Box(
                    modifier = Modifier
                        .padding(horizontal = 3.dp)
                        .size(if (isSelected) 10.dp else 6.dp)
                        .clip(CircleShape)
                        .background(
                            if (isSelected) PrimaryViolet 
                            else Color.LightGray
                        )
                        .animateContentSize()
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Tyre Position Chips
        LazyRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center,
            contentPadding = PaddingValues(horizontal = 20.dp)
        ) {
            itemsIndexed(tyres) { index, tyre ->
                val isSelected = pagerState.currentPage == index
                
                TyrePositionChip(
                    tyre = tyre,
                    isSelected = isSelected,
                    onClick = {
                        scope.launch {
                            pagerState.animateScrollToPage(index)
                        }
                    }
                )
            }
        }
    }
}

@Composable
private fun TyrePositionChip(
    tyre: TyreData,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val bgColor by animateColorAsState(
        targetValue = if (isSelected) PrimaryViolet.copy(alpha = 0.1f) else Color.Transparent,
        label = "chipBg"
    )
    val borderColor by animateColorAsState(
        targetValue = if (isSelected) PrimaryViolet else Color.LightGray,
        label = "chipBorder"
    )

    Box(
        modifier = Modifier
            .padding(horizontal = 4.dp)
            .border(1.5.dp, borderColor, RoundedCornerShape(50))
            .background(bgColor, RoundedCornerShape(50))
            .clickable { onClick() }
            .padding(horizontal = 14.dp, vertical = 8.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(10.dp)
                    .background(tyre.status.color, CircleShape)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                tyre.id,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = if (isSelected) PrimaryViolet else Color.Gray
            )
        }
    }
}

// ============ TYRE DETAIL CARD ============

@Composable
private fun TyreDetailCard(
    tyre: TyreData,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    // Wobble animation for problem tyres
    val infiniteTransition = rememberInfiniteTransition(label = "wobble")
    val wobble by infiniteTransition.animateFloat(
        initialValue = -2f,
        targetValue = 2f,
        animationSpec = infiniteRepeatable(
            animation = tween(80, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "wobbleAngle"
    )
    
    // Tyre rotation animation
    val tyreRotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(3000, easing = LinearEasing)
        ),
        label = "tyreRotation"
    )

    val cardRotation = if (tyre.status == TyreStatus.CRITICAL) wobble else 0f
    val cardElevation by animateDpAsState(
        targetValue = if (isSelected) 12.dp else 4.dp,
        label = "elevation"
    )

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(200.dp)
            .graphicsLayer(rotationZ = cardRotation)
            .clickable { onClick() },
        shape = RoundedCornerShape(24.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = cardElevation),
        colors = CardDefaults.cardColors(
            containerColor = if (tyre.status == TyreStatus.CRITICAL) 
                Color(0xFFFFF5F5) else Color.White
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(20.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Animated Tyre Visual
            Box(
                modifier = Modifier
                    .size(120.dp)
                    .border(
                        width = 4.dp,
                        color = tyre.status.color.copy(alpha = 0.3f),
                        shape = CircleShape
                    )
                    .padding(8.dp),
                contentAlignment = Alignment.Center
            ) {
                AnimatedTyreIcon(
                    rotation = tyreRotation,
                    status = tyre.status,
                    modifier = Modifier.size(100.dp)
                )
            }

            Spacer(modifier = Modifier.width(20.dp))

            Column(
                verticalArrangement = Arrangement.Center,
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    tyre.name,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = DarkViolet
                )
                
                Spacer(modifier = Modifier.height(12.dp))

                // Pressure
                TyreStatRow(
                    icon = Icons.Default.Speed,
                    label = "Pressure",
                    value = "${tyre.psi} PSI",
                    isWarning = tyre.psi < 28f
                )
                
                Spacer(modifier = Modifier.height(6.dp))

                // Temperature
                TyreStatRow(
                    icon = Icons.Default.Thermostat,
                    label = "Temp",
                    value = "${tyre.temp}°C",
                    isWarning = tyre.temp > 32
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Status Badge
                Box(
                    modifier = Modifier
                        .background(
                            tyre.status.color.copy(alpha = 0.15f),
                            RoundedCornerShape(50)
                        )
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .background(tyre.status.color, CircleShape)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            tyre.status.label,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = tyre.status.color
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))
                
                Text(
                    "🎤 Tap for details",
                    fontSize = 11.sp,
                    color = Color.Gray
                )
            }
        }
    }
}

@Composable
private fun TyreStatRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    value: String,
    isWarning: Boolean = false
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.padding(vertical = 2.dp)
    ) {
        Icon(
            icon,
            contentDescription = null,
            tint = if (isWarning) DangerRed else Color.Gray,
            modifier = Modifier.size(16.dp)
        )
        Spacer(modifier = Modifier.width(6.dp))
        Text(
            label,
            fontSize = 12.sp,
            color = Color.Gray
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            value,
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            color = if (isWarning) DangerRed else DarkViolet
        )
    }
}

// ============ ANIMATED TYRE ICON ============

@Composable
private fun AnimatedTyreIcon(
    rotation: Float,
    status: TyreStatus,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier) {
        val centerX = size.width / 2
        val centerY = size.height / 2
        val outerRadius = size.minDimension / 2 - 4f
        val rimRadius = outerRadius * 0.65f
        val hubRadius = outerRadius * 0.32f

        rotate(rotation, pivot = Offset(centerX, centerY)) {
            // Outer tyre (black rubber)
            drawCircle(
                color = Color(0xFF1A1A1A),
                radius = outerRadius,
                center = Offset(centerX, centerY)
            )

            // Tread patterns
            for (i in 0 until 12) {
                val angle = (i * 30f) * (PI / 180f)
                val startX = centerX + cos(angle).toFloat() * rimRadius
                val startY = centerY + sin(angle).toFloat() * rimRadius
                val endX = centerX + cos(angle).toFloat() * (outerRadius - 3f)
                val endY = centerY + sin(angle).toFloat() * (outerRadius - 3f)

                drawLine(
                    color = Color(0xFF0D0D0D),
                    start = Offset(startX, startY),
                    end = Offset(endX, endY),
                    strokeWidth = 4f,
                    cap = StrokeCap.Round
                )
            }

            // Rim (silver)
            drawCircle(
                color = Color(0xFFD0D0D0),
                radius = rimRadius,
                center = Offset(centerX, centerY)
            )

            // Rim spokes
            for (i in 0 until 5) {
                val angle = (i * 72f) * (PI / 180f)
                val startX = centerX + cos(angle).toFloat() * (hubRadius + 4f)
                val startY = centerY + sin(angle).toFloat() * (hubRadius + 4f)
                val endX = centerX + cos(angle).toFloat() * (rimRadius - 4f)
                val endY = centerY + sin(angle).toFloat() * (rimRadius - 4f)

                drawLine(
                    color = Color(0xFFA0A0A0),
                    start = Offset(startX, startY),
                    end = Offset(endX, endY),
                    strokeWidth = 6f,
                    cap = StrokeCap.Round
                )
            }

            // Center hub with status color
            drawCircle(
                color = status.color,
                radius = hubRadius,
                center = Offset(centerX, centerY)
            )
            
            // Hub highlight
            drawCircle(
                color = Color.White.copy(alpha = 0.3f),
                radius = hubRadius * 0.5f,
                center = Offset(centerX - 3f, centerY - 3f)
            )
        }

        // Outer ring (white border)
        drawCircle(
            color = Color.White,
            radius = outerRadius,
            center = Offset(centerX, centerY),
            style = Stroke(width = 3f)
        )
    }
}

// ============ SECTION HEADERS ============

@Composable
private fun SectionHeader(
    title: String,
    action: String? = null
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            title,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = DarkViolet
        )
        if (action != null) {
            Text(
                action,
                color = PrimaryViolet,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.clickable { }
            )
        }
    }
}

@Composable
private fun ServiceCentersSectionHeader(
    onFindNearbyClick: () -> Unit = {}
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            "Nearest Service Centers",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = DarkViolet
        )
        Spacer(modifier = Modifier.weight(1f))
        
        // Find Nearby button with Google Maps
        Surface(
            onClick = onFindNearbyClick,
            shape = RoundedCornerShape(20.dp),
            color = PrimaryViolet.copy(alpha = 0.1f)
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    Icons.Default.LocationOn,
                    contentDescription = null,
                    tint = PrimaryViolet,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    "Find Nearby",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    color = PrimaryViolet
                )
            }
        }
    }
}

// ============ LATEST ANALYSIS CARD ============

@Composable
private fun LatestAnalysisCard(report: AnalysisReport) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 8.dp)
            .clickable { },
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text(
                    report.date,
                    color = PrimaryViolet,
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    report.title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = DarkViolet
                )
                Text(
                    report.time,
                    color = Color.Gray,
                    fontSize = 12.sp
                )
            }

            Box(
                modifier = Modifier
                    .size(56.dp)
                    .background(
                        GoodGreen.copy(alpha = 0.15f),
                        CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    "${report.score}%",
                    color = GoodGreen,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )
            }
        }
    }
}

// ============ STAGGERED SERVICE CENTER CARD ============

@Composable
private fun StaggeredServiceCenterCard(
    center: ServiceCenter,
    index: Int,
    onClick: () -> Unit
) {
    var isVisible by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        delay(index * 120L) // Staggered delay
        isVisible = true
    }

    AnimatedVisibility(
        visible = isVisible,
        enter = slideInHorizontally(initialOffsetX = { it }) + fadeIn(
            animationSpec = tween(400)
        )
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 6.dp)
                .clickable { onClick() },
            shape = RoundedCornerShape(16.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        center.name,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        color = DarkViolet
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            center.distance,
                            color = Color.Gray,
                            fontSize = 13.sp
                        )
                        Text(" • ", color = Color.Gray)
                        Text(
                            center.rating.toString(),
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            color = DarkViolet
                        )
                        Icon(
                            Icons.Default.Star,
                            contentDescription = null,
                            tint = WarningYellow,
                            modifier = Modifier.size(14.dp)
                        )
                    }
                }

                Row {
                    // Call Button
                    IconButton(
                        onClick = { },
                        modifier = Modifier
                            .size(44.dp)
                            .background(
                                PrimaryViolet.copy(alpha = 0.1f),
                                RoundedCornerShape(12.dp)
                            )
                    ) {
                        Icon(
                            Icons.Default.Call,
                            contentDescription = "Call",
                            tint = PrimaryViolet,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    
                    Spacer(modifier = Modifier.width(8.dp))
                    
                    // Directions Button
                    IconButton(
                        onClick = { },
                        modifier = Modifier
                            .size(44.dp)
                            .background(
                                PrimaryViolet.copy(alpha = 0.1f),
                                RoundedCornerShape(12.dp)
                            )
                    ) {
                        Icon(
                            Icons.Default.Directions,
                            contentDescription = "Directions",
                            tint = PrimaryViolet,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }
        }
    }
}

// ============ TPMS SENSOR QUICK CARD ============

@Composable
private fun TpmsSensorQuickCard(onTpmsClick: () -> Unit = {}, isLive: Boolean = false) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .clickable { onTpmsClick() },
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFF1A1A2E)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // BLE Icon
            Box(
                modifier = Modifier
                    .size(52.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(
                        Brush.linearGradient(
                            colors = if (isLive) listOf(Color(0xFF2E7D32), Color(0xFF1B5E20))
                            else listOf(PrimaryViolet, DeepViolet)
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    if (isLive) Icons.Default.BluetoothConnected else Icons.Default.Bluetooth,
                    contentDescription = "TPMS",
                    tint = Color.White,
                    modifier = Modifier.size(28.dp)
                )
            }

            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "TPMS Sensors",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp
                    )
                    if (isLive) {
                        Spacer(modifier = Modifier.width(8.dp))
                        Box(
                            modifier = Modifier
                                .background(Color(0xFF4CAF50), RoundedCornerShape(6.dp))
                                .padding(horizontal = 8.dp, vertical = 2.dp)
                        ) {
                            Text("LIVE", color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.ExtraBold)
                        }
                    }
                }
                Text(
                    text = if (isLive) "Receiving live pressure data" else "Tap to connect BLE sensors",
                    color = Color.White.copy(alpha = 0.6f),
                    fontSize = 13.sp
                )
            }

            Icon(
                Icons.Default.ChevronRight,
                contentDescription = null,
                tint = Color.White.copy(alpha = 0.5f),
                modifier = Modifier.size(24.dp)
            )
        }
    }
}

// ============ ANYLINE TREAD SCAN QUICK CARD ============

@Composable
private fun AnylineTreadScanCard(onTreadScanClick: () -> Unit = {}) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .clickable { onTreadScanClick() },
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFF1A1A2E)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Icon
            Box(
                modifier = Modifier
                    .size(52.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(
                        Brush.linearGradient(
                            colors = listOf(Color(0xFFE53935), Color(0xFFB71C1C))
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Default.Scanner,
                    contentDescription = "Tread Scan",
                    tint = Color.White,
                    modifier = Modifier.size(28.dp)
                )
            }

            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "Tread Depth Scanner",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Box(
                        modifier = Modifier
                            .background(Color(0xFFE53935), RoundedCornerShape(6.dp))
                            .padding(horizontal = 8.dp, vertical = 2.dp)
                    ) {
                        Text("AI", color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.ExtraBold)
                    }
                }
                Text(
                    text = "Measure tyre tread via Anyline SDK",
                    color = Color.White.copy(alpha = 0.6f),
                    fontSize = 13.sp
                )
            }

            Icon(
                Icons.Default.ChevronRight,
                contentDescription = null,
                tint = Color.White.copy(alpha = 0.5f),
                modifier = Modifier.size(24.dp)
            )
        }
    }
}

// ============ PREMIUM BOTTOM NAV BAR ============

@Composable
private fun PremiumBottomNavBar(
    onCameraClick: () -> Unit,
    onAnalysisClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(80.dp)
    ) {
        // Background Navigation Bar using Row for proper spacing
        Surface(
            color = Color.White,
            tonalElevation = 8.dp,
            shadowElevation = 8.dp,
            modifier = Modifier
                .fillMaxWidth()
                .height(64.dp)
                .align(Alignment.BottomCenter)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Home
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .weight(1f)
                        .clickable { }
                        .padding(vertical = 8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .background(
                                color = PrimaryViolet.copy(alpha = 0.1f),
                                shape = RoundedCornerShape(12.dp)
                            )
                            .padding(horizontal = 16.dp, vertical = 4.dp)
                    ) {
                        Icon(
                            Icons.Default.Home,
                            contentDescription = null,
                            modifier = Modifier.size(24.dp),
                            tint = PrimaryViolet
                        )
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text("Home", fontSize = 11.sp, color = PrimaryViolet)
                }

                // Spacer for floating button
                Spacer(modifier = Modifier.width(80.dp))

                // Analysis
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .weight(1f)
                        .clickable { onAnalysisClick() }
                        .padding(vertical = 8.dp)
                ) {
                    BadgedBox(
                        badge = {
                            Badge(
                                containerColor = DangerRed,
                                contentColor = Color.White
                            ) {
                                Text("3")
                            }
                        }
                    ) {
                        Icon(
                            Icons.Default.Analytics,
                            contentDescription = null,
                            modifier = Modifier.size(24.dp),
                            tint = Color.Gray
                        )
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text("Analysis", fontSize = 11.sp, color = Color.Gray)
                }
            }
        }

        // Floating Camera Button
        Box(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .offset(y = (-12).dp)
                .size(68.dp)
                .shadow(12.dp, CircleShape)
                .background(
                    brush = Brush.linearGradient(
                        colors = listOf(PrimaryViolet, SecondaryPurple)
                    ),
                    shape = CircleShape
                )
                .clickable { onCameraClick() },
            contentAlignment = Alignment.Center
        ) {
            // Outer ring
            Box(
                modifier = Modifier
                    .size(60.dp)
                    .border(2.dp, Color.White.copy(alpha = 0.3f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Default.CameraAlt,
                    contentDescription = "Scan Tyre",
                    tint = Color.White,
                    modifier = Modifier.size(28.dp)
                )
            }
        }
    }
}

// ============ PREVIEW ============

@Composable
@Preview
private fun DashboardScreenPreview() {
    MaterialTheme {
        DashboardScreen()
    }
}
