package org.example.project

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.jetbrains.compose.ui.tooling.preview.Preview
import kotlin.math.PI
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin

// ============ PREMIUM THEME COLORS ============
private val PrimaryViolet = Color(0xFF6200EA)
private val SecondaryPurple = Color(0xFFBB86FC)
private val DeepViolet = Color(0xFF3700B3)
private val DarkViolet = Color(0xFF4A148C)
private val LightLavender = Color(0xFFF3E5F5)

// Plate Colors
private val PlateWhite = Color(0xFFF5F5F5)
private val PlateBlue = Color(0xFF003399)
private val PlateBorder = Color(0xFF1A1A1A)

// Digital Display Colors
private val DigitalCyan = Color(0xFF00E5FF)
private val DashboardDark = Color(0xFF212121)
private val DashboardBorder = Color(0xFF424242)

// Success Colors
private val SuccessGreen = Color(0xFF4CAF50)
private val CheckeredBlack = Color(0xFF1A1A1A)
private val CheckeredWhite = Color(0xFFFAFAFA)

private val BrandGradient = Brush.horizontalGradient(
    colors = listOf(PrimaryViolet, DeepViolet)
)

// ============ MAIN SCREEN ============

@Composable
fun UsageProfileScreen(
    viewModel: SetupViewModel,
    modifier: Modifier = Modifier,
    onSaveProfile: () -> Unit
) {
    // Form State - sync with ViewModel
    var licensePlate by remember { 
        mutableStateOf(
            buildString {
                append(viewModel.plateState)
                append(viewModel.plateDistrict)
                append(viewModel.plateSeries)
                append(viewModel.plateNumber)
            }
        )
    }
    var odometerReading by remember { mutableStateOf(viewModel.odometerReading) }
    var dailyDriveKm by remember { mutableStateOf(viewModel.averageDailyDriveKm) }
    
    // Animation State
    var isFinished by remember { mutableStateOf(false) }
    
    // Background Animation (Subtle breathing)
    val infiniteTransition = rememberInfiniteTransition(label = "background")
    val breathingAlpha by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 0.6f,
        animationSpec = infiniteRepeatable(
            animation = tween(3000, easing = EaseInOutSine),
            repeatMode = RepeatMode.Reverse
        ),
        label = "breathing"
    )

    val isFormValid = licensePlate.length >= 6 && odometerReading.isNotEmpty()

    Scaffold(
        containerColor = Color.Transparent,
        bottomBar = {
            AnimatedVisibility(
                visible = !isFinished,
                exit = slideOutVertically() + fadeOut()
            ) {
                FinishButton(
                    isEnabled = isFormValid,
                    onClick = {
                        // Parse and save to ViewModel
                        val plate = licensePlate.uppercase()
                        if (plate.length >= 10) {
                            viewModel.plateState = plate.take(2)
                            viewModel.plateDistrict = plate.substring(2, 4)
                            viewModel.plateSeries = plate.substring(4, 6)
                            viewModel.plateNumberDigits = plate.substring(6)
                        }
                        viewModel.odometerReading = odometerReading
                        viewModel.averageDailyDriveKm = dailyDriveKm
                        isFinished = true
                    }
                )
            }
        }
    ) { innerPadding ->
        Box(modifier = modifier.fillMaxSize()) {
            // 1. ANIMATED BACKGROUND
            UsageProfileBackground(breathingAlpha = breathingAlpha)

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .verticalScroll(rememberScrollState())
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // 2. TYRE ROAD PROGRESS (Step 3 of 3)
                TyreRoadProgressUsage(currentStep = 3, totalSteps = 3)

                Spacer(modifier = Modifier.height(32.dp))

                // 3. HEADER
                Text(
                    text = "Usage Profile",
                    style = MaterialTheme.typography.headlineMedium.copy(
                        fontWeight = FontWeight.ExtraBold,
                        color = DarkViolet
                    )
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "This helps us predict wear, rotations\nand service reminders.",
                    style = MaterialTheme.typography.bodyMedium.copy(
                        color = Color.Gray,
                        lineHeight = 22.sp
                    ),
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(40.dp))

                // 4. REALISTIC LICENSE PLATE INPUT
                Text(
                    "Vehicle Plate Number",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = DarkViolet
                    )
                )
                Spacer(modifier = Modifier.height(12.dp))
                IndianLicensePlateInput(
                    value = licensePlate,
                    onValueChange = { 
                        if (it.length <= 10) {
                            licensePlate = it.uppercase()
                        }
                    }
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    "Format: MH 02 AB 1234",
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.Gray
                )

                Spacer(modifier = Modifier.height(32.dp))

                // 5. DIGITAL ODOMETER INPUT
                Text(
                    "Current Odometer Reading (km)",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = DarkViolet
                    )
                )
                Spacer(modifier = Modifier.height(12.dp))
                DigitalOdometerInput(
                    value = odometerReading,
                    onValueChange = { 
                        if (it.length <= 7 && it.all { char -> char.isDigit() }) {
                            odometerReading = it
                        }
                    }
                )

                Spacer(modifier = Modifier.height(40.dp))

                // 6. ARC SLIDER (Speedometer Style)
                Text(
                    "Average Daily Drive: ${dailyDriveKm.toInt()} km",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = DarkViolet
                    )
                )
                Spacer(modifier = Modifier.height(24.dp))
                ArcDriveSlider(
                    value = dailyDriveKm,
                    onValueChange = { dailyDriveKm = it },
                    range = 0f..200f
                )

                // Extra space at bottom
                Spacer(modifier = Modifier.height(100.dp))
            }

            // 7. SUCCESS OVERLAY (Checkered Flag Animation)
            AnimatedVisibility(
                visible = isFinished,
                enter = fadeIn(tween(500)) + scaleIn(initialScale = 0.8f),
                modifier = Modifier.fillMaxSize()
            ) {
                CheckeredFlagSuccessOverlay(onComplete = onSaveProfile)
            }
        }
    }
}

// ============ ANIMATED BACKGROUND ============

@Composable
private fun UsageProfileBackground(breathingAlpha: Float) {
    Canvas(modifier = Modifier.fillMaxSize()) {
        val width = size.width
        val height = size.height

        // Base gradient
        drawRect(
            brush = Brush.verticalGradient(
                colors = listOf(LightLavender, Color.White, Color(0xFFF8F4FF))
            )
        )

        // Subtle racing stripe accents
        drawLine(
            color = PrimaryViolet.copy(alpha = breathingAlpha * 0.1f),
            start = Offset(0f, height * 0.3f),
            end = Offset(width, height * 0.25f),
            strokeWidth = 80f
        )
        drawLine(
            color = SecondaryPurple.copy(alpha = breathingAlpha * 0.08f),
            start = Offset(0f, height * 0.7f),
            end = Offset(width, height * 0.75f),
            strokeWidth = 60f
        )
    }
}

// ============ INDIAN LICENSE PLATE INPUT ============

@Composable
private fun IndianLicensePlateInput(
    value: String,
    onValueChange: (String) -> Unit
) {
    Card(
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(containerColor = PlateWhite),
        modifier = Modifier
            .fillMaxWidth()
            .height(72.dp)
            .border(3.dp, PlateBorder, RoundedCornerShape(8.dp))
    ) {
        Row(modifier = Modifier.fillMaxSize()) {
            // The Blue "IND" Strip (Ashoka Chakra)
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .width(48.dp)
                    .background(PlateBlue),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    // Ashoka Chakra (Simplified)
                    AshokaChakra(modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        "IND",
                        color = Color.White,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    )
                }
            }

            // The License Plate Text Field
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(PlateWhite),
                contentAlignment = Alignment.Center
            ) {
                BasicTextField(
                    value = formatLicensePlate(value),
                    onValueChange = { formatted ->
                        // Remove spaces for storage
                        onValueChange(formatted.replace(" ", ""))
                    },
                    textStyle = TextStyle(
                        fontSize = 28.sp,
                        fontWeight = FontWeight.Black,
                        fontFamily = FontFamily.Monospace,
                        color = Color.Black,
                        letterSpacing = 3.sp,
                        textAlign = TextAlign.Center
                    ),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(
                        capitalization = KeyboardCapitalization.Characters,
                        keyboardType = KeyboardType.Text
                    ),
                    decorationBox = { innerTextField ->
                        Box(contentAlignment = Alignment.Center) {
                            if (value.isEmpty()) {
                                Text(
                                    "MH 02 AB 1234",
                                    color = Color.LightGray,
                                    fontSize = 26.sp,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = FontFamily.Monospace,
                                    letterSpacing = 3.sp
                                )
                            }
                            innerTextField()
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}

// Format license plate with spaces: MH02AB1234 -> MH 02 AB 1234
private fun formatLicensePlate(raw: String): String {
    val clean = raw.replace(" ", "").uppercase()
    return buildString {
        clean.forEachIndexed { index, char ->
            append(char)
            // Add spaces after state (2), district (4), series (6)
            if (index == 1 || index == 3 || index == 5) {
                if (index < clean.length - 1) append(" ")
            }
        }
    }
}

// ============ ASHOKA CHAKRA ============

@Composable
private fun AshokaChakra(modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        val center = Offset(size.width / 2, size.height / 2)
        val radius = size.minDimension / 2 - 2f

        // Outer circle
        drawCircle(
            color = Color.White,
            radius = radius,
            center = center,
            style = Stroke(width = 2f)
        )

        // 24 Spokes
        for (i in 0 until 24) {
            val angle = (i * 15f) * (PI / 180f)
            val innerRadius = radius * 0.3f
            val startX = center.x + cos(angle).toFloat() * innerRadius
            val startY = center.y + sin(angle).toFloat() * innerRadius
            val endX = center.x + cos(angle).toFloat() * (radius - 1f)
            val endY = center.y + sin(angle).toFloat() * (radius - 1f)

            drawLine(
                color = Color.White,
                start = Offset(startX, startY),
                end = Offset(endX, endY),
                strokeWidth = 1f
            )
        }

        // Center hub
        drawCircle(
            color = Color.White,
            radius = radius * 0.25f,
            center = center,
            style = Stroke(width = 1.5f)
        )
    }
}

// ============ DIGITAL ODOMETER INPUT ============

@Composable
private fun DigitalOdometerInput(
    value: String,
    onValueChange: (String) -> Unit
) {
    // Infinite animation for the glow effect
    val infiniteTransition = rememberInfiniteTransition(label = "odometer_glow")
    val glowAlpha by infiniteTransition.animateFloat(
        initialValue = 0.5f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = EaseInOutSine),
            repeatMode = RepeatMode.Reverse
        ),
        label = "glow"
    )

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(80.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(DashboardDark)
            .border(2.dp, DashboardBorder, RoundedCornerShape(12.dp)),
        contentAlignment = Alignment.Center
    ) {
        // Subtle scanline effect
        Canvas(modifier = Modifier.fillMaxSize().alpha(0.1f)) {
            for (y in 0 until size.height.toInt() step 4) {
                drawLine(
                    color = Color.White,
                    start = Offset(0f, y.toFloat()),
                    end = Offset(size.width, y.toFloat()),
                    strokeWidth = 1f
                )
            }
        }

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            // Leading zeros display
            val displayValue = value.padStart(6, '0')
            
            Row {
                displayValue.forEachIndexed { index, char ->
                    DigitalDigit(
                        digit = char,
                        glowAlpha = if (index >= 6 - value.length) glowAlpha else 0.3f,
                        isActive = index >= 6 - value.length
                    )
                    if (index == 2) {
                        // Separator after thousands
                        Text(
                            ",",
                            color = DigitalCyan.copy(alpha = 0.5f),
                            fontSize = 28.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.width(8.dp))
            
            Text(
                "km",
                color = Color.Gray,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(top = 6.dp)
            )
        }

        // Hidden input field for keyboard
        BasicTextField(
            value = value,
            onValueChange = onValueChange,
            textStyle = TextStyle(color = Color.Transparent),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            modifier = Modifier
                .fillMaxSize()
                .alpha(0f)
        )
    }
}

@Composable
private fun DigitalDigit(
    digit: Char,
    glowAlpha: Float,
    isActive: Boolean
) {
    Text(
        text = digit.toString(),
        style = TextStyle(
            fontSize = 32.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.Monospace,
            color = if (isActive) DigitalCyan.copy(alpha = glowAlpha) else DigitalCyan.copy(alpha = 0.2f)
        ),
        modifier = Modifier.padding(horizontal = 2.dp)
    )
}

// ============ ARC DRIVE SLIDER (SPEEDOMETER) ============

@Composable
private fun ArcDriveSlider(
    value: Float,
    onValueChange: (Float) -> Unit,
    range: ClosedFloatingPointRange<Float>
) {
    val sweepAngle = 240f
    val startAngle = 150f

    // Needle animation
    val animatedValue by animateFloatAsState(
        targetValue = value,
        animationSpec = spring(stiffness = Spring.StiffnessLow),
        label = "needle"
    )

    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .size(280.dp)
            .pointerInput(Unit) {
                detectDragGestures { change, _ ->
                    val center = Offset(size.width / 2f, size.height / 2f)
                    val touchPoint = change.position

                    // Calculate angle from center
                    var angle = atan2(
                        touchPoint.y - center.y,
                        touchPoint.x - center.x
                    ) * (180 / PI).toFloat()

                    // Normalize to 0-360
                    if (angle < 0) angle += 360f

                    // Map angle to our arc (150° to 390° / -30°)
                    val normalizedAngle = when {
                        angle >= startAngle -> angle - startAngle
                        angle < startAngle - sweepAngle + 360 -> angle + 360 - startAngle
                        else -> 0f
                    }

                    val progress = (normalizedAngle / sweepAngle).coerceIn(0f, 1f)
                    onValueChange(progress * range.endInclusive)
                }
            }
    ) {
        Canvas(modifier = Modifier.size(240.dp)) {
            val canvasSize = size.minDimension
            val radius = canvasSize / 2 - 40f
            val center = Offset(size.width / 2, size.height / 2)

            // Track (Gray Arc)
            drawArc(
                color = Color.LightGray.copy(alpha = 0.3f),
                startAngle = startAngle,
                sweepAngle = sweepAngle,
                useCenter = false,
                style = Stroke(width = 24f, cap = StrokeCap.Round),
                topLeft = Offset(center.x - radius, center.y - radius),
                size = androidx.compose.ui.geometry.Size(radius * 2, radius * 2)
            )

            // Progress Arc (Gradient)
            val progressFactor = (animatedValue / range.endInclusive).coerceIn(0f, 1f)
            drawArc(
                brush = Brush.sweepGradient(
                    colors = listOf(
                        SecondaryPurple,
                        PrimaryViolet,
                        DeepViolet,
                        PrimaryViolet
                    )
                ),
                startAngle = startAngle,
                sweepAngle = sweepAngle * progressFactor,
                useCenter = false,
                style = Stroke(width = 24f, cap = StrokeCap.Round),
                topLeft = Offset(center.x - radius, center.y - radius),
                size = androidx.compose.ui.geometry.Size(radius * 2, radius * 2)
            )

            // Tick marks
            for (i in 0..10) {
                val tickAngle = startAngle + (sweepAngle * i / 10f)
                val tickRadian = tickAngle * (PI / 180f).toFloat()
                val innerRadius = radius + 20f
                val outerRadius = radius + 35f

                val startX = center.x + cos(tickRadian) * innerRadius
                val startY = center.y + sin(tickRadian) * innerRadius
                val endX = center.x + cos(tickRadian) * outerRadius
                val endY = center.y + sin(tickRadian) * outerRadius

                drawLine(
                    color = if (i % 5 == 0) DarkViolet else Color.Gray,
                    start = Offset(startX, startY),
                    end = Offset(endX, endY),
                    strokeWidth = if (i % 5 == 0) 3f else 2f,
                    cap = StrokeCap.Round
                )
            }

            // Needle
            val needleAngle = startAngle + (sweepAngle * progressFactor)
            val needleRadian = needleAngle * (PI / 180f).toFloat()
            val needleLength = radius - 20f

            val needleEndX = center.x + cos(needleRadian) * needleLength
            val needleEndY = center.y + sin(needleRadian) * needleLength

            // Needle shadow
            drawLine(
                color = Color.Black.copy(alpha = 0.3f),
                start = Offset(center.x + 2f, center.y + 2f),
                end = Offset(needleEndX + 2f, needleEndY + 2f),
                strokeWidth = 6f,
                cap = StrokeCap.Round
            )

            // Needle
            drawLine(
                color = PrimaryViolet,
                start = center,
                end = Offset(needleEndX, needleEndY),
                strokeWidth = 4f,
                cap = StrokeCap.Round
            )

            // Center hub
            drawCircle(
                color = DarkViolet,
                radius = 16f,
                center = center
            )
            drawCircle(
                color = SecondaryPurple,
                radius = 10f,
                center = center
            )
        }

        // Center Display
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.offset(y = 30.dp)
        ) {
            Text(
                text = "${animatedValue.toInt()}",
                style = MaterialTheme.typography.displaySmall.copy(
                    fontWeight = FontWeight.Bold,
                    color = PrimaryViolet
                )
            )
            Text(
                "km/day",
                color = Color.Gray,
                fontSize = 14.sp
            )
        }

        // Min/Max Labels
        Text(
            "0",
            color = Color.Gray,
            fontSize = 12.sp,
            modifier = Modifier
                .align(Alignment.BottomStart)
                .offset(x = 40.dp, y = (-20).dp)
        )
        Text(
            "200",
            color = Color.Gray,
            fontSize = 12.sp,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .offset(x = (-40).dp, y = (-20).dp)
        )
    }
}

// ============ FINISH BUTTON ============

@Composable
private fun FinishButton(
    isEnabled: Boolean,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(24.dp)
    ) {
        Button(
            onClick = onClick,
            enabled = isEnabled,
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
                .shadow(
                    elevation = if (isEnabled) 10.dp else 2.dp,
                    shape = RoundedCornerShape(16.dp)
                ),
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = Color.Transparent,
                disabledContainerColor = Color.LightGray.copy(alpha = 0.5f)
            ),
            contentPadding = PaddingValues()
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        if (isEnabled) BrandGradient
                        else Brush.horizontalGradient(listOf(Color.LightGray, Color.LightGray))
                    ),
                contentAlignment = Alignment.Center
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.Flag,
                        contentDescription = null,
                        tint = Color.White
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        "SAVE PROFILE",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
            }
        }
    }
}

// ============ CHECKERED FLAG SUCCESS OVERLAY ============

@Composable
private fun CheckeredFlagSuccessOverlay(onComplete: () -> Unit) {
    // Trigger navigation after delay
    LaunchedEffect(Unit) {
        kotlinx.coroutines.delay(2500)
        onComplete()
    }

    // Animations
    val infiniteTransition = rememberInfiniteTransition(label = "success")
    
    val flagWave by infiniteTransition.animateFloat(
        initialValue = -5f,
        targetValue = 5f,
        animationSpec = infiniteRepeatable(
            animation = tween(300, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "wave"
    )

    val scaleIn by animateFloatAsState(
        targetValue = 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "scale"
    )

    val confettiOffset by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1000f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "confetti"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        PrimaryViolet.copy(alpha = 0.95f),
                        DeepViolet.copy(alpha = 0.98f)
                    )
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        // Confetti particles
        Canvas(modifier = Modifier.fillMaxSize()) {
            val colors = listOf(
                Color.White, SecondaryPurple, SuccessGreen, 
                Color.Yellow, Color.Cyan
            )
            for (i in 0 until 30) {
                val x = (size.width * ((i * 37) % 100) / 100f)
                val y = ((confettiOffset + i * 50) % size.height)
                val color = colors[i % colors.size]
                
                rotate(confettiOffset + i * 10f, pivot = Offset(x, y)) {
                    drawRect(
                        color = color.copy(alpha = 0.8f),
                        topLeft = Offset(x, y),
                        size = androidx.compose.ui.geometry.Size(8f, 16f)
                    )
                }
            }
        }

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.graphicsLayer(scaleX = scaleIn, scaleY = scaleIn)
        ) {
            // Checkered Flag Icon
            Box(
                modifier = Modifier
                    .size(120.dp)
                    .rotate(flagWave)
            ) {
                CheckeredFlagCanvas()
            }

            Spacer(modifier = Modifier.height(32.dp))

            // Success Check
            Box(
                modifier = Modifier
                    .size(80.dp)
                    .background(SuccessGreen, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(48.dp)
                )
            }

            Spacer(modifier = Modifier.height(32.dp))

            Text(
                "🏁 You're Race Ready! 🏁",
                style = MaterialTheme.typography.headlineMedium.copy(
                    fontWeight = FontWeight.ExtraBold,
                    color = Color.White
                )
            )
            
            Spacer(modifier = Modifier.height(12.dp))
            
            Text(
                "Taking you to the dashboard...",
                color = Color.White.copy(alpha = 0.7f),
                fontSize = 16.sp
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Loading indicator
            CircularProgressIndicator(
                color = Color.White,
                strokeWidth = 3.dp,
                modifier = Modifier.size(32.dp)
            )
        }
    }
}

@Composable
private fun CheckeredFlagCanvas() {
    Canvas(modifier = Modifier.fillMaxSize()) {
        val squareSize = size.width / 6
        
        for (row in 0 until 6) {
            for (col in 0 until 6) {
                val isBlack = (row + col) % 2 == 0
                drawRect(
                    color = if (isBlack) CheckeredBlack else CheckeredWhite,
                    topLeft = Offset(col * squareSize, row * squareSize),
                    size = androidx.compose.ui.geometry.Size(squareSize, squareSize)
                )
            }
        }
    }
}

// ============ TYRE ROAD PROGRESS (STEP 3) ============

@Composable
private fun TyreRoadProgressUsage(
    currentStep: Int,
    totalSteps: Int
) {
    val progress = currentStep.toFloat() / totalSteps.toFloat()

    val infiniteTransition = rememberInfiniteTransition(label = "tyre_progress")
    val tyreRotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = LinearEasing)
        ),
        label = "tyre_rotation"
    )

    Column(modifier = Modifier.fillMaxWidth()) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp),
            contentAlignment = Alignment.CenterStart
        ) {
            // The Road
            Canvas(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .align(Alignment.Center)
            ) {
                drawRoundRect(
                    color = Color.LightGray.copy(alpha = 0.4f),
                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(3.dp.toPx())
                )

                val dashWidth = 20.dp.toPx()
                val dashGap = 10.dp.toPx()
                var startX = 0f
                while (startX < size.width) {
                    drawLine(
                        color = Color.White.copy(alpha = 0.6f),
                        start = Offset(startX, size.height / 2),
                        end = Offset(startX + dashWidth, size.height / 2),
                        strokeWidth = 2.dp.toPx(),
                        cap = StrokeCap.Round
                    )
                    startX += dashWidth + dashGap
                }
            }

            // Progress Line
            Box(
                modifier = Modifier
                    .fillMaxWidth(progress)
                    .height(6.dp)
                    .align(Alignment.CenterStart)
                    .clip(RoundedCornerShape(3.dp))
                    .background(BrandGradient)
            )

            // Rolling Tyre at end
            Box(
                modifier = Modifier
                    .fillMaxWidth(progress)
                    .wrapContentWidth(Alignment.End)
                    .offset(x = 16.dp)
            ) {
                MiniRollingTyreUsage(
                    rotation = tyreRotation,
                    modifier = Modifier.size(36.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Step Labels
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            StepLabelUsage("Profile", isActive = currentStep >= 1, isCurrent = currentStep == 1)
            StepLabelUsage("Vehicle", isActive = currentStep >= 2, isCurrent = currentStep == 2)
            StepLabelUsage("Finish", isActive = currentStep >= 3, isCurrent = currentStep == 3)
        }
    }
}

@Composable
private fun StepLabelUsage(
    text: String,
    isActive: Boolean,
    isCurrent: Boolean
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            modifier = Modifier
                .size(8.dp)
                .clip(CircleShape)
                .background(
                    if (isActive) PrimaryViolet else Color.LightGray.copy(alpha = 0.5f)
                )
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall.copy(
                fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Normal
            ),
            color = if (isActive) PrimaryViolet else Color.Gray
        )
    }
}

@Composable
private fun MiniRollingTyreUsage(
    rotation: Float,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier) {
        val centerX = size.width / 2
        val centerY = size.height / 2
        val outerRadius = size.minDimension / 2 - 2f
        val rimRadius = outerRadius * 0.65f
        val hubRadius = outerRadius * 0.3f

        rotate(rotation, pivot = Offset(centerX, centerY)) {
            drawCircle(
                color = Color(0xFF1A1A1A),
                radius = outerRadius,
                center = Offset(centerX, centerY)
            )

            for (i in 0 until 8) {
                val angle = (i * 45f) * (PI / 180f)
                val startX = centerX + cos(angle).toFloat() * rimRadius
                val startY = centerY + sin(angle).toFloat() * rimRadius
                val endX = centerX + cos(angle).toFloat() * (outerRadius - 1f)
                val endY = centerY + sin(angle).toFloat() * (outerRadius - 1f)

                drawLine(
                    color = Color(0xFF0A0A0A),
                    start = Offset(startX, startY),
                    end = Offset(endX, endY),
                    strokeWidth = 2f,
                    cap = StrokeCap.Round
                )
            }

            drawCircle(
                color = Color(0xFFD0D0D0),
                radius = rimRadius,
                center = Offset(centerX, centerY)
            )

            for (i in 0 until 5) {
                val angle = (i * 72f) * (PI / 180f)
                val startX = centerX + cos(angle).toFloat() * (hubRadius + 2f)
                val startY = centerY + sin(angle).toFloat() * (hubRadius + 2f)
                val endX = centerX + cos(angle).toFloat() * (rimRadius - 2f)
                val endY = centerY + sin(angle).toFloat() * (rimRadius - 2f)

                drawLine(
                    color = Color(0xFFA0A0A0),
                    start = Offset(startX, startY),
                    end = Offset(endX, endY),
                    strokeWidth = 3f,
                    cap = StrokeCap.Round
                )
            }

            drawCircle(
                color = PrimaryViolet,
                radius = hubRadius,
                center = Offset(centerX, centerY)
            )
        }

        drawCircle(
            color = Color.White,
            radius = outerRadius,
            center = Offset(centerX, centerY),
            style = Stroke(width = 2f)
        )
    }
}

@Composable
@Preview
private fun UsageProfileScreenPreview() {
    val vm = SetupViewModel()
    MaterialTheme {
        UsageProfileScreen(viewModel = vm, onSaveProfile = {})
    }
}

