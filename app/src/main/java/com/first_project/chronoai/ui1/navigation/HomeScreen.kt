package com.first_project.chronoai.ui1.navigation

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.ui.input.pointer.pointerInput
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
import android.os.Build
import android.view.HapticFeedbackConstants
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.ui.draw.blur
import androidx.compose.ui.graphics.graphicsLayer
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
    onNavigateToSettings: () -> Unit,
    onNavigateToDetail: (Int) -> Unit
) {
    val tasks by viewModel.personalTasks.collectAsState()
    val calendarEvents by viewModel.events.collectAsState()
    val selectedDate by viewModel.selectedDate.collectAsState()
    val briefing by viewModel.dailyBriefing.collectAsState()
    val forgottenTasks by viewModel.forgottenTasks.collectAsState()
    val progress by viewModel.completionProgress.collectAsState()
    val energyFilter by viewModel.energyFilter.collectAsState()
    val priorityFilter by viewModel.priorityFilter.collectAsState()
    val isCalendarLoading by viewModel.isCalendarLoading.collectAsState()
    
    val view = LocalView.current
    val context = LocalContext.current
    
    val dateListState = rememberLazyListState()
    val scrollState = rememberLazyListState()
    val today = remember { LocalDate.now() }

    val showStatusBarBlur by remember {
        derivedStateOf {
            scrollState.firstVisibleItemIndex > 0 || scrollState.firstVisibleItemScrollOffset > 10
        }
    }

    // Lambda Hoisting
    val onMicClick = remember(onNavigateToInput, view) {
        {
            view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
            onNavigateToInput(null)
        }
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
                state = scrollState,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 20.dp),
                contentPadding = PaddingValues(top = padding.calculateTopPadding(), bottom = 120.dp)
            ) {
                item(key = "briefing", contentType = "briefing") {
                    val greeting = remember {
                        when(Calendar.getInstance().get(Calendar.HOUR_OF_DAY)) {
                            in 0..11 -> "Good Morning"
                            in 12..16 -> "Good Afternoon"
                            else -> "Good Evening"
                        }
                    }
                    Column(modifier = Modifier.padding(top = 12.dp)) {
                        Text(
                            greeting,
                            style = MaterialTheme.typography.displayLarge,
                            fontWeight = FontWeight.Black,
                            color = MaterialTheme.colorScheme.onBackground,
                            lineHeight = 48.sp,
                            letterSpacing = (-2).sp,
                            modifier = Modifier.fillMaxWidth()
                        )

                        // Redemption Section on Home Screen
                        AnimatedVisibility(
                            visible = forgottenTasks.isNotEmpty() && selectedDate == today,
                            enter = expandVertically() + fadeIn(),
                            exit = shrinkVertically() + fadeOut()
                        ) {
                            Surface(
                                modifier = Modifier.padding(top = 16.dp).fillMaxWidth(),
                                shape = RoundedCornerShape(24.dp),
                                color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f),
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.secondary.copy(alpha = 0.2f))
                            ) {
                                Row(
                                    modifier = Modifier.padding(16.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Box(
                                        modifier = Modifier.size(40.dp).background(MaterialTheme.colorScheme.secondary, CircleShape),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(Icons.Default.Autorenew, null, tint = MaterialTheme.colorScheme.onSecondary, modifier = Modifier.size(20.dp))
                                    }
                                    Spacer(Modifier.width(16.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            "${forgottenTasks.size} tasks left behind",
                                            style = MaterialTheme.typography.titleSmall,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Text(
                                            "Redeem them for today?",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.8f)
                                        )
                                    }
                                    TextButton(
                                        onClick = { 
                                            view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                                            viewModel.moveForgottenTasksToToday(context, forgottenTasks) 
                                        }
                                    ) {
                                        Text("Move")
                                    }
                                }
                            }
                        }
                        
                        AnimatedVisibility(
                            visible = briefing.isNotEmpty(),
                            enter = expandVertically() + fadeIn(),
                            exit = shrinkVertically() + fadeOut()
                        ) {
                            Surface(
                                shape = MaterialTheme.shapes.large,
                                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f),
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)),
                                modifier = Modifier.padding(top = 12.dp)
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
                    val showTodayButton = selectedDate != today

                    Column(modifier = Modifier.padding(top = 24.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("SCHEDULE", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.secondary)
                            
                            AnimatedVisibility(
                                visible = showTodayButton,
                                enter = fadeIn() + slideInHorizontally { it / 2 },
                                exit = fadeOut() + slideOutHorizontally { it / 2 }
                            ) {
                                Button(
                                    onClick = { 
                                        viewModel.setSelectedDate(today)
                                    },
                                    contentPadding = PaddingValues(horizontal = 12.dp),
                                    modifier = Modifier.height(32.dp),
                                    shape = CircleShape,
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = MaterialTheme.colorScheme.primaryContainer,
                                        contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                                    )
                                ) {
                                    Icon(Icons.Default.Today, null, modifier = Modifier.size(16.dp))
                                    Spacer(Modifier.width(6.dp))
                                    Text("Today", style = MaterialTheme.typography.labelLarge)
                                }
                            }
                        }
                        
                        LazyRow(
                            state = dateListState,
                            modifier = Modifier.padding(top = 12.dp), 
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            items(45) { i ->
                                val date = today.minusDays(14).plusDays(i.toLong())
                                val isSelected = date == selectedDate
                                DateChip(date, isSelected) { 
                                    view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                                    viewModel.setSelectedDate(date) 
                                }
                            }
                        }
                        
                        LaunchedEffect(selectedDate) {
                            val index = java.time.temporal.ChronoUnit.DAYS.between(today.minusDays(14), selectedDate).toInt()
                            if (index in 0 until 45) {
                                dateListState.animateScrollToItem(index)
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

        // Status Bar Glass Blur Effect
        // Note: Using a fixed value of false as showStatusBarBlur is not in scope here.
        val blurAlpha by animateFloatAsState(
            targetValue = 0f,
            animationSpec = tween(300),
            label = "StatusBarBlurAlpha"
        )

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .windowInsetsTopHeight(WindowInsets.statusBars)
                .graphicsLayer { alpha = blurAlpha }
                .background(MaterialTheme.colorScheme.background.copy(alpha = 0.8f))
                .let { 
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                        it.blur(20.dp) 
                    } else it
                }
        )
    }
}

// HomeTopBar removed as per design update


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
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) { 
                view.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
                isExpanded = !isExpanded 
            }
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(
                    onClick = onToggle,
                    modifier = Modifier.pointerInput(Unit) {
                        awaitPointerEventScope {
                            while (true) {
                                awaitFirstDown(requireUnconsumed = false)
                                view.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
                                waitForUpOrCancellation()
                                view.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
                            }
                        }
                    }
                ) {
                    Icon(
                        if (isCompleted) Icons.Default.CheckCircle else Icons.Default.RadioButtonUnchecked,
                        null,
                        tint = if (isCompleted) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Spacer(Modifier.width(8.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            task.title,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            textDecoration = if (isCompleted) TextDecoration.LineThrough else null
                        )
                        if (task.isRecurring) {
                            Spacer(Modifier.width(6.dp))
                            Icon(
                                Icons.Default.Repeat, 
                                null, 
                                modifier = Modifier.size(14.dp),
                                tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.6f)
                            )
                        }
                    }
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
                    
                    if (task.schedulingReason != null) {
                        Surface(
                            shape = MaterialTheme.shapes.small,
                            color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.4f),
                            modifier = Modifier.padding(bottom = 12.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    Icons.Default.Info, 
                                    null, 
                                    modifier = Modifier.size(14.dp),
                                    tint = MaterialTheme.colorScheme.onSecondaryContainer
                                )
                                Spacer(Modifier.width(6.dp))
                                Text(
                                    task.schedulingReason,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSecondaryContainer
                                )
                            }
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        TextButton(onClick = onEdit) {
                            Icon(Icons.Default.Edit, null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(8.dp))
                            Text("Edit")
                        }
                        Spacer(Modifier.width(8.dp))
                        IconButton(onClick = onDelete) {
                            Icon(Icons.Default.DeleteOutline, null, tint = MaterialTheme.colorScheme.error.copy(alpha = 0.6f))
                        }
                    }
                }
            }
        }

        // Status Bar Glass Blur Effect
        // Note: Using a fixed value of false as showStatusBarBlur is not in scope here.
        val blurAlpha by animateFloatAsState(
            targetValue = 0f,
            animationSpec = tween(300),
            label = "StatusBarBlurAlpha"
        )

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .windowInsetsTopHeight(WindowInsets.statusBars)
                .graphicsLayer { alpha = blurAlpha }
                .background(MaterialTheme.colorScheme.background.copy(alpha = 0.8f))
                .let { 
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                        it.blur(20.dp) 
                    } else it
                }
        )
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
