package com.example.addictionreductionapp.repository

import android.util.Log
import com.example.addictionreductionapp.data.local.dao.AppUsageDao
import com.example.addictionreductionapp.data.local.entities.AppUsageEntity
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Repository for accessing and modifying application usage statistics.
 *
 * This layer abstracts away the Room DAO, exposing coroutine-safe operations
 * for the UI layer (via Flow) and the background tracking service.
 */
@Singleton
class AppUsageRepository @Inject constructor(
    private val appUsageDao: AppUsageDao
) {
    companion object {
        private const val TAG = "AppUsageRepository"
    }

    /**
     * Observe the top used apps for today.
     * @param today Date string in "YYYY-MM-DD" format.
     */
    fun getTopUsedAppsToday(today: String, limit: Int = 5): Flow<List<AppUsageEntity>> =
        appUsageDao.getTopUsedAppsToday(today, limit)

    /**
     * Observe the most opened apps for today.
     * @param today Date string in "YYYY-MM-DD" format.
     */
    fun getMostOpenedApps(today: String, limit: Int = 5): Flow<List<AppUsageEntity>> =
        appUsageDao.getMostOpenedApps(today, limit)

    /**
     * One-shot fetch of a single record for ([packageName], [date]).
     */
    suspend fun getUsageForAppOnDate(packageName: String, date: String): AppUsageEntity? =
        appUsageDao.getUsageForAppOnDate(packageName, date)

    /**
     * Safely persist a completed usage session into the app_usage table.
     *
     * Strategy:
     * 1. Run an atomic UPDATE — [AppUsageDao.accumulateUsage] returns the SQLite
     *    rows-affected count (Int).
     *    • rowsAffected == 1 → a row for (packageName, usageDate) already existed;
     *      minutes and openCount were incremented in-place.  Done.
     *    • rowsAffected == 0 → no row existed yet; fall through to INSERT.
     * 2. On the INSERT path, [AppUsageDao.insertUsage] returns the new rowId, or
     *    -1L if [OnConflictStrategy.IGNORE] suppressed a duplicate (race-condition
     *    safe — a concurrent INSERT from a second coroutine won the race).
     *
     * This pattern eliminates the read-modify-write anti-pattern and any timing
     * window between a SELECT and a subsequent INSERT/UPDATE.
     */
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

        // Step 1: Try to accumulate into existing row.
        // accumulateUsage() returns SQLite rows-affected (0 or 1).
        val rowsAffected: Int = appUsageDao.accumulateUsage(
            packageName = packageName,
            date = usageDate,
            additionalMinutes = durationMinutes,
            newEndTimestamp = sessionEndWall
        )

        Log.d(TAG, "app_usage SQL row count affected by UPDATE: $rowsAffected (pkg=$packageName date=$usageDate)")

        if (rowsAffected > 0) {
            // UPDATE path: existing row found and incremented.
            Log.i(TAG, "app_usage UPDATE SUCCESS: pkg=$packageName date=$usageDate +${durationMinutes}m (rowsAffected=$rowsAffected)")
            return
        }

        // Step 2: No existing row — INSERT a brand-new record for this app+date.
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
            // -1 means OnConflictStrategy.IGNORE suppressed a duplicate — safe to ignore.
            Log.w(TAG, "app_usage INSERT IGNORED (race-condition duplicate): pkg=$packageName date=$usageDate")
        }
    }
}
