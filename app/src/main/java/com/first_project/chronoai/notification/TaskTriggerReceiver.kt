package com.first_project.chronoai.notification

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.core.content.ContextCompat

class TaskTriggerReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val taskId = intent.getIntExtra("TASK_ID", -1)
        val startTime = intent.getLongExtra("START_TIME", 0)
        val endTime = intent.getLongExtra("END_TIME", 0)

        if (taskId != -1) {
            Log.d("TaskTriggerReceiver", "Received exact trigger for task $taskId. Starting LiveTaskService.")
            
            val serviceIntent = Intent(context, LiveTaskService::class.java).apply {
                putExtra("TASK_ID", taskId)
                putExtra("START_TIME", startTime)
                putExtra("END_TIME", endTime)
            }
            
            // Starting from a BroadcastReceiver is a permitted exception to Foreground Service restrictions
            ContextCompat.startForegroundService(context, serviceIntent)
        }
    }
}
