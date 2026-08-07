package com.example.addictionreductionapp

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.addictionreductionapp.data.repository.AppLimitRepository
import com.example.addictionreductionapp.data.repository.UserProfileRepository
import com.example.addictionreductionapp.data.repository.AppUsageRepository
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class ReportWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    @EntryPoint
    @InstallIn(SingletonComponent::class)
    interface WorkerEntryPoint {
        fun appLimitRepository(): AppLimitRepository
        fun userProfileRepository(): UserProfileRepository
        fun appUsageRepository(): AppUsageRepository
    }

    override suspend fun doWork(): Result = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
        val reportType = inputData.getString("report_type") ?: "daily"

        val entryPoint = EntryPointAccessors.fromApplication(
            applicationContext,
            WorkerEntryPoint::class.java
        )
        val selectedApps = entryPoint.appLimitRepository().getSelectedAppsOnce()

        if (selectedApps.isEmpty()) {
            return@withContext androidx.work.ListenableWorker.Result.success()
        }

        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.US)
        val appUsageRepo = entryPoint.appUsageRepository()

        val days = when (reportType) {
            "weekly" -> (0 until 7).map { sdf.format(Date(System.currentTimeMillis() - it * 24L * 60 * 60 * 1000)) }
            "monthly" -> (0 until 30).map { sdf.format(Date(System.currentTimeMillis() - it * 24L * 60 * 60 * 1000)) }
            else -> listOf(sdf.format(Date()))
        }

        var totalTime = 0L
        var mostUsedApp = ""
        var maxTime = 0L

        for (app in selectedApps) {
            var appTotal = 0L
            for (day in days) {
                val usage = appUsageRepo.getUsageForAppOnDate(app.packageName, day)
                appTotal += (usage?.usageMinutes ?: 0)
            }
            totalTime += appTotal
            if (appTotal > maxTime) {
                maxTime = appTotal
                mostUsedApp = app.appName
            }
        }

        val prefix = when (reportType) {
            "weekly" -> "This Week's"
            "monthly" -> "This Month's"
            else -> "Today's"
        }

        val totalHours = (totalTime / 60).toInt()
        val totalMins = (totalTime % 60).toInt()
        val mostUsedMins = maxTime.toInt()
        val profile = entryPoint.userProfileRepository().getProfile()
        val streak = profile?.streakCount ?: 0

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

        return@withContext Result.success()
    }
}
