package com.first_project.chronoai.ui1.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.first_project.chronoai.data.local.prefs.SchedulingPreferences
import com.first_project.chronoai.data.local.prefs.UserPreferencesRepo
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

enum class ThemeMode { LIGHT, DARK, SYSTEM }

class ThemeViewModel(private val repository: UserPreferencesRepo) : ViewModel() {

    val prefs: StateFlow<SchedulingPreferences> = repository.schedulingPreferences
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = SchedulingPreferences()
        )

    fun setThemeMode(mode: ThemeMode) {
        viewModelScope.launch { repository.updateThemeMode(mode) }
    }

    fun setWorkHours(start: Float, end: Float) {
        viewModelScope.launch { repository.updateWorkHours(start, end) }
    }

    fun setBufferTime(minutes: Int) {
        viewModelScope.launch { repository.updateBuffer(minutes) }
    }

    fun setEnergyWindows(enabled: Boolean) {
        viewModelScope.launch { repository.updateEnergyWindows(enabled) }
    }

    fun setFocusShield(enabled: Boolean) {
        viewModelScope.launch { repository.updateFocusShield(enabled) }
    }

    fun setVoicePersona(persona: String) {
        viewModelScope.launch { repository.updateVoicePersona(persona) }
    }

    fun setSmartSpacing(enabled: Boolean) {
        viewModelScope.launch { repository.updateSmartSpacing(enabled) }
    }

    fun completeOnboarding() {
        viewModelScope.launch { repository.updateOnboardingStatus(true) }
    }
}
