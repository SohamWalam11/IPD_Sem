package org.example.project.analysis

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ShowChart
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch

/**
 * Analysis Screen - Main screen with tabs for Report, History, and Telemetry.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AnalysisScreen(
    onBackClick: () -> Unit = {},
    onTyreClick: (TireStatus) -> Unit = {}
) {
    // Create ViewModel manually instead of using viewModel() for KMP compatibility
    val viewModel = remember { AnalysisViewModel() }
    val reportState by viewModel.reportState.collectAsState()
    val telemetryState by viewModel.telemetryState.collectAsState()
    
    val tabs = listOf("Report", "History", "Telemetry")
    val pagerState = rememberPagerState(pageCount = { tabs.size })
    val coroutineScope = rememberCoroutineScope()
    
    // Cleanup when leaving screen
    DisposableEffect(Unit) {
        onDispose {
            viewModel.stopLiveSimulation()
        }
    }
    
    // Start simulation when on telemetry tab
    LaunchedEffect(pagerState.currentPage) {
        if (pagerState.currentPage == 2) {
            viewModel.startLiveSimulation()
        } else {
            viewModel.stopLiveSimulation()
        }
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Analysis",
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Tab Row
            PrimaryTabRow(
                selectedTabIndex = pagerState.currentPage
            ) {
                tabs.forEachIndexed { index, title ->
                    Tab(
                        selected = pagerState.currentPage == index,
                        onClick = {
                            coroutineScope.launch {
                                pagerState.animateScrollToPage(index)
                            }
                        },
                        text = { Text(title) },
                        icon = {
                            Icon(
                                imageVector = when (index) {
                                    0 -> Icons.Default.Description
                                    1 -> Icons.Default.History
                                    else -> Icons.AutoMirrored.Filled.ShowChart
                                },
                                contentDescription = null
                            )
                        }
                    )
                }
            }
            
            // Tab Content
            HorizontalPager(
                state = pagerState,
                modifier = Modifier.fillMaxSize()
            ) { page ->
                when (page) {
                    0 -> ReportPage(
                        reportState = reportState,
                        onRefresh = { viewModel.refreshReport() },
                        onTyreClick = onTyreClick
                    )
                    1 -> HistoryPage()
                    2 -> TelemetryPage(
                        telemetryState = telemetryState,
                        onSelectTire = { viewModel.selectTire(it) },
                        onStartSimulation = { viewModel.startLiveSimulation() },
                        onStopSimulation = { viewModel.stopLiveSimulation() }
                    )
                }
            }
        }
    }
}

/**
 * History Page - Tab 2 - Shows past inspection history
 */
@Composable
fun HistoryPage(
    modifier: Modifier = Modifier
) {
    // Mock history data
    val historyItems = remember {
        listOf(
            HistoryItem(
                id = "1",
                date = "Jan 14, 2026",
                time = "10:30 AM",
                position = TirePosition.REAR_RIGHT,
                result = "Crack Detected",
                confidence = 0.92f,
                isCritical = true
            ),
            HistoryItem(
                id = "2",
                date = "Jan 12, 2026",
                time = "3:15 PM",
                position = TirePosition.FRONT_LEFT,
                result = "Good",
                confidence = 0.98f,
                isCritical = false
            ),
            HistoryItem(
                id = "3",
                date = "Jan 10, 2026",
                time = "9:00 AM",
                position = TirePosition.FRONT_RIGHT,
                result = "Good",
                confidence = 0.95f,
                isCritical = false
            ),
            HistoryItem(
                id = "4",
                date = "Jan 8, 2026",
                time = "2:45 PM",
                position = TirePosition.REAR_LEFT,
                result = "Worn",
                confidence = 0.87f,
                isCritical = true
            ),
            HistoryItem(
                id = "5",
                date = "Jan 5, 2026",
                time = "11:20 AM",
                position = TirePosition.FRONT_LEFT,
                result = "Good",
                confidence = 0.96f,
                isCritical = false
            )
        )
    }
    
    androidx.compose.foundation.lazy.LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Text(
                text = "Inspection History",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "${historyItems.size} inspections recorded",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        
        items(historyItems.size) { index ->
            HistoryItemCard(item = historyItems[index])
        }
    }
}

/**
 * Data class for history items.
 */
data class HistoryItem(
    val id: String,
    val date: String,
    val time: String,
    val position: TirePosition,
    val result: String,
    val confidence: Float,
    val isCritical: Boolean
)

/**
 * Card for displaying a history item.
 */
@Composable
private fun HistoryItemCard(
    item: HistoryItem,
    modifier: Modifier = Modifier
) {
    val criticalColor = androidx.compose.ui.graphics.Color(0xFFE53935)
    val safeColor = androidx.compose.ui.graphics.Color(0xFF43A047)
    
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = androidx.compose.foundation.shape.RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (item.isCritical)
                criticalColor.copy(alpha = 0.08f)
            else
                MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Result indicator
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .background(
                        color = if (item.isCritical) criticalColor.copy(alpha = 0.15f) else safeColor.copy(alpha = 0.15f),
                        shape = androidx.compose.foundation.shape.RoundedCornerShape(12.dp)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (item.isCritical) Icons.Default.Warning else Icons.Default.CheckCircle,
                    contentDescription = null,
                    tint = if (item.isCritical) criticalColor else safeColor,
                    modifier = Modifier.size(24.dp)
                )
            }
            
            Spacer(modifier = Modifier.width(12.dp))
            
            // Details
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = item.result,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = if (item.isCritical) criticalColor else MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = item.position.displayName,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "${(item.confidence * 100).toInt()}% confidence",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            
            // Date & Time
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = item.date,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = item.time,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
