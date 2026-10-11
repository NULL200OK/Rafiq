package com.rafiq.app

import android.app.Application
import android.os.Build
import android.os.Process
import com.rafiq.app.morning.MorningScheduler
import com.rafiq.app.morning.MorningStore
import java.io.File
import java.io.PrintWriter
import java.io.StringWriter
import java.util.Date

class RafiqApp : Application() {
    override fun onCreate() {
        super.onCreate()
        instance = this

        // ✅ صيّاد الأخطاء: يحفظ تقرير أي انهيار في ملف داخل التطبيق
        val defaultHandler = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            try {
                val sw = StringWriter()
                throwable.printStackTrace(PrintWriter(sw))
                val log = buildString {
                    append("=== Rafiq Crash Report ===\n")
                    append("Time: ").append(Date()).append("\n")
                    append("Android: ").append(Build.VERSION.SDK_INT)
                        .append(" (").append(Build.VERSION.RELEASE).append(")\n")
                    append("Device: ").append(Build.MANUFACTURER)
                        .append(" ").append(Build.MODEL).append("\n")
                    append("App: ").append(packageName).append("\n\n")
                    append(sw.toString())
                }
                File(filesDir, "rafiq_crash.txt").writeText(log)
            } catch (_: Exception) { }

            // استدعاء المعالج الأصلي ثم إنهاء العملية
            try {
                defaultHandler?.uncaughtException(thread, throwable)
            } catch (_: Exception) {
                Process.killProcess(Process.myPid())
                kotlin.system.exitProcess(10)
            }
        }

        if (MorningStore.isEnabled(this)) MorningScheduler.schedule(this)
    }

    companion object {
        lateinit var instance: RafiqApp
            private set
    }
}
