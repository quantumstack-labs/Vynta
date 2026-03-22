package com.first_project.chronoai.ai

import com.google.gson.annotations.SerializedName
import retrofit2.http.Body
import retrofit2.http.Header
import retrofit2.http.POST

data class GroqRequest(
    @SerializedName("model")
    val model: String,
    @SerializedName("messages")
    val messages: List<Message>,
    @SerializedName("temperature")
    val temperature: Double = 0.1,
    @SerializedName("stream")
    val stream: Boolean = false
)

data class Message(
    @SerializedName("role")
    val role: String,
    @SerializedName("content")
    val content: String
)

data class GroqResponse(
    @SerializedName("choices")
    val choices: List<Choice>
)

data class Choice(
    @SerializedName("message")
    val message: Message
)

interface GroqApiService {
    @POST("v1/chat/completions")
    suspend fun getCompletion(
        @Header("Authorization") apiKey: String,
        @Body request: GroqRequest
    ): GroqResponse
}
