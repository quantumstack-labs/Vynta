package com.first_project.chronoai.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import android.widget.Toast
import com.first_project.chronoai.BuildConfig
import com.first_project.chronoai.ai.GroqManager
import com.first_project.chronoai.data.CalendarRepository
import com.first_project.chronoai.data.local.db.DatabaseProvider
import com.first_project.chronoai.data.local.prefs.UserPreferencesRepo
import com.first_project.chronoai.notification.LiveNotificationManager
import com.first_project.chronoai.ui1.utils.FocusManager
import com.first_project.chronoai.ui1.widget.updateVyntaWidgets
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential
import com.google.api.client.http.javanet.NetHttpTransport
import com.google.api.client.json.gson.GsonFactory
import com.google.api.services.calendar.Calendar
import com.google.api.services.calendar.CalendarScopes
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.time.LocalDate

class NotificationActionReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action ?: return
        Log.d("NotificationAction", "Received action: $action")

        when (action) {
            "com.first_project.chronoai.ACTION_COMPLETE_TASK" -> {
                val taskId = intent.getIntExtra("TASK_ID", -1)
                if (taskId != -1) completeTask(context, taskId)
            }
            "com.first_project.chronoai.ACTION_REFLOW" -> {
                val title = intent.getStringExtra("TASK_TITLE") ?: "Current Task"
                triggerAiReflow(context, title)
            }
            "com.first_project.chronoai.ACTION_SHIELD" -> {
                toggleFocusShield(context)
            }
        }
    }

    private fun completeTask(context: Context, taskId: Int) {
        CoroutineScope(Dispatchers.IO).launch {
            val db = DatabaseProvider.getDatabase(context)
            val taskDao = db.taskDao()
            val task = taskDao.getTaskById(taskId)
            if (task != null) {
                taskDao.updateTask(task.copy(status = "COMPLETED"))
                LiveNotificationManager.cancelNotification(context)
                updateVyntaWidgets(context)
            }
        }
    }

    private fun toggleFocusShield(context: Context) {
        val focusManager = FocusManager(context)
        if (focusManager.hasDndPermission()) {
            // Check current status - for simplicity we just enable it
            focusManager.setFocusMode(true)
            Toast.makeText(context, "Focus Shield Activated", Toast.LENGTH_SHORT).show()
        } else {
            // Fallback for permission
            val intent = Intent(android.provider.Settings.ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        }
    }

    private fun triggerAiReflow(context: Context, taskTitle: String) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val prefsRepo = UserPreferencesRepo(context)
                val prefs = prefsRepo.schedulingPreferences.first()
                val groqManager = GroqManager(BuildConfig.GROQ_API_KEY)
                
                val account = GoogleSignIn.getLastSignedInAccount(context) ?: return@launch
                val credential = GoogleAccountCredential.usingOAuth2(context, listOf(CalendarScopes.CALENDAR))
                    .setSelectedAccount(account.account)
                
                val calendarService = Calendar.Builder(NetHttpTransport(), GsonFactory.getDefaultInstance(), credential)
                    .setApplicationName("Vynta").build()
                val repository = CalendarRepository(calendarService, context)
                
                val db = DatabaseProvider.getDatabase(context)
                val taskDao = db.taskDao()
                val todayTasks = taskDao.getAllTasksDirect().filter { 
                    it.status != "COMPLETED" && it.deadline?.startsWith(LocalDate.now().toString()) == true 
                }

                if (todayTasks.isEmpty()) return@launch

                val taskListText = todayTasks.joinToString("\n") { 
                    "- ${it.title} (Current: ${it.deadlineTime ?: "Anytime"}, Energy: ${it.energyLevel})" 
                }

                val problem = "I am currently stuck on '$taskTitle' and need to re-balance my day."
                val reflowJson = groqManager.analyzeReflow(problem, taskListText, prefs)
                
                // Simplified Reflow processing in receiver
                val cleanedJson = if (reflowJson.contains("[")) reflowJson.substring(reflowJson.indexOf("["), reflowJson.lastIndexOf("]") + 1) else ""
                if (cleanedJson.isNotBlank()) {
                    // Just notify user that AI is working - full processing usually needs ViewModel, 
                    // but we can apply basic logic here or just show a notification result.
                    Log.d("NotificationAction", "AI Reflow triggered successfully")
                    // Note: Full JSON parsing and DB update is complex for a receiver, 
                    // ideally we trigger a deep link to the app to handle the reflow UI.
                    val intent = Intent(context, com.first_project.chronoai.MainActivity::class.java).apply {
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        putExtra("shortcut", "reflow_active") // Not implemented yet but placeholder
                    }
                    // context.startActivity(intent)
                }
                
                CoroutineScope(Dispatchers.Main).launch {
                    Toast.makeText(context, "AI is re-balancing your day...", Toast.LENGTH_LONG).show()
                }
            } catch (e: Exception) {
                Log.e("NotificationAction", "Reflow failed", e)
            }
        }
    }
}
