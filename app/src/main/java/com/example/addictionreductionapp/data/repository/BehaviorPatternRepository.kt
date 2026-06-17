package com.example.addictionreductionapp.data.repository

import com.example.addictionreductionapp.data.local.entities.DailyBehaviorSnapshotEntity
import com.example.addictionreductionapp.data.models.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOn
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import javax.inject.Inject

class BehaviorPatternRepository @Inject constructor(
    private val snapshotRepository: DailyBehaviorSnapshotRepository,
    private val analyticsRepository: AnalyticsRepository
) {
    fun generateBehaviorPatternSummary(): Flow<BehaviorPatternSummary> {
        // Fetch 14 days of history to cover at least two weekends for stable weekend comparison.
        val snapshotsFlow = snapshotRepository.getHistoricalSnapshots(14)
        
        val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")
        val today = LocalDate.now().format(formatter)
        val topAppsFlow = analyticsRepository.getMostUsedAppsToday(today)
        
        return combine(snapshotsFlow, topAppsFlow) { snapshots, topApps ->
            // Assume snapshots are ordered descending by date (newest first, index 0 is latest).
            val weekendPattern = detectWeekendUsagePattern(snapshots)
            
            // For most short-term patterns, we only look at the last 7 days.
            val last7 = snapshots.take(7)
            
            val lateNightPattern = detectLateNightPattern(last7)
            val focusDecayPattern = detectFocusDecayPattern(snapshots)
            val recoveryPattern = detectRecoveryPattern(snapshots)
            val compulsiveSwitchingPattern = detectCompulsiveSwitchingPattern(last7)
            
            val appAddictionPattern = detectAppAddictionPattern(topApps)
            
            BehaviorPatternSummary(
                weekendUsage = weekendPattern,
                lateNight = lateNightPattern,
                focusDecay = focusDecayPattern,
                recovery = recoveryPattern,
                compulsiveSwitching = compulsiveSwitchingPattern,
                appAddiction = appAddictionPattern
            )
        }.flowOn(Dispatchers.IO)
    }

    private fun detectWeekendUsagePattern(snapshots: List<DailyBehaviorSnapshotEntity>): WeekendUsagePattern {
        var weekendTotal = 0
        var weekendCount = 0
        var weekdayTotal = 0
        var weekdayCount = 0
        
        for (snapshot in snapshots) {
            try {
                val date = LocalDate.parse(snapshot.date)
                if (date.dayOfWeek == DayOfWeek.SATURDAY || date.dayOfWeek == DayOfWeek.SUNDAY) {
                    weekendTotal += snapshot.totalScreenTimeMinutes
                    weekendCount++
                } else {
                    weekdayTotal += snapshot.totalScreenTimeMinutes
                    weekdayCount++
                }
            } catch (e: Exception) {
                // Ignore parse errors
            }
        }
        
        val weekendAvg = if (weekendCount > 0) weekendTotal.toFloat() / weekendCount else 0f
        val weekdayAvg = if (weekdayCount > 0) weekdayTotal.toFloat() / weekdayCount else 0f
        
        val percentageIncrease = if (weekdayAvg > 0) ((weekendAvg - weekdayAvg) / weekdayAvg) * 100f else 0f
        
        // Detect if weekend usage is 30%+ higher.
        val detected = weekendAvg > 0 && weekdayAvg > 0 && percentageIncrease >= 30f
        
        return WeekendUsagePattern(
            detected = detected,
            weekendAverageMinutes = weekendAvg,
            weekdayAverageMinutes = weekdayAvg,
            percentageIncrease = percentageIncrease
        )
    }

    private fun detectLateNightPattern(last7: List<DailyBehaviorSnapshotEntity>): LateNightPattern {
        val daysDetected = last7.count { it.lateNightUsageDetected }
        return LateNightPattern(
            detected = daysDetected >= 4,
            daysDetected = daysDetected
        )
    }

    private fun detectFocusDecayPattern(snapshots: List<DailyBehaviorSnapshotEntity>): FocusDecayPattern {
        var currentDeclines = 0
        for (i in 0 until snapshots.size - 1) {
            // Index 0 is newest. A decline means newest < older (i.e. score went down over time).
            if (snapshots[i].focusScore < snapshots[i+1].focusScore) {
                currentDeclines++
            } else {
                break
            }
        }
        return FocusDecayPattern(
            detected = currentDeclines >= 3,
            consecutiveDeclines = currentDeclines
        )
    }

    private fun detectRecoveryPattern(snapshots: List<DailyBehaviorSnapshotEntity>): RecoveryPattern {
        var currentImprovements = 0
        for (i in 0 until snapshots.size - 1) {
            // Index 0 is newest. An improvement means newest > older.
            if (snapshots[i].focusScore > snapshots[i+1].focusScore) {
                currentImprovements++
            } else {
                break
            }
        }
        return RecoveryPattern(
            detected = currentImprovements >= 3,
            consecutiveImprovements = currentImprovements
        )
    }

    private fun detectCompulsiveSwitchingPattern(last7: List<DailyBehaviorSnapshotEntity>): CompulsiveSwitchingPattern {
        val daysDetected = last7.count { it.compulsiveSwitchingDetected }
        return CompulsiveSwitchingPattern(
            detected = daysDetected >= 4,
            daysDetected = daysDetected
        )
    }

    private fun detectAppAddictionPattern(topApps: List<AppUsageSummary>): AppAddictionPattern {
        val dominantApp = topApps.firstOrNull()
        // Heuristic: If dominant app is used for more than 120 minutes (2 hours).
        val isAddictive = dominantApp != null && dominantApp.totalMinutes > 120 
        return AppAddictionPattern(
            detected = isAddictive,
            dominantAppPackage = dominantApp?.packageName,
            averageDailyMinutes = dominantApp?.totalMinutes?.toFloat() ?: 0f
        )
    }
}
