package com.rafiq.app.morning

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.rafiq.app.MainActivity
import com.rafiq.app.R
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class MorningReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val pending = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val greeting = GreetingBuilder.build(context)
                show(context, greeting)
            } catch (e: Exception) {
            } finally {
                runCatching { MorningScheduler.schedule(context) }
                pending.finish()
            }
        }
    }

    private fun show(context: Context, greeting: GreetingBuilder.Greeting) {
        val nm = NotificationManagerCompat.from(context)
        if (!nm.areNotificationsEnabled()) return

        if (Build.VERSION.SDK_INT >= 26) {
            nm.createNotificationChannel(
                NotificationChannel(
                    CHANNEL, context.getString(R.string.notif_channel),
                    NotificationManager.IMPORTANCE_HIGH
                )
            )
        }

        val open = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val piFlags = PendingIntent.FLAG_UPDATE_CURRENT or
                (if (Build.VERSION.SDK_INT >= 23) PendingIntent.FLAG_IMMUTABLE else 0)
        val contentPi = PendingIntent.getActivity(context, 2002, open, piFlags)

        val notification = NotificationCompat.Builder(context, CHANNEL)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(greeting.title)
            .setContentText(greeting.body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(greeting.body))
            .setAutoCancel(true)
            .setContentIntent(contentPi)
            .build()

        runCatching { nm.notify(3003, notification) }
    }

    companion object { const val CHANNEL = "rafiq_morning" }
}