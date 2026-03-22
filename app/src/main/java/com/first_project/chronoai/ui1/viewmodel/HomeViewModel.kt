package com.first_project.chronoai.ui1.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.first_project.chronoai.data.CalendarRepository
import com.first_project.chronoai.data.local.dao.TaskDao
import com.first_project.chronoai.data.local.entity.TaskEntity
import com.google.api.services.calendar.model.Event
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.util.*

class HomeViewModel(
    private val repository: CalendarRepository,
    private val taskDao: TaskDao
) : ViewModel() {
    private val _selectedDate = MutableStateFlow(LocalDate.now())
    val selectedDate: StateFlow<LocalDate> = _selectedDate.asStateFlow()

    private val _rawEvents = MutableStateFlow<List<Event>>(emptyList())
    
    // Timer to trigger re-filtering based on current time
    private val currentTimeTrigger = flow {
        while (true) {
            emit(ZonedDateTime.now())
            delay(60000) // Update every minute
        }
    }

    val events: StateFlow<List<Event>> = combine(_rawEvents, _selectedDate, currentTimeTrigger) { rawEvents, date, now ->
        // Return all events for the selected date without filtering out past ones,
        // so they don't disappear as the day progresses.
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
     */
    val personalTasks: StateFlow<List<TaskEntity>> = combine(
        taskDao.getAllTasks(),
        _selectedDate,
        _energyFilter,
        _priorityFilter,
        currentTimeTrigger
    ) { tasks, date, energy, priority, now ->
        val dateString = date.format(DateTimeFormatter.ISO_LOCAL_DATE)
        tasks.filter { task ->
            val matchesDate = task.deadline?.startsWith(dateString) == true || (task.deadline == null && date == LocalDate.now())
            val matchesEnergy = energy == null || task.energyLevel == energy
            val matchesPriority = priority == null || task.priority == priority
            
            // Removed isRemaining check so tasks don't disappear after their deadline passes.
            matchesDate && matchesEnergy && matchesPriority
        }.sortedByDescending { it.priority }
    }.stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    /**
     * AI Daily Briefing Logic
     */
    val dailyBriefing: StateFlow<String> = combine(personalTasks, events) { tasks, calendarEvents ->
        val pendingCount = tasks.count { it.status != "COMPLETED" }
        val highPriority = tasks.count { it.priority >= 4 && it.status != "COMPLETED" }
        
        when {
            pendingCount == 0 && calendarEvents.isEmpty() -> "Your schedule is clear. A perfect time to plan ahead."
            highPriority > 0 -> "You have $highPriority critical tasks requiring immediate attention."
            calendarEvents.size > 3 -> "A busy day with ${calendarEvents.size} calendar events. Pace yourself."
            else -> "A balanced day ahead with $pendingCount tasks. You've got this."
        }
    }.stateIn(viewModelScope, SharingStarted.Lazily, "Analyzing your day...")

    val completionProgress: StateFlow<Float> = taskDao.getAllTasks()
        .map { tasks ->
            val today = LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE)
            val todayTasks = tasks.filter { it.deadline?.startsWith(today) == true || it.deadline == null }
            if (todayTasks.isEmpty()) 0f
            else {
                val completed = todayTasks.count { it.status == "COMPLETED" }
                completed.toFloat() / todayTasks.size
            }
        }.stateIn(viewModelScope, SharingStarted.Lazily, 0f)

    init {
        fetchEvents()
    }

    fun setSelectedDate(date: LocalDate) {
        if (_selectedDate.value == date) return
        _selectedDate.value = date
        fetchEventsForDate(date)
    }

    fun setEnergyFilter(energy: String?) {
        _energyFilter.value = if (_energyFilter.value == energy) null else energy
    }

    fun setPriorityFilter(priority: Int?) {
        _priorityFilter.value = if (_priorityFilter.value == priority) null else priority
    }

    fun toggleTaskCompletion(context: Context, task: TaskEntity) {
        viewModelScope.launch {
            val newStatus = if (task.status == "COMPLETED") "SCHEDULED" else "COMPLETED"
            taskDao.updateTask(task.copy(status = newStatus))
        }
    }

    fun deleteTask(context: Context, task: TaskEntity) {
        viewModelScope.launch {
            taskDao.deleteTask(task)
            task.calendarEventId?.let { eventId ->
                try { repository.deleteEvent(eventId) } catch (e: Exception) {}
            }
            fetchEvents()
        }
    }

    fun fetchEvents() {
        fetchEventsForDate(_selectedDate.value)
    }

    private fun fetchEventsForDate(date: LocalDate) {
        viewModelScope.launch {
            _isCalendarLoading.value = true
            try {
                // Using the optimized date-specific fetch from repository
                val dayEvents = repository.getEventsForDate(date)
                _rawEvents.value = dayEvents
            } catch (e: Exception) {
                android.util.Log.e("HomeViewModel", "Error fetching events", e)
                _rawEvents.value = emptyList()
            } finally {
                _isCalendarLoading.value = false
            }
        }
    }
}
