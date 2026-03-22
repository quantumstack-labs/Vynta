package com.first_project.chronoai.domain

import android.content.Context
import com.first_project.chronoai.data.CalendarRepository
import com.first_project.chronoai.data.local.prefs.UserPreferencesRepo
import kotlinx.coroutines.flow.first

sealed interface ConflictResolverResult {
    data class Success(val start: Long, val end: Long) : ConflictResolverResult
    data class Conflict(val start: Long, val end: Long, val conflictingEvent: String) : ConflictResolverResult
    object NoSlots : ConflictResolverResult
}

class ConflictResolverService(
    private val context: Context, 
    private val repository: CalendarRepository,
    private val userPreferencesRepo: UserPreferencesRepo
) {

    suspend fun resolveAndVerify(
        startMillis: Long,
        durationMinutes: Int
    ): ConflictResolverResult {
        val prefs = userPreferencesRepo.schedulingPreferences.first()
        val bufferMinutes = prefs.bufferMinutes
        
        val endMillis = startMillis + (durationMinutes * 60 * 1000L)
        val bufferMillis = bufferMinutes * 60 * 1000L
        
        val existingEvents = repository.getUpcomingEvents()
        
        val conflict = existingEvents.find { event ->
            val eventStart = event.start.dateTime?.value ?: event.start.date?.value ?: 0L
            val eventEnd = event.end.dateTime?.value ?: event.end.date?.value ?: 0L
            
            // Overlap check with user-defined buffer
            (startMillis < eventEnd + bufferMillis) && (endMillis + bufferMillis > eventStart)
        }

        return if (conflict == null) {
            ConflictResolverResult.Success(startMillis, endMillis)
        } else {
            ConflictResolverResult.Conflict(startMillis, endMillis, conflict.summary ?: "Existing Task")
        }
    }
}
