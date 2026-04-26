package com.first_project.chronoai.ui1.navigation

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.first_project.chronoai.ai.GroqManager
import com.first_project.chronoai.ai.ResponseParser
import com.first_project.chronoai.ai.TaskModel
import com.first_project.chronoai.data.local.db.DatabaseProvider
import com.first_project.chronoai.data.local.entity.TaskEntity
import com.first_project.chronoai.data.local.prefs.SchedulingPreferences
import com.first_project.chronoai.data.local.prefs.UserPreferencesRepo
import com.first_project.chronoai.domain.ScheduleTaskUseCase
import com.first_project.chronoai.domain.SchedulingResult
import com.first_project.chronoai.ui1.utils.HapticManager
import com.first_project.chronoai.ui1.viewmodel.HomeViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.UUID

sealed class InputUiState {
    object Idle : InputUiState()
    object Loading : InputUiState()
    data class Success(val message: String, val task: TaskModel, val personaMessage: String) : InputUiState()
    data class Conflict(
        val conflictingEvents: List<com.google.api.services.calendar.model.TimePeriod>, 
        val suggestion: String?,
        val suggestedTime: String? = null
    ) : InputUiState()
    data class Error(val message: String) : InputUiState()
}

data class DetectedContext(
    val title: String = "",
    val deadlineDate: String? = null,
    val deadlineTime: String? = null,
    val priority: Int = 3,
    val energyLevel: String = "Medium",
    val subtasks: List<String> = emptyList(),
    val isRecurring: Boolean = false,
    val recurrencePattern: String? = null
)

class InputViewModel(
    private val aiManager: GroqManager,
    private val homeViewModel: HomeViewModel,
    private val scheduleTaskUseCase: ScheduleTaskUseCase,
    private val userPreferencesRepo: UserPreferencesRepo
) : ViewModel() {

    private val _uiState = MutableStateFlow<InputUiState>(InputUiState.Idle)
    val uiState: StateFlow<InputUiState> = _uiState.asStateFlow()

    private val _detectedContext = MutableStateFlow(DetectedContext())
    val detectedContext: StateFlow<DetectedContext> = _detectedContext.asStateFlow()

    val prefs: StateFlow<SchedulingPreferences> = userPreferencesRepo.schedulingPreferences
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = SchedulingPreferences()
        )

    private var editingTaskId: Int? = null
    private var analysisJob: Job? = null

    fun loadTaskForEditing(context: Context, taskId: Int) {
        editingTaskId = taskId
        viewModelScope.launch {
            val task = DatabaseProvider.getDatabase(context).taskDao().getTaskById(taskId)
            task?.let {
                val date = it.deadline?.substringBefore(" ")
                val time = it.deadline?.substringAfter(" ")
                _detectedContext.value = DetectedContext(
                    title = it.title,
                    deadlineDate = date,
                    deadlineTime = time,
                    priority = it.priority,
                    energyLevel = it.energyLevel ?: "Medium",
                    subtasks = it.subtasks,
                    isRecurring = it.isRecurring,
                    recurrencePattern = it.recurrencePattern
                )
            }
        }
    }

    fun onTextChanged(input: String) {
        // Debounce AI background processing to update intelligent defaults
        analysisJob?.cancel()
        if (input.length < 5) return

        analysisJob = viewModelScope.launch {
            delay(800) // Wait for user to stop typing
            try {
                val resultJson = aiManager.analyzeTask(input)
                val taskModel = ResponseParser.parse(resultJson, input)
                
                _detectedContext.value = DetectedContext(
                    title = taskModel.title,
                    deadlineDate = taskModel.deadlineDate,
                    deadlineTime = taskModel.deadlineTime,
                    priority = taskModel.priority,
                    energyLevel = taskModel.energyLevel,
                    subtasks = taskModel.proposedSubtasks,
                    isRecurring = taskModel.isRecurring,
                    recurrencePattern = taskModel.recurrencePattern
                )
            } catch (e: Exception) {
                // Silently fail for background analysis
            }
        }
    }

    fun processTask(
        context: Context, 
        input: String, 
        energyOverride: String? = null, 
        subtasksOverride: List<String>? = null,
        dateOverride: String? = null,
        timeOverride: String? = null,
        recurrenceOverride: Boolean? = null,
        rruleOverride: String? = null,
        skipAiReanalysis: Boolean = false,
        force: Boolean = false
    ) {
        viewModelScope.launch {
            _uiState.value = InputUiState.Loading
            val hapticManager = HapticManager(context)

            try {
                // Get existing event ID if editing
                val dao = DatabaseProvider.getDatabase(context).taskDao()
                val existingTask = editingTaskId?.let { dao.getTaskById(it) }
                val existingEventId = existingTask?.calendarEventId

                // 1. AI Analysis (final confirmation or skip if already accepted a suggestion)
                var taskModel = if (skipAiReanalysis) {
                    // Create a direct model from inputs if skipping AI
                    TaskModel(
                        title = _detectedContext.value.title.ifBlank { input },
                        durationMinutes = 60,
                        priority = _detectedContext.value.priority,
                        isRecurring = recurrenceOverride ?: _detectedContext.value.isRecurring,
                        recurrencePattern = rruleOverride ?: _detectedContext.value.recurrencePattern,
                        energyLevel = energyOverride ?: _detectedContext.value.energyLevel,
                        bestTime = "Anytime",
                        hasDeadline = true,
                        deadlineDate = dateOverride ?: _detectedContext.value.deadlineDate,
                        deadlineTime = timeOverride ?: _detectedContext.value.deadlineTime,
                        thoughtProcess = "Accepted AI Suggestion",
                        confidenceScore = 1.0f,
                        status = "SUCCESS",
                        aiMessage = "Perfect, I've scheduled that for you.",
                        proposedSubtasks = subtasksOverride ?: _detectedContext.value.subtasks
                    )
                } else {
                    val resultJson = aiManager.analyzeTask(input)
                    try {
                        ResponseParser.parse(resultJson, input)
                    } catch (e: Exception) {
                        TaskModel(
                            title = input,
                            durationMinutes = 60,
                            priority = 3,
                            isRecurring = recurrenceOverride ?: _detectedContext.value.isRecurring,
                            recurrencePattern = rruleOverride ?: _detectedContext.value.recurrencePattern,
                            energyLevel = energyOverride ?: "Medium",
                            bestTime = "Anytime",
                            hasDeadline = false,
                            deadlineDate = dateOverride,
                            deadlineTime = timeOverride,
                            thoughtProcess = "Fallback due to parsing error",
                            confidenceScore = 0.5f,
                            status = "SUCCESS",
                            aiMessage = "I've drafted this based on your input.",
                            proposedSubtasks = subtasksOverride ?: emptyList()
                        )
                    }
                }

                // Apply overrides if provided manually via UI
                val finalTime = if (timeOverride == "OPTIMAL") null else (timeOverride ?: taskModel.deadlineTime)
                
                taskModel = taskModel.copy(
                    energyLevel = energyOverride ?: taskModel.energyLevel,
                    proposedSubtasks = subtasksOverride ?: taskModel.proposedSubtasks,
                    deadlineDate = dateOverride ?: taskModel.deadlineDate,
                    deadlineTime = finalTime,
                    isRecurring = recurrenceOverride ?: taskModel.isRecurring,
                    recurrencePattern = rruleOverride ?: taskModel.recurrencePattern
                )

                // 2. Scheduling via Use Case
                val result = scheduleTaskUseCase.execute(taskModel, existingEventId, force = force)

                when (result) {
                    is SchedulingResult.Success -> {
                        // Create task entity for DB
                        val taskEntity = TaskEntity(
                            id = editingTaskId ?: UUID.randomUUID().hashCode(),
                            title = taskModel.title,
                            deadline = "${taskModel.deadlineDate ?: ""} ${taskModel.deadlineTime ?: ""}".trim(),
                            priority = taskModel.priority,
                            energyLevel = taskModel.energyLevel,
                            status = "SCHEDULED",
                            subtasks = taskModel.proposedSubtasks,
                            calendarEventId = result.eventId,
                            isRecurring = taskModel.isRecurring,
                            recurrencePattern = taskModel.recurrencePattern,
                            deadlineTime = taskModel.deadlineTime,
                            schedulingReason = result.reason
                        )
                        
                        // Insert or Update in DB
                        if (editingTaskId != null) {
                            dao.updateTask(taskEntity)
                        } else {
                            dao.insertTask(taskEntity)
                        }
                        
                        // Notify widget of the new task
                        com.first_project.chronoai.ui1.widget.updateVyntaWidgets(context)
                        
                        hapticManager.play(HapticManager.VyntaEffect.SUCCESS)
                        _uiState.value = InputUiState.Success(
                            message = result.message,
                            task = taskModel,
                            personaMessage = taskModel.aiMessage ?: result.message
                        )
                        homeViewModel.fetchEvents()
                    }
                    is SchedulingResult.Conflict -> {
                        hapticManager.play(HapticManager.VyntaEffect.ERROR)
                        _uiState.value = InputUiState.Conflict(
                            conflictingEvents = result.conflictingEvents, 
                            suggestion = result.suggestion,
                            suggestedTime = result.suggestedTime
                        )
                    }
                    is SchedulingResult.Error -> {
                        _uiState.value = InputUiState.Error(result.message)
                        hapticManager.play(HapticManager.VyntaEffect.ERROR)
                    }
                }

            } catch (e: Exception) {
                _uiState.value = InputUiState.Error("System Error: ${e.localizedMessage}")
                hapticManager.play(HapticManager.VyntaEffect.ERROR)
            }
        }
    }

    fun resetState() {
        _uiState.value = InputUiState.Idle
        _detectedContext.value = DetectedContext()
        editingTaskId = null
    }
}
