package com.first_project.chronoai.voice

import android.content.Context
import android.content.Intent
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class SpeechRecognitionManager(private val context: Context) {
    private val speechRecognizer = SpeechRecognizer.createSpeechRecognizer(context)
    
    private val _spokenText = MutableStateFlow("")
    val spokenText: StateFlow<String> = _spokenText

    private val _isListening = MutableStateFlow(false)
    val isListening: StateFlow<Boolean> = _isListening

    private val _audioAmplitude = MutableStateFlow(0f)
    val audioAmplitude: StateFlow<Float> = _audioAmplitude

    fun startListening() {
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
        }

        speechRecognizer.setRecognitionListener(object : RecognitionListener {
            override fun onReadyForSpeech(params: android.os.Bundle?) { 
                _spokenText.value = "Listening..." 
                _isListening.value = true
                _audioAmplitude.value = 0f
            }
            override fun onBeginningOfSpeech() {}
            override fun onRmsChanged(rmsdB: Float) {
                // rmsdB usually ranges from -10 to 10
                _audioAmplitude.value = rmsdB
            }
            override fun onBufferReceived(buffer: ByteArray?) {}
            override fun onEndOfSpeech() { 
                _spokenText.value = "Processing..." 
                _isListening.value = false
                _audioAmplitude.value = 0f
            }
            override fun onError(error: Int) { 
                _spokenText.value = "Error: $error" 
                _isListening.value = false
                _audioAmplitude.value = 0f
            }

            override fun onResults(results: android.os.Bundle?) {
                val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                _spokenText.value = matches?.get(0) ?: "No text found"
                _isListening.value = false
                _audioAmplitude.value = 0f
            }

            override fun onPartialResults(partialResults: android.os.Bundle?) {}
            override fun onEvent(eventType: Int, params: android.os.Bundle?) {}
        })

        speechRecognizer.startListening(intent)
    }

    fun stopListening() {
        speechRecognizer.stopListening()
        _isListening.value = false
        _audioAmplitude.value = 0f
    }
}
