package com.rafiq.app.morning

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import java.util.Calendar

object MorningScheduler {

    fun schedule(context: Context) {
        if (!MorningStore.isEnabled(context)) { cancel(context); return }
        val alarm = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

        val (hour, minute) = MorningStore.time(context)
        val triggerAt = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE, minute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
            if (timeInMillis <= System.currentTimeMillis()) add(Calendar.DAY_OF_YEAR, 1)
        }.timeInMillis

        val pi = pending(context)
        val canExact = Build.VERSION.SDK_INT < 31 || alarm.canScheduleExactAlarms()
        when {
            canExact && Build.VERSION.SDK_INT >= 23 ->
                alarm.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAt, pi)
            canExact ->
                @Suppress("DEPRECATION")
                alarm.setExact(AlarmManager.RTC_WAKEUP, triggerAt, pi)
            Build.VERSION.SDK_INT >= 23 ->
                alarm.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAt, pi)
            else ->
                @Suppress("DEPRECATION")
                alarm.set(AlarmManager.RTC_WAKEUP, triggerAt, pi)
        }
    }

    fun cancel(context: Context) {
        val alarm = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        alarm.cancel(pending(context))
    }

    private fun pending(context: Context): PendingIntent {
        val intent = Intent(context, MorningReceiver::class.java)
        val flags = PendingIntent.FLAG_UPDATE_CURRENT or
                (if (Build.VERSION.SDK_INT >= 23) PendingIntent.FLAG_IMMUTABLE else 0)
        return PendingIntent.getBroadcast(context, 1001, intent, flags)
    }
}