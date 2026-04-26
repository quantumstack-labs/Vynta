package com.first_project.chronoai.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Event
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.first_project.chronoai.data.local.entity.TaskEntity
import com.first_project.chronoai.ui.theme.*
import com.google.api.services.calendar.model.Event

@Composable
fun DailyProgressCard(progress: Float, completed: Int, total: Int) {
    Surface(
        shape = VyntaShapes.large,
        color = SurfaceElevated,
        modifier = Modifier.fillMaxWidth().padding(top = 24.dp)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("Daily Progress", style = VyntaTypography.titleMedium, color = TextPrimary)
                    Text("$completed/$total tasks completed", style = VyntaTypography.labelSmall, color = TextSecondary)
                }
                Text("${(progress * 100).toInt()}%", style = VyntaTypography.headlineMedium.copy(fontFamily = JetBrainsMonoFont), color = AccentPrimary)
            }
            Spacer(modifier = Modifier.height(16.dp))
            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier.fillMaxWidth().height(8.dp).clip(ShapePill),
                color = AccentPrimary,
                trackColor = SurfaceContainer,
            )
        }
    }
}

@Composable
fun UpcomingEventItem(event: Event) {
    Surface(
        shape = VyntaShapes.medium,
        color = SurfaceElevated,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(VyntaShapes.small)
                    .background(AccentBlue.copy(alpha = 0.2f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.Event, null, tint = AccentBlue, modifier = Modifier.size(20.dp))
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column {
                Text(event.summary ?: "No Title", style = VyntaTypography.bodyLarge, color = TextPrimary)
                val time = event.start?.dateTime?.toString() ?: event.start?.date?.toString() ?: "All day"
                Text(time, style = MonoTime, color = TextSecondary)
            }
        }
    }
}

@Composable
fun UpcomingTaskItem(task: TaskEntity, onToggle: () -> Unit) {
    Surface(
        shape = VyntaShapes.medium,
        color = SurfaceElevated,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Checkbox(
                checked = task.status == "COMPLETED",
                onCheckedChange = { onToggle() },
                colors = CheckboxDefaults.colors(checkedColor = AccentPrimary, uncheckedColor = TextTertiary)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    task.title,
                    style = VyntaTypography.bodyLarge,
                    color = if (task.status == "COMPLETED") TextSecondary else TextPrimary
                )
                task.energyLevel?.let {
                    Text(it, style = VyntaTypography.labelSmall, color = AccentGreen)
                }
            }
        }
    }
}
