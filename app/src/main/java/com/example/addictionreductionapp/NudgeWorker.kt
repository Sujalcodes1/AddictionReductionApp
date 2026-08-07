package com.example.addictionreductionapp

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.addictionreductionapp.data.repository.AppLimitRepository
import com.example.addictionreductionapp.data.repository.ReductionPlanRepository
import com.example.addictionreductionapp.data.repository.AppUsageRepository
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class NudgeWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    @EntryPoint
    @InstallIn(SingletonComponent::class)
    interface WorkerEntryPoint {
        fun appLimitRepository(): AppLimitRepository
        fun appUsageRepository(): AppUsageRepository
        fun reductionPlanRepository(): ReductionPlanRepository
    }

    override suspend fun doWork(): Result = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
        val entryPoint = EntryPointAccessors.fromApplication(
            applicationContext,
            WorkerEntryPoint::class.java
        )
        val selectedApps = entryPoint.appLimitRepository().getSelectedAppsOnce()

        if (selectedApps.isEmpty()) {
            return@withContext Result.success()
        }

        val today = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())
        val appUsageRepo = entryPoint.appUsageRepository()

        var shouldNudge = false
        for (app in selectedApps) {
            val usage = appUsageRepo.getUsageForAppOnDate(app.packageName, today)
            val timeMins = usage?.usageMinutes ?: 0

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

        checkLimitApproaching(entryPoint, today)

        return@withContext Result.success()
    }

    private suspend fun checkLimitApproaching(
        entryPoint: WorkerEntryPoint,
        today: String
    ) {
        val planRepo = entryPoint.reductionPlanRepository()
        val appUsageRepo = entryPoint.appUsageRepository()
        val activePlans = planRepo.getActive()

        for (plan in activePlans) {
            val used = appUsageRepo.getTotalMinutesForCategory(plan.category, today)
            val remaining = plan.currentTarget - used
            if (remaining in 1..10) {
                NotificationHelper.sendNotification(
                    context = applicationContext,
                    channelId = NotificationHelper.CHANNEL_LIMIT_APPROACHING,
                    notifId = ("limit_${plan.id}").hashCode(),
                    title = "$remaining min remaining",
                    body = "You have $remaining minutes of ${plan.category} remaining today. Stay mindful!"
                )
            }
        }
    }
}
