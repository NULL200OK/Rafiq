package com.rafiq.app.morning

import android.content.Context
import com.rafiq.app.R
import com.rafiq.app.brain.MemoryManager
import com.rafiq.app.data.local.RafiqDatabase
import com.rafiq.app.mode.RafiqMode
import kotlin.random.Random

object GreetingBuilder {

    data class Greeting(val title: String, val body: String)

    suspend fun build(context: Context): Greeting {
        val db = RafiqDatabase.getInstance(context)
        val name = MemoryManager.userName(db.userMemoryDao().recent(60))
        val kid = RafiqMode.current(context) == RafiqMode.KID
        val streak = MorningStore.displayStreak(context)

        val title = if (name.isBlank()) context.getString(R.string.morning_title)
                    else context.getString(R.string.morning_title_name, name)

        val lines = context.resources.getStringArray(
            if (kid) R.array.morning_lines_kid else R.array.morning_lines
        )
        val body = StringBuilder(lines[Random.nextInt(lines.size)])
        if (streak >= 3) body.append("\n").append(context.getString(R.string.morning_streak, streak))
        return Greeting(title, body.toString())
    }
}