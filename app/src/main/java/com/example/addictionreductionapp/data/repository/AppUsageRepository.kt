package com.example.addictionreductionapp.data.repository

import android.util.Log
import com.example.addictionreductionapp.data.local.dao.AppUsageDao
import com.example.addictionreductionapp.data.local.entities.AppUsageEntity
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AppUsageRepository @Inject constructor(
    private val appUsageDao: AppUsageDao
) {
    companion object {
        private const val TAG = "AppUsageRepository"
    }

    fun getTopUsedAppsToday(today: String, limit: Int = 5): Flow<List<AppUsageEntity>> =
        appUsageDao.getTopUsedAppsToday(today, limit)

    fun getMostOpenedApps(today: String, limit: Int = 5): Flow<List<AppUsageEntity>> =
        appUsageDao.getMostOpenedApps(today, limit)

    suspend fun getUsageForAppOnDate(packageName: String, date: String): AppUsageEntity? =
        appUsageDao.getUsageForAppOnDate(packageName, date)

    suspend fun persistSession(
        packageName: String,
        appName: String,
        category: String,
        sessionStartWall: Long,
        sessionEndWall: Long,
        durationMinutes: Int,
        usageDate: String
    ) {
        Log.d(TAG, "app_usage INSERT START: pkg=$packageName date=$usageDate minutes=$durationMinutes")

        val rowsAffected: Int = appUsageDao.accumulateUsage(
            packageName = packageName,
            date = usageDate,
            additionalMinutes = durationMinutes,
            newEndTimestamp = sessionEndWall
        )

        Log.d(TAG, "app_usage SQL row count affected by UPDATE: $rowsAffected (pkg=$packageName date=$usageDate)")

        if (rowsAffected > 0) {
            Log.i(TAG, "app_usage UPDATE SUCCESS: pkg=$packageName date=$usageDate +${durationMinutes}m (rowsAffected=$rowsAffected)")
            return
        }

        val entity = AppUsageEntity(
            packageName = packageName,
            appName = appName,
            usageMinutes = durationMinutes,
            openCount = 1,
            startTimestamp = sessionStartWall,
            endTimestamp = sessionEndWall,
            usageDate = usageDate,
            appCategory = category
        )

        val insertedRowId: Long = appUsageDao.insertUsage(entity)

        if (insertedRowId != -1L) {
            Log.i(TAG, "app_usage INSERT SUCCESS: pkg=$packageName date=$usageDate minutes=${durationMinutes}m rowId=$insertedRowId")
        } else {
            Log.w(TAG, "app_usage INSERT IGNORED (race-condition duplicate): pkg=$packageName date=$usageDate")
        }
    }

    suspend fun getTotalMinutesForCategory(category: String, date: String): Int =
        appUsageDao.getTotalMinutesForCategory(category, date)
}
