package com.example.addictionreductionapp.data.analytics

import android.app.usage.UsageStatsManager
import android.content.Context
import com.example.addictionreductionapp.data.local.entities.ReductionPlanEntity
import com.example.addictionreductionapp.data.local.entities.DailyBehaviorSnapshotEntity
import com.example.addictionreductionapp.data.local.entities.GoalEntity
import com.example.addictionreductionapp.data.models.ReductionMilestone
import com.example.addictionreductionapp.data.models.RoadmapPlan
import com.example.addictionreductionapp.utils.AppCategoryResolver
import dagger.hilt.android.qualifiers.ApplicationContext
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SmartReductionEngine @Inject constructor(
    @ApplicationContext private val context: Context
) {
    companion object {
        val TARGET_CATEGORIES = listOf("Social", "Entertainment", "Games")
        const val DEFAULT_STEP_DOWN = 10
        const val DEFAULT_FLOOR = 30
        const val DEFAULT_IMPORT_DAYS = 7
    }

    val targetCategories: List<String> get() = TARGET_CATEGORIES

    fun importHistoricalUsage(days: Int = DEFAULT_IMPORT_DAYS): Map<String, Int> {
        val usageStatsManager = context.getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager
        val endTime = System.currentTimeMillis()
        val startTime = endTime - (days * 24L * 60 * 60 * 1000)

        val stats = usageStatsManager.queryUsageStats(
            UsageStatsManager.INTERVAL_DAILY,
            startTime,
            endTime
        )

        val categoryTotals = mutableMapOf<String, Int>()

        for (stat in stats) {
            val category = AppCategoryResolver.resolveCategory(stat.packageName)
            val minutes = (stat.totalTimeInForeground / (1000 * 60)).toInt()
            if (minutes > 0) {
                categoryTotals[category] = (categoryTotals[category] ?: 0) + minutes
            }
        }

        return categoryTotals
    }

    fun computeBaseline(rawTotals: Map<String, Int>, days: Int = DEFAULT_IMPORT_DAYS): Map<String, Int> {
        return rawTotals.mapValues { (_, total) -> total / days.coerceAtLeast(1) }
    }

    suspend fun applyDailyReduction(
        plans: List<ReductionPlanEntity>,
        appLimits: List<com.example.addictionreductionapp.data.local.entities.AppLimitEntity>,
        savePlan: suspend (ReductionPlanEntity) -> Unit,
        saveAppLimit: suspend (com.example.addictionreductionapp.data.local.entities.AppLimitEntity) -> Unit
    ) {
        if (plans.isEmpty()) return

        for (plan in plans) {
            val newTarget = maxOf(
                plan.baselineMinutes - (plan.daysActive * plan.dailyStepDown),
                plan.floorMinutes
            )

            val categoryApps = appLimits.filter {
                AppCategoryResolver.resolveCategory(it.packageName) == plan.category
            }

            for (app in categoryApps) {
                saveAppLimit(app.copy(isSelected = true, limitMinutes = newTarget, isLocked = false))
            }

            savePlan(
                plan.copy(
                    currentTarget = newTarget,
                    daysActive = plan.daysActive + 1,
                    updatedAt = System.currentTimeMillis()
                )
            )
        }
    }

    /**
     * Generates a 4-week adaptive roadmap based on recent behavior and active goals (M5).
     *
     * Reduction rate adjusts by success rate:
     *   >= 80% success → 12% (aggressive)
     *   >= 50% success → 10% (normal)
     *   <  50% success →  5% (gentle)
     *
     * Goal-aware floor: Learning/Career goals allow lower screen-time floors.
     */
    fun generateAdaptiveRoadmap(
        snapshots: List<DailyBehaviorSnapshotEntity>,
        activeGoals: List<GoalEntity>,
        successRate: Float
    ): RoadmapPlan? {
        val recent = snapshots.sortedBy { it.date }.takeLast(7)
        if (recent.size < 3) return null

        val baselineAvg = (recent.sumOf { it.totalScreenTimeMinutes } / recent.size).coerceAtLeast(DEFAULT_FLOOR)

        val adjustedRate = when {
            successRate >= 0.8f -> 0.12f
            successRate >= 0.5f -> 0.10f
            else -> 0.05f
        }

        val hasProductiveGoal = activeGoals.any { it.goalType in listOf("LEARNING", "CAREER") }
        val floor = if (hasProductiveGoal) 20 else DEFAULT_FLOOR

        val today = LocalDate.now()
        val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")

        val milestones = (1..4).map { week ->
            val minWeek = (baselineAvg * (1f - adjustedRate * (week - 1))).toInt().coerceAtLeast(floor)
            val weekStart = today.plusWeeks((week - 1).toLong()).minusDays(today.dayOfWeek.value.toLong() - 1)
            val weekEnd = weekStart.plusDays(6)
            var achieved = 0
            for (s in recent) {
                try {
                    val d = LocalDate.parse(s.date, formatter)
                    if (!d.isBefore(weekStart) && !d.isAfter(weekEnd)) achieved += s.totalScreenTimeMinutes
                } catch (_: Exception) {}
            }
            ReductionMilestone(weekNumber = week, targetMinutes = minWeek, achievedMinutes = achieved,
                startDate = weekStart.format(formatter), endDate = weekEnd.format(formatter), isComplete = weekEnd.isBefore(today))
        }

        return RoadmapPlan(
            baselineDailyAverage = baselineAvg,
            milestones = milestones,
            currentWeek = 1
        )
    }
}
