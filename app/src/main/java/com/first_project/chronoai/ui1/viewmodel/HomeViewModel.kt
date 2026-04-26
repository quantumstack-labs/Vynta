package com.first_project.chronoai.ui1.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.first_project.chronoai.data.CalendarRepository
import com.first_project.chronoai.data.local.dao.TaskDao
import com.first_project.chronoai.data.local.entity.TaskEntity
import com.google.api.services.calendar.model.Event
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import java.time.LocalDate
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.util.*

@OptIn(ExperimentalCoroutinesApi::class)
class HomeViewModel(
    private val repository: CalendarRepository,
    private val taskDao: TaskDao,
    private val aiManager: com.first_project.chronoai.ai.GroqManager
) : ViewModel() {
    private val _selectedDate = MutableStateFlow(LocalDate.now())
    val selectedDate: StateFlow<LocalDate> = _selectedDate.asStateFlow()

    private val _rawEvents = MutableStateFlow<List<Event>>(emptyList())
    
    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    // Optimized: Only sort when raw events or date changes. 
    // Removed currentTimeTrigger as it was not being used in the logic.
    val events: StateFlow<List<Event>> = combine(_rawEvents, _selectedDate) { rawEvents, date ->
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
        val isToday = if (date == LocalDate.now()) 1 else 0
        
        combine(
            taskDao.getTasksForDate(dateString, isToday),
            _energyFilter,
            _priorityFilter,
            _rawEvents
        ) { tasks, energy, priority, rawEvents ->
            tasks.filter { task ->
                val matchesEnergy = energy == null || task.energyLevel == energy
                val matchesPriority = priority == null || task.priority == priority
                
                // Note: SQL already handled the date match, but we still ensure 
                // consistency here if needed for any reason.
                matchesEnergy && matchesPriority
            }.distinctBy { it.id }.sortedByDescending { it.priority }
        }
    }.stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    private val _dailyBriefing = MutableStateFlow("Analyzing your day...")
    val dailyBriefing: StateFlow<String> = _dailyBriefing.asStateFlow()

    private var briefingJob: kotlinx.coroutines.Job? = null

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
                val prompt = """
                    Generate a short, encouraging one-sentence morning briefing (max 15 words) for an Android app.
                    Context: User has $pendingCount $taskText and ${calendarEvents.size} calendar events today. 
                    $highPriority tasks are high priority.
                    Make it feel alive, supportive, and concise. Avoid "Here is your briefing".
                """.trimIndent()
                
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
        
        // Watch tasks and events to update briefing
        viewModelScope.launch {
            combine(personalTasks, events) { tasks, calendarEvents ->
                Pair(tasks, calendarEvents)
            }.collect { (tasks, calendarEvents) ->
                updateBriefing(tasks, calendarEvents)
            }
        }
    }

    val completionProgress: StateFlow<Float> = personalTasks
        .map { tasks ->
            if (tasks.isEmpty()) 0f
            else {
                val completed = tasks.count { it.status == "COMPLETED" }
                completed.toFloat() / tasks.size
            }
        }.stateIn(viewModelScope, SharingStarted.Lazily, 0f)

    val forgottenTasks: StateFlow<List<TaskEntity>> = taskDao.getAllTasks()
        .map { tasks ->
            val todayStr = LocalDate.now().toString()
            tasks.filter { 
                val taskDate = it.deadline?.split(" ")?.firstOrNull()
                it.status != "COMPLETED" && taskDate != null && taskDate < todayStr
            }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun moveForgottenTasksToToday(context: Context, tasks: List<TaskEntity>) {
        viewModelScope.launch {
            val todayStr = LocalDate.now().toString()
            tasks.forEach { task ->
                val newDeadline = todayStr + (task.deadline?.substringAfter(" ")?.let { " $it" } ?: "")
                taskDao.updateTask(task.copy(deadline = newDeadline))
            }
            com.first_project.chronoai.ui1.widget.updateVyntaWidgets(context)
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
            val newStatus = if (task.status == "COMPLETED") "SCHEDULED" else "COMPLETED"
            taskDao.updateTask(task.copy(status = newStatus))
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
}
