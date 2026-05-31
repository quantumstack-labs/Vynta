package com.first_project.chronoai.ui1.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.first_project.chronoai.data.CalendarRepository
import com.first_project.chronoai.data.local.dao.TaskDao
import com.first_project.chronoai.data.local.entity.TaskEntity
import com.first_project.chronoai.notification.PrecisionTriggerManager
import com.google.api.services.calendar.model.Event
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.*

@OptIn(ExperimentalCoroutinesApi::class, kotlinx.coroutines.FlowPreview::class)
class HomeViewModel(
    private val repository: CalendarRepository,
    private val taskDao: TaskDao,
    private val aiManager: com.first_project.chronoai.ai.GroqManager,
    private val userPreferencesRepo: com.first_project.chronoai.data.local.prefs.UserPreferencesRepo
) : ViewModel() {

    private val _voiceManager = com.first_project.chronoai.voice.VyntaVoiceManager(repository.getAppContext())
    
    val isSpeaking: StateFlow<Boolean> = _voiceManager.isSpeaking
    val currentVoiceCharIndex: StateFlow<Int> = _voiceManager.currentWordIndex

    fun stopSpeaking() {
        _voiceManager.stop()
    }

    private val _neuralInsights = MutableStateFlow<DailyBriefingResponse?>(null)
    val neuralInsights: StateFlow<DailyBriefingResponse?> = _neuralInsights.asStateFlow()

    private val _immersiveBriefing = MutableStateFlow("")
    val immersiveBriefing: StateFlow<String> = _immersiveBriefing.asStateFlow()

    private val _isSynthesizing = MutableStateFlow(false)
    val isSynthesizing: StateFlow<Boolean> = _isSynthesizing.asStateFlow()

    private var immersiveBriefingJob: Job? = null

    fun refreshImmersiveBriefing() {
        immersiveBriefingJob?.cancel()
        immersiveBriefingJob = viewModelScope.launch {
            _isSynthesizing.value = true
            try {
                val tasks = personalTasks.value
                val pending = tasks.filter { it.status != "COMPLETED" }
                val completed = tasks.filter { it.status == "COMPLETED" }

                val pendingText = if (pending.isEmpty()) "All missions secured."
                else pending.joinToString("\n") { "- ${it.title} (${it.deadlineTime ?: "Anytime"})" }
                
                val completedText = if (completed.isEmpty()) "None yet."
                else completed.joinToString("\n") { "- ${it.title}" }

                val persona = userPreferencesRepo.schedulingPreferences.first().voicePersona
                val prompt = com.first_project.chronoai.ai.PromptBuilder.buildImmersiveBriefingPrompt(pendingText, completedText, persona)
                val response = aiManager.analyzeTask("", prompt, useJsonMode = true)
                
                try {
                    val cleanedResponse = response.trim().removeSurrounding("```json", "```").trim()
                    val result = com.google.gson.Gson().fromJson(cleanedResponse, DailyBriefingResponse::class.java)
                    _neuralInsights.value = result
                    _immersiveBriefing.value = result.summary
                } catch (e: Exception) {
                    val startIndex = response.indexOf("{")
                    val endIndex = response.lastIndexOf("}")
                    if (startIndex != -1 && endIndex != -1 && endIndex > startIndex) {
                        try {
                            val jsonSub = response.substring(startIndex, endIndex + 1)
                            val result = com.google.gson.Gson().fromJson(jsonSub, DailyBriefingResponse::class.java)
                            _neuralInsights.value = result
                            _immersiveBriefing.value = result.summary
                        } catch (e2: Exception) {
                            _immersiveBriefing.value = response.trim().removeSurrounding("\"")
                        }
                    } else {
                        _immersiveBriefing.value = response.trim().removeSurrounding("\"")
                    }
                }
            } catch (e: Exception) {
                _immersiveBriefing.value = "Connection to neural core lost. Unable to synthesize."
            } finally {
                _isSynthesizing.value = false
            }
        }
    }

    data class DailyBriefingResponse(
        val summary: String,
        val insights: List<StrategicInsight>
    )

    data class StrategicInsight(
        val label: String,
        val observation: String,
        val action: String,
        val urgency: Float // 0.0 to 1.0
    )

    override fun onCleared() {
        super.onCleared()
        _voiceManager.shutdown()
    }

    fun speak(text: String) {
        viewModelScope.launch {
            val persona = userPreferencesRepo.schedulingPreferences.first().voicePersona
            _voiceManager.speak(text, persona)
        }
    }

    private val _selectedDate = MutableStateFlow(LocalDate.now())
    val selectedDate: StateFlow<LocalDate> = _selectedDate.asStateFlow()

    private val _rawEvents = MutableStateFlow<List<Event>>(emptyList())
    
    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    // Optimized: Only sort when raw events or date changes. 
    // Removed currentTimeTrigger as it was not being used in the logic.
    val events: StateFlow<List<Event>> = combine(_rawEvents, _selectedDate) { rawEvents, _ ->
        rawEvents.sortedBy { event ->
            event.start.dateTime?.toString() ?: event.start.date?.toString() ?: ""
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _isCalendarLoading = MutableStateFlow(false)
    val isCalendarLoading: StateFlow<Boolean> = _isCalendarLoading.asStateFlow()

    private val _energyFilter = MutableStateFlow<String?>(null)
    val energyFilter: StateFlow<String?> = _energyFilter.asStateFlow()

    private val _priorityFilter = MutableStateFlow<Int?>(null)
    val priorityFilter: StateFlow<Int?> = _priorityFilter.asStateFlow()

    /**
     * Requirement: Filtered and Sorted tasks for actionable UI
     * Optimized: Now uses SQL-level filtering for dates to improve performance.
     */
    val personalTasks: StateFlow<List<TaskEntity>> = _selectedDate.flatMapLatest { date ->
        val dateString = date.format(DateTimeFormatter.ISO_LOCAL_DATE)
        
        combine(
            taskDao.getAllTasks(), // Fetch all tasks to catch recurring ones on future dates
            _energyFilter,
            _priorityFilter,
            _rawEvents
        ) { allTasks, energy, priority, rawEvents ->
            allTasks.filter { task ->
                // A task matches this date if:
                // 1. Its stored deadline matches the date (for standard tasks)
                // 2. It has no deadline and we are looking at today
                // 3. It's a recurring task and its parent ID or recurring ID matches an event on THIS day
                val isCalendarMatch = task.calendarEventId != null && rawEvents.any { event ->
                    event.id == task.calendarEventId || event.recurringEventId == task.calendarEventId
                }
                
                val matchesDate = task.deadline?.startsWith(dateString) == true || 
                                 (task.deadline == null && date == LocalDate.now()) ||
                                 isCalendarMatch
                
                val matchesEnergy = energy == null || task.energyLevel == energy
                val matchesPriority = priority == null || task.priority == priority
                
                matchesDate && matchesEnergy && matchesPriority
            }.distinctBy { it.id }.sortedByDescending { it.priority }
        }
    }.stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    private val _dailyBriefing = MutableStateFlow("Analyzing your day...")
    val dailyBriefing: StateFlow<String> = _dailyBriefing.asStateFlow()

    private var briefingJob: Job? = null

    private fun updateBriefing(tasks: List<TaskEntity>, calendarEvents: List<Event>) {
        briefingJob?.cancel()
        
        val pendingCount = tasks.count { it.status != "COMPLETED" }
        val highPriority = tasks.count { it.priority >= 4 && it.status != "COMPLETED" }
        val taskText = if (pendingCount == 1) "task" else "tasks"

        // INSTANT UPDATE: Set static message first
        if (pendingCount == 0 && calendarEvents.isEmpty()) {
            _dailyBriefing.value = "Your schedule is clear. A perfect time to plan ahead."
            return
        }

        val baseMessage = when {
            highPriority > 0 -> "You have $highPriority critical tasks requiring immediate attention."
            calendarEvents.size > 3 -> "A busy day with ${calendarEvents.size} calendar events. Pace yourself."
            else -> "A balanced day ahead with $pendingCount $taskText. You've got this."
        }
        _dailyBriefing.value = baseMessage

        // Background AI enhancement
        briefingJob = viewModelScope.launch {
            try {
                val forgottenCount = forgottenTasks.value.size
                val persona = userPreferencesRepo.schedulingPreferences.first().voicePersona
                
                val prompt = com.first_project.chronoai.ai.PromptBuilder.buildBriefingPrompt(
                    pendingCount = pendingCount,
                    eventCount = calendarEvents.size,
                    highPriority = highPriority,
                    forgottenCount = forgottenCount,
                    persona = persona
                )
                
                val aiResponse = aiManager.analyzeTask("", prompt)
                if (aiResponse.isNotBlank() && !aiResponse.contains("Error")) {
                    _dailyBriefing.value = aiResponse.trim().removeSurrounding("\"")
                }
            } catch (e: Exception) {
                // Keep static message on failure
            }
        }
    }

    init {
        fetchEvents()
        
        // Watch tasks and events to update briefing and reschedule triggers
        viewModelScope.launch {
            personalTasks.collect { tasks ->
                val context = repository.getAppContext()
                tasks.filter { it.status != "COMPLETED" }.forEach { task ->
                    PrecisionTriggerManager.scheduleTaskTrigger(context, task)
                }
            }
        }

        viewModelScope.launch {
            combine(personalTasks, events) { tasks, calendarEvents ->
                Pair(tasks, calendarEvents)
            }
            .debounce(1000) // Avoid multiple calls when adding multiple tasks
            .collect { (tasks, calendarEvents) ->
                updateBriefing(tasks, calendarEvents)
                refreshImmersiveBriefing()
            }
        }
    }

    val temporalInsights: StateFlow<String> = taskDao.getAllTasks()
        .map { allTasks ->
            val completed = allTasks.filter { it.status == "COMPLETED" }
            if (completed.isEmpty()) return@map "Complete more tasks to unlock neural insights."
            
            val hourCounts = completed.mapNotNull { it.deadlineTime?.substringBefore(":")?.toIntOrNull() }
                .groupingBy { it }.eachCount()
            
            val peakHour = hourCounts.maxByOrNull { it.value }?.key
            if (peakHour != null) {
                val period = if (peakHour < 12) "AM" else "PM"
                val displayHour = if (peakHour % 12 == 0) 12 else peakHour % 12
                "Your neural peak is around $displayHour $period. You're most productive then."
            } else {
                "Analyzing your completion patterns..."
            }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "Calculating insights...")

    val completionProgress: StateFlow<Float> = combine(personalTasks, _selectedDate) { tasks, date ->
        if (tasks.isEmpty()) 0f
        else {
            val dateStr = date.toString()
            val completed = tasks.count { task ->
                if (task.isRecurring) task.completedDates.contains(dateStr)
                else task.status == "COMPLETED"
            }
            completed.toFloat() / tasks.size
        }
    }.stateIn(viewModelScope, SharingStarted.Lazily, 0f)

    /**
     * History Data: All tasks grouped by date for the Logbook
     */
    val historyTasks: StateFlow<Map<String, List<TaskEntity>>> = taskDao.getAllTasks()
        .map { allTasks ->
            allTasks.groupBy { task ->
                task.deadline?.take(10) ?: "Indefinite"
            }.toSortedMap(compareByDescending { it })
        }
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyMap())

    val forgottenTasks: StateFlow<List<TaskEntity>> = taskDao.getAllTasks()
        .map { tasks ->
            val todayStr = LocalDate.now().toString()
            tasks.filter { 
                val taskDate = it.deadline?.split(" ")?.firstOrNull()
                it.status != "COMPLETED" && taskDate != null && taskDate < todayStr
            }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _currentTimeTrigger = MutableStateFlow(System.currentTimeMillis())

    val isBehindSchedule: StateFlow<Boolean> = combine(personalTasks, _selectedDate, _currentTimeTrigger) { tasks, date, _ ->
        if (date != LocalDate.now()) return@combine false
        val nowStr = java.time.LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm"))
        tasks.any { it.status != "COMPLETED" && it.deadlineTime != null && it.deadlineTime < nowStr }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    fun triageLeftovers(context: android.content.Context, action: TriageAction) {
        viewModelScope.launch {
            val tasks = forgottenTasks.value
            val todayStr = LocalDate.now().toString()
            val tomorrowStr = LocalDate.now().plusDays(1).toString()

            tasks.forEach { task ->
                when (action) {
                    TriageAction.MOVE_TO_TODAY -> {
                        val newDeadline = todayStr + (task.deadline?.substringAfter(" ")?.let { " $it" } ?: "")
                        updateTaskDeadline(task, newDeadline)
                    }
                    TriageAction.MOVE_TO_TOMORROW -> {
                        val newDeadline = tomorrowStr + (task.deadline?.substringAfter(" ")?.let { " $it" } ?: "")
                        updateTaskDeadline(task, newDeadline)
                    }
                    TriageAction.DISMISS -> {
                        taskDao.deleteTask(task)
                    }
                }
            }
            com.first_project.chronoai.ui1.widget.updateVyntaWidgets(context)
            fetchEvents()
        }
    }

    private suspend fun updateTaskDeadline(task: TaskEntity, newDeadline: String) {
        task.calendarEventId?.let { eventId ->
            try {
                val startTime = java.time.LocalDateTime.parse(newDeadline, DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")).atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
                val endTime = startTime + (60 * 60 * 1000L)
                repository.updateEvent(
                    eventId = eventId,
                    title = task.title,
                    startTimeMillis = startTime,
                    endTimeMillis = endTime,
                    priority = task.priority,
                    energyLevel = task.energyLevel,
                    isRecurring = task.isRecurring,
                    rrule = task.recurrencePattern
                )
            } catch (e: Exception) { }
        }
        taskDao.updateTask(task.copy(deadline = newDeadline))
    }

    enum class TriageAction { MOVE_TO_TODAY, MOVE_TO_TOMORROW, DISMISS }

    fun moveForgottenTasksToToday(context: Context, tasks: List<TaskEntity>) {
        viewModelScope.launch {
            val todayStr = LocalDate.now().toString()
            tasks.forEach { task ->
                val newDeadline = todayStr + (task.deadline?.substringAfter(" ")?.let { " $it" } ?: "")
                
                // Update calendar if it exists
                task.calendarEventId?.let { eventId ->
                    try {
                        val startTime = java.time.LocalDateTime.parse(newDeadline, DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")).atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
                        val endTime = startTime + (60 * 60 * 1000L) // Default 1hr
                        repository.updateEvent(
                            eventId = eventId,
                            title = task.title,
                            startTimeMillis = startTime,
                            endTimeMillis = endTime,
                            priority = task.priority,
                            energyLevel = task.energyLevel,
                            isRecurring = task.isRecurring,
                            rrule = task.recurrencePattern
                        )
                    } catch (e: Exception) {
                        android.util.Log.e("HomeViewModel", "Failed to move calendar event", e)
                    }
                }

                taskDao.updateTask(task.copy(deadline = newDeadline))
            }
            com.first_project.chronoai.ui1.widget.updateVyntaWidgets(context)
            fetchEvents()
        }
    }

    fun setSelectedDate(date: LocalDate) {
        if (_selectedDate.value == date) return
        _selectedDate.value = date
        fetchEventsForDate(date)
    }

    fun setEnergyFilter(energy: String?) {
        _energyFilter.value = if (_energyFilter.value == energy) null else energy
    }

    fun clearError() {
        _error.value = null
    }

    fun setPriorityFilter(priority: Int?) {
        _priorityFilter.value = if (_priorityFilter.value == priority) null else priority
    }

    fun toggleTaskCompletion(context: Context, task: TaskEntity) {
        viewModelScope.launch {
            val dateStr = _selectedDate.value.toString()
            val updatedTask = if (task.isRecurring) {
                val newCompletedDates = if (task.completedDates.contains(dateStr)) {
                    task.completedDates.filter { it != dateStr }
                } else {
                    task.completedDates + dateStr
                }
                task.copy(completedDates = newCompletedDates)
            } else {
                val newStatus = if (task.status == "COMPLETED") "SCHEDULED" else "COMPLETED"
                task.copy(status = newStatus)
            }
            
            taskDao.updateTask(updatedTask)
            if (updatedTask.status != "COMPLETED") {
                PrecisionTriggerManager.scheduleTaskTrigger(context, updatedTask)
            }
            com.first_project.chronoai.ui1.widget.updateVyntaWidgets(context)
        }
    }

    fun deleteTask(context: Context, task: TaskEntity) {
        viewModelScope.launch {
            task.calendarEventId?.let { eventId ->
                try { 
                    repository.deleteEvent(eventId) 
                    android.util.Log.d("HomeViewModel", "Deleted calendar event: $eventId")
                } catch (e: Exception) {
                    android.util.Log.e("HomeViewModel", "Failed to delete calendar event", e)
                }
            }
            taskDao.deleteTask(task)
            com.first_project.chronoai.ui1.widget.updateVyntaWidgets(context)
            fetchEvents()
        }
    }

    fun fetchEvents() {
        fetchEventsForDate(_selectedDate.value)
    }

    private fun fetchEventsForDate(date: LocalDate) {
        viewModelScope.launch {
            _isCalendarLoading.value = true
            _error.value = null
            try {
                // Using the optimized date-specific fetch from repository
                val dayEvents = repository.getEventsForDate(date)
                _rawEvents.value = dayEvents
            } catch (e: Exception) {
                android.util.Log.e("HomeViewModel", "Error fetching events", e)
                _error.value = "Failed to sync calendar: ${e.localizedMessage}"
                _rawEvents.value = emptyList()
            } finally {
                _isCalendarLoading.value = false
            }
        }
    }

    fun requestAdaptiveReflow(context: Context, problem: String) {
        val hapticManager = com.first_project.chronoai.ui1.utils.HapticManager(context)
        viewModelScope.launch {
            _isCalendarLoading.value = true
            _error.value = "Vynta is re-balancing your day..."
            try {
                hapticManager.play(com.first_project.chronoai.ui1.utils.HapticManager.VyntaEffect.REFLOW)
                val prefs = userPreferencesRepo.schedulingPreferences.first()
                val todayTasks = personalTasks.value.filter { it.status != "COMPLETED" }
                
                if (todayTasks.isEmpty()) {
                    _error.value = "Your schedule is already clear. No reflow needed."
                    return@launch
                }

                val taskListText = todayTasks.joinToString("\n") { 
                    "- ${it.title} (ID: ${it.id}, Current: ${it.deadlineTime ?: "Anytime"}, Energy: ${it.energyLevel})" 
                }

                val reflowJson = aiManager.analyzeReflow(problem, taskListText, prefs)
                
                // Extract JSON array if AI returned markdown
                val cleanedJson = if (reflowJson.contains("[")) {
                    reflowJson.substring(reflowJson.indexOf("["), reflowJson.lastIndexOf("]") + 1)
                } else {
                    reflowJson
                }

                if (cleanedJson.trim() == "[]" || cleanedJson.isBlank()) {
                    _error.value = "AI: Your current flow is optimal. No changes suggested."
                    return@launch
                }

                val type = object : com.google.gson.reflect.TypeToken<List<ReflowTask>>() {}.type
                val suggestions: List<ReflowTask> = com.google.gson.Gson().fromJson(cleanedJson, type)

                suggestions.forEach { suggestion ->
                    val task = todayTasks.find { it.id == suggestion.id }
                    if (task != null) {
                        val newDeadline = "${suggestion.new_date} ${suggestion.new_time}"
                        
                        // Update Calendar
                        task.calendarEventId?.let { eventId ->
                            try {
                                val sdf = java.text.SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())
                                val startTime = sdf.parse(newDeadline)?.time ?: 0L
                                val endTime = startTime + (60 * 60 * 1000L)
                                repository.updateEvent(
                                    eventId = eventId,
                                    title = task.title,
                                    startTimeMillis = startTime,
                                    endTimeMillis = endTime,
                                    priority = task.priority,
                                    energyLevel = task.energyLevel
                                )
                            } catch (e: Exception) { }
                        }

                        // Update DB
                        taskDao.updateTask(task.copy(
                            deadline = newDeadline,
                            deadlineTime = suggestion.new_time,
                            schedulingReason = suggestion.reasoning
                        ))
                    }
                }
                
                fetchEvents()
                com.first_project.chronoai.ui1.widget.updateVyntaWidgets(context)
                _error.value = "AI: Re-flowed your day based on: $problem"
            } catch (e: Exception) {
                _error.value = "Reflow failed: ${e.localizedMessage}"
            } finally {
                _isCalendarLoading.value = false
            }
        }
    }

    data class ReflowTask(
        val id: Int,
        val title: String,
        val new_date: String,
        val new_time: String,
        val reasoning: String
    )
}
