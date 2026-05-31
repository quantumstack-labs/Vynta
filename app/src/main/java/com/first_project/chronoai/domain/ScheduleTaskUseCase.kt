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
    data class Success(val message: String, val eventId: String? = null, val reason: String? = null) : SchedulingResult()
    data class Conflict(
        val conflictingEvents: List<TimePeriod>, 
        val suggestion: String?,
        val suggestedTime: String? = null
    ) : SchedulingResult()
    data class Error(val message: String) : SchedulingResult()
}

class ScheduleTaskUseCase(
    private val calendarRepository: CalendarRepository,
    private val aiManager: GroqManager,
    private val userPreferencesRepo: UserPreferencesRepo
) {
    suspend fun execute(task: TaskModel, existingEventId: String? = null, force: Boolean = false): SchedulingResult = withContext(Dispatchers.IO) {
        try {
            val prefs = userPreferencesRepo.schedulingPreferences.first()
            
            // Requirement: Smart Spacing (Dynamic Gap Logic)
            val baseBuffer = prefs.bufferMinutes
            val dynamicBuffer = if (prefs.smartSpacingEnabled) {
                when (task.energyLevel) {
                    "High" -> 30 // Recovery for deep work
                    "Medium" -> 15
                    "Low" -> 5
                    else -> baseBuffer
                }
            } else {
                baseBuffer
            }
            val bufferMillis = dynamicBuffer * 60 * 1000L
            
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

            // Requirement: Respect Active Hours
            val startHour = startDateTime.hour.toFloat() + (startDateTime.minute / 60f)
            val endHour = endDateTime.hour.toFloat() + (endDateTime.minute / 60f)
            
            if (!force && (startHour < prefs.workStart || endHour > prefs.workEnd)) {
                val suggestion = findOptimalSlot(task, date, calendarRepository, aiManager)
                return@withContext suggestion
            }
            
            val startMillis = startDateTime.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
            val endMillis = endDateTime.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()

            // Fetch BusySlots with user-defined buffer
            val busySlots = try {
                calendarRepository.getBusySlots(startMillis - bufferMillis, endMillis + bufferMillis)
                    .let { slots ->
                        if (existingEventId == null) slots
                        else slots.filter { 
                            // Only filter out the EXACT same slot if we are updating, otherwise any overlap is a conflict
                            it.start.value != startMillis || it.end.value != endMillis 
                        }
                    }
            } catch (e: Exception) {
                // If API fails, we must not assume it's free. 
                val errorMsg = if (e.message?.contains("403") == true || e.message?.contains("Forbidden") == true) {
                    "Calendar API access denied. Please verify your Google Cloud project settings and ensure 'Google Calendar API' is enabled."
                } else {
                    "Unable to check calendar availability: ${e.localizedMessage}"
                }
                return@withContext SchedulingResult.Error(errorMsg)
            }

            // Case 2: Time provided but there's a conflict
            if (!force && busySlots.isNotEmpty()) {
                val suggestion = findOptimalSlot(task, date, calendarRepository, aiManager)
                // If finding optimal slot also fails (usually network), show the error
                if (suggestion is SchedulingResult.Conflict && suggestion.suggestion?.contains("Error") == true) {
                    return@withContext SchedulingResult.Error("Network error: Could not find a free spot. Please check your connection.")
                }
                return@withContext suggestion
            }

            // Requirement: Use AI-generated reason if available
            val reason = task.schedulingReason ?: if (task.energyLevel == "High") {
                "Aligned with your peak morning energy window for maximum focus."
            } else {
                "Scheduled based on your calendar availability."
            }

            // Case 1: No time was provided by user - FIND OPTIMAL SPOT
            if (task.deadlineTime == null) {
                val suggestion = findOptimalSlot(task, date, calendarRepository, aiManager)
                return@withContext suggestion
            }

            // Case 2: Time provided but there's a conflict
            if (!force && busySlots.isNotEmpty()) {
                val suggestion = findOptimalSlot(task, date, calendarRepository, aiManager)
                return@withContext suggestion
            }

            // Case 3: Smooth sailing - Schedule it
            val event = if (existingEventId != null) {
                calendarRepository.updateEvent(
                    eventId = existingEventId,
                    title = task.title,
                    startTimeMillis = startMillis,
                    endTimeMillis = endMillis,
                    priority = task.priority,
                    energyLevel = task.energyLevel,
                    isRecurring = task.isRecurring,
                    rrule = task.recurrencePattern
                )
            } else {
                calendarRepository.insertEvent(
                    title = task.title,
                    startTimeMillis = startMillis,
                    endTimeMillis = endMillis,
                    priority = task.priority,
                    energyLevel = task.energyLevel,
                    isRecurring = task.isRecurring,
                    rrule = task.recurrencePattern
                )
            }
            return@withContext if (event != null) SchedulingResult.Success("Task scheduled successfully", event.id, reason)
            else SchedulingResult.Error("Failed to sync with calendar")

        } catch (e: Exception) {
            return@withContext SchedulingResult.Error(e.message ?: "Unknown scheduling error")
        }
    }

    private suspend fun findOptimalSlot(
        task: TaskModel,
        date: LocalDate,
        calendarRepository: CalendarRepository,
        aiManager: GroqManager
    ): SchedulingResult {
        val prefs = userPreferencesRepo.schedulingPreferences.first()
        val bufferMinutes = prefs.bufferMinutes
        
        val windowStart = date.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
        val windowEnd = date.plusDays(1).atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
        val dayBusySlots = calendarRepository.getBusySlots(windowStart, windowEnd)
        
        val busyContext = if (dayBusySlots.isEmpty()) "No meetings today" else dayBusySlots.joinToString { 
            val s = LocalDateTime.ofInstant(java.time.Instant.ofEpochMilli(it.start.value), ZoneId.systemDefault()).format(DateTimeFormatter.ofPattern("HH:mm"))
            val e = LocalDateTime.ofInstant(java.time.Instant.ofEpochMilli(it.end.value), ZoneId.systemDefault()).format(DateTimeFormatter.ofPattern("HH:mm"))
            "$s-$e"
        }

        val schedulingMode = if ((prefs.workEnd - prefs.workStart) < 8) "STRICT (Narrow window, prioritize efficiency)" else "FLEXIBLE (Wide window, allow breathing room)"

        val prompt = """
            The user wants to schedule "${task.title}" (${task.durationMinutes} mins).
            Today's Date: $date.
            Current Time: ${LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm"))}.
            Current busy slots for $date: [$busyContext].
            Preferred Energy Level: ${task.energyLevel}.
            Required Buffer: $bufferMinutes minutes.
            Active Hours: ${prefs.workStart.toInt()}:00 to ${prefs.workEnd.toInt()}:00.
            Scheduling Mode: $schedulingMode.
            
            YOUR TASK:
            1. Find a free slot STRICTLY ON $date that fits their energy level and respects the $bufferMinutes-minute buffer.
            2. CRITICAL: Only suggest times that are in the FUTURE (after ${LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm"))} if the date is today). 
            3. CRITICAL: Suggested time MUST be WITHIN Active Hours (${prefs.workStart.toInt()}:00 to ${prefs.workEnd.toInt()}:00).
            4. DO NOT suggest tomorrow's date unless specifically requested.
            5. If High energy: Prefer 08:00-11:00. Medium: 11:00-14:00. Low: 14:00-17:00.
            6. FORMAT: Respond ONLY with this JSON. Ensure 'new_time' is a valid HH:mm time (00-23:00-59).
            7. MESSAGE: Keep 'suggestion' very brief, empathetic, and user-friendly. DO NOT mention math, buffers, or busy slots (e.g., "I've found a spot at 14:00! Shall we?").
            
            {
                "suggestion": "I've found a spot at [TIME]! Shall we?",
                "new_time": "HH:mm"
            }
        """.trimIndent()
        
        val aiResult = aiManager.analyzeTask(taskText = "", customPrompt = prompt)
        
        if (aiResult.startsWith("Error")) {
            return SchedulingResult.Error("Network error: Could not find a free spot. Please check your internet connection.")
        }
        
        var suggestedTime: String? = null
        var suggestionMessage = "I've found an optimal spot for this. Shall we schedule it?"

        try {
            val jsonStr = com.first_project.chronoai.ai.ResponseParser.extractJson(aiResult)
            val json = org.json.JSONObject(jsonStr)
            if (json.has("suggestion")) suggestionMessage = json.getString("suggestion")
            if (json.has("new_time")) suggestedTime = json.getString("new_time")
        } catch (e: Exception) {
            // Robust fallback for string extraction if JSON parsing fails
            val timeRegex = java.util.regex.Pattern.compile("\"new_time\"\\s*:\\s*\"(\\d{1,2}:\\d{2})")
            val matcher = timeRegex.matcher(aiResult)
            if (matcher.find()) {
                suggestedTime = matcher.group(1)
            }
            
            val msgRegex = java.util.regex.Pattern.compile("\"suggestion\"\\s*:\\s*\"([^\"]+)\"")
            val msgMatcher = msgRegex.matcher(aiResult)
            if (msgMatcher.find()) {
                suggestionMessage = msgMatcher.group(1) ?: "I've found a spot that works with your schedule."
            }
        }

        // Final normalization: Ensure time is in HH:mm format and mathematically valid
        suggestedTime = suggestedTime?.let {
            try {
                if (it.contains(":")) {
                    val parts = it.split(":")
                    var hour = parts[0].trim().toInt()
                    var min = parts[1].trim().take(2).toInt()
                    
                    // Fix potential AI math errors (like 11:62)
                    if (min >= 60) {
                        hour += min / 60
                        min %= 60
                    }
                    hour %= 24
                    
                    String.format(java.util.Locale.US, "%02d:%02d", hour, min)
                } else null
            } catch (e: Exception) { null }
        } ?: run {
            // Ultimate fallback based on energy if AI fails to provide a time
            when(task.energyLevel) {
                "High" -> "09:00"
                "Low" -> "15:00"
                else -> "11:00"
            }
        }
        
        // Ensure the message actually mentions the time so it's not confusing
        if (suggestionMessage.contains("[TIME]")) {
            suggestionMessage = suggestionMessage.replace("[TIME]", suggestedTime)
        } else if (!suggestionMessage.contains(suggestedTime)) {
            suggestionMessage = suggestionMessage.removeSuffix(".") + " at $suggestedTime."
        }
        
        return SchedulingResult.Conflict(dayBusySlots, suggestionMessage, suggestedTime)
    }
}
