package com.rafiq.app.morning

import android.content.Context

object MorningStore {

    private const val FILE = "rafiq_morning"
    private const val K_ENABLED = "enabled"
    private const val K_HOUR = "hour"
    private const val K_MINUTE = "minute"
    private const val K_STREAK = "streak"
    private const val K_LAST_DAY = "last_day"
    private const val DAY_MS = 86_400_000L

    private fun prefs(context: Context) =
        context.getSharedPreferences(FILE, Context.MODE_PRIVATE)

    fun isEnabled(context: Context) = prefs(context).getBoolean(K_ENABLED, false)

    fun setEnabled(context: Context, value: Boolean) =
        prefs(context).edit().putBoolean(K_ENABLED, value).apply()

    fun time(context: Context): Pair<Int, Int> {
        val p = prefs(context)
        return p.getInt(K_HOUR, 8) to p.getInt(K_MINUTE, 0)
    }

    fun setTime(context: Context, hour: Int, minute: Int) =
        prefs(context).edit().putInt(K_HOUR, hour).putInt(K_MINUTE, minute).apply()

    fun checkIn(context: Context): Int {
        val p = prefs(context)
        val today = System.currentTimeMillis() / DAY_MS
        val last = p.getLong(K_LAST_DAY, -1L)
        if (last == today) return p.getInt(K_STREAK, 1)
        val streak = if (last == today - 1) p.getInt(K_STREAK, 0) + 1 else 1
        p.edit().putLong(K_LAST_DAY, today).putInt(K_STREAK, streak).apply()
        return streak
    }

    fun displayStreak(context: Context): Int {
        val p = prefs(context)
        val today = System.currentTimeMillis() / DAY_MS
        val last = p.getLong(K_LAST_DAY, -1L)
        val streak = p.getInt(K_STREAK, 0)
        return if (last >= today - 1 && streak >= 3) streak else 0
    }
}