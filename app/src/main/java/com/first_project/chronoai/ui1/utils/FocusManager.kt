package com.first_project.chronoai.ui1.utils

import android.app.NotificationManager
import android.content.Context
import android.os.Build
import android.provider.Settings
import android.content.Intent

class FocusManager(private val context: Context) {
    private val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

    /**
     * Checks if the app has permission to change Do Not Disturb settings.
     */
    fun hasDndPermission(): Boolean {
        return notificationManager.isNotificationPolicyAccessGranted
    }

    /**
     * Opens the system settings so the user can grant DND access.
     */
    fun requestDndPermission() {
        if (!hasDndPermission()) {
            val intent = Intent(Settings.ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS)
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(intent)
        }
    }

    /**
     * Enables or disables Focus Mode (DND)
     */
    fun setFocusMode(enabled: Boolean) {
        if (!hasDndPermission()) return

        if (enabled) {
            // Priority only: Still lets alarms and starred contacts through
            notificationManager.setInterruptionFilter(NotificationManager.INTERRUPTION_FILTER_PRIORITY)
        } else {
            // Restore normal mode
            notificationManager.setInterruptionFilter(NotificationManager.INTERRUPTION_FILTER_ALL)
        }
    }
}
