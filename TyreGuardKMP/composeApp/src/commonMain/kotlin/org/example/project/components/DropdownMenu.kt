package org.example.project.components

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp

/**
 * More Info Dropdown Menu with Profile, Settings, Preferences options
 */
@Composable
fun MoreInfoDropdownMenu(
    expanded: Boolean,
    onDismiss: () -> Unit,
    onProfileClick: () -> Unit,
    onSettingsClick: () -> Unit,
    onPreferencesClick: () -> Unit,
    onNotificationsClick: () -> Unit = {},
    notificationCount: Int = 0,
    modifier: Modifier = Modifier,
    offset: DpOffset = DpOffset(0.dp, 0.dp)
) {
    DropdownMenu(
        expanded = expanded,
        onDismissRequest = onDismiss,
        modifier = modifier,
        offset = offset
    ) {
        // Profile
        DropdownMenuItem(
            text = { Text("Profile") },
            onClick = {
                onDismiss()
                onProfileClick()
            },
            leadingIcon = {
                Icon(Icons.Filled.Person, contentDescription = null)
            }
        )
        
        // Notifications with badge
        DropdownMenuItem(
            text = { Text("Notifications") },
            onClick = {
                onDismiss()
                onNotificationsClick()
            },
            leadingIcon = {
                IconWithBadge(
                    icon = Icons.Filled.Notifications,
                    contentDescription = null,
                    badgeCount = notificationCount
                )
            },
            trailingIcon = {
                if (notificationCount > 0) {
                    Badge(
                        containerColor = MaterialTheme.colorScheme.error
                    ) {
                        Text(notificationCount.toString())
                    }
                }
            }
        )
        
        HorizontalDivider()
        
        // Settings
        DropdownMenuItem(
            text = { Text("Settings") },
            onClick = {
                onDismiss()
                onSettingsClick()
            },
            leadingIcon = {
                Icon(Icons.Filled.Settings, contentDescription = null)
            }
        )
        
        // Preferences
        DropdownMenuItem(
            text = { Text("Preferences") },
            onClick = {
                onDismiss()
                onPreferencesClick()
            },
            leadingIcon = {
                Icon(Icons.Filled.Tune, contentDescription = null)
            }
        )
        
        HorizontalDivider()
        
        // Help
        DropdownMenuItem(
            text = { Text("Help & Support") },
            onClick = onDismiss,
            leadingIcon = {
                Icon(Icons.Filled.Help, contentDescription = null)
            }
        )
        
        // About
        DropdownMenuItem(
            text = { Text("About TyreGuard") },
            onClick = onDismiss,
            leadingIcon = {
                Icon(Icons.Filled.Info, contentDescription = null)
            }
        )
    }
}

/**
 * Top App Bar with More Info button
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TopBarWithMoreMenu(
    title: String,
    onBackClick: (() -> Unit)? = null,
    onProfileClick: () -> Unit = {},
    onSettingsClick: () -> Unit = {},
    onPreferencesClick: () -> Unit = {},
    onNotificationsClick: () -> Unit = {},
    notificationCount: Int = 0
) {
    var showMenu by remember { mutableStateOf(false) }
    
    TopAppBar(
        title = { Text(title) },
        navigationIcon = {
            if (onBackClick != null) {
                IconButton(onClick = onBackClick) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back"
                    )
                }
            }
        },
        actions = {
            // Notification bell with badge
            Box {
                IconButton(onClick = onNotificationsClick) {
                    IconWithBadge(
                        icon = Icons.Filled.Notifications,
                        contentDescription = "Notifications",
                        badgeCount = notificationCount
                    )
                }
            }
            
            // More menu
            Box {
                IconButton(onClick = { showMenu = true }) {
                    Icon(
                        imageVector = Icons.Filled.MoreVert,
                        contentDescription = "More options"
                    )
                }
                
                MoreInfoDropdownMenu(
                    expanded = showMenu,
                    onDismiss = { showMenu = false },
                    onProfileClick = onProfileClick,
                    onSettingsClick = onSettingsClick,
                    onPreferencesClick = onPreferencesClick,
                    onNotificationsClick = onNotificationsClick,
                    notificationCount = notificationCount,
                    offset = DpOffset(0.dp, 0.dp)
                )
            }
        }
    )
}
