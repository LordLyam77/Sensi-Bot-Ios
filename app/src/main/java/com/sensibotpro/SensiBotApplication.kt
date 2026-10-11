package com.sensibotpro

import android.app.Application
import android.util.Log

class SensiBotApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        
        // Global safety net: Catches any unexpected runtime crashes and logs them cleanly
        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            Log.e("SensiBotCrash", "FATAL CRASH on thread ${thread.name}", throwable)
            try {
                val crashFile = java.io.File(filesDir, "crash_log.txt")
                crashFile.writeText(Log.getStackTraceString(throwable))
            } catch (_: Throwable) {
            }
        }
    }
}
