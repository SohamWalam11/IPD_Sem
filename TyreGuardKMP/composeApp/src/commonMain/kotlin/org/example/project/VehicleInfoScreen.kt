package org.example.project

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.ElectricBolt
import androidx.compose.material.icons.filled.EvStation
import androidx.compose.material.icons.filled.LocalGasStation
import androidx.compose.material.icons.filled.WaterDrop
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.jetbrains.compose.ui.tooling.preview.Preview
import kotlin.math.cos
import kotlin.math.sin

// ============ PREMIUM THEME COLORS ============
private val PrimaryViolet = Color(0xFF6200EA)
private val SecondaryPurple = Color(0xFFBB86FC)
private val DeepViolet = Color(0xFF3700B3)
private val DarkViolet = Color(0xFF4A148C)
private val LightLavender = Color(0xFFF3E5F5)
private val SoftWhite = Color(0xFFFAFAFA)
private val SelectedCardBg = PrimaryViolet.copy(alpha = 0.1f)

private val BrandGradient = Brush.horizontalGradient(
    colors = listOf(PrimaryViolet, DeepViolet)
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VehicleInfoScreen(
    viewModel: SetupViewModel,
    modifier: Modifier = Modifier,
    onNext: () -> Unit
) {
    val brandOptions = listOf("Maruti Suzuki", "Hyundai", "Tata", "Honda", "Toyota", "Mahindra")
    
    // Dynamic model options based on brand
    val modelOptionsMap = mapOf(
        "Maruti Suzuki" to listOf("Swift", "Baleno", "Dzire", "Brezza", "Alto", "WagonR"),
        "Hyundai" to listOf("Creta", "i20", "Venue", "Verna", "Tucson", "Exter"),
        "Tata" to listOf("Nexon", "Punch", "Harrier", "Safari", "Altroz", "Tiago"),
        "Honda" to listOf("City", "Amaze", "Elevate", "Jazz"),
        "Toyota" to listOf("Innova", "Fortuner", "Glanza", "Hyryder", "Hilux"),
        "Mahindra" to listOf("Scorpio", "XUV700", "Thar", "Bolero", "XUV300")
    )

    var isBrandExpanded by remember { mutableStateOf(false) }
    var isModelExpanded by remember { mutableStateOf(false) }
    var brandError by remember { mutableStateOf<String?>(null) }
    var modelError by remember { mutableStateOf<String?>(null) }
    var yearError by remember { mutableStateOf<String?>(null) }
    var fuelError by remember { mutableStateOf<String?>(null) }

    val years = (1990..2026).toList().reversed()

    // Street Lights Background Animation
    val infiniteTransition = rememberInfiniteTransition(label = "street_lights")
    val backgroundOffset by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1000f,
        animationSpec = infiniteRepeatable(
            animation = tween(20000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "road_offset"
    )

    Scaffold(
        containerColor = Color.Transparent,
        bottomBar = {
            // Floating Gradient "NEXT STEP" Button
            FloatingNextButton(
                isEnabled = viewModel.vehicleBrand.isNotEmpty() && 
                           viewModel.carModel.isNotEmpty() && 
                           viewModel.yearOfManufacture != null &&
                           viewModel.fuelVariant != null,
                onClick = {
                    var valid = true
                    if (viewModel.vehicleBrand.isBlank()) {
                        brandError = "Select vehicle brand"
                        valid = false
                    }
                    if (viewModel.carModel.isBlank()) {
                        modelError = "Select car model"
                        valid = false
                    }
                    if (viewModel.yearOfManufacture == null) {
                        yearError = "Select manufacturing year"
                        valid = false
                    }
                    if (viewModel.fuelVariant == null) {
                        fuelError = "Select fuel variant"
                        valid = false
                    }
                    if (valid) {
                        onNext()
                    }
                }
            )
        }
    ) { innerPadding ->
        Box(modifier = modifier.fillMaxSize()) {
            // 1. MOVING BACKGROUND (Abstract Street Lights)
            MovingRoadBackground(offset = backgroundOffset)

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .verticalScroll(rememberScrollState())
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // 2. TYRE ROAD PROGRESS BAR - Step 2
                TyreRoadProgressVehicle(currentStep = 2, totalSteps = 3)

                Spacer(modifier = Modifier.height(32.dp))

                // 3. HEADER TEXT
                Text(
                    text = "Your Vehicle Identity",
                    style = MaterialTheme.typography.headlineMedium.copy(
                        fontWeight = FontWeight.ExtraBold,
                        color = DarkViolet
                    )
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "We tune alerts based on your car's engine.",
                    style = MaterialTheme.typography.bodyLarge.copy(
                        color = Color.Gray,
                        lineHeight = 24.sp
                    ),
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(24.dp))

                // 4. BRAND DROPDOWN (Animated)
                AnimatedDropdown(
                    label = "Vehicle Brand",
                    options = brandOptions,
                    selectedOption = viewModel.vehicleBrand,
                    onOptionSelected = { 
                        viewModel.vehicleBrand = it
                        viewModel.carModel = "" // Reset model on brand change
                        brandError = null
                    },
                    isError = brandError != null
                )
                if (brandError != null) {
                    Text(
                        text = brandError!!,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(start = 16.dp, top = 4.dp)
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // 5. MODEL DROPDOWN (Sequential Unlocking - appears after brand selected)
                AnimatedVisibility(
                    visible = viewModel.vehicleBrand.isNotEmpty(),
                    enter = slideInVertically(initialOffsetY = { -40 }) + fadeIn() + expandVertically(),
                    exit = slideOutVertically() + fadeOut() + shrinkVertically()
                ) {
                    Column {
                        AnimatedDropdown(
                            label = "Car Model",
                            options = modelOptionsMap[viewModel.vehicleBrand] ?: emptyList(),
                            selectedOption = viewModel.carModel,
                            onOptionSelected = { 
                                viewModel.carModel = it
                                modelError = null
                            },
                            isError = modelError != null
                        )
                        if (modelError != null) {
                            Text(
                                text = modelError!!,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.error,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(start = 16.dp, top = 4.dp)
                            )
                        }
                        Spacer(modifier = Modifier.height(16.dp))
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // 6. ODOMETER YEAR PICKER
                Text(
                    text = "Year of Manufacturing",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = DarkViolet
                    )
                )
                Spacer(modifier = Modifier.height(12.dp))
                OdometerYearPicker(
                    years = years,
                    selectedYear = viewModel.yearOfManufacture ?: 2020,
                    onYearSelected = { 
                        viewModel.yearOfManufacture = it
                        yearError = null
                    }
                )
                if (yearError != null) {
                    Text(
                        text = yearError!!,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(start = 16.dp, top = 4.dp)
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))

                // 7. FUEL VARIANT CARDS (Visual Selectors with Icons)
                Text(
                    text = "Fuel Variant",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = DarkViolet
                    )
                )
                Spacer(modifier = Modifier.height(12.dp))
                
                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    FuelCard(
                        label = "Petrol",
                        icon = Icons.Default.LocalGasStation,
                        isSelected = viewModel.fuelVariant == "Petrol",
                        onClick = { 
                            viewModel.fuelVariant = "Petrol"
                            fuelError = null
                        },
                        modifier = Modifier.weight(1f)
                    )
                    FuelCard(
                        label = "Diesel",
                        icon = Icons.Default.WaterDrop,
                        isSelected = viewModel.fuelVariant == "Diesel",
                        onClick = { 
                            viewModel.fuelVariant = "Diesel"
                            fuelError = null
                        },
                        modifier = Modifier.weight(1f)
                    )
                }
                Spacer(modifier = Modifier.height(12.dp))
                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    FuelCard(
                        label = "CNG",
                        icon = Icons.Default.EvStation,
                        isSelected = viewModel.fuelVariant == "CNG",
                        onClick = { 
                            viewModel.fuelVariant = "CNG"
                            fuelError = null
                        },
                        modifier = Modifier.weight(1f)
                    )
                    FuelCard(
                        label = "EV",
                        icon = Icons.Default.ElectricBolt,
                        isSelected = viewModel.fuelVariant == "EV",
                        onClick = { 
                            viewModel.fuelVariant = "EV"
                            fuelError = null
                        },
                        modifier = Modifier.weight(1f)
                    )
                }
                if (fuelError != null) {
                    Text(
                        text = fuelError!!,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(start = 16.dp, top = 4.dp)
                    )
                }

                // Extra space at bottom for scroll
                Spacer(modifier = Modifier.height(100.dp))
            }
        }
    }
}

// ============ MOVING ROAD BACKGROUND ============

@Composable
private fun MovingRoadBackground(offset: Float) {
    Canvas(modifier = Modifier.fillMaxSize().alpha(0.4f)) {
        val canvasWidth = size.width
        val canvasHeight = size.height
        
        // Draw a subtle "road" gradient
        drawRect(
            brush = Brush.verticalGradient(
                colors = listOf(Color.White, LightLavender, Color(0xFFF0EBFF))
            )
        )

        // Draw moving lines (abstract passing street lights)
        val lineCount = 6
        val spacing = canvasHeight / lineCount
        
        for (i in 0 until lineCount) {
            val yPos = (offset + (i * spacing)) % canvasHeight
            drawLine(
                color = PrimaryViolet.copy(alpha = 0.08f),
                start = Offset(0f, yPos),
                end = Offset(canvasWidth, yPos + 120f), // Slanted line
                strokeWidth = 180f
            )
        }
        
        // Secondary lighter lines for depth
        for (i in 0 until lineCount / 2) {
            val yPos = (offset * 0.7f + (i * spacing * 2)) % canvasHeight
            drawLine(
                color = SecondaryPurple.copy(alpha = 0.05f),
                start = Offset(0f, yPos + 50f),
                end = Offset(canvasWidth, yPos + 200f),
                strokeWidth = 100f
            )
        }
    }
}

// ============ ANIMATED DROPDOWN ============

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AnimatedDropdown(
    label: String,
    options: List<String>,
    selectedOption: String,
    onOptionSelected: (String) -> Unit,
    isError: Boolean = false
) {
    var expanded by remember { mutableStateOf(false) }

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = !expanded }
    ) {
        OutlinedTextField(
            value = selectedOption,
            onValueChange = {},
            readOnly = true,
            label = { Text(label) },
            leadingIcon = {
                Icon(
                    Icons.Default.DirectionsCar,
                    contentDescription = null,
                    tint = if (isError) MaterialTheme.colorScheme.error else PrimaryViolet
                )
            },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            shape = RoundedCornerShape(12.dp),
            isError = isError,
            modifier = Modifier
                .fillMaxWidth()
                .height(64.dp)
                .menuAnchor(),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = PrimaryViolet,
                unfocusedBorderColor = Color.LightGray,
                unfocusedContainerColor = Color.White,
                focusedContainerColor = Color.White,
                focusedLabelColor = PrimaryViolet
            )
        )
        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            options.forEach { option ->
                DropdownMenuItem(
                    text = { Text(option) },
                    onClick = {
                        onOptionSelected(option)
                        expanded = false
                    }
                )
            }
        }
    }
}

// ============ ODOMETER YEAR PICKER ============

@Composable
private fun OdometerYearPicker(
    years: List<Int>,
    selectedYear: Int,
    onYearSelected: (Int) -> Unit
) {
    val listState = rememberLazyListState()
    
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(100.dp)
            .background(Color(0xFFF5F5F5), RoundedCornerShape(16.dp))
            .border(1.dp, Color.LightGray, RoundedCornerShape(16.dp))
            .clip(RoundedCornerShape(16.dp))
    ) {
        LazyRow(
            state = listState,
            contentPadding = PaddingValues(horizontal = 140.dp),
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxSize()
        ) {
            items(years) { year ->
                val isSelected = year == selectedYear
                val scale by animateFloatAsState(
                    targetValue = if (isSelected) 1.3f else 0.85f,
                    animationSpec = spring(stiffness = Spring.StiffnessMedium),
                    label = "scale"
                )
                val color by animateColorAsState(
                    targetValue = if (isSelected) PrimaryViolet else Color.Gray,
                    label = "color"
                )
                
                Text(
                    text = year.toString(),
                    fontSize = 22.sp,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                    color = color,
                    modifier = Modifier
                        .padding(horizontal = 16.dp)
                        .graphicsLayer(scaleX = scale, scaleY = scale)
                        .clickable { onYearSelected(year) }
                )
            }
        }
        
        // Center Indicator (The "Needle")
        Box(
            modifier = Modifier
                .align(Alignment.Center)
                .width(3.dp)
                .height(50.dp)
                .background(
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            PrimaryViolet.copy(alpha = 0.8f),
                            PrimaryViolet,
                            PrimaryViolet.copy(alpha = 0.8f)
                        )
                    ),
                    shape = RoundedCornerShape(2.dp)
                )
        )
        
        // Glass Reflection Overlay (fades at edges)
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    brush = Brush.horizontalGradient(
                        colors = listOf(
                            Color.White.copy(alpha = 0.95f),
                            Color.Transparent,
                            Color.Transparent,
                            Color.White.copy(alpha = 0.95f)
                        )
                    )
                )
        )
    }
}

// ============ FUEL CARD ============

@Composable
private fun FuelCard(
    label: String,
    icon: ImageVector,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val borderColor by animateColorAsState(
        targetValue = if (isSelected) PrimaryViolet else Color.LightGray,
        label = "border"
    )
    val bgColor by animateColorAsState(
        targetValue = if (isSelected) SelectedCardBg else Color.White,
        label = "bg"
    )
    val iconScale by animateFloatAsState(
        targetValue = if (isSelected) 1.2f else 1f,
        animationSpec = spring(stiffness = Spring.StiffnessMedium),
        label = "iconScale"
    )

    Card(
        modifier = modifier
            .height(90.dp)
            .clickable { onClick() },
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(2.dp, borderColor),
        colors = CardDefaults.cardColors(containerColor = bgColor),
        elevation = CardDefaults.cardElevation(
            defaultElevation = if (isSelected) 4.dp else 1.dp
        )
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (isSelected) PrimaryViolet else Color.Gray,
                modifier = Modifier
                    .size(28.dp)
                    .graphicsLayer(scaleX = iconScale, scaleY = iconScale)
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.labelLarge,
                color = if (isSelected) PrimaryViolet else Color.Gray,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
            )
        }
    }
}

// ============ FLOATING NEXT BUTTON ============

@Composable
private fun FloatingNextButton(
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
                .shadow(elevation = if (isEnabled) 10.dp else 2.dp, shape = RoundedCornerShape(16.dp)),
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

// ============ TYRE ROAD PROGRESS BAR (VEHICLE STEP) ============

@Composable
private fun TyreRoadProgressVehicle(
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
                MiniRollingTyreVehicle(
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
            StepLabelVehicle("Profile", isActive = currentStep >= 1, isCurrent = currentStep == 1)
            StepLabelVehicle("Vehicle", isActive = currentStep >= 2, isCurrent = currentStep == 2)
            StepLabelVehicle("Finish", isActive = currentStep >= 3, isCurrent = currentStep == 3)
        }
    }
}

@Composable
private fun StepLabelVehicle(
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

// ============ MINI ROLLING TYRE ============

@Composable
private fun MiniRollingTyreVehicle(
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

            drawCircle(
                color = Color(0xFFD0D0D0),
                radius = rimRadius,
                center = Offset(centerX, centerY)
            )
            
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
private fun VehicleInfoScreenPreview() {
    val vm = SetupViewModel()
    MaterialTheme {
        VehicleInfoScreen(viewModel = vm, onNext = {})
    }
}

