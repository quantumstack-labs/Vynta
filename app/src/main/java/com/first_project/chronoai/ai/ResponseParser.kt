package com.first_project.chronoai.ai

import androidx.compose.runtime.Immutable
import org.json.JSONObject
import java.util.regex.Pattern

@Immutable
data class TaskModel(
    val title: String,
    val durationMinutes: Int,
    val priority: Int,
    val isRecurring: Boolean,
    val recurrencePattern: String?,
    val energyLevel: String,
    val bestTime: String,
    val hasDeadline: Boolean,
    val deadlineDate: String?,
    val deadlineTime: String?,
    val thoughtProcess: String? = null,
    val confidenceScore: Float? = null,
    val status: String = "SUCCESS",
    val aiMessage: String? = null,
    val proposedSubtasks: List<String> = emptyList()
)

object ResponseParser {
    fun parse(jsonText: String, rawInput: String? = null): TaskModel {
        if (jsonText.startsWith("Error:")) {
            throw Exception(jsonText)
        }

        val cleanJson = extractJson(jsonText)
        val obj = JSONObject(cleanJson)

        fun getOptString(key: String): String? {
            val s = obj.optString(key, null)
            return if (s == null || s == "null" || s.isBlank()) null else s
        }

        val subtasks = mutableListOf<String>()
        val subtasksJson = obj.optJSONArray("proposed_subtasks")
        if (subtasksJson != null) {
            for (i in 0 until subtasksJson.length()) {
                subtasks.add(subtasksJson.getString(i))
            }
        }

        var model = TaskModel(
            title = obj.optString("title", "New Task"),
            durationMinutes = obj.optInt("duration_minutes", 60),
            priority = obj.optInt("priority", 3),
            isRecurring = obj.optBoolean("is_recurring", false),
            recurrencePattern = getOptString("recurrence_pattern"),
            energyLevel = obj.optString("energy_level", "Medium"),
            bestTime = obj.optString("best_time", "Morning"),
            hasDeadline = obj.optBoolean("has_deadline", false),
            deadlineDate = getOptString("deadline_date"),
            deadlineTime = getOptString("deadline_time"),
            thoughtProcess = obj.optString("thought_process", null),
            confidenceScore = if (obj.has("confidence_score")) obj.optDouble("confidence_score").toFloat() else null,
            status = obj.optString("status", "SUCCESS"),
            aiMessage = cleanSuggestion(getOptString("ai_message") ?: ""),
            proposedSubtasks = subtasks
        )

        if (rawInput != null) {
            model = applyStructuredOverrides(model, rawInput)
        }
        
        return model
    }

    fun extractJson(text: String): String {
        val startIndex = text.indexOf('{')
        val endIndex = text.lastIndexOf('}')
        if (startIndex == -1 || endIndex == -1 || startIndex >= endIndex) return text
        return text.substring(startIndex, endIndex + 1)
    }

    /**
     * Aggressively cleans AI output to show only the user-facing message.
     */
    fun cleanSuggestion(text: String): String {
        if (text.isBlank()) return ""
        
        // 1. Try to parse as JSON if it contains {}
        try {
            val jsonStr = extractJson(text)
            if (jsonStr.contains("{")) {
                val obj = JSONObject(jsonStr)
                val candidate = obj.optString("suggestion") ?: obj.optString("ai_message") ?: obj.optString("best_time")
                if (!candidate.isNullOrBlank() && candidate != "null") return candidate
            }
        } catch (e: Exception) {}

        // 2. Remove Thought Process markers and everything inside them
        var cleaned = text.replace(Regex("(?s)thought\\s*process:.*?(?=\\n\\n|\\{|$)", RegexOption.IGNORE_CASE), "")
        
        // 3. Remove any JSON blocks
        cleaned = cleaned.replace(Regex("(?s)\\{.*?\\}"), "")
        
        // 4. Remove conversational prefix if it's too long and looks like reasoning
        if (cleaned.contains("Given the user", ignoreCase = true) || cleaned.contains("Based on", ignoreCase = true)) {
            val sentences = cleaned.split(Regex("(?<=[.!?])\\s+"))
            // Usually the last sentence or the one with a time is the actual suggestion
            val timePattern = Pattern.compile("\\d{1,2}:\\d{2}")
            val suggestion = sentences.lastOrNull { timePattern.matcher(it).find() } ?: sentences.lastOrNull()
            if (suggestion != null) return suggestion.trim()
        }

        return cleaned.trim().ifBlank { "I've found a spot in your schedule." }
    }

    private fun applyStructuredOverrides(model: TaskModel, input: String): TaskModel {
        var updated = model
        val dateRegex = Pattern.compile("@Date:\\s*([^|@]+)")
        val timeRegex = Pattern.compile("@Time:\\s*([^|@]+)")
        val priorityRegex = Pattern.compile("@Priority:\\s*([^|@]+)")
        val durationRegex = Pattern.compile("@Duration:\\s*([^|@]+)")

        dateRegex.matcher(input).takeIf { it.find() }?.let { updated = updated.copy(deadlineDate = it.group(1).trim(), hasDeadline = true) }
        timeRegex.matcher(input).takeIf { it.find() }?.let { updated = updated.copy(deadlineTime = it.group(1).trim()) }
        
        priorityRegex.matcher(input).takeIf { it.find() }?.let {
            val pStr = it.group(1).trim().lowercase()
            val pInt = when {
                pStr.contains("high") -> 5
                pStr.contains("medium") -> 3
                pStr.contains("low") -> 1
                else -> pStr.toIntOrNull() ?: updated.priority
            }
            updated = updated.copy(priority = pInt)
        }

        durationRegex.matcher(input).takeIf { it.find() }?.let {
            val dInt = it.group(1).trim().filter { c -> c.isDigit() }.toIntOrNull() ?: updated.durationMinutes
            updated = updated.copy(durationMinutes = dInt)
        }

        return updated
    }
}
