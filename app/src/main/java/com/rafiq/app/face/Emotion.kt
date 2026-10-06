package com.rafiq.app.face

import androidx.annotation.ColorInt

enum class Emotion(
    val tag: String,
    val emoji: String,
    @ColorInt val glow: Int,
    val mouthCurve: Float,
    val eyeScale: Float,
    val tilt: Float,
    val energy: Float
) {
    HAPPY("happy", "😊", 0xFFFFD54F.toInt(), 1.0f, 1.0f, 0f, 1.0f),
    EXCITED("excited", "🤩", 0xFFFF8A65.toInt(), 1.2f, 1.15f, 0f, 1.25f),
    SAD("sad", "😔", 0xFF90CAF9.toInt(), -1.0f, 0.85f, -3f, 0.55f),
    CARING("caring", "🥰", 0xFFF48FB1.toInt(), 0.6f, 0.95f, 0f, 0.85f),
    CURIOUS("curious", "🤔", 0xFF80CBC4.toInt(), 0.3f, 1.1f, 3f, 0.95f),
    SURPRISED("surprised", "😮", 0xFFCE93D8.toInt(), 0f, 1.35f, 0f, 1.15f),
    CALM("calm", "🙂", 0xFFA5D6A7.toInt(), 0.15f, 1.0f, 0f, 0.85f),
    THINKING("thinking", "💭", 0xFF90A4AE.toInt(), -0.2f, 0.95f, 4f, 0.7f),
    PROUD("proud", "😎", 0xFFFFB74D.toInt(), 0.8f, 0.95f, 0f, 1.0f);

    companion object {
        private val byTag = entries.associateBy { it.tag }
        fun fromTag(tag: String?): Emotion? = tag?.lowercase()?.trim()?.let { byTag[it] }
        val DEFAULT = CALM
    }
}