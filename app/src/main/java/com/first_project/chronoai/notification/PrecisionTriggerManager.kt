package com.first_project.chronoai.notification

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import com.first_project.chronoai.data.local.entity.TaskEntity
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter

object PrecisionTriggerManager {
    fun scheduleTaskTrigger(context: Context, task: TaskEntity) {
        val deadline = task.deadline ?: return
        try {
            val startTime = LocalDateTime.parse(deadline, DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"))
            val triggerTimeMillis = startTime.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
            
            // Use task's actual duration instead of hardcoded 1 hour
            val durationMillis = task.durationMinutes * 60 * 1000L
            val endTimeMillis = triggerTimeMillis + durationMillis

            // If the task has already started but not finished, start it NOW
            if (System.currentTimeMillis() in triggerTimeMillis..endTimeMillis) {
                Log.d("PrecisionTrigger", "Task ${task.title} is currently active. Starting service immediately.")
                val serviceIntent = Intent(context, LiveTaskService::class.java).apply {
                    putExtra("TASK_ID", task.id)
                    putExtra("START_TIME", triggerTimeMillis)
                    putExtra("END_TIME", endTimeMillis)
                }
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    context.startForegroundService(serviceIntent)
                } else {
                    context.startService(serviceIntent)
                }
                return
            }

            if (triggerTimeMillis < System.currentTimeMillis()) return

            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
            val intent = Intent(context, TaskTriggerReceiver::class.java).apply {
                putExtra("TASK_ID", task.id)
                putExtra("TASK_TITLE", task.title)
                putExtra("START_TIME", triggerTimeMillis)
                putExtra("END_TIME", endTimeMillis)
            }

            val pendingIntent = PendingIntent.getBroadcast(
                context, 
                task.id, 
                intent, 
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
            )

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                if (alarmManager.canScheduleExactAlarms()) {
                    alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerTimeMillis, pendingIntent)
                } else {
                    alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerTimeMillis, pendingIntent)
                }
            } else {
                alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerTimeMillis, pendingIntent)
            }
            Log.d("PrecisionTrigger", "Scheduled exact trigger for ${task.title} at $deadline")
        } catch (e: Exception) {
            Log.e("PrecisionTrigger", "Failed to schedule trigger", e)
        }
    }
}
