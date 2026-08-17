package com.example

import android.content.Context
import android.os.Build
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/** Captures startup/runtime crashes locally so a phone-only developer can diagnose them. */
object StartupCrashReporter {
    private const val PREFS = "reality_engine_diagnostics"
    private const val KEY_REPORT = "last_crash_report"

    fun install(context: Context) {
        val appContext = context.applicationContext
        val previous = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            try {
                val report = buildString {
                    append("Reality Engine crash report\n")
                    append("Time: ")
                    append(SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US).format(Date()))
                    append('\n')
                    append("Android: ").append(Build.VERSION.RELEASE).append(" (API ")
                        .append(Build.VERSION.SDK_INT).append(")\n")
                    append("Device: ").append(Build.MANUFACTURER).append(' ').append(Build.MODEL).append('\n')
                    append("Thread: ").append(thread.name).append('\n\n")
                    append(throwable.stackTraceToString().take(12000))
                }
                appContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
                    .edit().putString(KEY_REPORT, report).apply()
            } catch (_: Throwable) {
                // Never interfere with Android's normal crash handling.
            } finally {
                previous?.uncaughtException(thread, throwable)
            }
        }
    }

    fun getLastReport(context: Context): String? =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getString(KEY_REPORT, null)

    fun clear(context: Context) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit().remove(KEY_REPORT).apply()
    }
}
