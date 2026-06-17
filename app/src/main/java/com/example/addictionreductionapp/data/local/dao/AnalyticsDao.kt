package com.example.addictionreductionapp.data.local.dao

import androidx.room.Dao
import androidx.room.Query
import com.example.addictionreductionapp.data.models.AppUsageSummary
import com.example.addictionreductionapp.data.models.CategoryAnalytics
import com.example.addictionreductionapp.data.models.DailyUsageSummary
import com.example.addictionreductionapp.data.models.HourlyUsagePoint
import kotlinx.coroutines.flow.Flow

@Dao
interface AnalyticsDao {

    // total screen time today
    @Query("SELECT SUM(usage_minutes) FROM app_usage WHERE usage_date = :date")
    fun getTotalScreenTimeToday(date: String): Flow<Int?>

    // total opens today
    @Query("SELECT SUM(open_count) FROM app_usage WHERE usage_date = :date")
    fun getTotalOpensToday(date: String): Flow<Int?>

    // most used apps today
    @Query("""
        SELECT package_name as packageName, app_name as appName, 
               usage_minutes as totalMinutes, open_count as openCount
        FROM app_usage 
        WHERE usage_date = :date 
        ORDER BY usage_minutes DESC
    """)
    fun getMostUsedAppsToday(date: String): Flow<List<AppUsageSummary>>

    // most opened apps today
    @Query("""
        SELECT package_name as packageName, app_name as appName, 
               usage_minutes as totalMinutes, open_count as openCount
        FROM app_usage 
        WHERE usage_date = :date 
        ORDER BY open_count DESC
    """)
    fun getMostOpenedAppsToday(date: String): Flow<List<AppUsageSummary>>

    // category totals today
    @Query("""
        SELECT app_category as category, SUM(usage_minutes) as totalMinutes 
        FROM app_usage 
        WHERE usage_date = :date 
        GROUP BY app_category 
        ORDER BY totalMinutes DESC
    """)
    fun getCategoryTotalsToday(date: String): Flow<List<CategoryAnalytics>>

    // weekly usage totals
    @Query("SELECT SUM(usage_minutes) FROM app_usage WHERE usage_date >= :startDate AND usage_date <= :endDate")
    fun getWeeklyUsageTotal(startDate: String, endDate: String): Flow<Int?>

    // hourly usage aggregation
    @Query("""
        SELECT cast(strftime('%H', datetime(start_timestamp / 1000, 'unixepoch', 'localtime')) as integer) as hourOfDay, 
               SUM(usage_minutes) as minutesUsed
        FROM app_usage 
        WHERE usage_date = :date AND start_timestamp > 0
        GROUP BY hourOfDay
        ORDER BY hourOfDay ASC
    """)
    fun getHourlyUsageToday(date: String): Flow<List<HourlyUsagePoint>>

    // ── Historical Analytics Queries ─────────────────────────────────────────

    /**
     * Returns per-day screen time, open count, and category breakdowns for a date range.
     * Each row is one day's aggregated totals. Used to compute historical focus scores
     * and streak data in Kotlin via [FocusScoreEngine].
     */
    @Query("""
        SELECT usage_date as date,
               SUM(usage_minutes) as totalScreenTimeMinutes,
               SUM(open_count) as totalOpens
        FROM app_usage
        WHERE usage_date >= :startDate AND usage_date <= :endDate
        GROUP BY usage_date
        ORDER BY usage_date ASC
    """)
    fun getDailyScreenTimeSeries(startDate: String, endDate: String): Flow<List<DailyUsageSummary>>

    /**
     * Returns category-level breakdowns for every day in a date range.
     * Grouped by (date, category) so we can compute per-day productive/distracting ratios.
     */
    @Query("""
        SELECT usage_date as date,
               app_category as category,
               SUM(usage_minutes) as totalMinutes
        FROM app_usage
        WHERE usage_date >= :startDate AND usage_date <= :endDate
        GROUP BY usage_date, app_category
        ORDER BY usage_date ASC, totalMinutes DESC
    """)
    fun getDailyCategoryTotals(startDate: String, endDate: String): Flow<List<DailyCategoryRow>>

    /**
     * Returns total screen time for a single specific date (non-reactive suspend variant).
     */
    @Query("SELECT COALESCE(SUM(usage_minutes), 0) FROM app_usage WHERE usage_date = :date")
    suspend fun getDailyScreenTimeForDate(date: String): Int

    /**
     * Returns total open count for a single specific date (non-reactive suspend variant).
     */
    @Query("SELECT COALESCE(SUM(open_count), 0) FROM app_usage WHERE usage_date = :date")
    suspend fun getDailyOpensForDate(date: String): Int

    @Query("""
        SELECT package_name as packageName, app_name as appName, 
               usage_minutes as totalMinutes, open_count as openCount
        FROM app_usage 
        WHERE usage_date = :date 
        ORDER BY usage_minutes DESC
    """)
    suspend fun getMostUsedAppsForDate(date: String): List<AppUsageSummary>

    @Query("""
        SELECT app_category as category, SUM(usage_minutes) as totalMinutes 
        FROM app_usage 
        WHERE usage_date = :date 
        GROUP BY app_category 
        ORDER BY totalMinutes DESC
    """)
    suspend fun getCategoryTotalsForDate(date: String): List<CategoryAnalytics>

    @Query("SELECT COALESCE(SUM(usage_minutes), 0) FROM app_usage WHERE usage_date >= :startDate AND usage_date <= :endDate")
    suspend fun getWeeklyUsageTotalForDate(startDate: String, endDate: String): Int

    @Query("""
        SELECT cast(strftime('%H', datetime(start_timestamp / 1000, 'unixepoch', 'localtime')) as integer) as hourOfDay, 
               SUM(usage_minutes) as minutesUsed
        FROM app_usage 
        WHERE usage_date = :date AND start_timestamp > 0
        GROUP BY hourOfDay
        ORDER BY hourOfDay ASC
    """)
    suspend fun getHourlyUsageForDate(date: String): List<HourlyUsagePoint>

    @Query("""
        SELECT usage_date as date,
               SUM(usage_minutes) as totalScreenTimeMinutes,
               SUM(open_count) as totalOpens
        FROM app_usage
        WHERE usage_date >= :startDate AND usage_date <= :endDate
        GROUP BY usage_date
        ORDER BY usage_date ASC
    """)
    suspend fun getDailyScreenTimeSeriesSuspend(startDate: String, endDate: String): List<DailyUsageSummary>

    @Query("""
        SELECT usage_date as date,
               app_category as category,
               SUM(usage_minutes) as totalMinutes
        FROM app_usage
        WHERE usage_date >= :startDate AND usage_date <= :endDate
        GROUP BY usage_date, app_category
        ORDER BY usage_date ASC, totalMinutes DESC
    """)
    suspend fun getDailyCategoryTotalsSuspend(startDate: String, endDate: String): List<DailyCategoryRow>
}

/**
 * Projection class for the getDailyCategoryTotals query.
 * Each row is one (date, category, totalMinutes) tuple.
 */
data class DailyCategoryRow(
    val date: String,
    val category: String,
    val totalMinutes: Int
)

