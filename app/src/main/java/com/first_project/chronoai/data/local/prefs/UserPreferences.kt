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
    val focusShieldEnabled: Boolean = false,
    val voicePersona: String = "Atlas", // Atlas, Lyra, Sloane, Orion
    val smartSpacingEnabled: Boolean = false,
    val hasCompletedOnboarding: Boolean = false,
    val lastSeenVersion: Int = 0,
    val hasAcceptedTerms: Boolean = false
)

class UserPreferencesRepo(private val context: Context) {
    private val dataStore = context.dataStore

    companion object {
        val WORK_START = floatPreferencesKey("work_start")
        val WORK_END = floatPreferencesKey("work_end")
        val BUFFER_MINUTES = intPreferencesKey("buffer_minutes")
        val THEME_MODE = stringPreferencesKey("theme_mode")
        val ENERGY_WINDOWS = booleanPreferencesKey("energy_windows")
        val FOCUS_SHIELD = booleanPreferencesKey("focus_guard")
        val VOICE_PERSONA = stringPreferencesKey("voice_persona")
        val SMART_SPACING = booleanPreferencesKey("dynamic_gap")
        val HAS_COMPLETED_ONBOARDING = booleanPreferencesKey("has_completed_onboarding")
        val LAST_SEEN_VERSION = intPreferencesKey("last_seen_version")
        val HAS_ACCEPTED_TERMS = booleanPreferencesKey("has_accepted_terms")
    }

    val schedulingPreferences: Flow<SchedulingPreferences> = dataStore.data.map { prefs ->
        SchedulingPreferences(
            workStart = prefs[WORK_START] ?: 7f,
            workEnd = prefs[WORK_END] ?: 22f,
            bufferMinutes = prefs[BUFFER_MINUTES] ?: 15,
            themeMode = ThemeMode.valueOf(prefs[THEME_MODE] ?: ThemeMode.SYSTEM.name),
            energyWindowsEnabled = prefs[ENERGY_WINDOWS] ?: false,
            focusShieldEnabled = prefs[FOCUS_SHIELD] ?: false,
            voicePersona = prefs[VOICE_PERSONA] ?: "Atlas",
            smartSpacingEnabled = prefs[SMART_SPACING] ?: false,
            hasCompletedOnboarding = prefs[HAS_COMPLETED_ONBOARDING] ?: false,
            lastSeenVersion = prefs[LAST_SEEN_VERSION] ?: 0,
            hasAcceptedTerms = prefs[HAS_ACCEPTED_TERMS] ?: false
        )
    }

    suspend fun updateTermsAcceptance(accepted: Boolean) {
        dataStore.edit { it[HAS_ACCEPTED_TERMS] = accepted }
    }


    suspend fun updateLastSeenVersion(version: Int) {
        dataStore.edit { it[LAST_SEEN_VERSION] = version }
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

    suspend fun updateFocusShield(enabled: Boolean) {
        dataStore.edit { it[FOCUS_SHIELD] = enabled }
    }

    suspend fun updateVoicePersona(persona: String) {
        dataStore.edit { it[VOICE_PERSONA] = persona }
    }

    suspend fun updateSmartSpacing(enabled: Boolean) {
        dataStore.edit { it[SMART_SPACING] = enabled }
    }

    suspend fun updateOnboardingStatus(completed: Boolean) {
        dataStore.edit { it[HAS_COMPLETED_ONBOARDING] = completed }
    }
}
