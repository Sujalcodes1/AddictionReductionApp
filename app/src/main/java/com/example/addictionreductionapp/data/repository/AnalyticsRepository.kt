package com.example.addictionreductionapp.data.repository

import com.example.addictionreductionapp.data.analytics.FocusScoreEngine
import com.example.addictionreductionapp.data.local.dao.AnalyticsDao
import com.example.addictionreductionapp.data.local.dao.DailyCategoryRow
import com.example.addictionreductionapp.data.models.AppUsageSummary
import com.example.addictionreductionapp.data.models.CategoryAnalytics
import com.example.addictionreductionapp.data.models.DailyUsageSummary
import com.example.addictionreductionapp.data.models.FocusScore
import com.example.addictionreductionapp.data.models.FocusScoreDetails
import com.example.addictionreductionapp.data.models.HourlyUsagePoint
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.Dispatchers
import javax.inject.Inject

class AnalyticsRepository @Inject constructor(
    private val analyticsDao: AnalyticsDao,
    private val focusScoreEngine: FocusScoreEngine
) {
    // ── Existing single-day queries ──────────────────────────────────────────

    fun getTotalScreenTimeToday(date: String): Flow<Int?> {
        return analyticsDao.getTotalScreenTimeToday(date)
    }

    fun getTotalOpensToday(date: String): Flow<Int?> {
        return analyticsDao.getTotalOpensToday(date)
    }

    fun getMostUsedAppsToday(date: String): Flow<List<AppUsageSummary>> {
        return analyticsDao.getMostUsedAppsToday(date)
    }

    fun getMostOpenedAppsToday(date: String): Flow<List<AppUsageSummary>> {
        return analyticsDao.getMostOpenedAppsToday(date)
    }

    fun getCategoryTotalsToday(date: String): Flow<List<CategoryAnalytics>> {
        return analyticsDao.getCategoryTotalsToday(date)
    }

    fun getWeeklyUsageTotal(startDate: String, endDate: String): Flow<Int?> {
        return analyticsDao.getWeeklyUsageTotal(startDate, endDate)
    }

    fun getHourlyUsageToday(date: String): Flow<List<HourlyUsagePoint>> {
        return analyticsDao.getHourlyUsageToday(date)
    }

    // ── Historical Analytics ─────────────────────────────────────────────────

    /**
     * Returns per-day screen time + open count for a date range.
     */
    fun getDailyScreenTimeSeries(startDate: String, endDate: String): Flow<List<DailyUsageSummary>> {
        return analyticsDao.getDailyScreenTimeSeries(startDate, endDate)
    }

    /**
     * Returns per-day category breakdowns for a date range.
     */
    fun getDailyCategoryTotals(startDate: String, endDate: String): Flow<List<DailyCategoryRow>> {
        return analyticsDao.getDailyCategoryTotals(startDate, endDate)
    }

    /**
     * Computes historical FocusScoreDetails for each day in the date range.
     *
     * Combines getDailyScreenTimeSeries (per-day totals) with getDailyCategoryTotals
     * (per-day category breakdowns) and runs FocusScoreEngine.calculateScore() on each
     * day to produce a list of FocusScoreDetails ordered by date ascending.
     *
     * This is the core method that feeds both StreakEngine and BehavioralIntelligenceEngine.
     */
    fun getHistoricalFocusScores(
        startDate: String,
        endDate: String
    ): Flow<List<FocusScoreDetails>> {
        val dailySummariesFlow = analyticsDao.getDailyScreenTimeSeries(startDate, endDate)
        val dailyCategoriesFlow = analyticsDao.getDailyCategoryTotals(startDate, endDate)

        return combine(dailySummariesFlow, dailyCategoriesFlow) { summaries, categoryRows ->
            // Group category rows by date for O(1) lookup
            val categoriesByDate: Map<String, List<CategoryAnalytics>> =
                categoryRows.groupBy { it.date }
                    .mapValues { (_, rows) ->
                        rows.map { CategoryAnalytics(it.category, it.totalMinutes) }
                    }

            // Compute a FocusScoreDetails for each day that has data
            summaries.map { daily ->
                val categories = categoriesByDate[daily.date] ?: emptyList()
                focusScoreEngine.calculateScore(
                    totalScreenTimeMinutes = daily.totalScreenTimeMinutes,
                    totalOpens = daily.totalOpens,
                    categoryTotals = categories
                )
            }
        }.flowOn(Dispatchers.IO)
    }

    /**
     * Computes historical FocusScore (date + score only) for each day in the date range.
     * This is the simplified projection used by StreakEngine.calculateStreak().
     */
    fun getHistoricalFocusScoreList(
        startDate: String,
        endDate: String
    ): Flow<List<FocusScore>> {
        val dailySummariesFlow = analyticsDao.getDailyScreenTimeSeries(startDate, endDate)
        val dailyCategoriesFlow = analyticsDao.getDailyCategoryTotals(startDate, endDate)

        return combine(dailySummariesFlow, dailyCategoriesFlow) { summaries, categoryRows ->
            val categoriesByDate: Map<String, List<CategoryAnalytics>> =
                categoryRows.groupBy { it.date }
                    .mapValues { (_, rows) ->
                        rows.map { CategoryAnalytics(it.category, it.totalMinutes) }
                    }

            summaries.map { daily ->
                val categories = categoriesByDate[daily.date] ?: emptyList()
                val details = focusScoreEngine.calculateScore(
                    totalScreenTimeMinutes = daily.totalScreenTimeMinutes,
                    totalOpens = daily.totalOpens,
                    categoryTotals = categories
                )
                FocusScore(date = daily.date, score = details.score)
            }
        }.flowOn(Dispatchers.IO)
    }
}

