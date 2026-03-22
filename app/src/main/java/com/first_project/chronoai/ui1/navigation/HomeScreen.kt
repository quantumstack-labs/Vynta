package com.first_project.chronoai.ui1.navigation

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import android.view.HapticFeedbackConstants
import com.first_project.chronoai.data.local.entity.TaskEntity
import com.first_project.chronoai.ui.theme.*
import com.first_project.chronoai.ui1.viewmodel.HomeViewModel
import java.time.LocalDate
import java.util.*

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun HomeScreen(
    viewModel: HomeViewModel,
    onNavigateToInput: (Int?) -> Unit,
    onNavigateToHistory: () -> Unit,
    onNavigateToSettings: () -> Unit
) {
    val tasks by viewModel.personalTasks.collectAsState()
    val calendarEvents by viewModel.events.collectAsState()
    val selectedDate by viewModel.selectedDate.collectAsState()
    val briefing by viewModel.dailyBriefing.collectAsState()
    val progress by viewModel.completionProgress.collectAsState()
    val energyFilter by viewModel.energyFilter.collectAsState()
    val priorityFilter by viewModel.priorityFilter.collectAsState()
    val isCalendarLoading by viewModel.isCalendarLoading.collectAsState()
    
    val view = LocalView.current
    val context = LocalContext.current

    // Lambda Hoisting
    val onMicClick = remember(onNavigateToInput, view) {
        {
            view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
            onNavigateToInput(null)
        }
    }

    val onSettingsClickMemo = remember(onNavigateToSettings) {
        { onNavigateToSettings() }
    }

    LaunchedEffect(Unit) {
        viewModel.fetchEvents()
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Scaffold(
            containerColor = MaterialTheme.colorScheme.background,
            floatingActionButton = {
                FloatingActionButton(
                    onClick = onMicClick,
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                    shape = CircleShape,
                    modifier = Modifier.padding(bottom = 90.dp)
                ) {
                    Icon(Icons.Default.Mic, null)
                }
            }
        ) { padding ->
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(padding).padding(horizontal = 20.dp),
                contentPadding = PaddingValues(bottom = 120.dp)
            ) {
                item(key = "topbar", contentType = "header") { 
                    HomeTopBar(onSettingsClick = onSettingsClickMemo) 
                }
                
                item(key = "briefing", contentType = "briefing") {
                    AnimatedVisibility(
                        visible = briefing.isNotEmpty(),
                        enter = expandVertically() + fadeIn(),
                        exit = shrinkVertically() + fadeOut()
                    ) {
                        Column(modifier = Modifier.padding(top = 16.dp)) {
                            val greeting = remember {
                                when(Calendar.getInstance().get(Calendar.HOUR_OF_DAY)) {
                                    in 0..11 -> "Good Morning"
                                    in 12..16 -> "Good Afternoon"
                                    else -> "Good Evening"
                                }
                            }
                            Text(greeting, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
                            Spacer(Modifier.height(12.dp))
                            
                            Surface(
                                shape = MaterialTheme.shapes.large,
                                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f),
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
                            ) {
                                Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.AutoAwesome, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                                    Spacer(Modifier.width(12.dp))
                                    Text(
                                        briefing,
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                }
                            }
                        }
                    }
                }

                item(key = "progress", contentType = "stats") {
                    Column(modifier = Modifier.padding(top = 24.dp)) {
                        Text("DAILY PROGRESS", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.secondary)
                        Spacer(Modifier.height(8.dp))
                        val animatedProgress by animateFloatAsState(
                            targetValue = progress, 
                            animationSpec = spring(stiffness = Spring.StiffnessLow, dampingRatio = Spring.DampingRatioMediumBouncy),
                            label = "ProgressAnimation"
                        )
                        LinearProgressIndicator(
                            progress = { animatedProgress },
                            modifier = Modifier.fillMaxWidth().height(8.dp).clip(CircleShape),
                            color = MaterialTheme.colorScheme.primary,
                            trackColor = MaterialTheme.colorScheme.surfaceVariant
                        )
                    }
                }

                item(key = "dates", contentType = "date_selector") {
                    LazyRow(
                        modifier = Modifier.padding(top = 24.dp), 
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        items(14) { i ->
                            val date = remember { LocalDate.now().plusDays(i.toLong()) }
                            val isSelected = date == selectedDate
                            DateChip(date, isSelected) { 
                                view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                                viewModel.setSelectedDate(date) 
                            }
                        }
                    }
                }

                item(key = "filters", contentType = "filters") {
                    Column(modifier = Modifier.padding(top = 24.dp)) {
                        Text("ACTIVE FILTERS", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.secondary)
                        FlowRow(
                            modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            EnergyFilterChip("Low", energyFilter == "Low") { viewModel.setEnergyFilter("Low") }
                            EnergyFilterChip("Medium", energyFilter == "Medium") { viewModel.setEnergyFilter("Medium") }
                            EnergyFilterChip("High", energyFilter == "High") { viewModel.setEnergyFilter("High") }
                            
                            FilterChip(
                                selected = priorityFilter == 5,
                                onClick = { viewModel.setPriorityFilter(if (priorityFilter == 5) null else 5) },
                                label = { Text("Critical Only") },
                                leadingIcon = if (priorityFilter == 5) { { Icon(Icons.Default.Check, null, modifier = Modifier.size(16.dp)) } } else null,
                                colors = FilterChipDefaults.filterChipColors(selectedContainerColor = Color(0xFFE57373).copy(alpha = 0.2f), selectedLabelColor = Color(0xFFE57373))
                            )
                        }
                    }
                }

                item(key = "calendar_header", contentType = "section_header") {
                    Row(
                        modifier = Modifier.padding(top = 32.dp, bottom = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(modifier = Modifier.size(6.dp).background(Color(0xFF4CAF50), CircleShape))
                        Spacer(Modifier.width(8.dp))
                        Text("CALENDAR EVENTS", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        if (isCalendarLoading) {
                            Spacer(Modifier.width(12.dp))
                            CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                        }
                    }
                }

                if (calendarEvents.isEmpty() && !isCalendarLoading) {
                    item(key = "no_events", contentType = "empty_state") {
                        Text("No events for this day", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(vertical = 8.dp))
                    }
                } else {
                    itemsIndexed(
                        calendarEvents, 
                        key = { _, event -> event.id ?: event.summary ?: "" },
                        contentType = { _, _ -> "calendar_event" }
                    ) { _, event ->
                        val startTime = remember(event) {
                            event.start.dateTime?.toString()?.substringAfter("T")?.substring(0, 5) 
                                ?: event.start.date?.toString() ?: "All Day"
                        }
                        UpcomingEventCard(event.summary ?: "No Title", startTime)
                        Spacer(Modifier.height(12.dp))
                    }
                }

                item(key = "schedule_header", contentType = "section_header") {
                    Row(modifier = Modifier.padding(top = 24.dp, bottom = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                        Box(modifier = Modifier.size(6.dp).background(MaterialTheme.colorScheme.primary, CircleShape))
                        Spacer(Modifier.width(8.dp))
                        Text("VYNTA SCHEDULE", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }

                itemsIndexed(
                    tasks, 
                    key = { _, task -> task.id },
                    contentType = { _, _ -> "task_item" }
                ) { index, task ->
                    val time = remember(task.deadline) {
                        task.deadline?.substringAfter(" ")?.substring(0, 5) ?: "--:--"
                    }
                    Box(modifier = Modifier.animateItem()) {
                        UpcomingTaskCard(
                            task = task, 
                            time = time, 
                            onToggle = { viewModel.toggleTaskCompletion(context, task) },
                            onDelete = { 
                                view.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
                                viewModel.deleteTask(context, task) 
                            },
                            onEdit = { onNavigateToInput(task.id) }
                        )
                    }
                    Spacer(Modifier.height(12.dp))
                }
            }
        }
    }
}

@Composable
fun HomeTopBar(onSettingsClick: () -> Unit) {
    val view = LocalView.current
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column {
            Text(
                "VYNTA",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.ExtraBold,
                letterSpacing = 4.sp,
                color = MaterialTheme.colorScheme.primary
            )
            Text(
                "Elevate your day",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.secondary,
                letterSpacing = 1.sp
            )
        }
        IconButton(onClick = {
            view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
            onSettingsClick()
        }) {
            Icon(Icons.Default.Settings, null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
fun EnergyFilterChip(label: String, isSelected: Boolean, onClick: () -> Unit) {
    FilterChip(
        selected = isSelected,
        onClick = onClick,
        label = { Text(label) },
        leadingIcon = if (isSelected) { { Icon(Icons.Default.Check, null, modifier = Modifier.size(16.dp)) } } else null
    )
}

@Composable
fun UpcomingEventCard(title: String, time: String) {
    Surface(
        shape = RoundedCornerShape(20.dp),
        color = Color(0xFF4CAF50).copy(alpha = 0.1f),
        border = BorderStroke(1.dp, Color(0xFF4CAF50).copy(alpha = 0.2f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.Event, null, tint = Color(0xFF4CAF50))
            Spacer(Modifier.width(16.dp))
            Column {
                Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Text(time, style = MaterialTheme.typography.labelSmall, color = Color(0xFF4CAF50))
            }
        }
    }
}

@Composable
fun UpcomingTaskCard(
    task: TaskEntity,
    time: String,
    onToggle: () -> Unit,
    onDelete: () -> Unit,
    onEdit: () -> Unit
) {
    var isExpanded by remember { mutableStateOf(false) }
    val isCompleted = task.status == "COMPLETED"
    val view = LocalView.current
    
    Surface(
        shape = RoundedCornerShape(24.dp),
        color = if (isCompleted) MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f) else MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
        modifier = Modifier.fillMaxWidth()
            .clickable { 
                view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                isExpanded = !isExpanded 
            }
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onToggle) {
                    Icon(
                        if (isCompleted) Icons.Default.CheckCircle else Icons.Default.RadioButtonUnchecked,
                        null,
                        tint = if (isCompleted) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Spacer(Modifier.width(8.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        task.title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        textDecoration = if (isCompleted) TextDecoration.LineThrough else null
                    )
                    Text(time, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                }
                
                val rotation by animateFloatAsState(if (isExpanded) 180f else 0f)
                Icon(
                    Icons.Default.ExpandMore, 
                    null, 
                    modifier = Modifier.rotate(rotation).size(20.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            
            AnimatedVisibility(
                visible = isExpanded,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut()
            ) {
                Column(modifier = Modifier.padding(top = 16.dp, start = 48.dp)) {
                    if (task.subtasks.isNotEmpty()) {
                        task.subtasks.forEach { subtask ->
                            Row(
                                modifier = Modifier.padding(vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(modifier = Modifier.size(6.dp).background(MaterialTheme.colorScheme.primary.copy(alpha = 0.5f), CircleShape))
                                Spacer(Modifier.width(12.dp))
                                Text(subtask, style = MaterialTheme.typography.bodyMedium)
                            }
                        }
                        Spacer(Modifier.height(16.dp))
                    } else {
                        Text("No subtasks added", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Spacer(Modifier.height(16.dp))
                    }
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        TextButton(onClick = onEdit) {
                            Icon(Icons.Default.Edit, null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(8.dp))
                            Text("Edit Details")
                        }
                        Spacer(Modifier.width(8.dp))
                        IconButton(onClick = onDelete) {
                            Icon(Icons.Default.DeleteOutline, null, tint = MaterialTheme.colorScheme.error.copy(alpha = 0.6f))
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun DateChip(date: LocalDate, isSelected: Boolean, onClick: () -> Unit) {
    val backgroundColor by animateColorAsState(
        targetValue = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
        animationSpec = tween(300),
        label = "DateChipBg"
    )
    val contentColor by animateColorAsState(
        targetValue = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
        animationSpec = tween(300),
        label = "DateChipContent"
    )

    Surface(
        modifier = Modifier
            .width(60.dp)
            .height(80.dp)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        color = backgroundColor
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = date.dayOfWeek.getDisplayName(java.time.format.TextStyle.SHORT, Locale.getDefault()).uppercase(),
                style = MaterialTheme.typography.labelSmall,
                color = contentColor.copy(alpha = 0.7f)
            )
            Text(
                text = date.dayOfMonth.toString(),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = contentColor
            )
        }
    }
}
