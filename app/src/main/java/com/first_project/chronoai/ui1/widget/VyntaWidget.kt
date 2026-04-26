package com.first_project.chronoai.ui1.widget

import android.content.Context
import android.content.Intent
import android.net.Uri
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle as JTextStyle
import java.util.Locale
import com.first_project.chronoai.BuildConfig
import androidx.compose.ui.graphics.Color
import androidx.glance.color.ColorProvider
import androidx.compose.runtime.Composable
import androidx.glance.appwidget.ImageProvider
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.*
import androidx.glance.action.ActionParameters
import androidx.glance.action.actionParametersOf
import androidx.glance.action.clickable
import androidx.glance.appwidget.*
import androidx.glance.appwidget.action.ActionCallback
import androidx.glance.appwidget.action.actionRunCallback
import androidx.glance.appwidget.updateAll
import androidx.glance.appwidget.lazy.LazyColumn
import androidx.glance.appwidget.lazy.items
import androidx.glance.layout.*
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.text.TextDecoration
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.glance.state.PreferencesGlanceStateDefinition
import androidx.datastore.preferences.core.Preferences
import androidx.glance.appwidget.state.updateAppWidgetState
import com.first_project.chronoai.MainActivity
import com.first_project.chronoai.data.local.db.DatabaseProvider
import com.first_project.chronoai.data.local.entity.TaskEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.flow.map

class VyntaWidget : GlanceAppWidget() {

    override val stateDefinition = PreferencesGlanceStateDefinition

    companion object {
        private val SMALL_SQUARE = DpSize(100.dp, 100.dp)
        private val HORIZONTAL_RECTANGLE = DpSize(200.dp, 100.dp)
        private val BIG_SQUARE = DpSize(200.dp, 200.dp)
        
        val task_id_param = ActionParameters.Key<Int>("task_id")
    }

    override val sizeMode = SizeMode.Responsive(
        setOf(SMALL_SQUARE, HORIZONTAL_RECTANGLE, BIG_SQUARE)
    )

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        android.util.Log.d("VyntaWidget", "provideGlance started")
        val db = DatabaseProvider.getDatabase(context)
        val today = LocalDate.now()
        val dateString = today.format(DateTimeFormatter.ISO_LOCAL_DATE)
        
        // Optimized: Filtering in SQL instead of in-memory
        val tasksFlow = db.taskDao().getTasksForDateWithLimit(dateString, 1, 10)

        provideContent {
            val tasks by tasksFlow.collectAsState(initial = emptyList())
            android.util.Log.d("VyntaWidget", "provideContent recomposing with ${tasks.size} tasks")
            
            val briefing = generateRuleBasedBriefing(tasks)
            val size = LocalSize.current
            GlanceTheme {
                VyntaWidgetContent(context, size, tasks, briefing)
            }
        }
    }

    private fun generateRuleBasedBriefing(tasks: List<TaskEntity>): String {
        val pending = tasks.count { it.status != "COMPLETED" }
        val completed = tasks.count { it.status == "COMPLETED" }
        
        return when {
            tasks.isEmpty() -> "REST. ARCHITECT LATER."
            pending == 0 -> "ALL OBJECTIVES CLEARED. EXCELLENT."
            completed == 0 -> "READY TO CONQUER YOUR DAY?"
            pending == 1 -> "ONE STEP REMAINING. FINISH STRONG."
            else -> "$pending TASKS REMAINING. STAY FOCUSED."
        }
    }

    @Composable
    private fun VyntaWidgetContent(context: Context, size: DpSize, tasks: List<TaskEntity>, briefing: String) {
        val completedCount = tasks.count { it.status == "COMPLETED" }
        val totalCount = tasks.size
        val progress = if (totalCount > 0) completedCount.toFloat() / totalCount else 0f

        Column(
            modifier = GlanceModifier
                .fillMaxSize()
                .background(GlanceTheme.colors.background)
                .appWidgetBackground()
                .padding(16.dp)
        ) {
            WidgetHeader(context, progress)
            
            Spacer(GlanceModifier.height(8.dp))

            Text(
                text = briefing,
                style = TextStyle(
                    color = GlanceTheme.colors.primary, 
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold
                ),
                modifier = GlanceModifier.padding(horizontal = 4.dp)
            )
            
            Spacer(GlanceModifier.height(16.dp))

            if (size.width >= HORIZONTAL_RECTANGLE.width) {
                TaskListView(tasks)
            } else {
                CompactTaskView(tasks.firstOrNull { it.status != "COMPLETED" } ?: tasks.firstOrNull())
            }
        }
    }

    @Composable
    private fun WidgetHeader(context: Context, progress: Float) {
        val today = LocalDate.now()
        val dayName = today.dayOfWeek.getDisplayName(JTextStyle.SHORT, Locale.getDefault()).uppercase()

        Row(
            modifier = GlanceModifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = GlanceModifier.defaultWeight()) {
                Text(
                    text = "VYNTA",
                    style = TextStyle(
                        color = GlanceTheme.colors.onSurface,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Medium
                    )
                )
                Text(
                    text = dayName,
                    style = TextStyle(
                        color = GlanceTheme.colors.onSurfaceVariant, 
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold
                    )
                )
            }

            // High-fidelity Progress Bar
            Box(
                modifier = GlanceModifier
                    .size(40.dp, 4.dp)
                    .background(GlanceTheme.colors.surfaceVariant)
                    .cornerRadius(2.dp)
            ) {
                if (progress > 0) {
                    Box(
                        modifier = GlanceModifier
                            .fillMaxHeight()
                            .width((40 * progress).dp) 
                            .background(GlanceTheme.colors.primary)
                            .cornerRadius(2.dp)
                    ) {}
                }
            }
            
            Spacer(GlanceModifier.width(16.dp))
            
            Box(
                modifier = GlanceModifier
                    .size(32.dp)
                    .background(GlanceTheme.colors.onSurface)
                    .cornerRadius(10.dp)
                    .clickable(actionRunCallback<AddTaskAction>()),
                contentAlignment = Alignment.Center
            ) {
                 Text("+", style = TextStyle(
                    color = GlanceTheme.colors.surface,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Medium
                ))
            }
        }
    }

    @Composable
    private fun TaskListView(tasks: List<TaskEntity>) {
        if (tasks.isEmpty()) {
            Box(modifier = GlanceModifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("REST. ARCHITECT LATER.", style = TextStyle(color = GlanceTheme.colors.onSurfaceVariant, fontSize = 11.sp, fontWeight = FontWeight.Bold))
            }
        } else {
            LazyColumn(modifier = GlanceModifier.fillMaxSize()) {
                items(tasks, itemId = { it.id.toLong() }) { task ->
                    TaskItem(task)
                }
            }
        }
    }

    @Composable
    private fun CompactTaskView(task: TaskEntity?) {
        Column(modifier = GlanceModifier.fillMaxWidth().padding(top = 8.dp)) {
            Text("CURRENT OBJECTIVE", style = TextStyle(color = GlanceTheme.colors.primary, fontWeight = FontWeight.Bold, fontSize = 9.sp))
            Text(
                task?.title ?: "No Tasks",
                maxLines = 2,
                style = TextStyle(color = GlanceTheme.colors.onSurface, fontSize = 16.sp, fontWeight = FontWeight.Medium)
            )
        }
    }

    @Composable
    private fun TaskItem(task: TaskEntity) {
        val isCompleted = task.status == "COMPLETED"
        val time = task.deadline?.substringAfter(" ")?.substring(0, 5) ?: ""
        val toggleAction = actionRunCallback<CompleteTaskAction>(actionParametersOf(task_id_param to task.id))
        val detailAction = actionRunCallback<OpenDetailAction>(actionParametersOf(task_id_param to task.id))

        Row(
            modifier = GlanceModifier
                .fillMaxWidth()
                .padding(bottom = 6.dp)
                .background(if (isCompleted) ColorProvider(Color.Transparent, Color.Transparent) else GlanceTheme.colors.surfaceVariant)
                .cornerRadius(14.dp)
                .padding(4.dp), // Thin inner padding
            verticalAlignment = Alignment.CenterVertically
        ) {
            // SEPARATE CLICKABLE CHECKBOX
            Box(
                modifier = GlanceModifier
                    .size(48.dp) // Large enough to be a clear target
                    .background(ColorProvider(Color.Transparent, Color.Transparent)) // Ensure the entire area is clickable
                    .cornerRadius(24.dp)
                    .clickable(toggleAction),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = GlanceModifier
                        .size(24.dp)
                        .background(if (isCompleted) GlanceTheme.colors.primary else ColorProvider(Color.Transparent, Color.Transparent))
                        .cornerRadius(8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    if (isCompleted) {
                        Text("✓", style = TextStyle(color = GlanceTheme.colors.onPrimary, fontSize = 14.sp, fontWeight = FontWeight.Bold))
                    } else {
                        Box(
                            modifier = GlanceModifier.fillMaxSize().padding(2.dp)
                                .background(GlanceTheme.colors.onSurfaceVariant)
                                .cornerRadius(6.dp)
                        ) {}
                    }
                }
            }

            // SEPARATE CLICKABLE TITLE (Opens app)
            Column(
                modifier = GlanceModifier
                    .defaultWeight()
                    .padding(vertical = 8.dp, horizontal = 4.dp)
                    .cornerRadius(12.dp) // FIXED: Rounded highlight for title area
                    .clickable(detailAction)
            ) {
                Text(
                    task.title, 
                    maxLines = 1,
                    style = TextStyle(
                        color = if (isCompleted) GlanceTheme.colors.onSurfaceVariant else GlanceTheme.colors.onSurface, 
                        fontWeight = FontWeight.Medium,
                        fontSize = 14.sp,
                        textDecoration = if (isCompleted) TextDecoration.LineThrough else null
                    )
                )
                if (time.isNotEmpty() && !isCompleted) {
                    Text(
                        time,
                        style = TextStyle(color = GlanceTheme.colors.primary, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    )
                }
            }
        }
    }
}

class AddTaskAction : ActionCallback {
    override suspend fun onAction(context: Context, glanceId: GlanceId, parameters: ActionParameters) {
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
            data = Uri.parse("vynta://add_task")
        }
        context.startActivity(intent)
    }
}

class OpenDetailAction : ActionCallback {
    override suspend fun onAction(context: Context, glanceId: GlanceId, parameters: ActionParameters) {
        val taskId = parameters[VyntaWidget.task_id_param] ?: return
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
            data = Uri.parse("vynta://task_detail/$taskId")
        }
        context.startActivity(intent)
    }
}

class CompleteTaskAction : ActionCallback {
    override suspend fun onAction(context: Context, glanceId: GlanceId, parameters: ActionParameters) {
        val taskId = parameters[VyntaWidget.task_id_param] ?: return
        android.util.Log.d("VyntaWidget", "CompleteTaskAction: Updating task $taskId")
        
        withContext(Dispatchers.IO) {
            val db = DatabaseProvider.getDatabase(context)
            val taskDao = db.taskDao()
            val task = taskDao.getTaskById(taskId)
            task?.let {
                val newStatus = if (it.status == "COMPLETED") "SCHEDULED" else "COMPLETED"
                taskDao.updateTask(it.copy(status = newStatus))
                android.util.Log.d("VyntaWidget", "Task $taskId status changed to $newStatus")
            } ?: android.util.Log.e("VyntaWidget", "Task $taskId not found in DB")
        }
    }
}

suspend fun updateVyntaWidgets(context: Context) {
    VyntaWidget().updateAll(context)
}

class VyntaWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = VyntaWidget()
}
