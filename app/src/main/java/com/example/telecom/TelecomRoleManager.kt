package com.example.telecom

import android.app.Activity
import android.app.role.RoleManager
import android.content.Context
import android.content.Intent
import android.os.Build
import android.telecom.TelecomManager
import androidx.activity.result.ActivityResultLauncher

object TelecomRoleManager {

    /**
     * Checks if RealityEngine is currently the default dialer / phone app.
     * Uses Android's official RoleManager API on API 29+ and TelecomManager fallback on older versions.
     */
    fun isDefaultPhoneApp(context: Context): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val roleManager = context.getSystemService(RoleManager::class.java)
            roleManager?.isRoleHeld(RoleManager.ROLE_DIALER) == true
        } else {
            val telecomManager = context.getSystemService(Context.TELECOM_SERVICE) as? TelecomManager
            telecomManager?.defaultDialerPackage == context.packageName
        }
    }

    /**
     * Creates the official Intent to request the default dialer role from Android OS.
     */
    fun createDefaultDialerIntent(context: Context): Intent? {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val roleManager = context.getSystemService(RoleManager::class.java)
            if (roleManager != null && roleManager.isRoleAvailable(RoleManager.ROLE_DIALER)) {
                roleManager.createRequestRoleIntent(RoleManager.ROLE_DIALER)
            } else {
                null
            }
        } else {
            @Suppress("DEPRECATION")
            Intent(TelecomManager.ACTION_CHANGE_DEFAULT_DIALER).apply {
                putExtra(TelecomManager.EXTRA_CHANGE_DEFAULT_DIALER_PACKAGE_NAME, context.packageName)
            }
        }
    }

    /**
     * Launches the official role-selection request flow.
     */
    fun requestDefaultDialer(activity: Activity, launcher: ActivityResultLauncher<Intent>) {
        val intent = createDefaultDialerIntent(activity)
        if (intent != null) {
            launcher.launch(intent)
        }
    }
}
