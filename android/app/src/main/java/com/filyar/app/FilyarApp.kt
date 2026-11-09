package com.filyar.app

import android.app.Application
import com.filyar.app.core.ShizukuManager

class FilyarApp : Application() {
    override fun onCreate() {
        super.onCreate()
        ShizukuManager.init(this)
    }
}
