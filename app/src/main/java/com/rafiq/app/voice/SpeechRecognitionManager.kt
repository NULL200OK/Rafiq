package com.rafiq.app.voice

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.util.Log

class SpeechRecognitionManager(
    private val context: Context,
    private val onResult: (String) -> Unit,
    private val onError: (String) -> Unit,
    private val onReady: () -> Unit = {},
    private val onEnd: () -> Unit = {}
) {
    private var speechRecognizer: SpeechRecognizer? = null
    private var isListening = false

    init {
        if (SpeechRecognizer.isRecognitionAvailable(context)) {
            speechRecognizer = SpeechRecognizer.createSpeechRecognizer(context).apply {
                setRecognitionListener(recognitionListener)
            }
        } else {
            onError("خدمة التعرف على الكلام غير متوفرة على الجهاز")
        }
    }

    private val recognitionListener = object : RecognitionListener {
        override fun onReadyForSpeech(params: Bundle?) {
            isListening = true
            onReady()
        }
        override fun onBeginningOfSpeech() {}
        override fun onRmsChanged(rmsdB: Float) {}
        override fun onBufferReceived(buffer: ByteArray?) {}
        override fun onEndOfSpeech() {
            isListening = false
            onEnd()
        }
        override fun onError(error: Int) {
            isListening = false
            val message = when (error) {
                SpeechRecognizer.ERROR_AUDIO -> "خطأ في الصوت"
                SpeechRecognizer.ERROR_CLIENT -> "خطأ في العميل"
                SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> "لا توجد صلاحية الميكروفون"
                SpeechRecognizer.ERROR_NETWORK -> "خطأ في الشبكة"
                SpeechRecognizer.ERROR_NETWORK_TIMEOUT -> "انتهت مهلة الشبكة"
                SpeechRecognizer.ERROR_NO_MATCH -> "لم يتم التعرف على الكلام"
                SpeechRecognizer.ERROR_RECOGNIZER_BUSY -> "المحرك مشغول"
                SpeechRecognizer.ERROR_SERVER -> "خطأ في الخادم"
                SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> "لم يتم اكتشاف كلام"
                else -> "خطأ غير معروف: $error"
            }
            Log.e("SpeechRecognition", message)
            onError(message)
        }
        override fun onResults(results: Bundle?) {
            isListening = false
            val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
            val text = matches?.firstOrNull()?.trim().orEmpty()
            if (text.isNotEmpty()) onResult(text)
            else onError("لم يتم التعرف على الكلام")
        }
        override fun onPartialResults(partialResults: Bundle?) {}
        override fun onEvent(eventType: Int, params: Bundle?) {}
    }

    fun startListening() {
        val recognizer = speechRecognizer ?: run {
            onError("خدمة التعرف على الكلام غير متوفرة")
            return
        }
        if (isListening) return

        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
            putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 5)
            putExtra(RecognizerIntent.EXTRA_CALLING_PACKAGE, context.packageName)

            // ✅ اللغة الأساسية: العربية
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, "ar-SA")
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE, "ar-SA")

            // ✅ قائمة اللهجات العربية للتبديل التلقائي (يتكلم بأي لهجة)
            val additionalLanguages = arrayOf(
                "ar-SA", "ar-EG", "ar-AE", "ar-JO", "ar-LB",
                "ar-MA", "ar-DZ", "ar-TN", "ar-IQ", "ar-SY",
                "ar-KW", "ar-QA", "ar-BH", "ar-OM", "ar-YE",
                "ar-LY", "ar-SD", "ar-PS"
            )
            putExtra("android.speech.extra.EXTRA_ADDITIONAL_LANGUAGES", additionalLanguages)
        }

        recognizer.startListening(intent)
    }

    fun stopListening() {
        speechRecognizer?.stopListening()
        isListening = false
    }

    fun cancel() {
        speechRecognizer?.cancel()
        isListening = false
    }

    fun destroy() {
        speechRecognizer?.destroy()
        speechRecognizer = null
        isListening = false
    }
}
