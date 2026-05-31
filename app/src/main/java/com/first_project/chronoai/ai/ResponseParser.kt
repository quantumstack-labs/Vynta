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
    val schedulingReason: String? = null,
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
            schedulingReason = getOptString("scheduling_reason"),
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
        val durationOverrideRegex = Pattern.compile("@Duration:\\s*([^|@]+)")

        // 1. Explicit Overrides (@Key: Value)
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

        durationOverrideRegex.matcher(input).takeIf { it.find() }?.let {
            val dInt = it.group(1).trim().filter { c -> c.isDigit() }.toIntOrNull() ?: updated.durationMinutes
            updated = updated.copy(durationMinutes = dInt)
        }

        // 2. Natural Language Fallbacks
        // Match "X hours", "X h", "X mins", "X minutes"
        val hourPattern = Pattern.compile("(\\d+)\\s*(hours?|h)\\b", Pattern.CASE_INSENSITIVE)
        val minPattern = Pattern.compile("(\\d+)\\s*(minutes?|mins?|m)\\b", Pattern.CASE_INSENSITIVE)
        
        val hMatcher = hourPattern.matcher(input)
        if (hMatcher.find()) {
            updated = updated.copy(durationMinutes = hMatcher.group(1).toInt() * 60)
        } else {
            val mMatcher = minPattern.matcher(input)
            if (mMatcher.find()) {
                updated = updated.copy(durationMinutes = mMatcher.group(1).toInt())
            }
        }

        // 3. Time Range Fallback (from X to Y)
        val rangePattern = Pattern.compile("from\\s+(\\d{1,2}(?::\\d{2})?\\s*(?:am|pm)?)\\s+to\\s+(\\d{1,2}(?::\\d{2})?\\s*(?:am|pm)?)", Pattern.CASE_INSENSITIVE)
        val rMatcher = rangePattern.matcher(input)
        if (rMatcher.find()) {
            val startTimeStr = rMatcher.group(1)
            val endTimeStr = rMatcher.group(2)
            
            if (startTimeStr != null && endTimeStr != null) {
                val startMinutes = parseTimeToMinutes(startTimeStr)
                val endMinutes = parseTimeToMinutes(endTimeStr)
                
                if (startMinutes != null && endMinutes != null) {
                    var diff = endMinutes - startMinutes
                    if (diff < 0) diff += 24 * 60 // Handle overnight range if applicable
                    
                    if (diff > 0) {
                        updated = updated.copy(
                            deadlineTime = java.util.Locale.US.let { String.format(it, "%02d:%02d", startMinutes / 60, startMinutes % 60) },
                            durationMinutes = diff
                        )
                    }
                }
            }
        }

        return updated
    }

    private fun parseTimeToMinutes(timeStr: String): Int? {
        try {
            val clean = timeStr.lowercase().trim()
            val isPm = clean.contains("pm")
            val isAm = clean.contains("am")
            val digits = clean.replace(Regex("[^0-9:]"), "")
            
            val parts = digits.split(":")
            var hour = parts[0].toInt()
            val min = if (parts.size > 1) parts[1].toInt() else 0
            
            if (isPm && hour < 12) hour += 12
            if (isAm && hour == 12) hour = 0
            
            return hour * 60 + min
        } catch (e: Exception) {
            return null
        }
    }
}
