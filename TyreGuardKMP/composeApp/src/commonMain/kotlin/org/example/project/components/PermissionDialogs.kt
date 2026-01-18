package org.example.project.components

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog

/**
 * Permission type for confirmation dialogs
 */
enum class PermissionType {
    Camera,
    Location,
    Notifications,
    Storage
}

/**
 * Permission confirmation dialog for camera, location access
 */
@Composable
fun PermissionConfirmationDialog(
    permissionType: PermissionType,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    val (icon, title, description) = when (permissionType) {
        PermissionType.Camera -> Triple(
            Icons.Filled.CameraAlt,
            "Camera Access Required",
            "TyreGuard needs camera access to scan and analyze your tyres. This allows the app to detect defects, wear patterns, and provide accurate health assessments."
        )
        PermissionType.Location -> Triple(
            Icons.Filled.LocationOn,
            "Location Access Required",
            "TyreGuard needs location access to find nearby service centers and air pressure stations. This helps provide accurate distance calculations and directions."
        )
        PermissionType.Notifications -> Triple(
            Icons.Filled.Notifications,
            "Enable Notifications",
            "Stay informed about your tyre health! Get alerts for low pressure, scheduled maintenance reminders, and important safety updates."
        )
        PermissionType.Storage -> Triple(
            Icons.Filled.Storage,
            "Storage Access Required",
            "TyreGuard needs storage access to save analysis reports and tyre images for your records."
        )
    }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        modifier = modifier,
        icon = {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(48.dp),
                tint = MaterialTheme.colorScheme.primary
            )
        },
        title = {
            Text(
                text = title,
                textAlign = TextAlign.Center
            )
        },
        text = {
            Text(
                text = description,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        },
        confirmButton = {
            Button(
                onClick = onConfirm,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Allow Access")
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Not Now")
            }
        }
    )
}

/**
 * Multi-permission confirmation dialog for camera + location
 */
@Composable
fun MultiPermissionDialog(
    onConfirmAll: () -> Unit,
    onDismiss: () -> Unit,
    onOpenSettings: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = modifier.fillMaxWidth(),
            shape = MaterialTheme.shapes.extraLarge
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Header icons
                Row(
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.CameraAlt,
                        contentDescription = null,
                        modifier = Modifier.size(40.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Icon(
                        imageVector = Icons.Filled.LocationOn,
                        contentDescription = null,
                        modifier = Modifier.size(40.dp),
                        tint = MaterialTheme.colorScheme.secondary
                    )
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                Text(
                    text = "Permissions Required",
                    style = MaterialTheme.typography.headlineSmall,
                    textAlign = TextAlign.Center
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                Text(
                    text = "To provide you with the best tyre analysis experience, TyreGuard needs access to:",
                    style = MaterialTheme.typography.bodyMedium,
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // Permission items
                PermissionItem(
                    icon = Icons.Filled.CameraAlt,
                    title = "Camera",
                    description = "Scan and analyze tyres"
                )
                
                PermissionItem(
                    icon = Icons.Filled.LocationOn,
                    title = "Location",
                    description = "Find nearby service centers"
                )
                
                Spacer(modifier = Modifier.height(24.dp))
                
                // Buttons
                Button(
                    onClick = onConfirmAll,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Allow All")
                }
                
                Spacer(modifier = Modifier.height(8.dp))
                
                OutlinedButton(
                    onClick = onOpenSettings,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Customize in Settings")
                }
                
                TextButton(
                    onClick = onDismiss
                ) {
                    Text("Maybe Later")
                }
            }
        }
    }
}

@Composable
private fun PermissionItem(
    icon: ImageVector,
    title: String,
    description: String
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            modifier = Modifier.size(24.dp),
            tint = MaterialTheme.colorScheme.primary
        )
        
        Spacer(modifier = Modifier.width(16.dp))
        
        Column {
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall
            )
            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

/**
 * Confirmation dialog for destructive actions
 */
@Composable
fun ConfirmationDialog(
    title: String,
    message: String,
    confirmText: String = "Confirm",
    dismissText: String = "Cancel",
    isDestructive: Boolean = false,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = { Text(message) },
        confirmButton = {
            Button(
                onClick = onConfirm,
                colors = if (isDestructive) {
                    ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error
                    )
                } else ButtonDefaults.buttonColors()
            ) {
                Text(confirmText)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(dismissText)
            }
        }
    )
}
