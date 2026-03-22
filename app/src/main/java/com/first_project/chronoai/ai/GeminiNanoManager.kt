package com.first_project.chronoai.ai

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Deprecated: We are switching to Groq for AI processing.
 * This class is kept for reference but is no longer used.
 */
class GeminiNanoManager(private val context: Context) {

    suspend fun analyzeTask(taskText: String): String = withContext(Dispatchers.IO) {
        return@withContext "Gemini Nano is deprecated. Use GroqManager instead."
    }

    fun isAvailable(): Boolean {
        return false
    }
}
