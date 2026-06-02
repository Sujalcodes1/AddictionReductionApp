package com.example.addictionreductionapp.data.models

data class DailyAnalytics(
    val date: String,
    val totalScreenTimeMinutes: Int,
    val totalOpens: Int,
    val mostUsedAppPackage: String?,
    val mostUsedAppTime: Int
)

data class WeeklyAnalytics(
    val weekStartDate: String,
    val totalScreenTimeMinutes: Int,
    val dailyAverageMinutes: Int
)

data class CategoryAnalytics(
    val category: String,
    val totalMinutes: Int
)

data class AppUsageSummary(
    val packageName: String,
    val appName: String,
    val totalMinutes: Int,
    val openCount: Int
)

data class HourlyUsagePoint(
    val hourOfDay: Int,
    val minutesUsed: Int
)

data class FocusScore(
    val date: String,
    val score: Int
)

data class UsageTrend(
    val date: String,
    val usageMinutes: Int
)

data class StreakInfo(
    val currentStreakDays: Int,
    val longestStreakDays: Int,
    val lastActiveDate: String?
)

data class FocusScoreDetails(
    val score: Int,
    val productiveRatio: Float,
    val distractionRatio: Float,
    val totalAppSwitches: Int,
    val explanation: String
)

data class StreakAnalysis(
    val streakInfo: StreakInfo,
    val isProductiveDayStreak: Boolean,
    val isRecovered: Boolean,
    val explanation: String
)

/**
 * Room projection class for per-day aggregated usage data.
 * Used by AnalyticsDao.getDailyScreenTimeSeries() to return
 * one row per calendar date within a date range.
 */
data class DailyUsageSummary(
    val date: String,
    val totalScreenTimeMinutes: Int,
    val totalOpens: Int
)

