package org.example.project

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

/**
 * Notification data class
 */
data class NotificationItem(
    val id: String,
    val title: String,
    val message: String,
    val type: NotificationType,
    val timestamp: String,
    val isRead: Boolean = false
)

enum class NotificationType {
    Alert,
    Reminder,
    Info,
    Success
}

private val mockNotifications = listOf(
    NotificationItem(
        id = "1",
        title = "Low Pressure Alert",
        message = "Rear Right tyre pressure is below recommended level (26.5 PSI)",
        type = NotificationType.Alert,
        timestamp = "2 hours ago",
        isRead = false
    ),
    NotificationItem(
        id = "2",
        title = "Tyre Health Update",
        message = "Your monthly tyre health report is ready to view",
        type = NotificationType.Info,
        timestamp = "1 day ago",
        isRead = false
    ),
    NotificationItem(
        id = "3",
        title = "Service Reminder",
        message = "It's been 3 months since your last tyre rotation",
        type = NotificationType.Reminder,
        timestamp = "2 days ago",
        isRead = true
    ),
    NotificationItem(
        id = "4",
        title = "Analysis Complete",
        message = "Your tyre scan has been analyzed. Overall health: 85%",
        type = NotificationType.Success,
        timestamp = "3 days ago",
        isRead = true
    )
)

/**
 * Notifications Screen - Shows all app notifications
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotificationsScreen(
    onBackClick: () -> Unit = {},
    notifications: List<NotificationItem> = mockNotifications,
    modifier: Modifier = Modifier
) {
    var notificationList by remember { mutableStateOf(notifications) }
    
    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = { Text("Notifications") },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                },
                actions = {
                    TextButton(
                        onClick = {
                            notificationList = notificationList.map { it.copy(isRead = true) }
                        }
                    ) {
                        Text("Mark all read")
                    }
                }
            )
        }
    ) { paddingValues ->
        if (notificationList.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        imageVector = Icons.Filled.NotificationsOff,
                        contentDescription = null,
                        modifier = Modifier.size(64.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "No notifications",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Unread section
                val unread = notificationList.filter { !it.isRead }
                if (unread.isNotEmpty()) {
                    item {
                        Text(
                            text = "New",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(vertical = 8.dp)
                        )
                    }
                    items(unread) { notification ->
                        NotificationCard(
                            notification = notification,
                            onClick = {
                                notificationList = notificationList.map {
                                    if (it.id == notification.id) it.copy(isRead = true) else it
                                }
                            }
                        )
                    }
                }
                
                // Read section
                val read = notificationList.filter { it.isRead }
                if (read.isNotEmpty()) {
                    item {
                        Text(
                            text = "Earlier",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(vertical = 8.dp)
                        )
                    }
                    items(read) { notification ->
                        NotificationCard(
                            notification = notification,
                            onClick = { }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun NotificationCard(
    notification: NotificationItem,
    onClick: () -> Unit
) {
    val (icon, iconColor) = when (notification.type) {
        NotificationType.Alert -> Icons.Filled.Warning to Color(0xFFF44336)
        NotificationType.Reminder -> Icons.Filled.Schedule to Color(0xFFFF9800)
        NotificationType.Info -> Icons.Filled.Info to Color(0xFF2196F3)
        NotificationType.Success -> Icons.Filled.CheckCircle to Color(0xFF4CAF50)
    }
    
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (notification.isRead) 
                MaterialTheme.colorScheme.surface 
            else 
                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
        )
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.Top
        ) {
            // Icon
            Surface(
                shape = CircleShape,
                color = iconColor.copy(alpha = 0.15f),
                modifier = Modifier.size(40.dp)
            ) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier.fillMaxSize()
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = iconColor,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
            
            Spacer(modifier = Modifier.width(12.dp))
            
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = notification.title,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = if (notification.isRead) FontWeight.Normal else FontWeight.Bold
                    )
                    Text(
                        text = notification.timestamp,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = notification.message,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            
            // Unread indicator
            if (!notification.isRead) {
                Spacer(modifier = Modifier.width(8.dp))
                Surface(
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(8.dp)
                ) {}
            }
        }
    }
}
