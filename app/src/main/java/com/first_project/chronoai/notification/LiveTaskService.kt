package com.first_project.chronoai.notification

import android.app.Service
import android.content.Intent
import android.os.IBinder
import android.util.Log
import com.first_project.chronoai.data.local.db.DatabaseProvider
import com.first_project.chronoai.data.local.entity.TaskEntity
import kotlinx.coroutines.*

class LiveTaskService : Service() {
    private val serviceScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private var updateJob: Job? = null
    
    private val focusManager by lazy { com.first_project.chronoai.ui1.utils.FocusManager(this) }
    
    private var currentTaskId: Int = -1
    private var currentStartTime: Long = 0
    private var currentEndTime: Long = 0

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val taskId = intent?.getIntExtra("TASK_ID", -1) ?: -1
        val startTime = intent?.getLongExtra("START_TIME", 0) ?: 0
        val endTime = intent?.getLongExtra("END_TIME", 0) ?: 0

        if ((taskId == -1) || (startTime == 0L) || (endTime == 0L)) {
            stopSelf()
            return START_NOT_STICKY
        }

        // If it's a new task or the first time starting
        if (taskId != currentTaskId) {
            currentTaskId = taskId
            currentStartTime = startTime
            currentEndTime = endTime
            
            // Automatically enable Focus Mode (DND) when a live task begins
            focusManager.setFocusMode(true)
            
            startUpdatingProgress()
        }

        return START_STICKY
    }

    private fun startUpdatingProgress() {
        updateJob?.cancel()
        updateJob = serviceScope.launch {
            val db = DatabaseProvider.getDatabase(applicationContext)
            val taskDao = db.taskDao()

            while (isActive) {
                val now = System.currentTimeMillis()
                
                // If the task has ended
                if (now > currentEndTime) {
                    Log.d("LiveTaskService", "Task time ended. Stopping service.")
                    stopSelf()
                    break
                }

                val task = taskDao.getTaskById(currentTaskId)
                if (task == null || task.status == "COMPLETED") {
                    Log.d("LiveTaskService", "Task completed or deleted. Stopping service.")
                    stopSelf()
                    break
                }

                val total = currentEndTime - currentStartTime
                val elapsed = now - currentStartTime
                val progress = if (total > 0) ((elapsed.toFloat() / total.toFloat()) * 100).toInt().coerceIn(0, 100) else 0

                val notification = LiveNotificationManager.buildNotification(applicationContext, task, progress)
                startForeground(2002, notification)

                delay(60_000) // Update every minute
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        // Automatically disable Focus Mode (DND) when the live task ends/stops
        focusManager.setFocusMode(false)
        serviceScope.cancel()
        Log.d("LiveTaskService", "Service destroyed")
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
