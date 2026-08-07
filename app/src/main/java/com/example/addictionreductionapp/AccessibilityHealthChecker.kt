package com.example.addictionreductionapp

import android.content.Context
import android.provider.Settings
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import java.util.concurrent.TimeUnit

/**
 * Utility to check if the accessibility service is still enabled and alert the user if not.
 * Used by BootReceiver, onUnbind, and periodic health check worker.
 */
object AccessibilityHealthChecker {

    private const val TAG = "AccessibilityHealth"

    fun isEnabled(context: Context): Boolean {
        return try {
            val enabledServices = Settings.Secure.getString(
                context.contentResolver,
                Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
            ) ?: return false
            enabledServices.contains(context.packageName)
        } catch (e: Exception) {
            android.util.Log.e(TAG, "Failed to check accessibility status", e)
            false
        }
    }

    /**
     * If accessibility is disabled, fire a high-priority notification
     * guiding the user back to settings.
     */
    fun checkAndNotify(context: Context) {
        if (isEnabled(context)) return

        NotificationHelper.sendNotification(
            context = context,
            channelId = NotificationHelper.CHANNEL_NUDGE,
            notifId = 3001,
            title = "SmartFocus Accessibility Disabled",
            body = "App monitoring has stopped. Tap to re-enable in Settings."
        )
    }

    /**
     * Schedule an immediate one-time health check (used after onUnbind).
     */
    fun scheduleImmediate(context: Context) {
        val request = OneTimeWorkRequestBuilder<AccessibilityHealthWorker>()
            .setInitialDelay(5, TimeUnit.SECONDS)
            .build()
        WorkManager.getInstance(context)
            .enqueueUniqueWork("accessibility_health_immediate", ExistingWorkPolicy.REPLACE, request)
    }
}
