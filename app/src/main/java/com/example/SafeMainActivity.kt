package com.example

import android.app.Activity
import android.os.Bundle
import android.widget.TextView

/** Minimal Android-only bootstrap used to isolate pre-UI startup failures. */
class SafeMainActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(TextView(this).apply {
            text = "REALITY ENGINE\n\nBOOTSTRAP OK"
            textSize = 20f
            setPadding(48, 96, 48, 48)
        })
    }
}
