package com.first_project.chronoai.ai

import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

class GroqManager(private val apiKey: String) {

    private val apiService: GroqApiService by lazy {
        val logging = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY
        }
        val client = OkHttpClient.Builder()
            .addInterceptor(logging)
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .build()

        Retrofit.Builder()
            .baseUrl("https://api.groq.com/openai/")
            .addConverterFactory(GsonConverterFactory.create())
            .client(client)
            .build()
            .create(GroqApiService::class.java)
    }

    suspend fun analyzeTask(taskText: String, customPrompt: String? = null, useJsonMode: Boolean = false): String {
        // Robust Key Stripping: Remove whitespace AND surrounding quotes
        val rawKey = apiKey.trim()
        val trimmedKey = rawKey.removeSurrounding("\"").removeSurrounding("'").trim()
        
        if (trimmedKey.isEmpty()) {
            return "Error: Groq API Key is missing. Check local.properties."
        }

        // Debug Log (Masked)
        val maskedKey = if (trimmedKey.length > 8) "${trimmedKey.take(4)}...${trimmedKey.takeLast(4)}" else "***"
        android.util.Log.d("GroqManager", "Using API Key (len: ${trimmedKey.length}): $maskedKey")

        return try {
            val prompt = customPrompt ?: PromptBuilder.buildTaskPrompt(taskText, com.first_project.chronoai.data.local.prefs.SchedulingPreferences())
            val request = GroqRequest(
                model = "llama-3.1-8b-instant", // Switched to 8B for higher rate limits and speed
                messages = listOf(
                    Message(role = "user", content = prompt)
                ),
                responseFormat = if (useJsonMode) ResponseFormat("json_object") else null
            )
            val response = apiService.getCompletion("Bearer $trimmedKey", request)
            response.choices.firstOrNull()?.message?.content ?: "{}"
        } catch (e: retrofit2.HttpException) {
            val errorBody = e.response()?.errorBody()?.string()
            if (e.code() == 429) {
                return "Error: Neural Core is busy. Please wait a moment."
            }
            android.util.Log.e("GroqManager", "HTTP Error: $errorBody")
            "Error: ${e.code()} - $errorBody"
        } catch (e: Exception) {
            android.util.Log.e("GroqManager", "API request failed", e)
            "Error: ${e.localizedMessage ?: "Unknown error"}"
        }
    }

    suspend fun analyzeReflow(problem: String, currentSchedule: String, prefs: com.first_project.chronoai.data.local.prefs.SchedulingPreferences): String {
        val trimmedKey = apiKey.trim().removeSurrounding("\"").removeSurrounding("'").trim()
        if (trimmedKey.isEmpty()) return "Error: API Key missing"

        return try {
            val prompt = PromptBuilder.buildReflowPrompt(problem, currentSchedule, prefs)
            val request = GroqRequest(
                model = "llama-3.1-8b-instant", // Switched to 8B for higher rate limits and speed
                messages = listOf(Message(role = "user", content = prompt))
            )
            val response = apiService.getCompletion("Bearer $trimmedKey", request)
            response.choices.firstOrNull()?.message?.content ?: "[]"
        } catch (e: Exception) {
            android.util.Log.e("GroqManager", "Reflow API failed", e)
            "[]"
        }
    }
}
