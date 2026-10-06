package com.rafiq.app.prefs

import android.content.Context

object RafiqPrefs {

    private const val FILE = "rafiq_prefs"
    private const val K_SPEAK = "speak_enabled"
    private const val K_HINT = "long_press_hint_shown"
    private const val K_MODE = "mode"
    private const val K_TRAINER = "trainer_lang"
    private const val K_WELCOME = "welcome_shown"

    private fun prefs(context: Context) =
        context.getSharedPreferences(FILE, Context.MODE_PRIVATE)

    fun speakEnabled(context: Context): Boolean = prefs(context).getBoolean(K_SPEAK, true)
    fun setSpeakEnabled(context: Context, value: Boolean) =
        prefs(context).edit().putBoolean(K_SPEAK, value).apply()

    fun hintShown(context: Context): Boolean = prefs(context).getBoolean(K_HINT, false)
    fun setHintShown(context: Context) =
        prefs(context).edit().putBoolean(K_HINT, true).apply()

    fun mode(context: Context): String = prefs(context).getString(K_MODE, "normal") ?: "normal"
    fun setMode(context: Context, value: String) =
        prefs(context).edit().putString(K_MODE, value).apply()

    fun trainerLang(context: Context): String = prefs(context).getString(K_TRAINER, "") ?: ""
    fun setTrainerLang(context: Context, value: String) =
        prefs(context).edit().putString(K_TRAINER, value).apply()

    fun welcomeShown(context: Context): Boolean = prefs(context).getBoolean(K_WELCOME, false)
    fun setWelcomeShown(context: Context) =
        prefs(context).edit().putBoolean(K_WELCOME, true).apply()
}