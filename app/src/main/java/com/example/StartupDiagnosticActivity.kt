package com.example

import android.app.Activity
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.os.Bundle
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.graphics.Color
import android.view.Gravity
import android.view.ViewGroup

class StartupDiagnosticActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val report = StartupCrashReporter.getLastReport(this).orEmpty()
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(32, 48, 32, 32)
            setBackgroundColor(Color.rgb(10, 12, 16))
        }

        val title = TextView(this).apply {
            text = "REALITY ENGINE\nSTARTUP FAILURE"
            setTextColor(Color.rgb(255, 180, 60))
            textSize = 24f
            gravity = Gravity.CENTER
        }
        root.addView(title, LinearLayout.LayoutParams(-1, ViewGroup.LayoutParams.WRAP_CONTENT))

        val body = TextView(this).apply {
            text = report.ifBlank { "No diagnostic report was saved." }
            setTextColor(Color.WHITE)
            textSize = 13f
            setPadding(0, 32, 0, 24)
            setTextIsSelectable(true)
        }
        root.addView(body, LinearLayout.LayoutParams(-1, 0, 1f))

        val copy = Button(this).apply {
            text = "COPY REPORT"
            setOnClickListener {
                val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                clipboard.setPrimaryClip(ClipData.newPlainText("Reality Engine crash report", report))
            }
        }
        root.addView(copy, LinearLayout.LayoutParams(-1, ViewGroup.LayoutParams.WRAP_CONTENT))

        val dismiss = Button(this).apply {
            text = "CLEAR & TRY AGAIN"
            setOnClickListener {
                StartupCrashReporter.clear(this@StartupDiagnosticActivity)
                finish()
            }
        }
        root.addView(dismiss, LinearLayout.LayoutParams(-1, ViewGroup.LayoutParams.WRAP_CONTENT))

        setContentView(ScrollView(this).apply { addView(root) })
    }
}
