package com.example.addictionreductionapp

import android.app.usage.UsageStatsManager
import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.addictionreductionapp.data.AppDataStore
import java.util.Calendar

class NudgeWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        AppDataStore.loadFromPrefs(applicationContext)
        val selectedApps = AppDataStore.apps.filter { it.isSelected }

        if (selectedApps.isEmpty()) {
            return Result.success()
        }

        val usageStatsManager = applicationContext.getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager
        val calendar = Calendar.getInstance()
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        val startTime = calendar.timeInMillis
        val endTime = System.currentTimeMillis()

        val stats = usageStatsManager.queryUsageStats(UsageStatsManager.INTERVAL_DAILY, startTime, endTime)

        var shouldNudge = false
        for (app in selectedApps) {
            val pkgStats = stats.filter { it.packageName == app.packageName }
            val timeInForeground = pkgStats.sumOf { it.totalTimeInForeground }
            val timeMins = (timeInForeground / (1000 * 60)).toInt()

            if (timeMins >= app.limitMinutes / 2) {
                shouldNudge = true
                break
            }
        }

        if (shouldNudge) {
            val nudges = listOf(
                "You've been scrolling too long. Take a break!",
                "Time to rest your eyes. Look at something 20 feet away.",
                "You're halfway through your limit. Scroll mindfully!",
                "Ready for a digital detox? Put the phone down for 5 minutes."
            )
            val nudge = nudges.random()
            
            NotificationHelper.sendNotification(
                context = applicationContext,
                channelId = NotificationHelper.CHANNEL_NUDGE,
                notifId = System.currentTimeMillis().toInt(),
                title = "Time for a break?",
                body = nudge
            )
        }

        return Result.success()
    }
}
