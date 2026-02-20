package org.example.project.ble

import android.Manifest
import android.bluetooth.BluetoothAdapter
import android.content.Intent
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.rememberMultiplePermissionsState

// --------------------------------------------------------------
// PREMIUM AUTOMOTIVE PALETTE — "Luxury Clean"
// --------------------------------------------------------------
private val OffWhiteBg       = Color(0xFFF5F7FA)
private val PureWhite        = Color(0xFFFFFFFF)
private val TextPrimary      = Color(0xFF1A1C1E)
private val TextSecondary    = Color(0xFF64748B)
private val StatusGood       = Color(0xFF2E7D32)
private val StatusWarning    = Color(0xFFFF6D00)
private val StatusCritical   = Color(0xFFD32F2F)
private val TetherLineColor  = Color(0xFFE0E0E0)
private val AccentBlue       = Color(0xFF1565C0)
private val ScanPulse        = Color(0xFF42A5F5)

/**
 * Premium TPMS Screen — "Spatial Dashboard" design inspired by Land Rover InControl app.
 * Dark/Clean aesthetic, tethered tire cards, high-fidelity car silhouette.
 */
@OptIn(ExperimentalPermissionsApi::class, ExperimentalMaterial3Api::class)
@Composable
fun TpmsScreen(
    onBackClick: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val tpmsManager = remember { TpmsBluetoothManager.getInstance(context) }
    val tpmsState by tpmsManager.state.collectAsState()
    val discoveredSensors by tpmsManager.discoveredSensors.collectAsState()

    // BLE Permissions
    val blePermissions = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        listOf(Manifest.permission.BLUETOOTH_SCAN, Manifest.permission.BLUETOOTH_CONNECT, Manifest.permission.ACCESS_FINE_LOCATION)
    } else {
        listOf(Manifest.permission.BLUETOOTH, Manifest.permission.BLUETOOTH_ADMIN, Manifest.permission.ACCESS_FINE_LOCATION)
    }
    val permissionState = rememberMultiplePermissionsState(blePermissions)
    val enableBtLauncher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) {}

    LaunchedEffect(permissionState.allPermissionsGranted) {
        if (permissionState.allPermissionsGranted && tpmsManager.isBluetoothEnabled()) {
            tpmsManager.startScanning()
        }
    }
    DisposableEffect(Unit) { onDispose { tpmsManager.stopScanning() } }

    val positionData = tpmsState.getAllPositionData()
    val fl = positionData[TyreWheelPosition.FRONT_LEFT]
    val fr = positionData[TyreWheelPosition.FRONT_RIGHT]
    val rl = positionData[TyreWheelPosition.REAR_LEFT]
    val rr = positionData[TyreWheelPosition.REAR_RIGHT]

    val allPressures = listOfNotNull(fl, fr, rl, rr)
    val hasIssue = allPressures.any { it.pressurePsi < 28f || it.hasAlarm }
    
    // Calculate overall health (0-100)
    val healthScore = if (allPressures.isNotEmpty()) {
        val goodCount = allPressures.count { it.pressurePsi >= 30f && !it.hasAlarm }
        val warningCount = allPressures.count { it.pressurePsi in 28f..30f && !it.hasAlarm }
        val criticalCount = allPressures.count { it.pressurePsi < 28f || it.hasAlarm }
        ((goodCount * 100 + warningCount * 60 + criticalCount * 20) / allPressures.size)
    } else 0

    Scaffold(
        containerColor = OffWhiteBg,
        topBar = {
            TopAppBar(
                title = { Text("My Range Rover", fontWeight = FontWeight.Bold, color = TextPrimary) },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back", tint = TextPrimary)
                    }
                },
                actions = {
                    // Scan Logic
                    IconButton(onClick = {
                        if (tpmsState.isScanning) tpmsManager.stopScanning()
                        else if (!permissionState.allPermissionsGranted) permissionState.launchMultiplePermissionRequest()
                        else if (!tpmsManager.isBluetoothEnabled()) enableBtLauncher.launch(Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE))
                        else tpmsManager.startScanning()
                    }) {
                         val iconTint = if (tpmsState.isScanning) ScanPulse else TextSecondary
                         Icon(
                             if (tpmsState.isScanning) Icons.Default.BluetoothSearching else Icons.Default.Bluetooth,
                             "Scan",
                             tint = iconTint
                         )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = OffWhiteBg)
            )
        },
        bottomBar = { PremiumBottomBar() }
    ) { padding ->
        Column(
            modifier = modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // 1. Header (Status)
            Spacer(modifier = Modifier.height(8.dp))
            if (tpmsState.isScanning) {
                LinearProgressIndicator(
                    modifier = Modifier.fillMaxWidth(0.3f).height(2.dp).clip(RoundedCornerShape(2.dp)),
                    color = ScanPulse, trackColor = ScanPulse.copy(alpha = 0.2f)
                )
                Spacer(modifier = Modifier.height(8.dp))
            }
            
            Text(
                text = if (allPressures.isEmpty()) "Connect Sensors" else if (healthScore > 90) "All Systems Good" else "Attention Required",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
                color = if (allPressures.isEmpty()) TextSecondary else if (healthScore > 90) StatusGood else StatusCritical
            )

            Spacer(modifier = Modifier.height(24.dp))

            // 2. The SPATIAL Car View
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(500.dp) // Large hero area
            ) {
                // THE CAR IMAGE (Using Canvas placeholder as requested "Hero Element")
                // In production, replace PremiumCarTopView with Image(painterResource(...))
                PremiumCarTopView(
                    modifier = Modifier
                        .fillMaxWidth(0.32f)
                        .fillMaxHeight(0.85f)
                        .alpha(0.9f)
                )

                // --- Positioning the Cards ---
                // We use alignment to push them to the corners, visually connecting to tires
                
                // Front Left
                TetheredTireCard(
                    data = fl,
                    label = "Front Left",
                    alignment = Alignment.TopStart,
                    modifier = Modifier.align(Alignment.TopStart).padding(top = 60.dp, start = 16.dp)
                )

                // Front Right
                TetheredTireCard(
                    data = fr,
                    label = "Front Right",
                    alignment = Alignment.TopEnd,
                    modifier = Modifier.align(Alignment.TopEnd).padding(top = 60.dp, end = 16.dp)
                )

                // Rear Left
                TetheredTireCard(
                    data = rl,
                    label = "Rear Left",
                    alignment = Alignment.BottomStart,
                    modifier = Modifier.align(Alignment.BottomStart).padding(bottom = 60.dp, start = 16.dp)
                )

                // Rear Right
                TetheredTireCard(
                    data = rr,
                    label = "Rear Right",
                    alignment = Alignment.BottomEnd,
                    modifier = Modifier.align(Alignment.BottomEnd).padding(bottom = 60.dp, end = 16.dp)
                )
            }

            // 3. Quick Actions (Climate/Lock/Fuel)
            Spacer(modifier = Modifier.height(32.dp))
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                QuickActionIcon(icon = Icons.Default.Lock, label = "Lock")
                QuickActionIcon(icon = Icons.Default.AcUnit, label = "Climate")
                QuickActionIcon(icon = Icons.Default.LocalGasStation, label = "Fuel")
                QuickActionIcon(icon = Icons.Default.Lightbulb, label = "Lights")
            }
            
            // 4. Discovered Sensors (for assignment)
            val unassigned = discoveredSensors.filter { d -> tpmsState.sensorConfigs.none { it.macAddress == d.macAddress } }
            if (unassigned.isNotEmpty()) {
                Spacer(modifier = Modifier.height(32.dp))
                Text("Nearby Devices", style = MaterialTheme.typography.titleSmall, color = TextSecondary)
                Spacer(modifier = Modifier.height(8.dp))
                unassigned.forEach { sensor ->
                    PremiumDiscoveredRow(sensor) { pos -> tpmsManager.assignSensorToPosition(sensor.macAddress, pos) }
                }
            }
            
            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

// --------------------------------------------------------------
// COMPONENT: TETHERED TIRE CARD
// --------------------------------------------------------------
@Composable
private fun TetheredTireCard(
    data: TpmsSensorData?,
    label: String,
    alignment: Alignment,
    modifier: Modifier = Modifier
) {
    val isLeft = alignment == Alignment.TopStart || alignment == Alignment.BottomStart
    
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
    ) {
        // If on the Left side, Card is first, then Line
        if (isLeft) {
            TireInfoCard(data, label)
            TireLine(isLeft = true)
        } else {
            // If on Right side, Line is first, then Card
            TireLine(isLeft = false)
            TireInfoCard(data, label)
        }
    }
}

@Composable
private fun TireLine(isLeft: Boolean) {
    Canvas(modifier = Modifier.width(40.dp).height(20.dp)) {
        val y = size.height / 2
        // Draw a line with a small circle at the end (touching the car)
        val startX = if (isLeft) 0f else size.width
        val endX = if (isLeft) size.width else 0f
        
        drawLine(
            color = TetherLineColor,
            start = Offset(startX, y),
            end = Offset(endX, y),
            strokeWidth = 3f,
            cap = StrokeCap.Round
        )
        // The "Dot" touching the car
        drawCircle(
            color = TetherLineColor,
            radius = 5f,
            center = Offset(endX, y)
        )
    }
}

@Composable
private fun TireInfoCard(data: TpmsSensorData?, label: String) {
    val psi = data?.pressurePsi ?: 0f
    val temp = data?.temperatureCelsius ?: 0f
    
    val statusColor = when {
        data == null -> TextSecondary
        psi < 28f -> StatusCritical
        psi < 32f -> StatusWarning
        else -> StatusGood
    }

    Card(
        colors = CardDefaults.cardColors(containerColor = PureWhite),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp), // Soft premium shadow
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier.width(115.dp)
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Status Indicator (Small colored dot + Label)
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(modifier = Modifier.size(6.dp).background(statusColor, CircleShape))
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = if (data == null) "--" else if (psi < 28) "LOW" else "NORMAL",
                    style = MaterialTheme.typography.labelSmall,
                    color = statusColor,
                    fontWeight = FontWeight.Bold,
                    fontSize = 11.sp
                )
            }
            
            Spacer(modifier = Modifier.height(6.dp))
            
            // The Big PSI Number
            if (data != null && psi > 0) {
                Text(
                    text = "%.0f".format(psi),
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.ExtraBold,
                    color = TextPrimary,
                    fontSize = 28.sp
                )
                Text(
                    text = "psi",
                    style = MaterialTheme.typography.labelSmall,
                    color = TextSecondary,
                    fontSize = 12.sp
                )
            } else {
                 Text(
                    text = "--",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.ExtraBold,
                    color = TextSecondary.copy(alpha = 0.4f),
                    fontSize = 28.sp
                )
                Text("psi", style = MaterialTheme.typography.labelSmall, color = TextSecondary.copy(alpha = 0.4f))
            }

            // Temperature (Subtle)
            Spacer(modifier = Modifier.height(6.dp))
            if (data != null && temp > 0) {
                Text(
                    text = "%.0f\u00B0C".format(temp),
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary,
                    fontSize = 12.sp
                )
            }
        }
    }
}

// --------------------------------------------------------------
// COMPONENT: PREMIUM BOTTOM BAR
// --------------------------------------------------------------
@Composable
private fun PremiumBottomBar() {
    NavigationBar(
        containerColor = PureWhite,
        tonalElevation = 8.dp
    ) {
        NavigationBarItem(
            icon = { Icon(Icons.Default.Home, contentDescription = null) },
            label = { Text("Home") },
            selected = true,
            onClick = { },
            colors = NavigationBarItemDefaults.colors(
                indicatorColor = OffWhiteBg,
                selectedIconColor = TextPrimary,
                unselectedIconColor = TextSecondary
            )
        )
        NavigationBarItem(
            icon = { Icon(Icons.Default.Settings, contentDescription = null) },
            label = { Text("Settings") },
            selected = false,
            onClick = { },
             colors = NavigationBarItemDefaults.colors(
                indicatorColor = OffWhiteBg,
                selectedIconColor = TextPrimary,
                unselectedIconColor = TextSecondary
            )
        )
    }
}

@Composable
private fun QuickActionIcon(icon: ImageVector, label: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            modifier = Modifier
                .size(60.dp)
                .background(PureWhite, RoundedCornerShape(16.dp))
                .border(1.dp, Color(0xFFE0E0E0), RoundedCornerShape(16.dp)),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, contentDescription = null, tint = TextPrimary, modifier = Modifier.size(24.dp))
        }
        Spacer(modifier = Modifier.height(6.dp))
        Text(label, style = MaterialTheme.typography.labelSmall, color = TextSecondary, fontSize = 11.sp)
    }
}

@Composable
private fun PremiumDiscoveredRow(sensor: TpmsSensorData, onAssign: (TyreWheelPosition) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    Card(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 4.dp).clickable { expanded = !expanded },
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = PureWhite),
        elevation = CardDefaults.cardElevation(1.dp)
    ) {
        Column(Modifier.padding(16.dp)) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Bluetooth, null, tint = AccentBlue, modifier = Modifier.size(20.dp))
                Spacer(Modifier.width(12.dp))
                Text(sensor.macAddress, fontWeight = FontWeight.Medium, fontSize = 14.sp, color = TextPrimary)
                Spacer(Modifier.weight(1f))
                Text("Add", fontSize = 12.sp, color = AccentBlue, fontWeight = FontWeight.Bold)
            }
            AnimatedVisibility(expanded) {
                Row(Modifier.fillMaxWidth().padding(top = 12.dp), horizontalArrangement = Arrangement.SpaceEvenly) {
                    listOf(TyreWheelPosition.FRONT_LEFT, TyreWheelPosition.FRONT_RIGHT, TyreWheelPosition.REAR_LEFT, TyreWheelPosition.REAR_RIGHT).forEach { pos ->
                        AssistChip(onClick = { onAssign(pos); expanded = false }, label = { Text(pos.shortLabel) })
                    }
                }
            }
        }
    }
}

// --------------------------------------------------------------
// COMPONENT: PREMIUM CAR VISUAL (Canvas Placeholder)
// --------------------------------------------------------------
@Composable
private fun PremiumCarTopView(modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val bodyColor = Color(0xFF3A3A3A) // Dark metallic grey
        val windowColor = Color(0xFF90A4AE)
        val wheelColor = Color(0xFF212121)
        val rimColor = Color(0xFFBDBDBD)

        val wheelW = w * 0.22f
        val wheelH = h * 0.12f
        val wheelRad = CornerRadius(8f)

        // 4 Wheels
        val wheels = listOf(
            Offset(-wheelW * 0.4f, h * 0.12f), // FL
            Offset(w - wheelW * 0.6f, h * 0.12f), // FR
            Offset(-wheelW * 0.4f, h * 0.76f), // RL
            Offset(w - wheelW * 0.6f, h * 0.76f) // RR
        )
        
        wheels.forEach { offset ->
            drawRoundRect(wheelColor, offset, Size(wheelW, wheelH), wheelRad)
            // Rim detail
            drawRoundRect(rimColor, Offset(offset.x + 4, offset.y + 4), Size(wheelW - 8, wheelH - 8), CornerRadius(4f), style = Stroke(2f))
        }

        // Car Body (Aerodynamic shape)
        val bodyInset = w * 0.1f
        val bodyPath = Path().apply {
            moveTo(bodyInset + 30f, h * 0.02f) // Nose
            lineTo(w - bodyInset - 30f, h * 0.02f)
            quadraticTo(w - bodyInset, h * 0.03f, w - bodyInset, h * 0.15f) // FR Corner
            lineTo(w - bodyInset + 10f, h * 0.5f) // Side flare
            lineTo(w - bodyInset, h * 0.95f) // RR Corner
            quadraticTo(w * 0.5f, h * 0.98f, bodyInset, h * 0.95f) // Rear Bumper
            lineTo(bodyInset - 10f, h * 0.5f) // Side flare
            lineTo(bodyInset, h * 0.15f) // FL Corner
            quadraticTo(bodyInset, h * 0.03f, bodyInset + 30f, h * 0.02f) // Back to nose
            close()
        }
        
        // Shadow/Glow
        drawPath(bodyPath, Color.Black.copy(alpha = 0.2f), style = Stroke(width = 10f))
        drawPath(bodyPath, bodyColor)

        // Windshield
        val wp = w * 0.18f
        val windshieldPath = Path().apply {
            moveTo(wp + 10f, h * 0.18f)
            lineTo(w - wp - 10f, h * 0.18f)
            lineTo(w - wp - 20f, h * 0.35f)
            lineTo(wp + 20f, h * 0.35f)
            close()
        }
        drawPath(windshieldPath, windowColor)
        
        // Roof (Darker)
        drawRoundRect(
            Color(0xFF263238),
            Offset(wp + 25f, h * 0.36f),
            Size(w - 2 * (wp + 25f), h * 0.34f),
            CornerRadius(10f)
        )
        
        // Rear Window
        val rearWindowPath = Path().apply {
            moveTo(wp + 20f, h * 0.71f)
            lineTo(w - wp - 20f, h * 0.71f)
            lineTo(w - wp, h * 0.82f)
            lineTo(wp, h * 0.82f)
            close()
        }
        drawPath(rearWindowPath, windowColor)
        
        // Mirrors
        drawRoundRect(bodyColor, Offset(-10f, h * 0.24f), Size(40f, 25f), CornerRadius(5f))
        drawRoundRect(bodyColor, Offset(w - 30f, h * 0.24f), Size(40f, 25f), CornerRadius(5f))
    }
}
