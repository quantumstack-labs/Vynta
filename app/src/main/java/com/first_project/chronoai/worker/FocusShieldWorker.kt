package com.first_project.chronoai.worker

import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.work.*
import com.first_project.chronoai.data.CalendarRepository
import com.first_project.chronoai.data.local.db.DatabaseProvider
import com.first_project.chronoai.data.local.prefs.UserPreferencesRepo
import com.first_project.chronoai.notification.LiveNotificationManager
import com.first_project.chronoai.notification.LiveTaskService
import com.first_project.chronoai.ui1.utils.FocusManager
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential
import com.google.api.client.http.javanet.NetHttpTransport
import com.google.api.client.json.gson.GsonFactory
import com.google.api.services.calendar.Calendar
import com.google.api.services.calendar.CalendarScopes
import com.google.android.gms.auth.api.signin.GoogleSignIn
import kotlinx.coroutines.flow.first
import java.time.LocalDate
import java.util.concurrent.TimeUnit

class FocusShieldWorker(
    context: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(context, workerParams) {

    override suspend fun doWork(): Result {
        val prefsRepo = UserPreferencesRepo(applicationContext)
        val prefs = prefsRepo.schedulingPreferences.first()
        val focusManager = FocusManager(applicationContext)

        val account = GoogleSignIn.getLastSignedInAccount(applicationContext)
        if (account == null) return Result.failure()

        val credential = GoogleAccountCredential.usingOAuth2(
            applicationContext,
            listOf(
                CalendarScopes.CALENDAR,
                CalendarScopes.CALENDAR_READONLY
            )
        ).setSelectedAccount(account.account)

        val service = Calendar.Builder(
            NetHttpTransport(),
            GsonFactory.getDefaultInstance(),
            credential
        ).setApplicationName("ChronoAI").build()

        val calendarRepository = CalendarRepository(service, applicationContext)
        val now = System.currentTimeMillis()
        
        // Fetch events for today
        val events = calendarRepository.getEventsForDate(LocalDate.now())
        Log.d("FocusShieldWorker", "Fetched ${events.size} events for today")
        
        // A task is "active" if NOW is between its start and end time
        val activeEvent = events.find { event ->
            val start = event.start.dateTime?.value ?: event.start.date?.value ?: 0L
            val end = event.end.dateTime?.value ?: event.end.date?.value ?: 0L
            now in start..end
        }

        if (activeEvent != null) {
            Log.d("FocusShieldWorker", "Active task found: ${activeEvent.summary}.")
            
            if (prefs.focusShieldEnabled) {
                focusManager.setFocusMode(true)
            }

            val db = DatabaseProvider.getDatabase(applicationContext)
            val taskDao = db.taskDao()
            val taskEntity = taskDao.getAllTasksDirect().find { 
                it.calendarEventId == activeEvent.id || it.calendarEventId == activeEvent.recurringEventId 
            }
            
            if (taskEntity != null && taskEntity.status != "COMPLETED") {
                val start = activeEvent.start.dateTime?.value ?: activeEvent.start.date?.value ?: 0L
                val end = activeEvent.end.dateTime?.value ?: activeEvent.end.date?.value ?: 0L
                
                // Start the Real-Time Foreground Service
                val serviceIntent = Intent(applicationContext, LiveTaskService::class.java).apply {
                    putExtra("TASK_ID", taskEntity.id)
                    putExtra("START_TIME", start)
                    putExtra("END_TIME", end)
                }
                
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                    try {
                        applicationContext.startForegroundService(serviceIntent)
                    } catch (e: Exception) {
                        Log.e("FocusShieldWorker", "Failed to start foreground service from background", e)
                        // Fallback: Show standard notification if service fails
                        LiveNotificationManager.showLiveProgressNotification(applicationContext, taskEntity, 0)
                    }
                } else {
                    applicationContext.startService(serviceIntent)
                }
            } else {
                LiveNotificationManager.cancelNotification(applicationContext)
            }
        } else {
            Log.d("FocusShieldWorker", "No active task. Cleaning up.")
            focusManager.setFocusMode(false)
            LiveNotificationManager.cancelNotification(applicationContext)
        }

        return Result.success()
    }

    companion object {
        fun enqueue(context: Context) {
            val request = PeriodicWorkRequestBuilder<FocusShieldWorker>(15, TimeUnit.MINUTES)
                .setConstraints(Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build())
                .build()
            
            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                "FocusShieldWorker",
                ExistingPeriodicWorkPolicy.KEEP,
                request
            )
        }
    }
}
