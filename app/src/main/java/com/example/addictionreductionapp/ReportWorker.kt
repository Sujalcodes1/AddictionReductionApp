package com.example.addictionreductionapp

import android.app.usage.UsageStatsManager
import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.addictionreductionapp.data.AppDataStore

class ReportWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        val reportType = inputData.getString("report_type") ?: "daily"
        
        // Load data to ensure apps list and streak count are populated
        AppDataStore.loadFromPrefs(applicationContext)
        val selectedApps = AppDataStore.apps.filter { it.isSelected }

        if (selectedApps.isEmpty()) {
            return Result.success()
        }

        val usageStatsManager = applicationContext.getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager
        val endTime = System.currentTimeMillis()
        val startTime = when (reportType) {
            "weekly" -> endTime - 7 * 24 * 60 * 60 * 1000L
            "monthly" -> endTime - 30 * 24 * 60 * 60 * 1000L
            else -> endTime - 24 * 60 * 60 * 1000L
        }

        val stats = usageStatsManager.queryUsageStats(UsageStatsManager.INTERVAL_DAILY, startTime, endTime)
        
        var totalTime = 0L
        var mostUsedApp = ""
        var maxTime = 0L

        for (app in selectedApps) {
            val pkgStats = stats.filter { it.packageName == app.packageName }
            val timeInForeground = pkgStats.sumOf { it.totalTimeInForeground }
            totalTime += timeInForeground
            if (timeInForeground > maxTime) {
                maxTime = timeInForeground
                mostUsedApp = app.name
            }
        }

        val prefix = when (reportType) {
            "weekly" -> "This Week's"
            "monthly" -> "This Month's"
            else -> "Today's"
        }
        
        val totalHours = (totalTime / (1000 * 60 * 60)).toInt()
        val totalMins = ((totalTime / (1000 * 60)) % 60).toInt()
        val mostUsedMins = (maxTime / (1000 * 60)).toInt()
        val streak = AppDataStore.streakCount.intValue

        val message = "$prefix Screen Report — You used tracked apps for ${totalHours}h ${totalMins}m. Most used: $mostUsedApp (${mostUsedMins}m). Streak: $streak days!"
        
        val channelId = when(reportType) {
            "weekly" -> NotificationHelper.CHANNEL_WEEKLY
            "monthly" -> NotificationHelper.CHANNEL_MONTHLY
            else -> NotificationHelper.CHANNEL_DAILY
        }

        NotificationHelper.sendNotification(
            context = applicationContext,
            channelId = channelId,
            notifId = reportType.hashCode(),
            title = "$prefix Report",
            body = message
        )

        return Result.success()
    }
}
