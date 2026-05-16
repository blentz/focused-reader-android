package com.focusedreader

import android.app.Application
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class FocusedReaderApp : Application() {
    override fun onCreate() {
        super.onCreate()
        CrashLogger.install(this)
    }
}
