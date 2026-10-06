package com.rafiq.app

import android.app.Application
import com.rafiq.app.morning.MorningScheduler
import com.rafiq.app.morning.MorningStore

class RafiqApp : Application() {
    override fun onCreate() {
        super.onCreate()
        instance = this
        if (MorningStore.isEnabled(this)) MorningScheduler.schedule(this)
    }

    companion object {
        lateinit var instance: RafiqApp
            private set
    }
}