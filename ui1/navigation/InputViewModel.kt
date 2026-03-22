package com.first_project.chronoai.ui1.navigation

import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.view.HapticFeedbackConstants
import android.view.View
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.first_project.chronoai.ai.GroqManager
import com.first_project.chronoai.ai.ResponseParser
import com.first_project.chronoai.ai.TaskModel
import com.first_project.chronoai.data.local.db.DatabaseProvider
import com.first_project.chronoai.data.local.entity.TaskEntity
import com.first_project.chronoai.ui1.utils.HapticManager
import com.first_project.chronoai.ui1.viewmodel.HomeViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId

sealed class InputUiState {
    object Idle : InputUiState()
    object Analyzing : InputUiState()
    data class Success(val message: String) : InputUiState()
    data class Error(val message: String) : InputUiState()
    data class ActionRequired(val missingFields: List<String>, val partialTask: TaskModel) : InputUiState()
    data class NeedClarification(val question: String, val partialTask: TaskModel) : InputUiState()
}

data class DetectedContext(
    val date: String? = null,
    val duration: String? = null,
    val energy: String? = null,
    val priority: Int? = null
)

class InputViewModel(
    private val aiManager: GroqManager,
    private val homeViewModel: HomeViewModel
) : ViewModel() {

    private val _uiState = MutableStateFlow<InputUiState>(InputUiState.Idle)
    val uiState: StateFlow<InputUiState> = _uiState.asStateFlow()

    private val _detectedContext = MutableStateFlow(DetectedContext())
    val detectedContext: StateFlow<DetectedContext> = _detectedContext.asStateFlow()

    private var autoParseJob: Job? = null

    /**
     * Requirement 2: LIVE CONTEXT PREVIEW
     * Debounced auto-parsing while the user types.
     */
    fun onTextChanged(text: String) {
        autoParseJob?.cancel()
        if (text.length < 5) {
            _detectedContext.value = DetectedContext()
            return
        }

        autoParseJob = viewModelScope.launch {
            delay(800) // Debounce
            try {
                val raw = aiManager.analyzeTask(text)
                if (!raw.startsWith("Error:")) {
                    val model = ResponseParser.parse(raw)
                    _detectedContext.value = DetectedContext(
                        date = model.deadlineDate,
                        duration = "${model.durationMinutes}m",
                        energy = model.energyLevel,
                        priority = model.priority
                    )
                }
            } catch (e: Exception) {
                // Fail silently for live preview
            }
        }
    }

    fun updateDetectedContext(context: DetectedContext) {
        _detectedContext.value = context
    }

    /**
     * Requirement 3: THE 'SMART' SUBMISSION
     */
    fun processTask(context: Context, spokenText: String, energyLevel: String, view: View? = null) {
        if (spokenText.isEmpty()) return

        val hapticManager = HapticManager(context)
        _uiState.value = InputUiState.Analyzing
        hapticManager.play(HapticManager.VyntaEffect.AI_PROCESSING)

        viewModelScope.launch {
            try {
                val raw = try {
                    aiManager.analyzeTask(spokenText)
                } catch (e: Exception) {
                    hapticManager.play(HapticManager.VyntaEffect.ERROR)
                    _uiState.value = InputUiState.Error("AI Analysis failed: ${e.message}")
                    return@launch
                }

                if (raw.startsWith("Error:")) {
                    hapticManager.play(HapticManager.VyntaEffect.ERROR)
                    _uiState.value = InputUiState.Error(raw)
                    return@launch
                }

                val taskModel = try {
                    ResponseParser.parse(raw)
                } catch (e: Exception) {
                    hapticManager.play(HapticManager.VyntaEffect.ERROR)
                    _uiState.value = InputUiState.Error("Parsing failed: ${e.message}")
                    return@launch
                }

                // Requirement 4: HUMAN-LIKE CLARIFICATION
                // Check if the AI indicated clarification is needed (logic simplified for brevity)
                if (taskModel.title.lowercase().contains("clarify") || taskModel.title.isBlank()) {
                    _uiState.value = InputUiState.NeedClarification(
                        question = "I'm not sure about the task details. Could you clarify what exactly you'd like to schedule?",
                        partialTask = taskModel
                    )
                    return@launch
                }

                val missingFields = ResponseParser.getMissingFields(taskModel)
                if (missingFields.isNotEmpty()) {
                    hapticManager.play(HapticManager.VyntaEffect.ERROR)
                    _uiState.value = InputUiState.ActionRequired(missingFields, taskModel)
                    return@launch
                }

                // 3. TaskEntity Persistence
                val taskEntity = TaskEntity(
                    title = taskModel.title,
                    deadline = "${taskModel.deadlineDate} ${taskModel.deadlineTime ?: "09:00"}",
                    energyLevel = energyLevel,
                    status = "SCHEDULED",
                    priority = taskModel.priority
                )
                DatabaseProvider.getDatabase(context).taskDao().insertTask(taskEntity)

                // 4. Scheduling Engine
                val date = LocalDate.parse(taskModel.deadlineDate)
                val time = LocalTime.parse(taskModel.deadlineTime ?: "09:00")
                val startDateTime = LocalDateTime.of(date, time)
                val endDateTime = startDateTime.plusMinutes(taskModel.durationMinutes.toLong())
                
                val startMillis = startDateTime.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
                val endMillis = endDateTime.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()

                val result = homeViewModel.scheduleEvent(
                    title = taskModel.title,
                    startTime = startMillis,
                    endTime = endMillis,
                    priority = taskModel.priority,
                    isRecurring = taskModel.isRecurring,
                    rrule = taskModel.recurrencePattern
                )
                
                if (result != null) {
                    // Requirement 3: CONFIRM haptic pattern upon success
                    hapticManager.play(HapticManager.VyntaEffect.SUCCESS)
                    _uiState.value = InputUiState.Success("Successfully scheduled!")
                } else {
                    hapticManager.play(HapticManager.VyntaEffect.ERROR)
                    _uiState.value = InputUiState.Error("Scheduling conflict detected in Google Calendar.")
                }

            } catch (e: Exception) {
                hapticManager.play(HapticManager.VyntaEffect.ERROR)
                _uiState.value = InputUiState.Error(e.message ?: "Unknown error")
            }
        }
    }

    fun resetState() {
        _uiState.value = InputUiState.Idle
    }
}