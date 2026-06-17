package com.example.addictionreductionapp.data.repository

import com.example.addictionreductionapp.data.analytics.BehavioralIntelligenceEngine
import com.example.addictionreductionapp.data.analytics.FocusScoreEngine
import com.example.addictionreductionapp.data.local.dao.AnalyticsDao
import com.example.addictionreductionapp.data.local.dao.DailyBehaviorSnapshotDao
import com.example.addictionreductionapp.data.local.entities.DailyBehaviorSnapshotEntity
import com.example.addictionreductionapp.data.models.CategoryAnalytics
import com.example.addictionreductionapp.data.models.FocusScoreDetails
import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import javax.inject.Inject
import javax.inject.Singleton
import android.util.Log

@Singleton
class SnapshotReconciliationManager @Inject constructor(
    private val analyticsDao: AnalyticsDao,
    private val snapshotDao: DailyBehaviorSnapshotDao,
    private val focusScoreEngine: FocusScoreEngine,
    private val behaviorEngine: BehavioralIntelligenceEngine
) {
    suspend fun reconcileMissingSnapshots() = withContext(Dispatchers.IO) {
        val missingDates = snapshotDao.getMissingSnapshotDates()
        if (missingDates.isEmpty()) return@withContext

        val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")

        for (dateStr in missingDates) {
            val date = LocalDate.parse(dateStr, formatter)
            val startDate7 = date.minusDays(7).format(formatter)

            // 1. Fetch the metrics for this specific date
            val totalTime = analyticsDao.getDailyScreenTimeForDate(dateStr)
            val totalOpens = analyticsDao.getDailyOpensForDate(dateStr)
            val topApps = analyticsDao.getMostUsedAppsForDate(dateStr)
            val categories = analyticsDao.getCategoryTotalsForDate(dateStr)
            val hourlyUsage = analyticsDao.getHourlyUsageForDate(dateStr)

            // 2. Fetch weekly total (last 7 days leading to this date)
            val weeklyTotal = analyticsDao.getWeeklyUsageTotalForDate(startDate7, dateStr)
            val weeklyAvg = if (weeklyTotal > 0) weeklyTotal / 7 else 0

            // 3. Calculate today's focus score
            val focusScore = focusScoreEngine.calculateScore(totalTime, totalOpens, categories)

            // 4. Calculate historical scores (last 7 days up to this date)
            val dailySummaries = analyticsDao.getDailyScreenTimeSeriesSuspend(startDate7, dateStr)
            val dailyCategories = analyticsDao.getDailyCategoryTotalsSuspend(startDate7, dateStr)
            
            val categoriesByDate = dailyCategories.groupBy { it.date }.mapValues { (_, rows) ->
                rows.map { CategoryAnalytics(it.category, it.totalMinutes) }
            }

            val historicalDetails = dailySummaries.map { daily ->
                val dayCats = categoriesByDate[daily.date] ?: emptyList()
                focusScoreEngine.calculateScore(
                    totalScreenTimeMinutes = daily.totalScreenTimeMinutes,
                    totalOpens = daily.totalOpens,
                    categoryTotals = dayCats
                )
            }

            // Replace the last element (which should be 'dateStr') with our calculated focusScore
            val previousDays = if (historicalDetails.isNotEmpty()) {
                historicalDetails.dropLast(1)
            } else {
                emptyList()
            }
            val recentScores = previousDays + focusScore

            // 5. Generate Snapshot
            val snapshot = behaviorEngine.generateSnapshot(
                usageToday = topApps,
                hourlyUsage = hourlyUsage,
                totalOpens = totalOpens,
                totalScreenTimeMinutes = totalTime,
                weeklyAverageMinutes = weeklyAvg,
                recentScores = recentScores,
                timestamp = System.currentTimeMillis() // use current timestamp for creation
            )

            val entity = DailyBehaviorSnapshotEntity(
                date = dateStr,
                totalScreenTimeMinutes = totalTime,
                totalOpens = totalOpens,
                focusScore = focusScore.score,
                productiveRatio = focusScore.productiveRatio,
                distractionRatio = focusScore.distractionRatio,
                appSwitches = totalOpens,
                overallRiskScore = snapshot.overallRiskScore,
                doomscrollDetected = snapshot.doomscroll.detected,
                compulsiveSwitchingDetected = snapshot.compulsiveSwitching.detected,
                lateNightUsageDetected = snapshot.lateNightUsage.detected,
                relapseDetected = snapshot.relapse.detected,
                createdAt = System.currentTimeMillis()
            )

            // 6. Save
            snapshotDao.insertSnapshot(entity)
        }
    }

    suspend fun rebuildAllSnapshots(context: Context) = withContext(Dispatchers.IO) {
        Log.d("SnapshotRebuild", "Starting rebuild...")
        val allDates = snapshotDao.getAllSnapshotDates()
        if (allDates.isEmpty()) {
            Log.d("SnapshotRebuild", "No existing snapshots found. Finished rebuild.")
            return@withContext
        }

        Log.d("SnapshotRebuild", "Total rows to rebuild: ${allDates.size}")
        val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")
        var rebuiltCount = 0

        for (dateStr in allDates) {
            // Capture old risk score before deletion
            val db = context.openOrCreateDatabase("regain_database", Context.MODE_PRIVATE, null)
            var oldRiskScore = -1f
            try {
                val cursor = db.rawQuery(
                    "SELECT overallRiskScore FROM daily_behavior_snapshots WHERE date = ?",
                    arrayOf(dateStr)
                )
                if (cursor.moveToFirst()) {
                    oldRiskScore = cursor.getFloat(0)
                }
                cursor.close()
            } catch (e: Exception) {
                Log.w("SnapshotRebuild", "Could not read old score for $dateStr: ${e.message}")
            } finally {
                db.close()
            }

            Log.d("SnapshotRebuild", "Rebuilding $dateStr | oldRiskScore=$oldRiskScore")

            // Delete existing row
            snapshotDao.deleteSnapshotByDate(dateStr)

            val date = LocalDate.parse(dateStr, formatter)
            val startDate7 = date.minusDays(7).format(formatter)

            // 1. Fetch the metrics for this specific date
            val totalTime = analyticsDao.getDailyScreenTimeForDate(dateStr)
            val totalOpens = analyticsDao.getDailyOpensForDate(dateStr)
            val topApps = analyticsDao.getMostUsedAppsForDate(dateStr)
            val categories = analyticsDao.getCategoryTotalsForDate(dateStr)
            val hourlyUsage = analyticsDao.getHourlyUsageForDate(dateStr)

            // 2. Fetch weekly total (last 7 days leading to this date)
            val weeklyTotal = analyticsDao.getWeeklyUsageTotalForDate(startDate7, dateStr)
            val weeklyAvg = if (weeklyTotal > 0) weeklyTotal / 7 else 0

            // 3. Calculate today's focus score
            val focusScore = focusScoreEngine.calculateScore(totalTime, totalOpens, categories)

            // 4. Calculate historical scores (last 7 days up to this date)
            val dailySummaries = analyticsDao.getDailyScreenTimeSeriesSuspend(startDate7, dateStr)
            val dailyCategories = analyticsDao.getDailyCategoryTotalsSuspend(startDate7, dateStr)
            
            val categoriesByDate = dailyCategories.groupBy { it.date }.mapValues { (_, rows) ->
                rows.map { CategoryAnalytics(it.category, it.totalMinutes) }
            }

            val historicalDetails = dailySummaries.map { daily ->
                val dayCats = categoriesByDate[daily.date] ?: emptyList()
                focusScoreEngine.calculateScore(
                    totalScreenTimeMinutes = daily.totalScreenTimeMinutes,
                    totalOpens = daily.totalOpens,
                    categoryTotals = dayCats
                )
            }

            // Replace the last element (which should be 'dateStr') with our calculated focusScore
            val previousDays = if (historicalDetails.isNotEmpty()) {
                historicalDetails.dropLast(1)
            } else {
                emptyList()
            }
            val recentScores = previousDays + focusScore

            // 5. Generate Snapshot using new formula
            val snapshot = behaviorEngine.generateSnapshot(
                usageToday = topApps,
                hourlyUsage = hourlyUsage,
                totalOpens = totalOpens,
                totalScreenTimeMinutes = totalTime,
                weeklyAverageMinutes = weeklyAvg,
                recentScores = recentScores,
                timestamp = System.currentTimeMillis()
            )

            val newRiskScore = snapshot.overallRiskScore
            Log.d("SnapshotRebuild", "  $dateStr | oldRiskScore=$oldRiskScore → newRiskScore=$newRiskScore")

            val entity = DailyBehaviorSnapshotEntity(
                date = dateStr,
                totalScreenTimeMinutes = totalTime,
                totalOpens = totalOpens,
                focusScore = focusScore.score,
                productiveRatio = focusScore.productiveRatio,
                distractionRatio = focusScore.distractionRatio,
                appSwitches = totalOpens,
                overallRiskScore = newRiskScore,
                doomscrollDetected = snapshot.doomscroll.detected,
                compulsiveSwitchingDetected = snapshot.compulsiveSwitching.detected,
                lateNightUsageDetected = snapshot.lateNightUsage.detected,
                relapseDetected = snapshot.relapse.detected,
                createdAt = System.currentTimeMillis()
            )

            // 6. Save
            snapshotDao.insertSnapshot(entity)
            rebuiltCount++
        }
        Log.d("SnapshotRebuild", "Finished rebuild. Total rows rebuilt: $rebuiltCount")
    }
}
