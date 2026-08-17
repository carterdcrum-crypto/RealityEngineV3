package com.example

import android.app.Application

class RealityEngineApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        // Install crash capture as early as possible, but never launch UI from Application.
        // The safe launcher owns startup diagnostics so Android can always reach a screen.
        StartupCrashReporter.install(this)
    }
}
