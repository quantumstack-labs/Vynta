package com.first_project.chronoai.ui1.navigation

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.EventNote
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import android.view.HapticFeedbackConstants
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.first_project.chronoai.ai.GroqManager
import com.first_project.chronoai.data.local.db.DatabaseProvider
import com.first_project.chronoai.data.local.entity.TaskEntity
import com.first_project.chronoai.BuildConfig
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.LocalDate

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistoryScreen() {
    val context = LocalContext.current
    val db = remember { DatabaseProvider.getDatabase(context) }
    val tasks by db.taskDao().getAllTasks().collectAsState(initial = emptyList())
    val view = LocalView.current
    val scope = rememberCoroutineScope()
    
    val groqManager = remember { GroqManager(BuildConfig.GROQ_API_KEY) }
    var aiMomentumMessage by remember { mutableStateOf("Great consistency!") }
    var aiWinCelebration by remember { mutableStateOf("") }
    var aiRedemptionMessage by remember { mutableStateOf("") }
    var isGeneratingMessage by remember { mutableStateOf(false) }

    val todayStr = remember { LocalDate.now().toString() }

    val completedCount by remember {
        derivedStateOf { tasks.count { it.status == "COMPLETED" } }
    }
    
    val winOfTheDay by remember {
        derivedStateOf {
            tasks.filter { it.status == "COMPLETED" && it.deadline?.startsWith(todayStr) == true }
                .maxWithOrNull(compareBy({ it.priority }, { it.subtasks.size }))
        }
    }

    val forgottenTasks by remember {
        derivedStateOf {
            tasks.filter { 
                val taskDate = it.deadline?.split(" ")?.firstOrNull()
                it.status != "COMPLETED" && taskDate != null && taskDate < todayStr
            }
        }
    }
    
    val progress by remember { 
        derivedStateOf { if (tasks.isNotEmpty()) completedCount.toFloat() / tasks.size else 0f }
    }

    LaunchedEffect(completedCount, tasks.size, winOfTheDay, forgottenTasks.size) {
        if (tasks.isNotEmpty()) {
            val progressPercent = (progress * 100).toInt()
            
            isGeneratingMessage = true
            
            val prompt = when {
                forgottenTasks.isNotEmpty() -> {
                    """
                    Context: User has ${forgottenTasks.size} tasks forgotten from previous days. 
                    Example task: "${forgottenTasks.first().title}".
                    Provide a supportive, non-guilt-tripping "Redemption" message (max 10 words) encouraging them to move these to today.
                    Format: "Redemption: [Your message]"
                    """
                }
                winOfTheDay != null -> {
                    """
                    Task: "${winOfTheDay?.title}" was the user's biggest completion today.
                    Provide a very short, punchy celebration (max 8 words) explaining why completing a high-priority task like this matters.
                    Format: "Victory Note: [Your message]"
                    """
                }
                else -> {
                    """
                    Analyze stats: $completedCount finished out of ${tasks.size}.
                    Provide a short, professional "Weekly Momentum" message (under 6 words).
                    Respond ONLY with the message.
                    """
                }
            }
            
            try {
                val response = groqManager.analyzeTask("", prompt)
                if (response.isNotBlank() && !response.contains("Error")) {
                    when {
                        response.contains("Redemption:") -> {
                            aiRedemptionMessage = response.substringAfter("Redemption:").trim().replace("\"", "")
                        }
                        winOfTheDay != null && response.contains("Victory Note:") -> {
                            aiWinCelebration = response.substringAfter("Victory Note:").trim().replace("\"", "")
                        }
                        else -> {
                            aiMomentumMessage = response.replace("\"", "")
                        }
                    }
                }
            } catch (e: Exception) {
            } finally {
                isGeneratingMessage = false
            }
        }
    }

    var selectedFilter by remember { mutableStateOf("All") }

    // Optimization: Deferred calculation using derivedStateOf for filtering
    val filteredTasks by remember {
        derivedStateOf {
            when (selectedFilter) {
                "Done" -> tasks.filter { it.status == "COMPLETED" }
                "Pending" -> tasks.filter { it.status != "COMPLETED" }
                "High Energy" -> tasks.filter { it.energyLevel == "High" }
                else -> tasks
            }
        }
    }

    // Optimization: Grouping logic also inside derivedStateOf
    val groupedTasks by remember {
        derivedStateOf {
            filteredTasks.sortedByDescending { it.id }.groupBy { 
                it.deadline?.split(" ")?.firstOrNull() ?: "Unscheduled"
            }
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Scaffold(
            containerColor = MaterialTheme.colorScheme.background,
            topBar = {
                CenterAlignedTopAppBar(
                    title = { Text("Logbook", fontWeight = FontWeight.ExtraBold, letterSpacing = 1.sp) },
                    colors = TopAppBarDefaults.centerAlignedTopAppBarColors(containerColor = Color.Transparent)
                )
            }
        ) { padding ->
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(padding).padding(horizontal = 20.dp),
                contentPadding = PaddingValues(bottom = 120.dp)
            ) {
                item(key = "momentum_card", contentType = "momentum") {
                    Column(modifier = Modifier.padding(top = 16.dp)) {
                        // Smart Redemption Card (If there are forgotten tasks)
                        if (forgottenTasks.isNotEmpty()) {
                            RedemptionCard(
                                count = forgottenTasks.size,
                                message = aiRedemptionMessage,
                                onRedeem = {
                                    scope.launch {
                                        withContext(kotlinx.coroutines.Dispatchers.IO) {
                                            forgottenTasks.forEach { task ->
                                                val newDeadline = todayStr + (task.deadline?.substringAfter(" ")?.let { " $it" } ?: "")
                                                db.taskDao().updateTask(task.copy(deadline = newDeadline))
                                            }
                                            com.first_project.chronoai.ui1.widget.updateVyntaWidgets(context)
                                        }
                                    }
                                }
                            )
                            Spacer(Modifier.height(16.dp))
                        }

                        // Win of the Day Highlight (Only if there is a win)
                        winOfTheDay?.let { win ->
                            WinOfTheDayCard(win, aiWinCelebration)
                            Spacer(Modifier.height(16.dp))
                        }

                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(28.dp),
                            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.1f))
                        ) {
                            Column(modifier = Modifier.padding(24.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box(
                                        modifier = Modifier.size(48.dp).background(MaterialTheme.colorScheme.primary, CircleShape),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(Icons.Default.Insights, null, tint = MaterialTheme.colorScheme.onPrimary)
                                    }
                                    Spacer(Modifier.width(16.dp))
                                    Column {
                                        Text("Weekly Momentum", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
                                        Text(
                                            text = aiMomentumMessage, 
                                            style = MaterialTheme.typography.titleMedium, 
                                            fontWeight = FontWeight.Bold,
                                            modifier = Modifier.animateContentSize()
                                        )
                                    }
                                }
                                
                                Spacer(Modifier.height(24.dp))
                                
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                    InsightStat("Finished", completedCount.toString())
                                    InsightStat("Total", tasks.size.toString())
                                    InsightStat("Rate", "${(progress * 100).toInt()}%")
                                }
                                
                                Spacer(Modifier.height(20.dp))
                                
                                val animatedProgress by animateFloatAsState(targetValue = progress, animationSpec = spring())
                                LinearProgressIndicator(
                                    progress = { animatedProgress },
                                    modifier = Modifier.fillMaxWidth().height(10.dp).clip(CircleShape),
                                    color = MaterialTheme.colorScheme.primary,
                                    trackColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
                                )
                            }
                        }
                    }
                }

                item(key = "filters", contentType = "filters") {
                    LazyRow(
                        modifier = Modifier.padding(top = 24.dp, bottom = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        val filters = listOf("All", "Done", "Pending", "High Energy")
                        items(filters, key = { it }) { filter ->
                            FilterChip(
                                selected = selectedFilter == filter,
                                onClick = { 
                                    view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                                    selectedFilter = filter 
                                },
                                label = { Text(filter) },
                                shape = CircleShape,
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = MaterialTheme.colorScheme.primary,
                                    selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                                )
                            )
                        }
                    }
                }

                item(key = "timeline_label", contentType = "header") {
                    Text(
                        "TIMELINE",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 16.dp, bottom = 16.dp),
                        letterSpacing = 2.sp
                    )
                }

                groupedTasks.forEach { (date, tasksInGroup) ->
                    item(key = "header_$date", contentType = "date_header") {
                        Text(
                            text = if (date == todayStr) "TODAY" else date,
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(vertical = 8.dp)
                        )
                    }
                    items(tasksInGroup, key = { it.id }, contentType = { "task_card" }) { task ->
                        HistoryTaskCard(task)
                        Spacer(Modifier.height(12.dp))
                    }
                }
            }
        }
    }
}

@Composable
fun RedemptionCard(count: Int, message: String, onRedeem: () -> Unit) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(28.dp),
        color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.7f),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.secondary.copy(alpha = 0.2f))
    ) {
        Row(
            modifier = Modifier.padding(20.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .background(MaterialTheme.colorScheme.secondary, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Default.Autorenew,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSecondary,
                    modifier = Modifier.size(32.dp)
                )
            }
            
            Spacer(Modifier.width(20.dp))
            
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    "REDEMPTION",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.secondary,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
                )
                Text(
                    "$count tasks left behind",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSecondaryContainer
                )
                if (message.isNotBlank()) {
                    Text(
                        message,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.7f),
                        modifier = Modifier.padding(top = 2.dp)
                    )
                }
                
                Button(
                    onClick = onRedeem,
                    modifier = Modifier.padding(top = 12.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.secondary,
                        contentColor = MaterialTheme.colorScheme.onSecondary
                    ),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)
                ) {
                    Text("Move to Today", style = MaterialTheme.typography.labelLarge)
                }
            }
        }
    }
}

@Composable
fun WinOfTheDayCard(task: TaskEntity, celebration: String) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(28.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
    ) {
        Row(
            modifier = Modifier.padding(20.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .background(MaterialTheme.colorScheme.primary, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Default.EmojiEvents,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier.size(32.dp)
                )
            }
            
            Spacer(Modifier.width(20.dp))
            
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    "WIN OF THE DAY",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
                )
                Text(
                    task.title,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                if (celebration.isNotBlank()) {
                    Text(
                        celebration,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                        fontStyle = androidx.compose.ui.text.font.FontStyle.Italic,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun InsightStat(label: String, value: String) {
    Column {
        Text(value, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.ExtraBold)
        Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
fun HistoryTaskCard(task: TaskEntity) {
    val isCompleted = task.status == "COMPLETED"
    val view = LocalView.current
    
    Surface(
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surface,
        modifier = Modifier.fillMaxWidth().clickable { 
            view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
        },
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
    ) {
        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .background(
                        if (isCompleted) MaterialTheme.colorScheme.primary.copy(alpha = 0.1f) 
                        else MaterialTheme.colorScheme.surfaceVariant, 
                        RoundedCornerShape(12.dp)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    if (isCompleted) Icons.Default.DoneAll else Icons.AutoMirrored.Filled.EventNote,
                    null,
                    tint = if (isCompleted) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(24.dp)
                )
            }
            Spacer(Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    task.title,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    textDecoration = if (isCompleted) TextDecoration.LineThrough else TextDecoration.None,
                    fontWeight = FontWeight.SemiBold
                )
                val time = remember(task.deadline) { task.deadline?.substringAfter(" ") ?: "No time set" }
                Text(
                    time,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            
            if (task.subtasks.isNotEmpty()) {
                Surface(
                    color = MaterialTheme.colorScheme.secondaryContainer,
                    shape = CircleShape
                ) {
                    Text(
                        "${task.subtasks.size} steps",
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}
