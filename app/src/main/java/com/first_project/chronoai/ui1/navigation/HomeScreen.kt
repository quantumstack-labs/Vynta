package com.first_project.chronoai.ui1.navigation

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import android.view.HapticFeedbackConstants
import com.first_project.chronoai.ui1.utils.HapticManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.first_project.chronoai.data.local.entity.TaskEntity
import com.first_project.chronoai.ui.theme.*
import com.first_project.chronoai.ui1.viewmodel.HomeViewModel
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.*
import com.google.api.services.calendar.model.Event
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.rememberSwipeToDismissBoxState

sealed class TimelineItem {
    abstract val time: String
    abstract val sortTime: Long
    abstract val id: String

    data class Merged(val task: TaskEntity, val event: Event) : TimelineItem() {
        override val time: String = event.start.dateTime?.toString()?.substringAfter("T")?.substring(0, 5)
            ?: event.start.date?.toString() ?: "All Day"
        override val sortTime: Long = event.start.dateTime?.value ?: (event.start.date?.value ?: 0L)
        override val id: String = "merged_${task.id}_${event.id}"
    }

    data class StandaloneTask(val task: TaskEntity) : TimelineItem() {
        override val time: String = task.deadline?.substringAfter(" ")?.substring(0, 5) ?: "--:--"
        override val sortTime: Long = try {
            task.deadline?.let {
                LocalDateTime.parse(it, DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))
                    .atZone(java.time.ZoneId.systemDefault()).toEpochSecond()
            } ?: 0L
        } catch (e: Exception) { 0L }
        override val id: String = "task_${task.id}"
    }

    data class StandaloneEvent(val event: Event) : TimelineItem() {
        override val time: String = event.start.dateTime?.toString()?.substringAfter("T")?.substring(0, 5)
            ?: event.start.date?.toString() ?: "All Day"
        override val sortTime: Long = event.start.dateTime?.value ?: (event.start.date?.value ?: 0L)
        override val id: String = "event_${event.id ?: event.summary ?: UUID.randomUUID().toString()}"
    }

    data class GhostTask(val task: TaskEntity) : TimelineItem() {
        override val time: String = "GHOST"
        override val sortTime: Long = 0L // Top of the list or in gaps? 0 for now.
        override val id: String = "ghost_${task.id}"
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    viewModel: HomeViewModel,
    onNavigateToInput: (Int?) -> Unit,
    onNavigateToFocus: (String) -> Unit = {},
    onNavigateToBriefing: () -> Unit = {}
) {
    val tasks by viewModel.personalTasks.collectAsState()
    val calendarEvents by viewModel.events.collectAsState()
    val selectedDate by viewModel.selectedDate.collectAsState()
    val briefing by viewModel.dailyBriefing.collectAsState()
    val progress by viewModel.completionProgress.collectAsState()
    val forgottenTasks by viewModel.forgottenTasks.collectAsState()
    val isBehindSchedule by viewModel.isBehindSchedule.collectAsState()
    val error by viewModel.error.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(error) {
        error?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearError()
        }
    }
    
    val timelineItems = remember(tasks, calendarEvents, selectedDate, forgottenTasks) {
        val mergedItems = mutableListOf<TimelineItem>()
        val matchedTaskIds = mutableSetOf<Int>()

        calendarEvents.forEach { event ->
            val linkedTask = tasks.find { it.calendarEventId == event.id || it.calendarEventId == event.recurringEventId }
            if (linkedTask != null) {
                mergedItems.add(TimelineItem.Merged(linkedTask, event))
                matchedTaskIds.add(linkedTask.id)
            } else {
                mergedItems.add(TimelineItem.StandaloneEvent(event))
            }
        }

        tasks.filter { !matchedTaskIds.contains(it.id) }.forEach { task ->
            mergedItems.add(TimelineItem.StandaloneTask(task))
        }

        // Add Ghost Tasks (Forgotten tasks appearing in gaps for TODAY only)
        if (selectedDate == LocalDate.now()) {
            forgottenTasks.forEach { task ->
                mergedItems.add(TimelineItem.GhostTask(task))
            }
        }

        mergedItems.sortedBy { it.sortTime }
    }

    val context = LocalContext.current
    val today = remember { LocalDate.now() }
    var taskToDelete by remember { mutableStateOf<TaskEntity?>(null) }

    LaunchedEffect(Unit) { viewModel.fetchEvents() }

    Box(modifier = Modifier.fillMaxSize()) {
        AdaptiveMeshGradient()

        if (taskToDelete != null) {
            VyntaDeleteConfirmationDialog(
                taskTitle = taskToDelete?.title ?: "",
                onConfirm = {
                    taskToDelete?.let { viewModel.deleteTask(context, it) }
                    taskToDelete = null
                },
                onDismiss = { taskToDelete = null }
            )
        }

        Scaffold(
            containerColor = Color.Transparent,
            snackbarHost = { 
                SnackbarHost(
                    hostState = snackbarHostState,
                    modifier = Modifier
                        .navigationBarsPadding()
                        .padding(bottom = 112.dp) // Lift above the persistent dock
                ) { data ->
                    VyntaSnackbar(data)
                }
            }
        ) { padding ->
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(horizontal = 24.dp),
                contentPadding = PaddingValues(top = padding.calculateTopPadding(), bottom = 140.dp),
                verticalArrangement = Arrangement.spacedBy(24.dp)
            ) {
                item {
                    val greeting = remember {
                        val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
                        when (hour) {
                            in 0..11 -> "Good Morning"
                            in 12..16 -> "Good Afternoon"
                            else -> "Good Evening"
                        }
                    }
                    Column {
                        Text(greeting, style = VyntaTypography.displayLarge)
                        Text(LocalDate.now().format(DateTimeFormatter.ofPattern("EEEE, MMMM d")).uppercase(), 
                             style = VyntaTypography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }

                if (forgottenTasks.isNotEmpty() && selectedDate == LocalDate.now()) {
                    item {
                        MorningTriageCard(
                            count = forgottenTasks.size,
                            onAction = { action -> viewModel.triageLeftovers(context, action) }
                        )
                    }
                }

                item {
                    VyntaCard(
                        modifier = Modifier.fillMaxWidth().height(260.dp),
                        containerColor = MaterialTheme.colorScheme.primaryContainer,
                        contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                    ) {
                        Column(modifier = Modifier.padding(24.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text("A balanced\nday ahead", style = VyntaTypography.headlineLarge, lineHeight = 38.sp)
                                    Text("${tasks.count { it.status != "COMPLETED" }} critical tasks pending", style = VyntaTypography.bodyMedium)
                                }
                                
                                val infiniteTransition = rememberInfiniteTransition()
                                val scale by infiniteTransition.animateFloat(
                                    initialValue = 1f,
                                    targetValue = if (isBehindSchedule) 1.15f else 1f,
                                    animationSpec = infiniteRepeatable(
                                        animation = tween(800, easing = FastOutSlowInEasing),
                                        repeatMode = RepeatMode.Reverse
                                    )
                                )
                                
                                IconButton(
                                    onClick = { viewModel.requestAdaptiveReflow(context, "I'm running late, please re-balance my day.") },
                                    modifier = Modifier
                                        .scale(scale)
                                        .background(
                                            if (isBehindSchedule) MaterialTheme.colorScheme.error.copy(alpha = 0.2f) 
                                            else MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.1f), 
                                            CircleShape
                                        )
                                ) {
                                    Icon(
                                        Icons.Default.AutoAwesome, 
                                        "Reflow", 
                                        tint = if (isBehindSchedule) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onPrimaryContainer
                                    )
                                }
                            }
                            
                            Spacer(Modifier.height(12.dp))
                            
                            Surface(
                                onClick = onNavigateToBriefing,
                                shape = ShapePill,
                                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.1f),
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.2f))
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(Icons.Default.GraphicEq, null, modifier = Modifier.size(16.dp))
                                    Spacer(Modifier.width(8.dp))
                                    Text("BEGIN DAILY BRIEF", style = VyntaTypography.labelSmall, fontWeight = FontWeight.ExtraBold)
                                }
                            }
                            
                            if (isBehindSchedule) {
                                Spacer(Modifier.height(8.dp))
                                Surface(
                                    color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.7f),
                                    shape = ShapePill,
                                    onClick = { viewModel.requestAdaptiveReflow(context, "I'm running late, please re-balance my day.") }
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(Icons.Default.Warning, null, modifier = Modifier.size(14.dp), tint = MaterialTheme.colorScheme.onErrorContainer)
                                        Spacer(Modifier.width(8.dp))
                                        Text("Behind schedule. Tap to AI Reflow.", style = VyntaTypography.labelSmall, color = MaterialTheme.colorScheme.onErrorContainer)
                                    }
                                }
                            }
                            
                            Spacer(Modifier.weight(1f))
                            
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text("DAILY PROGRESS", style = VyntaTypography.labelSmall)
                                Spacer(Modifier.weight(1f))
                                Text("${(progress * 100f).toInt()}%", style = VyntaTypography.titleLarge)
                            }
                            Spacer(Modifier.height(8.dp))
                            LiquidProgressIndicator(
                                progress = progress,
                                modifier = Modifier.fillMaxWidth().height(12.dp),
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                    }
                }

                item {
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("WEEKLY VIEW", style = VyntaTypography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Spacer(Modifier.weight(1f))
                            TextButton(onClick = { viewModel.setSelectedDate(today) }) {
                                Text("TODAY", style = VyntaTypography.labelSmall, color = MaterialTheme.colorScheme.primary)
                            }
                        }
                        Spacer(Modifier.height(8.dp))
                        val scrollState = rememberScrollState()
                        val density = LocalDensity.current
                        val screenWidth = androidx.compose.ui.platform.LocalConfiguration.current.screenWidthDp.dp
                        val view = LocalView.current
                        
                        // Scroll to center today on first launch or when "TODAY" is clicked
                        LaunchedEffect(selectedDate) {
                            if (selectedDate == today) {
                                val itemWidth = 80.dp
                                val spacing = 12.dp
                                val index = 7 
                                val totalItemWidth = itemWidth + spacing
                                val centerOffset = (totalItemWidth * index) + (itemWidth / 2) - (screenWidth / 2)
                                val offsetPx = with(density) { centerOffset.toPx() }.toInt()
                                scrollState.animateScrollTo(if (offsetPx > 0) offsetPx else 0)
                            }
                        }

                        // Subtle haptic feedback when scrolling
                        LaunchedEffect(scrollState.value) {
                            if (scrollState.isScrollInProgress) {
                                val step = with(density) { 92.dp.toPx() }
                                if (scrollState.value % step.toInt() < 20) {
                                    view.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
                                }
                            }
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth().horizontalScroll(scrollState),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            repeat(30) { i ->
                                val date = today.minusDays(7).plusDays(i.toLong())
                                val isSelected = date == selectedDate
                                DateChip(date, isSelected) { 
                                    view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                                    viewModel.setSelectedDate(date) 
                                }
                            }
                        }
                    }
                }

                item {
                    Text("VYNTA SCHEDULE", style = VyntaTypography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }

                itemsIndexed(timelineItems, key = { _, item -> item.id }) { index, item ->
                    VyntaTimelineCard(
                        item = item,
                        onToggleTask = { task -> viewModel.toggleTaskCompletion(context, task) },
                        onEditTask = { taskId -> onNavigateToInput(taskId) },
                        onDeleteTask = { task -> taskToDelete = task },
                        onStartFocus = { title -> onNavigateToFocus(title) }
                    )
                }
            }
        }
    }
}

@Composable
fun DateChip(date: LocalDate, isSelected: Boolean, onClick: () -> Unit) {
    Surface(
        modifier = Modifier.size(width = 80.dp, height = 100.dp).clickable(onClick = onClick),
        shape = VyntaShapes.large,
        color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
        contentColor = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
            Text(date.dayOfWeek.name.take(3), style = VyntaTypography.labelSmall)
            Text(date.dayOfMonth.toString(), style = VyntaTypography.titleLarge, fontWeight = FontWeight.ExtraBold)
        }
    }
}

@Composable
fun MorningTriageCard(count: Int, onAction: (HomeViewModel.TriageAction) -> Unit) {
    VyntaCard(
        modifier = Modifier.fillMaxWidth(),
        containerColor = MaterialTheme.colorScheme.tertiaryContainer,
        contentColor = MaterialTheme.colorScheme.onTertiaryContainer
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.History, null, modifier = Modifier.size(24.dp))
                Spacer(Modifier.width(12.dp))
                Text("Yesterday's Leftovers", style = VyntaTypography.titleMedium)
            }
            Spacer(Modifier.height(8.dp))
            Text(
                "I noticed $count tasks didn't get done yesterday. Shall we find a spot for them in today's flow?",
                style = VyntaTypography.bodyMedium
            )
            Spacer(Modifier.height(16.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(
                    onClick = { onAction(HomeViewModel.TriageAction.MOVE_TO_TODAY) },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.tertiary)
                ) {
                    Text("Today", style = VyntaTypography.labelSmall)
                }
                OutlinedButton(
                    onClick = { onAction(HomeViewModel.TriageAction.MOVE_TO_TOMORROW) },
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Tomorrow", style = VyntaTypography.labelSmall)
                }
                IconButton(onClick = { onAction(HomeViewModel.TriageAction.DISMISS) }) {
                    Icon(Icons.Default.Close, null)
                }
            }
        }
    }
}

@Composable
fun VyntaTimelineCard(
    item: TimelineItem,
    onToggleTask: (TaskEntity) -> Unit,
    onEditTask: (Int) -> Unit,
    onDeleteTask: (TaskEntity) -> Unit,
    onStartFocus: (String) -> Unit = {}
) {
    val context = LocalContext.current
    val haptic = remember { HapticManager(context) }

    val task = when(item) {
        is TimelineItem.Merged -> item.task
        is TimelineItem.StandaloneTask -> item.task
        is TimelineItem.GhostTask -> item.task
        else -> null
    }
    val isGhost = item is TimelineItem.GhostTask
    val isCompleted = task?.status == "COMPLETED"
    
    val dismissState = rememberSwipeToDismissBoxState(
        positionalThreshold = { distance -> distance * 0.7f },
        confirmValueChange = { value ->
            when (value) {
                SwipeToDismissBoxValue.StartToEnd -> {
                    haptic.play(HapticManager.VyntaEffect.CLICK)
                    // Edit action
                    task?.let { onEditTask(it.id) }
                    false // Don't actually dismiss the card
                }
                SwipeToDismissBoxValue.EndToStart -> {
                    haptic.play(HapticManager.VyntaEffect.AI_CRUNCHING)
                    // Trigger confirmation dialog instead of immediate delete
                    task?.let { onDeleteTask(it) }
                    false // Don't dismiss until confirmed
                }
                else -> false
            }
        }
    )

    // Trigger haptic when swiping across the threshold
    LaunchedEffect(dismissState.targetValue) {
        if (dismissState.targetValue != SwipeToDismissBoxValue.Settled) {
            haptic.play(HapticManager.VyntaEffect.AI_CRUNCHING) // Granular feedback while swiping
        }
    }

    SwipeToDismissBox(
        state = dismissState,
        enableDismissFromStartToEnd = !isGhost && task != null,
        enableDismissFromEndToStart = !isGhost && task != null,
        backgroundContent = {
            val direction = dismissState.dismissDirection
            val color = when (direction) {
                SwipeToDismissBoxValue.StartToEnd -> MaterialTheme.colorScheme.primaryContainer
                SwipeToDismissBoxValue.EndToStart -> MaterialTheme.colorScheme.errorContainer
                else -> Color.Transparent
            }
            val alignment = when (direction) {
                SwipeToDismissBoxValue.StartToEnd -> Alignment.CenterStart
                SwipeToDismissBoxValue.EndToStart -> Alignment.CenterEnd
                else -> Alignment.Center
            }
            val icon = when (direction) {
                SwipeToDismissBoxValue.StartToEnd -> Icons.Default.Edit
                SwipeToDismissBoxValue.EndToStart -> Icons.Default.Delete
                else -> null
            }
            val label = when (direction) {
                SwipeToDismissBoxValue.StartToEnd -> "EDIT"
                SwipeToDismissBoxValue.EndToStart -> "DELETE"
                else -> ""
            }

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(vertical = 4.dp)
                    .background(color, VyntaShapes.large)
                    .padding(horizontal = 24.dp),
                contentAlignment = alignment
            ) {
                if (icon != null) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (direction == SwipeToDismissBoxValue.StartToEnd) {
                            Icon(icon, null, tint = MaterialTheme.colorScheme.onPrimaryContainer)
                            Spacer(Modifier.width(12.dp))
                            Text(label, style = VyntaTypography.labelSmall, color = MaterialTheme.colorScheme.onPrimaryContainer)
                        } else {
                            Text(label, style = VyntaTypography.labelSmall, color = MaterialTheme.colorScheme.onErrorContainer)
                            Spacer(Modifier.width(12.dp))
                            Icon(icon, null, tint = MaterialTheme.colorScheme.onErrorContainer)
                        }
                    }
                }
            }
        }
    ) {
        VyntaCard(
            modifier = Modifier.fillMaxWidth().alpha(if (isGhost) 0.6f else 1f),
            onClick = { 
                if (isGhost) {
                    task?.let { onToggleTask(it.copy(status = "SCHEDULED")) }
                } else {
                    task?.let { onEditTask(it.id) } 
                }
            },
            containerColor = when {
                isGhost -> Color.Transparent
                isCompleted -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.15f)
                else -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
            },
            showBorder = isGhost,
            shape = VyntaShapes.large
        ) {
            Row(modifier = Modifier.padding(24.dp), verticalAlignment = Alignment.CenterVertically) {
                if (isGhost) {
                    Icon(Icons.Default.AutoAwesome, null, tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f))
                    Spacer(Modifier.width(12.dp))
                } else if (task != null) {
                    VyntaCheckbox(
                        checked = isCompleted,
                        onCheckedChange = { 
                            haptic.play(HapticManager.VyntaEffect.TASK_COMPLETE)
                            onToggleTask(task) 
                        }
                    )
                    Spacer(Modifier.width(16.dp))
                } else {
                    Box(
                        modifier = Modifier.size(48.dp).background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.Event, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                    }
                    Spacer(Modifier.width(20.dp))
                }
                
                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            task?.title ?: (item as? TimelineItem.StandaloneEvent)?.event?.summary ?: "Untitled",
                            style = VyntaTypography.titleMedium,
                            textDecoration = if (isCompleted) TextDecoration.LineThrough else null,
                            color = if (isCompleted) MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f) else MaterialTheme.colorScheme.onSurface
                        )
                        if (isGhost) {
                            Spacer(Modifier.width(8.dp))
                            Surface(color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f), shape = ShapePill) {
                                Text("GHOST", style = VyntaTypography.labelSmall, modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp), color = MaterialTheme.colorScheme.onSecondaryContainer)
                            }
                        } else if (task?.priority ?: 0 >= 4 && !isCompleted) {
                            Spacer(Modifier.width(8.dp))
                            Surface(color = MaterialTheme.colorScheme.errorContainer, shape = ShapePill) {
                                Text("HIGH", style = VyntaTypography.labelSmall, modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp), color = MaterialTheme.colorScheme.onErrorContainer)
                            }
                        }
                    }
                    Text(
                        if (isGhost) "Yesterday's leftover. Tap to schedule." else (task?.schedulingReason ?: "Scheduled event from life flow."), 
                        style = VyntaTypography.bodyMedium, 
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = if (isCompleted || isGhost) 0.5f else 1f),
                        maxLines = 2
                    )
                    Spacer(Modifier.height(8.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Schedule, null, modifier = Modifier.size(14.dp), tint = MaterialTheme.colorScheme.primary)
                        Spacer(Modifier.width(6.dp))
                        Text(if (isGhost) "Unscheduled" else item.time, style = VyntaTypography.labelSmall, color = MaterialTheme.colorScheme.primary)
                        
                        if (task != null && task.energyLevel == "High" && !isCompleted && !isGhost) {
                            Spacer(Modifier.width(16.dp))
                            TextButton(
                                onClick = { onStartFocus(task.title) },
                                modifier = Modifier.height(28.dp),
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 0.dp)
                            ) {
                                Icon(Icons.Default.Shield, null, modifier = Modifier.size(14.dp))
                                Spacer(Modifier.width(4.dp))
                                Text("SHIELD", style = VyntaTypography.labelSmall)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun VyntaSnackbar(data: SnackbarData) {
    Surface(
        modifier = Modifier
            .padding(horizontal = 16.dp)
            .fillMaxWidth(),
        shape = ShapePill,
        color = Color(0xFF0D0D0D).copy(alpha = 0.9f), // Vynta Surface Deep
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)),
        shadowElevation = 8.dp
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                Icons.Default.AutoAwesome, 
                contentDescription = null, 
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(18.dp)
            )
            Spacer(Modifier.width(12.dp))
            Text(
                text = data.visuals.message.uppercase(),
                style = VyntaTypography.labelSmall.copy(
                    letterSpacing = 1.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                ),
                maxLines = 2
            )
        }
    }
}

@Composable
fun VyntaDeleteConfirmationDialog(
    taskTitle: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Color(0xFF1A1A1A),
        title = {
            Text(
                "DELETION PROTOCOL",
                style = VyntaTypography.labelSmall,
                color = MaterialTheme.colorScheme.error,
                letterSpacing = 2.sp
            )
        },
        text = {
            Column {
                Text(
                    "Are you sure you want to remove this mission from your timeline?",
                    style = VyntaTypography.bodyLarge,
                    color = Color.White
                )
                Spacer(Modifier.height(12.dp))
                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f),
                    shape = VyntaShapes.medium,
                    border = BorderStroke(1.dp, Color.White.copy(alpha = 0.1f))
                ) {
                    Text(
                        taskTitle,
                        modifier = Modifier.padding(16.dp),
                        style = VyntaTypography.titleMedium,
                        color = Color.White.copy(alpha = 0.7f)
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onConfirm,
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
            ) {
                Text("ERASE", style = VyntaTypography.labelSmall, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("ABORT", style = VyntaTypography.labelSmall, color = Color.White.copy(alpha = 0.6f))
            }
        },
        shape = VyntaShapes.large
    )
}

// Removed Bento components and moved to GlassComponents.kt

// Reuse TimelineItem from original file but keep it in sync
