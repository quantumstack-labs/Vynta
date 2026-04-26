package com.first_project.chronoai.ui1.navigation

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import android.view.HapticFeedbackConstants
import com.first_project.chronoai.data.local.entity.TaskEntity
import com.first_project.chronoai.ui.theme.*
import com.first_project.chronoai.ui1.viewmodel.HomeViewModel

@OptIn(ExperimentalMaterial3Api::class, ExperimentalSharedTransitionApi::class)
@Composable
fun TaskDetailScreen(
    taskId: Int,
    viewModel: HomeViewModel,
    onBack: () -> Unit,
    sharedTransitionScope: SharedTransitionScope,
    animatedVisibilityScope: AnimatedVisibilityScope
) {
    val tasks by viewModel.personalTasks.collectAsState()
    val task = tasks.find { it.id == taskId }
    val view = LocalView.current
    val context = LocalContext.current

    if (task == null) {
        ErrorScreen(
            title = "Task Not Found",
            message = "We couldn't locate the details for this task. It may have been removed.",
            buttonText = "Return to Schedule",
            onRetry = onBack
        )
        return
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = { Text("Task Details", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = {
                        view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                        onBack()
                    }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, null, tint = MaterialTheme.colorScheme.onBackground)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
            )
        }
    ) { padding ->
        with(sharedTransitionScope) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(24.dp)
                    .sharedElement(
                        rememberSharedContentState(key = "task-${task.id}"),
                        animatedVisibilityScope = animatedVisibilityScope
                    )
                    .background(MaterialTheme.colorScheme.surfaceVariant, MaterialTheme.shapes.large)
                    .padding(24.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    val badgeColor = if (task.status == "COMPLETED") Color(0xFF4CAF50) else MaterialTheme.colorScheme.primary
                    Surface(
                        color = badgeColor.copy(alpha = 0.1f),
                        shape = CircleShape
                    ) {
                        Text(
                            task.status,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                            style = MaterialTheme.typography.labelSmall,
                            color = badgeColor,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Spacer(Modifier.weight(1f))
                    Icon(Icons.Default.MoreVert, null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                }

                Spacer(Modifier.height(24.dp))

                Text(
                    task.title,
                    style = MaterialTheme.typography.headlineMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.Bold
                )

                Spacer(Modifier.height(32.dp))

                DetailItem(Icons.Default.Schedule, "Time", task.deadline?.substringAfter(" ") ?: "Not set")
                DetailItem(Icons.Default.CalendarToday, "Date", task.deadline?.substringBefore(" ") ?: "Not set")
                DetailItem(Icons.Default.Bolt, "Energy", task.energyLevel ?: "Medium")
                DetailItem(Icons.Default.PriorityHigh, "Priority", "Level ${task.priority}")

                Spacer(Modifier.weight(1f))

                Button(
                    onClick = {
                        view.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
                        viewModel.toggleTaskCompletion(context, task)
                    },
                    modifier = Modifier.fillMaxWidth().height(56.dp),
                    shape = CircleShape,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (task.status == "COMPLETED") MaterialTheme.colorScheme.surface else MaterialTheme.colorScheme.primary
                    )
                ) {
                    Text(
                        if (task.status == "COMPLETED") "MARK AS INCOMPLETE" else "COMPLETE TASK",
                        style = MaterialTheme.typography.titleMedium,
                        color = if (task.status == "COMPLETED") MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onPrimary,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

@Composable
fun DetailItem(icon: androidx.compose.ui.graphics.vector.ImageVector, label: String, value: String) {
    Row(
        modifier = Modifier.padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Surface(
            modifier = Modifier.size(40.dp),
            shape = CircleShape,
            color = MaterialTheme.colorScheme.background
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(icon, null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(20.dp))
            }
        }
        Spacer(Modifier.width(16.dp))
        Column {
            Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(value, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface)
        }
    }
}
