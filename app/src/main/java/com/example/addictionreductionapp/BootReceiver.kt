package com.example.addictionreductionapp

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED ||
            intent.action == Intent.ACTION_LOCKED_BOOT_COMPLETED ||
            intent.action == Intent.ACTION_MY_PACKAGE_REPLACED) {
            WorkScheduler.scheduleAll(context)
            // Check if accessibility was disabled after reboot (M10)
            AccessibilityHealthChecker.checkAndNotify(context)
        }
    }
}
