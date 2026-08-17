package com.example

import android.app.Activity
import android.os.Bundle
import android.widget.TextView
import androidx.lifecycle.ViewModelProvider
import com.example.ui.RealityEngineViewModel

/** Minimal launcher used to keep startup failures visible on a phone without ADB. */
class SafeMainActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        StartupCrashReporter.install(this)
        super.onCreate(savedInstanceState)
        showBootScreen()

        try {
            val vm = ViewModelProvider(this)[RealityEngineViewModel::class.java]
            setContentView(TextView(this).apply {
                text = "Reality Engine initialized.\n\nNext: launch the full interface."
                textSize = 18f
                setPadding(48, 96, 48, 48)
                setOnClickListener {
                    startActivity(android.content.Intent(this@SafeMainActivity, MainActivity::class.java))
                }
            })
        } catch (t: Throwable) {
            val report = "Reality Engine startup failure\n\n" + t.stackTraceToString().take(12000)
            StartupCrashReporter.saveReport(this, report)
            setContentView(TextView(this).apply {
                text = "REALITY ENGINE\n\nSTARTUP FAILURE\n\n$report"
                textSize = 13f
                setPadding(32, 64, 32, 32)
            })
        }
    }

    private fun showBootScreen() {
        setContentView(TextView(this).apply {
            text = "REALITY ENGINE\n\nBOOTING..."
            textSize = 20f
            setPadding(48, 96, 48, 48)
        })
    }
}
