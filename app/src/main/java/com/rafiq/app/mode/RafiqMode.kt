package com.rafiq.app.mode

import android.content.Context
import com.rafiq.app.prefs.RafiqPrefs

enum class RafiqMode(val id: String, val emoji: String) {
    NORMAL("normal", "🙂"),
    KID("kid", "🧒"),
    SENIOR("senior", "👴");

    companion object {
        fun fromId(id: String?): RafiqMode = entries.firstOrNull { it.id == id } ?: NORMAL
        fun current(context: Context): RafiqMode = fromId(RafiqPrefs.mode(context))
    }
}