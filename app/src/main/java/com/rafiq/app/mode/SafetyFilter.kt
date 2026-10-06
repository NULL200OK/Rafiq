package com.rafiq.app.mode

import com.rafiq.app.offline.OfflineEngine

object SafetyFilter {

    private val BLOCKED = setOf(
        "fuck", "fucking", "shit", "bitch", "bastard", "dick", "cunt",
        "crap", "asshole", "whore", "slut", "damn",
        "merde", "putain", "connard", "connasse", "salaud", "salope",
        "encule", "pute", "baiser", "nique",
        "خرا", "زق", "لعنه", "شرموط", "عاهر", "قذر", "وسخ"
    )

    fun isDirty(text: String): Boolean {
        val words = OfflineEngine.normalize(text).split(" ").filter { it.isNotBlank() }
        return words.any { it in BLOCKED }
    }
}