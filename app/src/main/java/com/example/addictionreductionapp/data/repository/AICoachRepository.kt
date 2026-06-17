package com.example.addictionreductionapp.data.repository

import com.example.addictionreductionapp.data.models.BehaviorPatternSummary
import com.example.addictionreductionapp.data.models.BehaviorTrendSummary
import com.example.addictionreductionapp.data.models.CoachCategory
import com.example.addictionreductionapp.data.models.CoachInsight
import com.example.addictionreductionapp.data.models.CoachPriority
import com.example.addictionreductionapp.data.models.RelapsePrediction
import com.example.addictionreductionapp.data.models.RelapseRiskLevel
import com.example.addictionreductionapp.data.models.TrendDirection
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import javax.inject.Inject
import javax.inject.Singleton

/**
 * AICoachRepository
 *
 * Combines outputs from [HistoricalTrendsRepository], [BehaviorPatternRepository], and
 * [RelapsePredictionRepository] to produce a ranked, actionable [List<CoachInsight>].
 *
 * No Room schema changes. No UI changes. Read-only consumption of existing repositories.
 *
 * ## Insight Rule Reference
 *
 * Rule 1 — riskLevel == CRITICAL         → "High relapse risk detected"    CRITICAL  RELAPSE
 * Rule 2 — riskLevel == HIGH             → "Relapse risk increasing"        HIGH      RELAPSE
 * Rule 3 — focusTrend.DECLINING          → "Focus is trending downward"     HIGH      FOCUS
 * Rule 4 — screenTimeTrend.DECLINING     → "Screen time is increasing"      HIGH      DISTRACTION
 * Rule 5 — productivityTrend.DECLINING   → "Productivity is decreasing"     HIGH      PRODUCTIVITY
 * Rule 6 — lateNight.detected            → "Late-night usage may affect…"   HIGH      SLEEP
 * Rule 7 — weekendUsage.detected         → "Weekend binge pattern detected" MEDIUM    APP_USAGE
 * Rule 8 — compulsiveSwitching.detected  → "Frequent app switching detected"HIGH      DISTRACTION
 * Rule 9 — recovery.detected             → "Recovery progress detected"     LOW       FOCUS
 *
 * Output is sorted CRITICAL → HIGH → MEDIUM → LOW via CoachPriority.ordinal.
 *
 * // Future ML Training Input: CoachInsight list can be serialised alongside
 * // BehaviorTrendSummary features to form labelled training examples.
 */
@Singleton
class AICoachRepository @Inject constructor(
    private val historicalTrendsRepository: HistoricalTrendsRepository,
    private val behaviorPatternRepository: BehaviorPatternRepository,
    private val relapsePredictionRepository: RelapsePredictionRepository
) {

    /**
     * Reactive stream of ranked [CoachInsight] objects.
     *
     * Combines three upstream flows:
     *   - [HistoricalTrendsRepository.generateBehaviorTrendSummary] (7-day window, nullable)
     *   - [BehaviorPatternRepository.generateBehaviorPatternSummary]
     *   - [RelapsePredictionRepository.generateRelapsePrediction]
     *
     * Emits a new list whenever any upstream source emits.
     */
    fun generateInsights(): Flow<List<CoachInsight>> = combine(
        historicalTrendsRepository.generateBehaviorTrendSummary(windowDays = 7),
        behaviorPatternRepository.generateBehaviorPatternSummary(),
        relapsePredictionRepository.generateRelapsePrediction()
    ) { trendSummary, patternSummary, prediction ->
        buildInsights(trendSummary, patternSummary, prediction)
    }

    // ── Core insight builder ──────────────────────────────────────────────────

    private fun buildInsights(
        trendSummary: BehaviorTrendSummary?,   // null when < 14 snapshot rows exist
        patternSummary: BehaviorPatternSummary,
        prediction: RelapsePrediction
    ): List<CoachInsight> {
        val insights = mutableListOf<CoachInsight>()

        // ── RULES 1–2: Relapse prediction ──────────────────────────────────────
        // prediction.reasons is List<String>; always present (may be ["All metrics stable"]).
        when (prediction.riskLevel) {
            RelapseRiskLevel.CRITICAL -> insights += CoachInsight(
                title = "High relapse risk detected",
                description = prediction.reasons.joinToString(". "),
                category = CoachCategory.RELAPSE,
                priority = CoachPriority.CRITICAL
            )
            RelapseRiskLevel.HIGH -> insights += CoachInsight(
                title = "Relapse risk increasing",
                description = "Your current behavior patterns suggest an elevated risk of relapse.",
                category = CoachCategory.RELAPSE,
                priority = CoachPriority.HIGH
            )
            RelapseRiskLevel.MEDIUM,
            RelapseRiskLevel.LOW -> { /* no relapse insight for low/medium risk */ }
        }

        // ── RULES 3–5: Historical trend insights (null-safe) ───────────────────
        // TrendDirection.DECLINING means the outcome is worsening for each metric:
        //   focusTrend.DECLINING     → focus score fell    (bad)
        //   screenTimeTrend.DECLINING → screen time rose   (bad — lower is better)
        //   productivityTrend.DECLINING → productivity fell (bad)
        if (trendSummary != null) {
            // Rule 3 — Focus declining
            if (trendSummary.focusTrend.trendDirection == TrendDirection.DECLINING) {
                insights += CoachInsight(
                    title = "Focus is trending downward",
                    description = "Your focus sessions have been shorter and less consistent recently.",
                    category = CoachCategory.FOCUS,
                    priority = CoachPriority.HIGH
                )
            }

            // Rule 4 — Screen time rising (DECLINING means raw value went UP = bad)
            if (trendSummary.screenTimeTrend.trendDirection == TrendDirection.DECLINING) {
                insights += CoachInsight(
                    title = "Screen time is increasing",
                    description = "Your daily screen time has risen above your recent baseline.",
                    category = CoachCategory.DISTRACTION,
                    priority = CoachPriority.HIGH
                )
            }

            // Rule 5 — Productivity declining
            if (trendSummary.productivityTrend.trendDirection == TrendDirection.DECLINING) {
                insights += CoachInsight(
                    title = "Productivity is decreasing",
                    description = "Tracked productivity scores have fallen over the past week.",
                    category = CoachCategory.PRODUCTIVITY,
                    priority = CoachPriority.HIGH
                )
            }
        }

        // ── RULES 6–9: Behavior pattern insights ──────────────────────────────
        // Field names verified against BehaviorPatternSummary:
        //   lateNight           → LateNightPattern.detected
        //   weekendUsage        → WeekendUsagePattern.detected
        //   compulsiveSwitching → CompulsiveSwitchingPattern.detected
        //   recovery            → RecoveryPattern.detected

        // Rule 6 — Late night usage
        if (patternSummary.lateNight.detected) {
            insights += CoachInsight(
                title = "Late-night usage may affect sleep",
                description = "You have been using your device after 10 PM on multiple nights.",
                category = CoachCategory.SLEEP,
                priority = CoachPriority.HIGH
            )
        }

        // Rule 7 — Weekend binge
        if (patternSummary.weekendUsage.detected) {
            insights += CoachInsight(
                title = "Weekend binge pattern detected",
                description = "Your weekend usage is significantly higher than your weekday average.",
                category = CoachCategory.APP_USAGE,
                priority = CoachPriority.MEDIUM
            )
        }

        // Rule 8 — Compulsive switching
        if (patternSummary.compulsiveSwitching.detected) {
            insights += CoachInsight(
                title = "Frequent app switching detected",
                description = "You are switching between apps more than 30 times per hour on average.",
                category = CoachCategory.DISTRACTION,
                priority = CoachPriority.HIGH
            )
        }

        // Rule 9 — Recovery progress (positive signal)
        if (patternSummary.recovery.detected) {
            insights += CoachInsight(
                title = "Recovery progress detected",
                description = "Your behavior trends show improvement. Keep up the consistency.",
                category = CoachCategory.FOCUS,
                priority = CoachPriority.LOW
            )
        }

        // ── Sort CRITICAL → HIGH → MEDIUM → LOW via ordinal ───────────────────
        // CoachPriority ordinal: CRITICAL=0, HIGH=1, MEDIUM=2, LOW=3
        return insights.sortedBy { it.priority.ordinal }
    }
}
