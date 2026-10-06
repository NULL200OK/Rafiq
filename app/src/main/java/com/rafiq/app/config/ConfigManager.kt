package com.rafiq.app.config

import android.content.Context

object ConfigManager {

    private const val PREFS = "rafiq_secure_config"
    private const val K_API = "k_api"
    private const val K_URL = "k_url"
    private const val K_MODEL = "k_model"

    const val DEFAULT_BASE_URL = "https://api.z.ai/api/coding/paas/v4"
    const val DEFAULT_MODEL = "glm-5.3-flash"

    fun save(context: Context, apiKey: String, baseUrl: String, model: String) {
        val encrypted = CryptoManager.encrypt(context, apiKey) ?: return
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit()
            .putString(K_API, encrypted)
            .putString(K_URL, baseUrl)
            .putString(K_MODEL, model)
            .apply()
    }

    fun load(context: Context): LlmConfig? {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val stored = prefs.getString(K_API, null) ?: return null
        val key = CryptoManager.decrypt(context, stored) ?: return null
        return LlmConfig(
            apiKey = key,
            baseUrl = prefs.getString(K_URL, null) ?: DEFAULT_BASE_URL,
            model = prefs.getString(K_MODEL, null) ?: DEFAULT_MODEL
        )
    }

    fun isConfigured(context: Context): Boolean = load(context) != null

    fun clear(context: Context) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().clear().apply()
    }
}