package com.example.addictionreductionapp

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters

/**
 * Periodic worker that checks if the accessibility service is still enabled.
 * If disabled, fires a notification guiding the user to re-enable it.
 *
 * Scheduled every 15 minutes by [WorkScheduler].
 */
class AccessibilityHealthWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        AccessibilityHealthChecker.checkAndNotify(applicationContext)
        return Result.success()
    }
}
