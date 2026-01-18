package org.example.project

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

/**
 * Settings & Preferences Screen
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onBackClick: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    // State for toggles
    var notificationsEnabled by remember { mutableStateOf(true) }
    var pushNotifications by remember { mutableStateOf(true) }
    var lowPressureAlerts by remember { mutableStateOf(true) }
    var maintenanceReminders by remember { mutableStateOf(true) }
    var darkMode by remember { mutableStateOf(false) }
    var autoBackup by remember { mutableStateOf(true) }
    var metricUnits by remember { mutableStateOf(false) }
    
    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = { Text("Settings") },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                }
            )
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Notifications Section
            item {
                SettingsSectionHeader(title = "Notifications")
            }
            
            item {
                SettingsCard {
                    SettingsToggleItem(
                        icon = Icons.Filled.Notifications,
                        title = "Enable Notifications",
                        subtitle = "Receive app notifications",
                        isChecked = notificationsEnabled,
                        onCheckedChange = { notificationsEnabled = it }
                    )
                    
                    HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
                    
                    SettingsToggleItem(
                        icon = Icons.Filled.PhoneAndroid,
                        title = "Push Notifications",
                        subtitle = "Get alerts even when app is closed",
                        isChecked = pushNotifications,
                        onCheckedChange = { pushNotifications = it },
                        enabled = notificationsEnabled
                    )
                    
                    HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
                    
                    SettingsToggleItem(
                        icon = Icons.Filled.Warning,
                        title = "Low Pressure Alerts",
                        subtitle = "Alert when tyre pressure is low",
                        isChecked = lowPressureAlerts,
                        onCheckedChange = { lowPressureAlerts = it },
                        enabled = notificationsEnabled
                    )
                    
                    HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
                    
                    SettingsToggleItem(
                        icon = Icons.Filled.Build,
                        title = "Maintenance Reminders",
                        subtitle = "Remind for scheduled maintenance",
                        isChecked = maintenanceReminders,
                        onCheckedChange = { maintenanceReminders = it },
                        enabled = notificationsEnabled
                    )
                }
            }
            
            // Appearance Section
            item {
                SettingsSectionHeader(title = "Appearance")
            }
            
            item {
                SettingsCard {
                    SettingsToggleItem(
                        icon = Icons.Filled.DarkMode,
                        title = "Dark Mode",
                        subtitle = "Use dark theme",
                        isChecked = darkMode,
                        onCheckedChange = { darkMode = it }
                    )
                }
            }
            
            // Units Section
            item {
                SettingsSectionHeader(title = "Units & Measurement")
            }
            
            item {
                SettingsCard {
                    SettingsToggleItem(
                        icon = Icons.Filled.Straighten,
                        title = "Metric Units",
                        subtitle = if (metricUnits) "Using kPa and Celsius" else "Using PSI and Fahrenheit",
                        isChecked = metricUnits,
                        onCheckedChange = { metricUnits = it }
                    )
                }
            }
            
            // Data & Storage Section
            item {
                SettingsSectionHeader(title = "Data & Storage")
            }
            
            item {
                SettingsCard {
                    SettingsToggleItem(
                        icon = Icons.Filled.CloudSync,
                        title = "Auto Backup",
                        subtitle = "Backup data to cloud automatically",
                        isChecked = autoBackup,
                        onCheckedChange = { autoBackup = it }
                    )
                    
                    HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
                    
                    SettingsNavigationItem(
                        icon = Icons.Filled.Storage,
                        title = "Clear Cache",
                        subtitle = "Free up storage space",
                        onClick = { /* TODO */ }
                    )
                    
                    HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
                    
                    SettingsNavigationItem(
                        icon = Icons.Filled.Download,
                        title = "Export Data",
                        subtitle = "Download your tyre history",
                        onClick = { /* TODO */ }
                    )
                }
            }
            
            // Privacy Section
            item {
                SettingsSectionHeader(title = "Privacy & Security")
            }
            
            item {
                SettingsCard {
                    SettingsNavigationItem(
                        icon = Icons.Filled.Lock,
                        title = "App Lock",
                        subtitle = "Require biometrics to open app",
                        onClick = { /* TODO */ }
                    )
                    
                    HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
                    
                    SettingsNavigationItem(
                        icon = Icons.Filled.PrivacyTip,
                        title = "Privacy Policy",
                        subtitle = "View our privacy policy",
                        onClick = { /* TODO */ }
                    )
                    
                    HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
                    
                    SettingsNavigationItem(
                        icon = Icons.Filled.Gavel,
                        title = "Terms of Service",
                        subtitle = "View terms and conditions",
                        onClick = { /* TODO */ }
                    )
                }
            }
            
            // About Section
            item {
                SettingsSectionHeader(title = "About")
            }
            
            item {
                SettingsCard {
                    SettingsNavigationItem(
                        icon = Icons.Filled.Info,
                        title = "App Version",
                        subtitle = "1.0.0 (Build 1)",
                        onClick = { /* TODO */ },
                        showArrow = false
                    )
                    
                    HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
                    
                    SettingsNavigationItem(
                        icon = Icons.Filled.Star,
                        title = "Rate the App",
                        subtitle = "Share your feedback",
                        onClick = { /* TODO */ }
                    )
                    
                    HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
                    
                    SettingsNavigationItem(
                        icon = Icons.Filled.Email,
                        title = "Contact Support",
                        subtitle = "Get help from our team",
                        onClick = { /* TODO */ }
                    )
                }
            }
            
            item {
                Spacer(modifier = Modifier.height(32.dp))
            }
        }
    }
}

@Composable
private fun SettingsSectionHeader(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.primary,
        fontWeight = FontWeight.SemiBold,
        modifier = Modifier.padding(vertical = 8.dp, horizontal = 4.dp)
    )
}

@Composable
private fun SettingsCard(
    content: @Composable ColumnScope.() -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(content = content)
    }
}

@Composable
private fun SettingsToggleItem(
    icon: ImageVector,
    title: String,
    subtitle: String,
    isChecked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    enabled: Boolean = true
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = if (enabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(24.dp)
        )
        
        Spacer(modifier = Modifier.width(16.dp))
        
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
                color = if (enabled) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        
        Switch(
            checked = isChecked,
            onCheckedChange = onCheckedChange,
            enabled = enabled
        )
    }
}

@Composable
private fun SettingsNavigationItem(
    icon: ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit,
    showArrow: Boolean = true
) {
    Surface(
        onClick = onClick,
        color = MaterialTheme.colorScheme.surface
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(24.dp)
            )
            
            Spacer(modifier = Modifier.width(16.dp))
            
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyLarge
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            
            if (showArrow) {
                Icon(
                    imageVector = Icons.Filled.ChevronRight,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
