package com.first_project.chronoai.worker

import android.content.Context
import android.util.Log
import androidx.work.*
import com.first_project.chronoai.data.CalendarRepository
import com.first_project.chronoai.data.local.prefs.UserPreferencesRepo
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

        if (!prefs.focusShieldEnabled) {
            FocusManager(applicationContext).setFocusMode(false)
            return Result.success()
        }

        val account = GoogleSignIn.getLastSignedInAccount(applicationContext)
        if (account == null) return Result.failure()

        val credential = GoogleAccountCredential.usingOAuth2(
            applicationContext,
            listOf(CalendarScopes.CALENDAR_READONLY)
        ).setSelectedAccount(account.account)

        val service = Calendar.Builder(
            NetHttpTransport(),
            GsonFactory.getDefaultInstance(),
            credential
        ).setApplicationName("ChronoAI").build()

        val calendarRepository = CalendarRepository(service)
        val now = System.currentTimeMillis()
        
        // Fetch events for today
        val events = calendarRepository.getEventsForDate(LocalDate.now())
        
        // A task is "active" if NOW is between its start and end time
        val activeEvent = events.find { event ->
            val start = event.start.dateTime?.value ?: event.start.date?.value ?: 0L
            val end = event.end.dateTime?.value ?: event.end.date?.value ?: 0L
            
            val description = event.description ?: ""
            
            // 1. Check for Energy Tag
            val hasHighEnergyTag = description.contains("Energy: High", ignoreCase = true)
            
            // 2. Check for Priority Tag (e.g., Priority: 1, 2, or 5)
            val hasHighPriorityTag = description.contains("Priority: 1", ignoreCase = true) || 
                                    description.contains("Priority: 2", ignoreCase = true) || 
                                    description.contains("Priority: 5", ignoreCase = true)
            
            // 3. Check title for high-energy keywords
            val titleKeywords = listOf("focus", "deep work", "coding", "development", "critical", "study")
            val isHighEnergyTitle = titleKeywords.any { event.summary?.contains(it, ignoreCase = true) == true }
            
            val isHighEnergy = hasHighEnergyTag || hasHighPriorityTag || isHighEnergyTitle

            now in start..end && isHighEnergy
        }

        val focusManager = FocusManager(applicationContext)
        if (activeEvent != null) {
            Log.d("FocusShieldWorker", "Active task found: ${activeEvent.summary}. Enabling Focus Shield.")
            focusManager.setFocusMode(true)
        } else {
            Log.d("FocusShieldWorker", "No active task. Disabling Focus Shield.")
            focusManager.setFocusMode(false)
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
