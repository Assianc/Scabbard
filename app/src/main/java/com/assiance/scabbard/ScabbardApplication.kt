package com.assiance.scabbard

import android.app.Application
import com.assiance.scabbard.data.AppSettings

class ScabbardApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        AppSettings.init(this)
    }
}
