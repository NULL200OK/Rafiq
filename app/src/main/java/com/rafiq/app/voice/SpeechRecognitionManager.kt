package com.rafiq.app.voice

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import java.util.Locale

class SpeechRecognitionManager(private val context: Context) {

    var onPartialResult: ((String) -> Unit)? = null
    var onFinalResult: ((String) -> Unit)? = null
    var onStateChange: ((Boolean) -> Unit)? = null
    var onError: ((Int) -> Unit)? = null

    private var recognizer: SpeechRecognizer? = null
    var isListening = false
        private set

    val isAvailable: Boolean get() = SpeechRecognizer.isRecognitionAvailable(context)

    fun startListening(language: String? = null) {
        if (isListening) return
        if (!isAvailable) { onError?.invoke(ERR_UNAVAILABLE); return }

        recognizer?.destroy()
        recognizer = SpeechRecognizer.createSpeechRecognizer(context).apply {
            setRecognitionListener(listener)
            startListening(buildIntent(language))
        }
        isListening = true
        onStateChange?.invoke(true)
    }

    fun cancel() {
        runCatching { recognizer?.destroy() }
        recognizer = null
        if (isListening) {
            isListening = false
            onStateChange?.invoke(false)
        }
    }

    private fun buildIntent(language: String?) = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
        putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
        putExtra(RecognizerIntent.EXTRA_LANGUAGE, language ?: Locale.getDefault().toString())
        putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
        putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1)
    }

    private val listener = object : RecognitionListener {
        override fun onReadyForSpeech(params: Bundle?) {}
        override fun onBeginningOfSpeech() {}
        override fun onRmsChanged(rmsdB: Float) {}
        override fun onBufferReceived(buffer: ByteArray?) {}
        override fun onEndOfSpeech() {}
        override fun onError(error: Int) {
            isListening = false
            onStateChange?.invoke(false)
            onError?.invoke(error)
        }
        override fun onResults(results: Bundle?) {
            isListening = false
            onStateChange?.invoke(false)
            val text = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)?.firstOrNull()
            if (!text.isNullOrBlank()) onFinalResult?.invoke(text)
        }
        override fun onPartialResults(partialResults: Bundle?) {
            val text = partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)?.firstOrNull()
            if (!text.isNullOrBlank()) onPartialResult?.invoke(text)
        }
        override fun onEvent(eventType: Int, params: Bundle?) {}
    }

    companion object { const val ERR_UNAVAILABLE = -99 }
}