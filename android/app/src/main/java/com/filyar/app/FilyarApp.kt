package com.filyar.app

import android.app.Application
import com.filyar.app.core.ShizukuManager

class FilyarApp : Application() {
    override fun onCreate() {
        super.onCreate()
        // Remember the last crash, so Settings can show why the app closed.
        val previous = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, error ->
            runCatching {
                java.io.File(filesDir, "last_crash.txt")
                    .writeText("${java.util.Date()}\nthread: ${thread.name}\n${android.util.Log.getStackTraceString(error)}")
            }
            previous?.uncaughtException(thread, error)
        }
        ShizukuManager.init(this)
    }
}
