package com.rafiq.app.voice

import android.content.Context
import android.os.Bundle
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.speech.tts.Voice
import android.util.Log
import java.util.Locale

class TextToSpeechManager(
    context: Context,
    private val onReady: () -> Unit = {},
    private val onDone: (String) -> Unit = {},
    private val onError: (String) -> Unit = {}
) : TextToSpeech.OnInitListener {

    private var tts: TextToSpeech? = TextToSpeech(context.applicationContext, this)
    private var isReady = false

    override fun onInit(status: Int) {
        if (status != TextToSpeech.SUCCESS) {
            onError("فشل تهيئة محرك النطق")
            return
        }

        val engine = tts ?: return

        // ✅ تعيين اللغة العربية
        val result = engine.setLanguage(Locale("ar"))
        if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
            onError("اللغة العربية غير مدعومة على هذا الجهاز")
            return
        }

        // ✅ اختيار أفضل صوت عربي متاح (لجعل النطق بشرياً)
        try {
            val voices: Set<Voice>? = engine.voices
            if (!voices.isNullOrEmpty()) {
                val arabicVoices = voices.filter { it.locale.language == "ar" }
                if (arabicVoices.isNotEmpty()) {
                    // أولوية: صوت عالي الجودة لا يحتاج شبكة
                    val bestVoice = arabicVoices.firstOrNull {
                        !it.isNetworkConnectionRequired && it.quality >= Voice.QUALITY_HIGH
                    } ?: arabicVoices.firstOrNull { !it.isNetworkConnectionRequired }
                        ?: arabicVoices.firstOrNull { it.quality >= Voice.QUALITY_HIGH }
                        ?: arabicVoices.first()

                    engine.voice = bestVoice
                    Log.d("TTS", "الصوت المختار: ${bestVoice.name} (${bestVoice.locale})")
                }
            }
        } catch (e: Exception) {
            Log.w("TTS", "تعذر اختيار الصوت: ${e.message}")
        }

        // ✅ إعدادات النطق الطبيعي
        engine.setSpeechRate(0.95f)  // سرعة طبيعية
        engine.setPitch(1.0f)        // نبرة طبيعية

        engine.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
            override fun onStart(utteranceId: String?) {}
            override fun onDone(utteranceId: String?) {
                utteranceId?.let { onDone(it) }
            }
            @Deprecated("Deprecated in Java")
            override fun onError(utteranceId: String?) {
                utteranceId?.let { onError("خطأ في النطق: $it") }
            }
            override fun onError(utteranceId: String?, errorCode: Int) {
                utteranceId?.let { onError("خطأ في النطق ($errorCode): $it") }
            }
        })

        isReady = true
        onReady()
    }

    fun speak(text: String, utteranceId: String = "rafiq_${System.currentTimeMillis()}") {
        if (!isReady) {
            onError("محرك النطق غير جاهز")
            return
        }
        val params = Bundle()
        tts?.speak(text, TextToSpeech.QUEUE_FLUSH, params, utteranceId)
    }

    fun stop() = tts?.stop()
    fun isSpeaking(): Boolean = tts?.isSpeaking == true

    fun shutdown() {
        tts?.stop()
        tts?.shutdown()
        tts = null
        isReady = false
    }
}
