package com.rafiq.app.voice

import android.content.Context
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import java.util.Locale

class TextToSpeechManager(private val context: Context) {

    var onSpeakingChanged: ((Boolean) -> Unit)? = null
    var onLanguageMissing: ((String) -> Unit)? = null
    var onWord: (() -> Unit)? = null

    private var tts: TextToSpeech? = null
    private var ready = false
    private var enabled = true
    private var activeCount = 0

    private var rate = 1.0f
    private var pitch = 1.05f

    fun setProfile(speechRate: Float, speechPitch: Float) {
        rate = speechRate; pitch = speechPitch
    }

    private val progressListener = object : UtteranceProgressListener() {
        override fun onStart(utteranceId: String?) {}
        override fun onDone(utteranceId: String?) = utteranceFinished()
        @Deprecated("Deprecated in Java")
        override fun onError(utteranceId: String?) = utteranceFinished()
        override fun onError(utteranceId: String?, errorCode: Int) = utteranceFinished()
        override fun onRangeStart(utteranceId: String?, start: Int, end: Int, frame: Int) {
            onWord?.invoke()
        }
    }

    init {
        tts = TextToSpeech(context) { status ->
            ready = status == TextToSpeech.SUCCESS
            if (ready) tts?.setOnUtteranceProgressListener(progressListener)
        }
    }

    val isEnabled get() = enabled
    val isSpeaking: Boolean get() = activeCount > 0

    fun setEnabled(value: Boolean) {
        enabled = value
        if (!value) stop()
    }

    fun speak(text: String) {
        if (!ready || !enabled || text.isBlank()) return
        val clean = text.trim()
        val locale = localeFor(clean)

        val availability = tts?.isLanguageAvailable(locale) ?: TextToSpeech.LANG_NOT_SUPPORTED
        if (availability < TextToSpeech.LANG_AVAILABLE) {
            onLanguageMissing?.invoke(locale.language)
            return
        }
        tts?.language = locale
        tts?.setSpeechRate(rate)
        tts?.setPitch(pitch)

        activeCount++
        onSpeakingChanged?.invoke(true)
        tts?.speak(clean, TextToSpeech.QUEUE_ADD, null, "rafiq_${System.nanoTime()}")
    }

    fun stop() {
        activeCount = 0
        tts?.stop()
        onSpeakingChanged?.invoke(false)
    }

    fun shutdown() {
        tts?.stop()
        tts?.shutdown()
        tts = null
        ready = false
    }

    private fun utteranceFinished() {
        activeCount = (activeCount - 1).coerceAtLeast(0)
        if (activeCount == 0) onSpeakingChanged?.invoke(false)
    }

    companion object {
        fun localeFor(text: String): Locale {
            val t = text.take(150)
            return when {
                t.any { it.code in 0x0600..0x06FF } -> Locale("ar")
                t.any { Character.isLetter(it) && it.code in 0xC0..0x24F } -> Locale.FRENCH
                else -> Locale.getDefault()
            }
        }
    }
}