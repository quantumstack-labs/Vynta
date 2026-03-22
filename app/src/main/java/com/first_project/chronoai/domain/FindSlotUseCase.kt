package com.first_project.chronoai.domain

import android.content.Context
import com.first_project.chronoai.data.CalendarRepository
import com.google.api.services.calendar.model.TimePeriod
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.time.*
import java.util.*

class FindSlotUseCase(private val context: Context, private val repository: CalendarRepository) {

    data class TimeSlot(val start: Long, val end: Long)

    /**
     * Requirement 2: THE SCHEDULING ENGINE
     * Algorithm: Find a slot using free/busy data from Google Calendar.
     */
    suspend fun findOptimalSlot(
        durationMinutes: Int,
        energyLevel: String,
        deadlineDate: String?,
        deadlineTime: String?
    ): TimeSlot = withContext(Dispatchers.IO) {
        val prefs = context.getSharedPreferences("user_prefs", Context.MODE_PRIVATE)
        val workStart = prefs.getFloat("work_start", 7f).toInt()
        val workEnd = prefs.getFloat("work_end", 22f).toInt()
        val bufferMinutes = 15 // Requirement: Page 7
        
        val zoneId = ZoneId.systemDefault()
        var targetDate = LocalDate.now()
        if (!deadlineDate.isNullOrBlank()) {
            try { targetDate = LocalDate.parse(deadlineDate) } catch (e: Exception) {}
        }

        // Search for a slot in the next 7 days
        for (i in 0 until 7) {
            val date = targetDate.plusDays(i.toLong())
            
            // Define search window for this day
            val windowStart = date.atTime(workStart, 0).atZone(zoneId).toInstant().toEpochMilli()
            val windowEnd = date.atTime(workEnd, 0).atZone(zoneId).toInstant().toEpochMilli()
            
            // Requirement 2: Call getBusySlots() from CalendarRepository
            val busySlots = repository.getBusySlots(windowStart, windowEnd)
            
            val slot = findAvailableSlotInWindow(
                windowStart, 
                windowEnd, 
                durationMinutes, 
                bufferMinutes, 
                busySlots
            )
            
            if (slot != null) return@withContext slot
        }

        // Fallback: Default to AI suggestion or immediate next available (simplified for core logic)
        val now = LocalDateTime.now().plusMinutes(bufferMinutes.toLong())
        val start = now.atZone(zoneId).toInstant().toEpochMilli()
        TimeSlot(start, start + (durationMinutes * 60 * 1000L))
    }

    private fun findAvailableSlotInWindow(
        windowStart: Long,
        windowEnd: Long,
        durationMinutes: Int,
        bufferMinutes: Int,
        busySlots: List<TimePeriod>
    ): TimeSlot? {
        val durationMillis = durationMinutes * 60 * 1000L
        val bufferMillis = bufferMinutes * 60 * 1000L
        
        var currentTime = windowStart
        val now = System.currentTimeMillis()
        if (currentTime < now) currentTime = now + bufferMillis

        // Sort busy slots by start time
        val sortedBusy = busySlots.sortedBy { it.start.value }

        while (currentTime + durationMillis <= windowEnd) {
            val potentialEnd = currentTime + durationMillis
            
            // Check for overlap with any busy slot, including buffer
            val conflict = sortedBusy.find { busy ->
                val busyStart = busy.start.value
                val busyEnd = busy.end.value
                // Conflict if [currentTime - buffer, potentialEnd + buffer] overlaps with [busyStart, busyEnd]
                (currentTime < busyEnd + bufferMillis) && (potentialEnd + bufferMillis > busyStart)
            }

            if (conflict == null) {
                return TimeSlot(currentTime, potentialEnd)
            } else {
                // Advance to the end of the conflicting event + buffer
                currentTime = conflict.end.value + bufferMillis
            }
        }
        return null
    }
}