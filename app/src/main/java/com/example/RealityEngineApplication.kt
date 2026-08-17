package com.example

import android.app.Application
import android.content.Intent

class RealityEngineApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        StartupCrashReporter.install(this)

        val report = StartupCrashReporter.getLastReport(this)
        if (!report.isNullOrBlank()) {
            // Put diagnostics in front of MainActivity on the next launch.
            startActivity(
                Intent(this, StartupDiagnosticActivity::class.java).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
                }
            )
        }
    }
}
