package org.example.project

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.jetbrains.compose.ui.tooling.preview.Preview
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.cos
import kotlin.math.sin

// ============ PREMIUM THEME COLORS ============
private val PrimaryViolet = Color(0xFF6200EA)
private val SecondaryPurple = Color(0xFFBB86FC)
private val DeepViolet = Color(0xFF3700B3)
private val DarkViolet = Color(0xFF4A148C)
private val LightLavender = Color(0xFFF3E5F5)
private val SoftWhite = Color(0xFFFAFAFA)

private val BrandGradient = Brush.horizontalGradient(
    colors = listOf(PrimaryViolet, DeepViolet)
)

private val BackgroundGradient = Brush.verticalGradient(
    colors = listOf(LightLavender, SoftWhite, LightLavender.copy(alpha = 0.5f))
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PersonalInfoScreen(
    viewModel: SetupViewModel,
    modifier: Modifier = Modifier,
    onNext: () -> Unit
) {
    var nameError by remember { mutableStateOf<String?>(null) }
    var dobError by remember { mutableStateOf<String?>(null) }
    var mobileError by remember { mutableStateOf<String?>(null) }

    var showDatePicker by remember { mutableStateOf(false) }
    val datePickerState = rememberDatePickerState(
        initialSelectedDateMillis = System.currentTimeMillis()
    )
    
    // Mesh gradient animation
    val infiniteTransition = rememberInfiniteTransition(label = "mesh_gradient")
    val meshOffset1 by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 100f,
        animationSpec = infiniteRepeatable(
            animation = tween(4000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "mesh1"
    )
    val breathingAlpha by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 0.6f,
        animationSpec = infiniteRepeatable(
            animation = tween(3000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "breathing"
    )

    if (showDatePicker) {
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    val selectedMillis = datePickerState.selectedDateMillis
                    if (selectedMillis != null) {
                        val formatter = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
                        viewModel.dateOfBirth = formatter.format(Date(selectedMillis))
                        dobError = null
                    }
                    showDatePicker = false
                }) {
                    Text("OK", color = PrimaryViolet, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) {
                    Text("Cancel", color = Color.Gray)
                }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }

    Scaffold(
        containerColor = Color.Transparent,
        bottomBar = {
            // Floating Gradient "NEXT STEP" Button
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp)
            ) {
                Button(
                    onClick = {
                        var valid = true
                        if (viewModel.name.isBlank()) {
                            nameError = "Name is required"
                            valid = false
                        }
                        if (viewModel.dateOfBirth.isBlank()) {
                            dobError = "Please select your date of birth"
                            valid = false
                        }
                        if (viewModel.mobileNumber.length != 10) {
                            mobileError = "Mobile number must be exactly 10 digits"
                            valid = false
                        } else if (!viewModel.mobileNumber.all { it.isDigit() }) {
                            mobileError = "Mobile number must contain only digits"
                            valid = false
                        }
                        if (valid) {
                            onNext()
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp)
                        .shadow(elevation = 10.dp, shape = RoundedCornerShape(16.dp)),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent),
                    contentPadding = PaddingValues()
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(BrandGradient),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                "NEXT STEP",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Icon(
                                Icons.Rounded.ChevronRight,
                                contentDescription = null,
                                tint = Color.White
                            )
                        }
                    }
                }
            }
        }
    ) { innerPadding ->
        Box(modifier = modifier.fillMaxSize()) {
            // Animated Mesh Gradient Background
            ProfileMeshBackground(
                meshOffset = meshOffset1,
                breathingAlpha = breathingAlpha
            )
            
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .verticalScroll(rememberScrollState())
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // 1. TYRE ROAD PROGRESS BAR
                TyreRoadProgress(currentStep = 1, totalSteps = 3)

                Spacer(modifier = Modifier.height(32.dp))

                // 2. HEADER TEXT
                Text(
                    text = "Tell us about you",
                    style = MaterialTheme.typography.headlineMedium.copy(
                        fontWeight = FontWeight.ExtraBold,
                        color = DarkViolet
                    )
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "We'll personalize TyreGuard for your driving profile.",
                    style = MaterialTheme.typography.bodyLarge.copy(
                        color = Color.Gray,
                        lineHeight = 24.sp
                    ),
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(40.dp))

                // 3. FORM FIELDS

                // Name Field
                ModernTextField(
                    value = viewModel.name,
                    onValueChange = {
                        viewModel.name = it
                        nameError = null
                    },
                    label = "Full Name",
                    icon = Icons.Default.Person,
                    isError = nameError != null,
                    errorMessage = nameError
                )

                Spacer(modifier = Modifier.height(20.dp))

                // Date of Birth (Clickable)
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { showDatePicker = true }
                ) {
                    ModernTextField(
                        value = viewModel.dateOfBirth,
                        onValueChange = {},
                        label = "Date of Birth",
                        icon = Icons.Default.CalendarToday,
                        readOnly = true,
                        enabled = false,
                        isError = dobError != null,
                        errorMessage = dobError
                    )
                    // Transparent overlay to catch clicks
                    Box(
                        modifier = Modifier
                            .matchParentSize()
                            .clickable { showDatePicker = true }
                    )
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Phone Number Row with integrated country code
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Country Code Box
                    OutlinedTextField(
                        value = viewModel.countryCode,
                        onValueChange = { viewModel.countryCode = it },
                        modifier = Modifier
                            .width(85.dp)
                            .height(64.dp),
                        shape = RoundedCornerShape(12.dp),
                        readOnly = true,
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            unfocusedBorderColor = Color.LightGray,
                            focusedBorderColor = PrimaryViolet,
                            unfocusedContainerColor = Color.White,
                            focusedContainerColor = Color.White
                        ),
                        textStyle = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center
                        )
                    )

                    // Mobile Number Field
                    Column(modifier = Modifier.weight(1f)) {
                        ModernTextField(
                            value = viewModel.mobileNumber,
                            onValueChange = { newValue ->
                                val filtered = newValue.filter { it.isDigit() }.take(10)
                                viewModel.mobileNumber = filtered
                                mobileError = when {
                                    filtered.isEmpty() -> null
                                    filtered.length < 10 -> "Enter 10 digit mobile number"
                                    else -> null
                                }
                            },
                            label = "Mobile Number",
                            icon = Icons.Default.Phone,
                            keyboardType = KeyboardType.Phone,
                            isError = mobileError != null,
                            modifier = Modifier.fillMaxWidth()
                        )
                        // Character counter
                        Text(
                            text = "${viewModel.mobileNumber.length}/10 digits",
                            style = MaterialTheme.typography.labelSmall,
                            color = if (mobileError != null) MaterialTheme.colorScheme.error else Color.Gray,
                            modifier = Modifier.padding(start = 16.dp, top = 4.dp)
                        )
                    }
                }
                
                if (mobileError != null) {
                    Text(
                        text = mobileError!!,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(start = 97.dp, top = 4.dp)
                    )
                }
                
                // Extra space at bottom for scroll
                Spacer(modifier = Modifier.height(100.dp))
            }
        }
    }
}

// ============ TYRE ROAD PROGRESS BAR ============

@Composable
private fun TyreRoadProgress(
    currentStep: Int,
    totalSteps: Int
) {
    val progress = currentStep.toFloat() / totalSteps.toFloat()
    
    // Tyre rotation animation
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
            // The Road (Gray Line with dashed effect)
            Canvas(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .align(Alignment.Center)
            ) {
                // Road background
                drawRoundRect(
                    color = Color.LightGray.copy(alpha = 0.4f),
                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(3.dp.toPx())
                )
                
                // Road markings (dashed line)
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

            // The Progress (Gradient Line)
            Box(
                modifier = Modifier
                    .fillMaxWidth(progress)
                    .height(6.dp)
                    .align(Alignment.CenterStart)
                    .clip(RoundedCornerShape(3.dp))
                    .background(BrandGradient)
            )

            // The Rolling Tyre Icon at the progress point
            Box(
                modifier = Modifier
                    .fillMaxWidth(progress)
                    .wrapContentWidth(Alignment.End)
                    .offset(x = 16.dp)
            ) {
                MiniRollingTyre(
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
            StepLabel("Profile", isActive = currentStep >= 1, isCurrent = currentStep == 1)
            StepLabel("Vehicle", isActive = currentStep >= 2, isCurrent = currentStep == 2)
            StepLabel("Finish", isActive = currentStep >= 3, isCurrent = currentStep == 3)
        }
    }
}

@Composable
private fun StepLabel(
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

// ============ MINI ROLLING TYRE FOR PROGRESS BAR ============

@Composable
private fun MiniRollingTyre(
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
            // Tyre rubber
            drawCircle(
                color = Color(0xFF1A1A1A),
                radius = outerRadius,
                center = Offset(centerX, centerY)
            )
            
            // Treads
            for (i in 0 until 8) {
                val angle = (i * 45f) * (Math.PI / 180f)
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

            // Silver rim
            drawCircle(
                color = Color(0xFFD0D0D0),
                radius = rimRadius,
                center = Offset(centerX, centerY)
            )
            
            // Spokes
            for (i in 0 until 5) {
                val angle = (i * 72f) * (Math.PI / 180f)
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
            
            // Purple hub
            drawCircle(
                color = PrimaryViolet,
                radius = hubRadius,
                center = Offset(centerX, centerY)
            )
        }
        
        // White border ring
        drawCircle(
            color = Color.White,
            radius = outerRadius,
            center = Offset(centerX, centerY),
            style = Stroke(width = 2f)
        )
    }
}

// ============ MODERN TEXT FIELD ============

@Composable
private fun ModernTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    icon: ImageVector,
    readOnly: Boolean = false,
    enabled: Boolean = true,
    keyboardType: KeyboardType = KeyboardType.Text,
    isError: Boolean = false,
    errorMessage: String? = null,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            label = { Text(label) },
            leadingIcon = { 
                Icon(
                    icon,
                    contentDescription = null,
                    tint = if (isError) MaterialTheme.colorScheme.error else PrimaryViolet
                )
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(64.dp),
            shape = RoundedCornerShape(12.dp),
            readOnly = readOnly,
            enabled = enabled,
            isError = isError,
            keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
            colors = OutlinedTextFieldDefaults.colors(
                unfocusedBorderColor = Color.LightGray,
                focusedBorderColor = PrimaryViolet,
                unfocusedContainerColor = Color.White,
                focusedContainerColor = Color.White,
                disabledContainerColor = Color.White,
                disabledBorderColor = Color.LightGray,
                disabledTextColor = Color.Black,
                disabledLabelColor = Color.Gray,
                errorBorderColor = MaterialTheme.colorScheme.error,
                focusedLabelColor = PrimaryViolet
            ),
            singleLine = true
        )
        if (errorMessage != null) {
            Text(
                text = errorMessage,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.padding(start = 16.dp, top = 4.dp)
            )
        }
    }
}

// ============ MESH GRADIENT BACKGROUND ============

@Composable
private fun ProfileMeshBackground(
    meshOffset: Float,
    breathingAlpha: Float,
    modifier: Modifier = Modifier
) {
    Box(modifier = modifier.fillMaxSize()) {
        // Base gradient layer
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(BackgroundGradient)
        )
        
        // Animated mesh blobs
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .blur(60.dp)
        ) {
            val width = size.width
            val height = size.height
            
            // Top-right lavender blob
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        LightLavender.copy(alpha = breathingAlpha * 0.5f),
                        SecondaryPurple.copy(alpha = breathingAlpha * 0.2f),
                        Color.Transparent
                    ),
                    center = Offset(width - meshOffset * 2, meshOffset * 1.5f),
                    radius = width * 0.5f
                ),
                radius = width * 0.4f,
                center = Offset(width - meshOffset * 2, meshOffset * 1.5f)
            )
            
            // Bottom-left purple blob
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        PrimaryViolet.copy(alpha = breathingAlpha * 0.2f),
                        SecondaryPurple.copy(alpha = breathingAlpha * 0.1f),
                        Color.Transparent
                    ),
                    center = Offset(meshOffset * 2, height - meshOffset * 3),
                    radius = width * 0.5f
                ),
                radius = width * 0.4f,
                center = Offset(meshOffset * 2, height - meshOffset * 3)
            )
        }
    }
}

@Composable
@Preview
private fun PersonalInfoScreenPreview() {
    val vm = SetupViewModel().apply {
        name = "Soham"
        countryCode = "+91"
        mobileNumber = "9876543210"
    }
    MaterialTheme {
        PersonalInfoScreen(viewModel = vm, onNext = {})
    }
}
