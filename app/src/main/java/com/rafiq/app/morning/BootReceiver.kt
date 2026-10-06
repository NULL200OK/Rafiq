package com.rafiq.app.morning

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action in setOf(
                Intent.ACTION_BOOT_COMPLETED,
                "android.intent.action.TIMEZONE_CHANGED"
            ) && MorningStore.isEnabled(context)
        ) {
            MorningScheduler.schedule(context)
        }
    }
}