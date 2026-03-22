package com.first_project.chronoai.ui1.navigation

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.first_project.chronoai.ai.GroqManager
import com.first_project.chronoai.ai.ResponseParser
import com.first_project.chronoai.ai.TaskModel
import com.first_project.chronoai.data.local.db.DatabaseProvider
import com.first_project.chronoai.data.local.entity.TaskEntity
import com.first_project.chronoai.data.local.prefs.UserPreferencesRepo
import com.first_project.chronoai.domain.ScheduleTaskUseCase
import com.first_project.chronoai.domain.SchedulingResult
import com.first_project.chronoai.ui1.util.HapticManager
import com.first_project.chronoai.ui1.viewmodel.HomeViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.UUID

sealed class InputUiState {
    object Idle : InputUiState()
    object Loading : InputUiState()
    data class Success(val message: String, val task: TaskModel, val personaMessage: String) : InputUiState()
    data class Conflict(val conflictingEvents: List<com.google.api.services.calendar.model.TimePeriod>, val suggestion: String?) : InputUiState()
    data class Error(val message: String) : InputUiState()
}

class InputViewModel(
    private val aiManager: GroqManager,
    private val homeViewModel: HomeViewModel,
    private val scheduleTaskUseCase: ScheduleTaskUseCase,
    private val userPreferencesRepo: UserPreferencesRepo
) : ViewModel() {

    private val _uiState = MutableStateFlow<InputUiState>(InputUiState.Idle)
    val uiState: StateFlow<InputUiState> = _uiState.asStateFlow()

    fun processTask(context: Context, input: String) {
        viewModelScope.launch {
            _uiState.value = InputUiState.Loading
            val hapticManager = HapticManager(context)

            try {
                // 1. AI Analysis
                val resultJson = aiManager.analyzeTask(input)
                val taskModel = ResponseParser.parseTaskResponse(resultJson)

                if (taskModel == null) {
                    _uiState.value = InputUiState.Error("I couldn't quite understand that. Could you try being more specific?")
                    hapticManager.play(HapticManager.VyntaEffect.ERROR)
                    return@launch
                }

                // 2. Scheduling via Use Case
                val result = scheduleTaskUseCase.execute(taskModel)

                when (result) {
                    is SchedulingResult.Success -> {
                        val subtasks = taskModel.proposedSubtasks.take(3)
                        val taskEntity = TaskEntity(
                            id = UUID.randomUUID().hashCode(),
                            title = taskModel.title,
                            deadline = "${taskModel.deadlineDate} ${taskModel.deadlineTime}",
                            priority = taskModel.priority,
                            energyLevel = taskModel.energyLevel,
                            status = "SCHEDULED",
                            subtasks = subtasks,
                            calendarEventId = result.eventId
                        )
                        DatabaseProvider.getDatabase(context).taskDao().insertTask(taskEntity)
                        hapticManager.play(HapticManager.VyntaEffect.SUCCESS)
                        _uiState.value = InputUiState.Success(
                            message = result.message,
                            task = taskModel.copy(proposedSubtasks = subtasks),
                            personaMessage = taskModel.aiMessage
                        )
                        homeViewModel.fetchEvents()
                    }
                    is SchedulingResult.Conflict -> {
                        hapticManager.play(HapticManager.VyntaEffect.ERROR)
                        val cleanedSuggestion = result.suggestedSlot?.let { ResponseParser.cleanSuggestion(it) }
                        _uiState.value = InputUiState.Conflict(result.conflictingEvents, cleanedSuggestion)
                    }
                    is SchedulingResult.Error -> {
                        // Updated to show the actual error message for better debugging
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
    }
}
