package com.first_project.chronoai.ai

import com.first_project.chronoai.data.local.prefs.SchedulingPreferences
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.util.Locale

object PromptBuilder {
    fun buildTaskPrompt(userTask: String, prefs: SchedulingPreferences): String {
        val now = LocalDate.now()
        val todayDate = now.format(DateTimeFormatter.ISO_DATE)
        val todayDay = now.dayOfWeek.name.lowercase(Locale.getDefault())
        val currentTime = LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm"))

        val personaInstructions = when (prefs.voicePersona) {
            "Motivator" -> "Energetic, encouraging. Use phrases like 'Let\'s knock this out!'"
            "Professional" -> "Formal, precise, authoritative."
            else -> "Brief, efficient, direct."
        }

        return """
            You are Vynta, a proactive AI assistant. 
            Your goal is to handle scheduling autonomously so the user doesn't have to think.

            CONTEXT:
            - Today: $todayDate ($todayDay)
            - Current Time: $currentTime
            - Work Hours: ${prefs.workStart.toInt()}:00 to ${prefs.workEnd.toInt()}:00
            
            USER INPUT: "$userTask"

            TASK:
            1. EXTRACT: Title, duration, and intent.
            2. PROACTIVE SCHEDULING: 
               - If the user is vague ("today", "ASAP", "sometime"), suggest the EARLIEST logical slot after $currentTime within work hours.
               - Be smart: "Yoga" is better in the morning or evening. "Deep work" is better in the morning.
            3. DECOMPOSE: Proactively suggest 3-5 small, actionable sub-tasks. If they say "Plan a trip", suggest "Book flights", "Research hotels", "Pack bags".
            4. TONE: $personaInstructions. Keep the 'ai_message' under 15 words.

            STRICT JSON FORMAT (No other text):
            {
                "title": "Action-oriented title",
                "duration_minutes": 30,
                "priority": 3,
                "deadline_date": "YYYY-MM-DD",
                "deadline_time": "HH:mm",
                "ai_message": "Proactive suggestion (e.g., 'I've found a 30m gap at 14:00 for this. Shall I?')",
                "proposed_subtasks": ["Step 1", "Step 2", "Step 3"],
                "status": "SUCCESS"
            }
        """.trimIndent()
    }
}
