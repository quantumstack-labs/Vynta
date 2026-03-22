package com.first_project.chronoai.data.local.prefs

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import com.first_project.chronoai.ui1.viewmodel.ThemeMode
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

data class SchedulingPreferences(
    val workStart: Float = 7f,
    val workEnd: Float = 22f,
    val bufferMinutes: Int = 15,
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val energyWindowsEnabled: Boolean = false,
    val focusGuardEnabled: Boolean = false,
    val voicePersona: String = "Minimalist", // Minimalist, Motivator, Professional
    val dynamicGapEnabled: Boolean = false
)

class UserPreferencesRepo(private val context: Context) {
    private val dataStore = context.dataStore

    companion object {
        val WORK_START = floatPreferencesKey("work_start")
        val WORK_END = floatPreferencesKey("work_end")
        val BUFFER_MINUTES = intPreferencesKey("buffer_minutes")
        val THEME_MODE = stringPreferencesKey("theme_mode")
        val ENERGY_WINDOWS = booleanPreferencesKey("energy_windows")
        val FOCUS_GUARD = booleanPreferencesKey("focus_guard")
        val VOICE_PERSONA = stringPreferencesKey("voice_persona")
        val DYNAMIC_GAP = booleanPreferencesKey("dynamic_gap")
    }

    val schedulingPreferences: Flow<SchedulingPreferences> = dataStore.data.map { prefs ->
        SchedulingPreferences(
            workStart = prefs[WORK_START] ?: 7f,
            workEnd = prefs[WORK_END] ?: 22f,
            bufferMinutes = prefs[BUFFER_MINUTES] ?: 15,
            themeMode = ThemeMode.valueOf(prefs[THEME_MODE] ?: ThemeMode.SYSTEM.name),
            energyWindowsEnabled = prefs[ENERGY_WINDOWS] ?: false,
            focusGuardEnabled = prefs[FOCUS_GUARD] ?: false,
            voicePersona = prefs[VOICE_PERSONA] ?: "Minimalist",
            dynamicGapEnabled = prefs[DYNAMIC_GAP] ?: false
        )
    }

    suspend fun updateWorkHours(start: Float, end: Float) {
        dataStore.edit { it[WORK_START] = start; it[WORK_END] = end }
    }

    suspend fun updateBuffer(minutes: Int) {
        dataStore.edit { it[BUFFER_MINUTES] = minutes }
    }

    suspend fun updateThemeMode(mode: ThemeMode) {
        dataStore.edit { it[THEME_MODE] = mode.name }
    }

    suspend fun updateEnergyWindows(enabled: Boolean) {
        dataStore.edit { it[ENERGY_WINDOWS] = enabled }
    }

    suspend fun updateFocusGuard(enabled: Boolean) {
        dataStore.edit { it[FOCUS_GUARD] = enabled }
    }

    suspend fun updateVoicePersona(persona: String) {
        dataStore.edit { it[VOICE_PERSONA] = persona }
    }

    suspend fun updateDynamicGap(enabled: Boolean) {
        dataStore.edit { it[DYNAMIC_GAP] = enabled }
    }
}
