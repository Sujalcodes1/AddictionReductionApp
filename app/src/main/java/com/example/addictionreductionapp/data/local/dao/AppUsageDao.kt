package com.example.addictionreductionapp.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.addictionreductionapp.data.local.entities.AppUsageEntity
import kotlinx.coroutines.flow.Flow

/**
 * Data Access Object for [AppUsageEntity].
 *
 * All read operations are exposed as [Flow] so the UI layer (via ViewModel)
 * automatically receives updates whenever the underlying table changes —
 * consistent with [FocusSessionDao] and [UserProfileDao] patterns.
 *
 * One-shot suspend functions are used for write operations and for service/worker
 * code where a direct result is needed on an IO coroutine without a lifecycle.
 *
 * ## Date format convention
 * All `date` parameters must be passed as "YYYY-MM-DD" strings (e.g. "2025-05-27").
 * This matches [AppUsageEntity.usageDate] and allows SQLite to do lexicographic
 * date comparisons correctly without epoch arithmetic.
 *
 * ## Indexed columns used by queries
 * - `usage_date`                  — single-column index (day-scoped reads)
 * - `(package_name, usage_date)`  — composite unique index (per-app-per-day writes)
 */
@Dao
interface AppUsageDao {

    // ── Writes ────────────────────────────────────────────────────────────────

    /**
     * Insert a brand-new daily usage record for an app.
     *
     * Use [OnConflictStrategy.IGNORE] to silently skip if a record with the same
     * (package_name, usage_date) combination already exists.  For accumulating
     * usage on an existing record, call [updateUsage] instead.
     *
     * @return The row-id of the newly inserted record, or -1 if ignored.
     */
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertUsage(entity: AppUsageEntity): Long

    /**
     * Persist updates to an existing [AppUsageEntity].
     *
     * Matches by [AppUsageEntity.id] (Room's default for @Update).
     * Typically used after fetching the current record and incrementing
     * [AppUsageEntity.usageMinutes] / [AppUsageEntity.openCount] in-memory.
     *
     * @return Number of rows updated (0 if the id was not found).
     */
    @Update
    suspend fun updateUsage(entity: AppUsageEntity): Int

    /**
     * Atomically adds [additionalMinutes] and increments the open count for a
     * specific app on a specific date.
     *
     * **Return value is critical:** Room maps the SQLite rows-affected count to
     * the returned [Int].
     * - `1` → a row for ([packageName], [date]) existed and was updated.
     * - `0` → no row existed yet; the caller must fall through to [insertUsage].
     *
     * This is the only correct INSERT-or-UPDATE pattern: checking the return
     * value avoids a second SELECT round-trip and is safe under concurrent writes.
     *
     * @param packageName       The app's package name.
     * @param date              Calendar date in "YYYY-MM-DD" format.
     * @param additionalMinutes Minutes to add to the running total.
     * @param newEndTimestamp   Epoch ms of the latest session-end event.
     * @return                  SQLite rows-affected count (0 or 1).
     */
    @Query(
        """
        UPDATE app_usage
        SET usage_minutes  = usage_minutes  + :additionalMinutes,
            open_count     = open_count     + 1,
            end_timestamp  = :newEndTimestamp
        WHERE package_name = :packageName
          AND usage_date   = :date
        """
    )
    suspend fun accumulateUsage(
        packageName: String,
        date: String,
        additionalMinutes: Int,
        newEndTimestamp: Long
    ): Int

    /**
     * Delete usage records older than [cutoffDate] to prevent unbounded table growth.
     *
     * Schedule this via [androidx.work.WorkManager] (e.g. weekly) using a
     * [androidx.work.PeriodicWorkRequest] to keep the database lean.
     *
     * @param cutoffDate Records with [AppUsageEntity.usageDate] strictly before
     *                   this value (in "YYYY-MM-DD" format) will be deleted.
     *                   Example: pass `2025-04-27` to retain the last 30 days.
     */
    @Query("DELETE FROM app_usage WHERE usage_date < :cutoffDate")
    suspend fun deleteOldUsageData(cutoffDate: String)

    // ── Daily reads ───────────────────────────────────────────────────────────

    /**
     * Observe all app usage records for [today] ("YYYY-MM-DD").
     *
     * Emits a new list whenever any record on that date is inserted or updated.
     * Results are ordered by [AppUsageEntity.usageMinutes] descending so the
     * highest-consuming apps appear first in the UI.
     *
     * @param today Calendar date string in "YYYY-MM-DD" format.
     */
    @Query(
        """
        SELECT * FROM app_usage
        WHERE usage_date = :today
        ORDER BY usage_minutes DESC
        """
    )
    fun getTodayUsage(today: String): Flow<List<AppUsageEntity>>

    /**
     * Return the top [limit] apps by total usage minutes for [today].
     *
     * Designed for dashboard summary cards — avoids loading the full list when
     * only a short "Most Used" widget is needed.
     *
     * @param today Calendar date string in "YYYY-MM-DD" format.
     * @param limit Maximum number of rows to return. Defaults to 5.
     */
    @Query(
        """
        SELECT * FROM app_usage
        WHERE usage_date = :today
        ORDER BY usage_minutes DESC
        LIMIT :limit
        """
    )
    fun getTopUsedAppsToday(today: String, limit: Int = 5): Flow<List<AppUsageEntity>>

    /**
     * Return the top [limit] apps sorted by total [AppUsageEntity.openCount] for [today].
     *
     * "Most opened" differs from "most used by time" — an app opened 30 times
     * briefly may rank higher here than one used for a long single session.
     *
     * @param today Calendar date string in "YYYY-MM-DD" format.
     * @param limit Maximum number of rows to return. Defaults to 5.
     */
    @Query(
        """
        SELECT * FROM app_usage
        WHERE usage_date = :today
        ORDER BY open_count DESC
        LIMIT :limit
        """
    )
    fun getMostOpenedApps(today: String, limit: Int = 5): Flow<List<AppUsageEntity>>

    // ── Weekly / range reads ──────────────────────────────────────────────────

    /**
     * Observe all app usage records within an inclusive date range [[startDate], [endDate]].
     *
     * The lexicographic ordering of "YYYY-MM-DD" strings makes a BETWEEN clause
     * correct and efficient with the `usage_date` index.
     *
     * @param startDate Inclusive start in "YYYY-MM-DD" format (e.g. 7 days ago).
     * @param endDate   Inclusive end in "YYYY-MM-DD" format (e.g. today).
     */
    @Query(
        """
        SELECT * FROM app_usage
        WHERE usage_date BETWEEN :startDate AND :endDate
        ORDER BY usage_date DESC, usage_minutes DESC
        """
    )
    fun getWeeklyUsage(startDate: String, endDate: String): Flow<List<AppUsageEntity>>

    /**
     * Observe weekly totals per app: package name, app name, summed minutes,
     * and summed open counts across [[startDate], [endDate]].
     *
     * Use this for the "Weekly Summary" screen where one row per app is needed
     * regardless of how many daily records exist.
     *
     * @param startDate Inclusive start in "YYYY-MM-DD" format.
     * @param endDate   Inclusive end in "YYYY-MM-DD" format.
     */
    @Query(
        """
        SELECT
            package_name,
            app_name,
            app_category,
            SUM(usage_minutes) AS usage_minutes,
            SUM(open_count)    AS open_count,
            MIN(start_timestamp) AS start_timestamp,
            MAX(end_timestamp)   AS end_timestamp,
            :startDate           AS usage_date,
            0                    AS id
        FROM app_usage
        WHERE usage_date BETWEEN :startDate AND :endDate
        GROUP BY package_name
        ORDER BY usage_minutes DESC
        """
    )
    fun getWeeklyUsagePerApp(startDate: String, endDate: String): Flow<List<AppUsageEntity>>

    // ── One-shot reads (for Services / Workers) ───────────────────────────────

    /**
     * One-shot fetch of a single record for ([packageName], [date]).
     *
     * Use inside a Service or Worker to check whether an existing record exists
     * before deciding to call [insertUsage] vs [updateUsage] / [accumulateUsage].
     *
     * @param packageName The app's package name.
     * @param date        Calendar date in "YYYY-MM-DD" format.
     * @return The matching [AppUsageEntity], or null if no record exists yet.
     */
    @Query(
        """
        SELECT * FROM app_usage
        WHERE package_name = :packageName
          AND usage_date   = :date
        LIMIT 1
        """
    )
    suspend fun getUsageForAppOnDate(packageName: String, date: String): AppUsageEntity?

    /**
     * One-shot fetch of the total usage minutes across all apps for [date].
     *
     * Returns 0 when no records exist for that date (COALESCE handles the
     * null produced by SUM on an empty result set).
     *
     * @param date Calendar date in "YYYY-MM-DD" format.
     */
    @Query(
        """
        SELECT COALESCE(SUM(usage_minutes), 0) FROM app_usage
        WHERE usage_date = :date
        """
    )
    suspend fun getTotalMinutesOnDate(date: String): Int
}
