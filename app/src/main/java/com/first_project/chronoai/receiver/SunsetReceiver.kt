package com.first_project.chronoai.receiver

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import com.first_project.chronoai.MainActivity
import com.first_project.chronoai.R
import com.first_project.chronoai.data.local.dao.TaskDao
import com.first_project.chronoai.data.local.db.AppDatabase
import com.first_project.chronoai.data.local.db.DatabaseProvider
import com.first_project.chronoai.data.local.prefs.UserPreferencesRepo
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.time.LocalDate

class SunsetReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action
        val database = DatabaseProvider.getDatabase(context)
        val taskDao = database.taskDao()
        val prefsRepo = UserPreferencesRepo(context)

        if (action == "ACTION_SNOOZE_ALL") {
            CoroutineScope(Dispatchers.IO).launch {
                val todayStr = LocalDate.now().toString()
                val tomorrowStr = LocalDate.now().plusDays(1).toString()
                val pendingTasks = taskDao.getAllTasksDirect().filter { 
                    it.status != "COMPLETED" && it.deadline?.startsWith(todayStr) == true 
                }
                pendingTasks.forEach { task ->
                    val newDeadline = tomorrowStr + (task.deadline?.substringAfter(" ")?.let { " $it" } ?: "")
                    taskDao.updateTask(task.copy(deadline = newDeadline))
                }
                val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                notificationManager.cancel(1001)
            }
            return
        }

        CoroutineScope(Dispatchers.IO).launch {
            val prefs = prefsRepo.schedulingPreferences.first()
            if (!prefs.notificationsEnabled) return@launch

            val todayStr = LocalDate.now().toString()
            val pendingTasks = taskDao.getAllTasksDirect().filter { 
                it.status != "COMPLETED" && it.deadline?.startsWith(todayStr) == true 
            }

            if (pendingTasks.isNotEmpty()) {
                showSunsetNotification(context, pendingTasks.size)
            }
        }
    }

    private fun showSunsetNotification(context: Context, taskCount: Int) {
        val channelId = "sunset_reflection"
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            val channel = NotificationChannel(channelId, "Sunset Reflection", NotificationManager.IMPORTANCE_DEFAULT)
            notificationManager.createNotificationChannel(channel)
        }

        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent = PendingIntent.getActivity(context, 0, intent, PendingIntent.FLAG_IMMUTABLE)

        val snoozeIntent = Intent(context, SunsetReceiver::class.java).apply {
            action = "ACTION_SNOOZE_ALL"
        }
        val snoozePendingIntent = PendingIntent.getBroadcast(context, 1, snoozeIntent, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)

        val notification = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle("Sunset Reflection")
            .setContentText("You have $taskCount tasks remaining. Want to snooze them to tomorrow and relax?")
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(pendingIntent)
            .addAction(android.R.drawable.ic_lock_idle_alarm, "Snooze to Tomorrow", snoozePendingIntent)
            .setAutoCancel(true)
            .build()

        notificationManager.notify(1001, notification)
    }
}
