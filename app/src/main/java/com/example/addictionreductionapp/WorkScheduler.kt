package com.example.addictionreductionapp

import android.content.Context
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.workDataOf
import java.util.Calendar
import java.util.concurrent.TimeUnit

object WorkScheduler {
    fun scheduleAll(context: Context) {
        NotificationHelper.createChannels(context)
        val workManager = WorkManager.getInstance(context)
        val now = Calendar.getInstance()

        // Daily Report: 24h, 9 PM
        val dailyTarget = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 21)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
        }
        if (now.after(dailyTarget)) {
            dailyTarget.add(Calendar.DAY_OF_YEAR, 1)
        }
        val dailyDelay = dailyTarget.timeInMillis - now.timeInMillis
        val dailyRequest = PeriodicWorkRequestBuilder<ReportWorker>(24, TimeUnit.HOURS)
            .setInitialDelay(dailyDelay, TimeUnit.MILLISECONDS)
            .setInputData(workDataOf("report_type" to "daily"))
            .build()
        workManager.enqueueUniquePeriodicWork("daily_report", ExistingPeriodicWorkPolicy.KEEP, dailyRequest)

        // Weekly Report: 7 days, 9 AM
        val weeklyTarget = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 9)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
        }
        if (now.after(weeklyTarget)) {
            weeklyTarget.add(Calendar.DAY_OF_YEAR, 1)
        }
        val weeklyDelay = weeklyTarget.timeInMillis - now.timeInMillis
        val weeklyRequest = PeriodicWorkRequestBuilder<ReportWorker>(7, TimeUnit.DAYS)
            .setInitialDelay(weeklyDelay, TimeUnit.MILLISECONDS)
            .setInputData(workDataOf("report_type" to "weekly"))
            .build()
        workManager.enqueueUniquePeriodicWork("weekly_report", ExistingPeriodicWorkPolicy.KEEP, weeklyRequest)

        // Monthly Report: 30 days
        val monthlyRequest = PeriodicWorkRequestBuilder<ReportWorker>(30, TimeUnit.DAYS)
            .setInputData(workDataOf("report_type" to "monthly"))
            .build()
        workManager.enqueueUniquePeriodicWork("monthly_report", ExistingPeriodicWorkPolicy.KEEP, monthlyRequest)

        // Hourly Nudge: 1 hour
        val nudgeRequest = PeriodicWorkRequestBuilder<NudgeWorker>(1, TimeUnit.HOURS)
            .build()
        workManager.enqueueUniquePeriodicWork("hourly_nudge", ExistingPeriodicWorkPolicy.KEEP, nudgeRequest)

        // Daily Behavior Snapshot Generation (Safety Net): 24h, 11:55 PM
        val snapshotTarget = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 23)
            set(Calendar.MINUTE, 55)
            set(Calendar.SECOND, 0)
        }
        if (now.after(snapshotTarget)) {
            snapshotTarget.add(Calendar.DAY_OF_YEAR, 1)
        }
        val snapshotDelay = snapshotTarget.timeInMillis - now.timeInMillis
        val snapshotRequest = PeriodicWorkRequestBuilder<DailySnapshotWorker>(24, TimeUnit.HOURS)
            .setInitialDelay(snapshotDelay, TimeUnit.MILLISECONDS)
            .build()
        workManager.enqueueUniquePeriodicWork("daily_snapshot_generation", ExistingPeriodicWorkPolicy.KEEP, snapshotRequest)

        // Accessibility Health Check: every 15 min (M10 — prevents service disabling)
        val healthRequest = PeriodicWorkRequestBuilder<AccessibilityHealthWorker>(15, TimeUnit.MINUTES)
            .build()
        workManager.enqueueUniquePeriodicWork("accessibility_health_check", ExistingPeriodicWorkPolicy.KEEP, healthRequest)
    }
}
