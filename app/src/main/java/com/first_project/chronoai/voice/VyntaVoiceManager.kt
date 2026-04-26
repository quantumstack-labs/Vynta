package com.first_project.chronoai.voice

import android.content.Context
import android.speech.tts.TextToSpeech
import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import java.util.*

class VyntaVoiceManager(context: Context) : TextToSpeech.OnInitListener {
    private var tts: TextToSpeech? = TextToSpeech(context, this)
    
    private val _isSpeaking = MutableStateFlow(false)
    val isSpeaking: StateFlow<Boolean> = _isSpeaking

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            val result = tts?.setLanguage(Locale.US)
            if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                Log.e("VyntaVoice", "Language not supported")
            }
            
            tts?.setOnUtteranceProgressListener(object : android.speech.tts.UtteranceProgressListener() {
                override fun onStart(utteranceId: String?) {
                    _isSpeaking.value = true
                }

                override fun onDone(utteranceId: String?) {
                    _isSpeaking.value = false
                }

                @Deprecated("Deprecated in Java")
                override fun onError(utteranceId: String?) {
                    _isSpeaking.value = false
                }
            })
        }
    }

    fun speak(text: String, persona: String = "Atlas") {
        val params = android.os.Bundle()
        params.putString(TextToSpeech.Engine.KEY_PARAM_UTTERANCE_ID, "VyntaUtteranceId")
        
        // Adjust voice parameters based on persona
        when (persona) {
            "Atlas" -> {
                tts?.setPitch(0.8f) // Deeper
                tts?.setSpeechRate(0.9f) // Slower/Steady
            }
            "Lyra" -> {
                tts?.setPitch(1.2f) // Higher/Lighter
                tts?.setSpeechRate(1.1f) // Faster/Energetic
            }
            "Sloane" -> {
                tts?.setPitch(1.0f) // Neutral
                tts?.setSpeechRate(1.0f) // Standard
            }
            "Orion" -> {
                tts?.setPitch(0.7f) // Very Deep
                tts?.setSpeechRate(0.8f) // Very Slow/Wise
            }
        }

        tts?.speak(text, TextToSpeech.QUEUE_FLUSH, params, "VyntaUtteranceId")
    }

    fun stop() {
        tts?.stop()
        _isSpeaking.value = false
    }

    fun shutdown() {
        tts?.shutdown()
    }
}
