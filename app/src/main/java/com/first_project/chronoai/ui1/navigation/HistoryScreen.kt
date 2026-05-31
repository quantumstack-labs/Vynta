package com.first_project.chronoai.ui1.navigation

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.draw.clip
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.first_project.chronoai.data.local.db.DatabaseProvider
import com.first_project.chronoai.data.local.entity.TaskEntity
import com.first_project.chronoai.ui.theme.*
import androidx.compose.material3.*
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun HistoryScreen(viewModel: com.first_project.chronoai.ui1.viewmodel.HomeViewModel) {
    val historyGrouped by viewModel.historyTasks.collectAsState()
    val insights by viewModel.temporalInsights.collectAsState()
    
    // We want metrics across all history for the logbook
    val allTasks = remember(historyGrouped) { historyGrouped.values.flatten() }
    val completedCount = allTasks.count { it.status == "COMPLETED" }
    val overallProgress = if (allTasks.isNotEmpty()) completedCount.toFloat() / allTasks.size else 0f

    var selectedFilter by remember { mutableStateOf("All") }
    val filteredHistory = remember(historyGrouped, selectedFilter) {
        historyGrouped.mapValues { (_, tasks) ->
            when (selectedFilter) {
                "Done" -> tasks.filter { it.status == "COMPLETED" }
                "Pending" -> tasks.filter { it.status != "COMPLETED" }
                else -> tasks
            }
        }.filter { it.value.isNotEmpty() }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        AdaptiveMeshGradient()

        Scaffold(
            containerColor = Color.Transparent
        ) { padding ->
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(horizontal = 24.dp),
                contentPadding = PaddingValues(top = padding.calculateTopPadding(), bottom = 140.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                item {
                    Column(modifier = Modifier.padding(vertical = 16.dp)) {
                        Text("Logbook", style = VyntaTypography.displayLarge)
                        Text("YOUR JOURNEY, DOCUMENTED.", style = VyntaTypography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }

                item {
                    VyntaCard(
                        modifier = Modifier.fillMaxWidth(),
                        containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.05f),
                        showBorder = true
                    ) {
                        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Psychology, null, tint = MaterialTheme.colorScheme.primary)
                            Spacer(Modifier.width(16.dp))
                            Text(insights, style = VyntaTypography.bodyMedium, color = MaterialTheme.colorScheme.primary)
                        }
                    }
                }

                item {
                    VyntaCard(
                        modifier = Modifier.fillMaxWidth().height(180.dp),
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                    ) {
                        Column(modifier = Modifier.padding(24.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text("All-Time Momentum", style = VyntaTypography.titleLarge, fontWeight = FontWeight.Bold)
                                Spacer(Modifier.width(8.dp))
                                Icon(Icons.Default.AutoGraph, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                            }
                            Spacer(Modifier.height(20.dp))
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                StatBlock(completedCount.toString(), "COMPLETED")
                                StatBlock(allTasks.size.toString(), "TOTAL")
                                StatBlock("${(overallProgress * 100).toInt()}%", "SUCCESS")
                            }
                            Spacer(Modifier.weight(1f))
                            LiquidProgressIndicator(
                                progress = overallProgress,
                                modifier = Modifier.fillMaxWidth().height(12.dp)
                            )
                        }
                    }
                }

                item {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(vertical = 8.dp)) {
                        listOf("All", "Done", "Pending").forEach { filter ->
                            GlassActionPill(
                                label = filter,
                                isSelected = selectedFilter == filter,
                                onClick = { selectedFilter = filter }
                            )
                        }
                    }
                }

                filteredHistory.forEach { (date, tasks) ->
                    stickyHeader {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(MaterialTheme.colorScheme.background.copy(alpha = 0.8f))
                                .padding(vertical = 8.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    if (date == java.time.LocalDate.now().toString()) "TODAY" else date.uppercase(),
                                    style = VyntaTypography.labelSmall,
                                    color = MaterialTheme.colorScheme.primary,
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(Modifier.width(12.dp))
                                HorizontalDivider(modifier = Modifier.weight(1f), color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f))
                            }
                        }
                    }

                    items(tasks, key = { it.id }) { task ->
                        HistoryTaskCard(task)
                    }
                }
            }
        }
    }
}

@Composable
fun StatBlock(value: String, label: String) {
    Column {
        Text(value, style = VyntaTypography.displayMedium, fontWeight = FontWeight.ExtraBold, fontSize = 32.sp)
        Text(label, style = VyntaTypography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
fun HistoryTaskCard(task: TaskEntity) {
    val isCompleted = task.status == "COMPLETED"
    
    VyntaCard(
        modifier = Modifier.fillMaxWidth(),
        containerColor = if (isCompleted) MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.15f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
        shape = VyntaShapes.large
    ) {
        Row(modifier = Modifier.padding(20.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .background(
                        if (isCompleted) MaterialTheme.colorScheme.primary.copy(alpha = 0.1f) 
                        else MaterialTheme.colorScheme.surfaceVariant, 
                        CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    if (isCompleted) Icons.Default.CheckCircle else Icons.Default.RadioButtonUnchecked, 
                    null, 
                    tint = if (isCompleted) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(20.dp)
                )
            }
            
            Spacer(Modifier.width(16.dp))
            
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    task.title, 
                    style = VyntaTypography.titleMedium, 
                    fontWeight = FontWeight.Bold,
                    textDecoration = if (isCompleted) TextDecoration.LineThrough else null,
                    color = if (isCompleted) MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f) else MaterialTheme.colorScheme.onSurface
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Schedule, null, modifier = Modifier.size(12.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(Modifier.width(4.dp))
                    Text(
                        task.deadlineTime ?: "Flexible", 
                        style = VyntaTypography.labelSmall, 
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            if (task.priority >= 4 && !isCompleted) {
                Surface(color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.2f), shape = ShapePill) {
                    Text("CRITICAL", style = VyntaTypography.labelSmall, modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp), color = MaterialTheme.colorScheme.error)
                }
            }
        }
    }
}
