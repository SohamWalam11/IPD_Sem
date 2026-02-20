package org.example.project.permissions

import android.Manifest
import android.os.Build
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberMultiplePermissionsState
import com.google.accompanist.permissions.rememberPermissionState
import kotlinx.coroutines.delay

// Theme colors
private val PrimaryViolet = Color(0xFF6200EA)
private val SecondaryPurple = Color(0xFFBB86FC)
private val DeepViolet = Color(0xFF3700B3)
private val GoodGreen = Color(0xFF4CAF50)
private val LightLavender = Color(0xFFF3E5F5)

/**
 * Permission Request Screen
 * Asks for camera and location permissions with nice UI
 */
@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun PermissionRequestScreen(
    onAllPermissionsGranted: () -> Unit,
    onSkip: () -> Unit,
    modifier: Modifier = Modifier
) {
    // Define required permissions
    val permissionsToRequest = buildList {
        add(Manifest.permission.CAMERA)
        add(Manifest.permission.ACCESS_FINE_LOCATION)
        add(Manifest.permission.ACCESS_COARSE_LOCATION)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            add(Manifest.permission.POST_NOTIFICATIONS)
        }
        // BLE permissions for TPMS sensor connectivity
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            add(Manifest.permission.BLUETOOTH_SCAN)
            add(Manifest.permission.BLUETOOTH_CONNECT)
        }
    }
    
    val permissionsState = rememberMultiplePermissionsState(
        permissions = permissionsToRequest
    ) { permissionsResult ->
        // Check if all critical permissions are granted
        val cameraGranted = permissionsResult[Manifest.permission.CAMERA] == true
        val locationGranted = permissionsResult[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
                permissionsResult[Manifest.permission.ACCESS_COARSE_LOCATION] == true
        
        if (cameraGranted && locationGranted) {
            onAllPermissionsGranted()
        }
    }
    
    var currentPermissionIndex by remember { mutableStateOf(0) }
    
    val permissionItems = listOf(
        PermissionItem(
            icon = Icons.Default.CameraAlt,
            title = "Camera Access",
            description = "Capture tyre images for AI-powered defect detection and 3D scanning",
            permission = Manifest.permission.CAMERA
        ),
        PermissionItem(
            icon = Icons.Default.LocationOn,
            title = "Location Access",
            description = "Find nearby service centers and track your driving routes for tyre analysis",
            permission = Manifest.permission.ACCESS_FINE_LOCATION
        ),
        PermissionItem(
            icon = Icons.Default.Bluetooth,
            title = "Bluetooth Access",
            description = "Connect to JK Tyre Treel TPMS sensors for real-time tyre pressure monitoring",
            permission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S)
                Manifest.permission.BLUETOOTH_SCAN else Manifest.permission.ACCESS_FINE_LOCATION
        )
    )
    
    // Check if all permissions are already granted
    LaunchedEffect(permissionsState.allPermissionsGranted) {
        if (permissionsState.allPermissionsGranted) {
            delay(500)
            onAllPermissionsGranted()
        }
    }
    
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(LightLavender, Color.White, LightLavender.copy(alpha = 0.5f))
                )
            )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(48.dp))
            
            // Title
            Text(
                text = "Enable Permissions",
                style = MaterialTheme.typography.headlineMedium.copy(
                    fontWeight = FontWeight.Bold
                ),
                color = DeepViolet
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Text(
                text = "TyreGuard needs a few permissions to provide you the best experience",
                style = MaterialTheme.typography.bodyMedium,
                color = Color.Gray,
                textAlign = TextAlign.Center
            )
            
            Spacer(modifier = Modifier.height(48.dp))
            
            // Permission cards
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                permissionItems.forEachIndexed { index, item ->
                    val isGranted = permissionsState.permissions
                        .find { it.permission == item.permission }
                        ?.status?.isGranted == true
                    
                    PermissionCard(
                        item = item,
                        isGranted = isGranted,
                        onRequestPermission = {
                            currentPermissionIndex = index
                            permissionsState.launchMultiplePermissionRequest()
                        }
                    )
                }
            }
            
            // Action buttons
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Grant All button
                Button(
                    onClick = {
                        permissionsState.launchMultiplePermissionRequest()
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = PrimaryViolet
                    )
                ) {
                    Text(
                        text = "Grant All Permissions",
                        fontWeight = FontWeight.SemiBold
                    )
                }
                
                Spacer(modifier = Modifier.height(12.dp))
                
                // Skip button
                TextButton(onClick = onSkip) {
                    Text(
                        text = "Maybe Later",
                        color = Color.Gray
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

data class PermissionItem(
    val icon: ImageVector,
    val title: String,
    val description: String,
    val permission: String
)

@Composable
private fun PermissionCard(
    item: PermissionItem,
    isGranted: Boolean,
    onRequestPermission: () -> Unit,
    modifier: Modifier = Modifier
) {
    // Animation for granted state
    val scale by animateFloatAsState(
        targetValue = if (isGranted) 1.02f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
        label = "scale"
    )
    
    Card(
        modifier = modifier
            .fillMaxWidth()
            .scale(scale),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isGranted) GoodGreen.copy(alpha = 0.1f) else Color.White
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Icon
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .clip(CircleShape)
                    .background(
                        if (isGranted) GoodGreen.copy(alpha = 0.2f)
                        else PrimaryViolet.copy(alpha = 0.1f)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (isGranted) Icons.Default.Check else item.icon,
                    contentDescription = null,
                    modifier = Modifier.size(28.dp),
                    tint = if (isGranted) GoodGreen else PrimaryViolet
                )
            }
            
            Spacer(modifier = Modifier.width(16.dp))
            
            // Text content
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = item.title,
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.SemiBold
                    )
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = item.description,
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.Gray
                )
            }
            
            // Status indicator
            if (isGranted) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = GoodGreen.copy(alpha = 0.15f)
                ) {
                    Text(
                        text = "Granted",
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        style = MaterialTheme.typography.labelMedium,
                        color = GoodGreen,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            } else {
                FilledTonalButton(
                    onClick = onRequestPermission,
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Enable")
                }
            }
        }
    }
}
