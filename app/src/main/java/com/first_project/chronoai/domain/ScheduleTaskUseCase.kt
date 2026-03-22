package com.first_project.chronoai.domain

import com.first_project.chronoai.ai.GroqManager
import com.first_project.chronoai.ai.TaskModel
import com.first_project.chronoai.data.CalendarRepository
import com.first_project.chronoai.data.local.prefs.UserPreferencesRepo
import com.google.api.services.calendar.model.TimePeriod
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter

sealed class SchedulingResult {
    data class Success(val message: String, val eventId: String? = null) : SchedulingResult()
    data class Conflict(val conflictingEvents: List<TimePeriod>, val suggestedSlot: String?) : SchedulingResult()
    data class Error(val message: String) : SchedulingResult()
}

class ScheduleTaskUseCase(
    private val calendarRepository: CalendarRepository,
    private val aiManager: GroqManager,
    private val userPreferencesRepo: UserPreferencesRepo
) {
    suspend fun execute(task: TaskModel): SchedulingResult = withContext(Dispatchers.IO) {
        try {
            val prefs = userPreferencesRepo.schedulingPreferences.first()
            val bufferMillis = prefs.bufferMinutes * 60 * 1000L
            
            val date = try {
                task.deadlineDate?.let { LocalDate.parse(it) } ?: LocalDate.now()
            } catch (e: Exception) {
                LocalDate.now()
            }
            
            val time = try {
                task.deadlineTime?.let { LocalTime.parse(it) } ?: LocalTime.now().plusHours(1).withMinute(0)
            } catch (e: Exception) {
                LocalTime.now().plusHours(1).withMinute(0)
            }

            val startDateTime = LocalDateTime.of(date, time)
            val endDateTime = startDateTime.plusMinutes(task.durationMinutes.toLong())
            
            val startMillis = startDateTime.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
            val endMillis = endDateTime.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()

            // Fetch BusySlots with user-defined buffer
            val busySlots = calendarRepository.getBusySlots(startMillis - bufferMillis, endMillis + bufferMillis)

            if (busySlots.isEmpty()) {
                val event = calendarRepository.insertEvent(
                    title = task.title,
                    startTimeMillis = startMillis,
                    endTimeMillis = endMillis,
                    priority = task.priority,
                    isRecurring = task.isRecurring,
                    rrule = task.recurrencePattern
                )
                return@withContext if (event != null) SchedulingResult.Success("Task scheduled successfully", event.id)
                else SchedulingResult.Error("Failed to insert event into calendar")
            }

            // Intelligent Negotiation
            val windowStart = startDateTime.toLocalDate().atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
            val windowEnd = startDateTime.toLocalDate().plusDays(1).atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
            val dayBusySlots = calendarRepository.getBusySlots(windowStart, windowEnd)
            
            val busyContext = dayBusySlots.joinToString { 
                "${DateTimeFormatter.ISO_INSTANT.format(java.time.Instant.ofEpochMilli(it.start.value))} to ${DateTimeFormatter.ISO_INSTANT.format(java.time.Instant.ofEpochMilli(it.end.value))}"
            }

            val prompt = """
                The user wants to schedule "${task.title}" for ${task.durationMinutes} minutes around ${task.deadlineTime}.
                However, there is a conflict. 
                Current Busy Slots: [$busyContext].
                
                YOUR TASK:
                1. Find the next best available time slot for this task today.
                2. Respond ONLY with a JSON object in this format:
                {
                    "suggestion": "A short, sweet, and friendly message suggesting the new time. Example: 'I found a spot for your yoga at 19:50! Shall we?'",
                    "new_time": "HH:mm"
                }
                
                Keep the suggestion very brief (max 15 words) and human-like.
            """.trimIndent()
            
            val aiSuggestion = aiManager.analyzeTask(prompt)
            
            return@withContext SchedulingResult.Conflict(busySlots, aiSuggestion)

        } catch (e: Exception) {
            return@withContext SchedulingResult.Error(e.message ?: "Unknown scheduling error")
        }
    }
}
