package com.example.addictionreductionapp

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.addictionreductionapp.data.analytics.SmartReductionEngine
import com.example.addictionreductionapp.data.repository.AppLimitRepository
import com.example.addictionreductionapp.data.repository.ReductionPlanRepository
import com.example.addictionreductionapp.data.repository.SnapshotReconciliationManager
import com.example.addictionreductionapp.data.repository.StreakSyncManager
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent

class DailySnapshotWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    @EntryPoint
    @InstallIn(SingletonComponent::class)
    interface WorkerEntryPoint {
        fun reconciliationManager(): SnapshotReconciliationManager
        fun streakSyncManager(): StreakSyncManager
        fun smartReductionEngine(): SmartReductionEngine
        fun reductionPlanRepository(): ReductionPlanRepository
        fun appLimitRepository(): AppLimitRepository
    }

    override suspend fun doWork(): Result {
        return try {
            val entryPoint = EntryPointAccessors.fromApplication(
                applicationContext,
                WorkerEntryPoint::class.java
            )
            entryPoint.reconciliationManager().reconcileMissingSnapshots()
            entryPoint.streakSyncManager().syncTodayStreak()

            val engine = entryPoint.smartReductionEngine()
            val planRepo = entryPoint.reductionPlanRepository()
            val appLimitRepo = entryPoint.appLimitRepository()
            val activePlans = planRepo.getActive()
            if (activePlans.isNotEmpty()) {
                val allAppLimits = appLimitRepo.getAllAppsOnce()
                engine.applyDailyReduction(
                    plans = activePlans,
                    appLimits = allAppLimits,
                    savePlan = { planRepo.upsert(it) },
                    saveAppLimit = { appLimitRepo.upsert(it) }
                )
            }

            Result.success()
        } catch (e: Exception) {
            e.printStackTrace()
            Result.retry()
        }
    }
}
