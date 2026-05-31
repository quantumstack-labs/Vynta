package com.first_project.chronoai.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.first_project.chronoai.data.local.db.DatabaseProvider
import com.first_project.chronoai.notification.PrecisionTriggerManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class BootReceiver : BroadcastReceiver() {
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action
        Log.d("BootReceiver", "Received intent: $action")
        
        if (action == Intent.ACTION_BOOT_COMPLETED || 
            action == "android.intent.action.QUICKBOOT_POWERON" ||
            action == Intent.ACTION_TIMEZONE_CHANGED ||
            action == Intent.ACTION_TIME_CHANGED) {
            
            scope.launch {
                try {
                    val db = DatabaseProvider.getDatabase(context)
                    val tasks = db.taskDao().getAllTasksDirect()
                    
                    // Reschedule all scheduled tasks that aren't completed yet
                    tasks.filter { it.status == "SCHEDULED" && it.deadline != null }.forEach { task ->
                        PrecisionTriggerManager.scheduleTaskTrigger(context, task)
                    }
                    Log.d("BootReceiver", "Rescheduled ${tasks.size} tasks after reboot/time change")
                } catch (e: Exception) {
                    Log.e("BootReceiver", "Failed to reschedule tasks", e)
                }
            }
        }
    }
}
