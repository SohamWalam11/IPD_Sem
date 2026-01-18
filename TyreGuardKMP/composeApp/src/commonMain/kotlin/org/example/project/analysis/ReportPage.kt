package org.example.project.analysis

import androidx.compose.foundation.clickable
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
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

// Critical colors
private val CriticalRed = Color(0xFFE53935)
private val CriticalRedLight = Color(0x1AE53935)
private val SafeGreen = Color(0xFF43A047)

/**
 * Report Page - Tab 1 of Analysis Screen
 * Displays detailed list of all 4 tires with sensor data and inspection status.
 */
@Composable
fun ReportPage(
    reportState: ReportState,
    onRefresh: () -> Unit = {},
    onTyreClick: (TireStatus) -> Unit = {},
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Header with refresh
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Tire Status Report",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Last updated: ${formatTime(reportState.lastUpdated)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                
                IconButton(onClick = onRefresh) {
                    if (reportState.isLoading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(24.dp),
                            strokeWidth = 2.dp
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Refresh"
                        )
                    }
                }
            }
        }
        
        // Tire cards - clickable to show detail
        items(reportState.tires) { tire ->
            TireDetailCard(
                tireStatus = tire,
                onClick = { onTyreClick(tire) }
            )
        }
        
        // Summary card
        item {
            SummaryCard(tires = reportState.tires)
        }
    }
}

/**
 * Reusable Tire Detail Card component.
 * Shows tire position, sensor data, and defects with critical styling.
 */
@Composable
fun TireDetailCard(
    tireStatus: TireStatus,
    onClick: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val isCritical = tireStatus.isCritical
    
    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .then(
                if (isCritical) {
                    Modifier.border(
                        width = 2.dp,
                        color = CriticalRed,
                        shape = RoundedCornerShape(16.dp)
                    )
                } else Modifier
            ),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isCritical) CriticalRedLight else MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            // Header: Tire Name + Icon
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Tire position icon
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(
                                if (isCritical) CriticalRed.copy(alpha = 0.2f)
                                else MaterialTheme.colorScheme.primaryContainer
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = getTireIcon(tireStatus.position),
                            contentDescription = null,
                            tint = if (isCritical) CriticalRed else MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(28.dp)
                        )
                    }
                    
                    Column {
                        Text(
                            text = tireStatus.position.displayName,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = tireStatus.position.shortName,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                
                // Status indicator
                StatusChip(isCritical = isCritical)
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Data Grid: Pressure, Temp, Tread Health
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                DataCell(
                    icon = Icons.Default.Speed,
                    label = "Pressure",
                    value = "%.1f PSI".format(tireStatus.pressurePsi),
                    isCritical = tireStatus.pressurePsi < 28f || tireStatus.pressurePsi > 38f
                )
                
                DataCell(
                    icon = Icons.Default.Thermostat,
                    label = "Temperature",
                    value = "%.1f°C".format(tireStatus.temperatureCelsius),
                    isCritical = tireStatus.temperatureCelsius > 40f
                )
                
                DataCell(
                    icon = Icons.Default.Circle,
                    label = "Tread",
                    value = tireStatus.treadHealth.displayText,
                    isCritical = tireStatus.treadHealth == TreadHealth.CRITICAL ||
                            tireStatus.treadHealth == TreadHealth.WORN
                )
            }
            
            // Defect Chips (if any)
            if (tireStatus.defects.isNotEmpty()) {
                Spacer(modifier = Modifier.height(12.dp))
                HorizontalDivider()
                Spacer(modifier = Modifier.height(12.dp))
                
                Text(
                    text = "Detected Issues",
                    style = MaterialTheme.typography.labelMedium,
                    color = CriticalRed,
                    fontWeight = FontWeight.SemiBold
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                // FlowRow for defect chips
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    tireStatus.defects.forEach { defect ->
                        DefectChip(defect = defect)
                    }
                }
            }
        }
    }
}

@Composable
private fun DataCell(
    icon: ImageVector,
    label: String,
    value: String,
    isCritical: Boolean
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = if (isCritical) CriticalRed else MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(20.dp)
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = value,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
            color = if (isCritical) CriticalRed else MaterialTheme.colorScheme.onSurface
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun StatusChip(isCritical: Boolean) {
    Surface(
        shape = RoundedCornerShape(20.dp),
        color = if (isCritical) CriticalRed.copy(alpha = 0.15f) else SafeGreen.copy(alpha = 0.15f)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = if (isCritical) Icons.Default.Warning else Icons.Default.CheckCircle,
                contentDescription = null,
                tint = if (isCritical) CriticalRed else SafeGreen,
                modifier = Modifier.size(16.dp)
            )
            Text(
                text = if (isCritical) "Critical" else "OK",
                style = MaterialTheme.typography.labelMedium,
                color = if (isCritical) CriticalRed else SafeGreen,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

@Composable
private fun DefectChip(defect: String) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = CriticalRed.copy(alpha = 0.1f)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.Error,
                contentDescription = null,
                tint = CriticalRed,
                modifier = Modifier.size(14.dp)
            )
            Text(
                text = defect,
                style = MaterialTheme.typography.labelSmall,
                color = CriticalRed,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

@Composable
private fun SummaryCard(tires: List<TireStatus>) {
    val criticalCount = tires.count { it.isCritical }
    val avgPressure = tires.map { it.pressurePsi }.average()
    
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = "Summary",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "$criticalCount",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        color = if (criticalCount > 0) CriticalRed else SafeGreen
                    )
                    Text(
                        text = "Critical Issues",
                        style = MaterialTheme.typography.labelSmall
                    )
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "%.1f".format(avgPressure),
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Avg PSI",
                        style = MaterialTheme.typography.labelSmall
                    )
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "${tires.size}",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Total Tires",
                        style = MaterialTheme.typography.labelSmall
                    )
                }
            }
        }
    }
}

private fun getTireIcon(position: TirePosition): ImageVector {
    return when (position) {
        TirePosition.FRONT_LEFT -> Icons.Default.NorthWest
        TirePosition.FRONT_RIGHT -> Icons.Default.NorthEast
        TirePosition.REAR_LEFT -> Icons.Default.SouthWest
        TirePosition.REAR_RIGHT -> Icons.Default.SouthEast
    }
}

private fun formatTime(timestamp: Long): String {
    val diff = System.currentTimeMillis() - timestamp
    return when {
        diff < 60_000 -> "Just now"
        diff < 3600_000 -> "${diff / 60_000} min ago"
        else -> "${diff / 3600_000}h ago"
    }
}
