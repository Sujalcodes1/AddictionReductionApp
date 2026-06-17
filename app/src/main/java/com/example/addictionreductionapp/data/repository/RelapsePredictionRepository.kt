package com.example.addictionreductionapp.data.repository

import com.example.addictionreductionapp.data.models.BehaviorPatternSummary
import com.example.addictionreductionapp.data.models.BehaviorTrendSummary
import com.example.addictionreductionapp.data.models.RelapsePrediction
import com.example.addictionreductionapp.data.models.RelapseRiskLevel
import com.example.addictionreductionapp.data.models.TrendDirection
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOn
import javax.inject.Inject

/**
 * Deterministic relapse prediction engine.
 *
 * Combines outputs from [HistoricalTrendsRepository] (7-day window) and
 * [BehaviorPatternRepository] to produce a [RelapsePrediction] with:
 *   - Risk level (LOW / MEDIUM / HIGH / CRITICAL)
 *   - Confidence score (0.0–1.0)
 *   - Human-readable reasons list
 *   - Actionable recommendation string
 *
 * ## Risk Classification Rules
 *
 * CRITICAL — all of the following:
 *   Focus trend DECLINING AND Risk trend DECLINING AND Compulsive switching detected
 *   OR 3+ high-risk patterns simultaneously active
 *
 * HIGH — any of the following:
 *   2+ negative trends (DECLINING)
 *   OR FocusDecayPattern detected (3+ consecutive focus drops)
 *   OR CompulsiveSwitchingPattern detected (4+ days in last 7)
 *
 * MEDIUM — any of the following:
 *   Exactly 1 negative trend
 *   OR exactly 1 detected behavior pattern (weekend/lateNight/appAddiction)
 *
 * LOW — none of the above:
 *   No negative patterns, stable or improving trends
 *
 * ## Confidence Calculation
 *
 * Base confidence starts at 0.3 (minimum — insufficient data still yields a prediction).
 * Boosted by:
 *   +0.3 if trend data is available (non-null BehaviorTrendSummary)
 *   +0.2 if pattern data has sufficient history (patternSummary is non-trivial)
 *   +0.1 per each additional corroborating signal (max +0.2)
 * Capped at 1.0.
 */
class RelapsePredictionRepository @Inject constructor(
    private val historicalTrendsRepository: HistoricalTrendsRepository,
    private val behaviorPatternRepository: BehaviorPatternRepository
) {

    /**
     * Generates a [RelapsePrediction] by combining 7-day trend data with behavior patterns.
     *
     * Returns a valid prediction even when historical data is insufficient — in that case
     * the risk defaults to LOW with low confidence and an explanatory reason.
     */
    fun generateRelapsePrediction(): Flow<RelapsePrediction> {
        val trendsFlow = historicalTrendsRepository.generateBehaviorTrendSummary(windowDays = 7)
        val patternsFlow = behaviorPatternRepository.generateBehaviorPatternSummary()

        return combine(trendsFlow, patternsFlow) { trendSummary, patternSummary ->
            buildPrediction(trendSummary, patternSummary)
        }.flowOn(Dispatchers.IO)
    }

    // ── Core prediction builder ──────────────────────────────────────────────

    private fun buildPrediction(
        trendSummary: BehaviorTrendSummary?,
        patternSummary: BehaviorPatternSummary
    ): RelapsePrediction {
        val reasons = mutableListOf<String>()

        // ── Count negative trends ────────────────────────────────────────────
        val negativeTrendCount = countNegativeTrends(trendSummary, reasons)

        // ── Count detected behavior patterns ─────────────────────────────────
        val detectedPatternCount = countDetectedPatterns(patternSummary, reasons)

        // ── Extract key signals for CRITICAL check ───────────────────────────
        val focusDeclining = trendSummary?.focusTrend?.trendDirection == TrendDirection.DECLINING
        val riskDeclining = trendSummary?.riskTrend?.trendDirection == TrendDirection.DECLINING
        val compulsiveDetected = patternSummary.compulsiveSwitching.detected
        val focusDecayDetected = patternSummary.focusDecay.detected

        // ── Classify risk level ──────────────────────────────────────────────
        val riskLevel = classifyRisk(
            negativeTrendCount = negativeTrendCount,
            detectedPatternCount = detectedPatternCount,
            focusDeclining = focusDeclining,
            riskDeclining = riskDeclining,
            compulsiveDetected = compulsiveDetected,
            focusDecayDetected = focusDecayDetected
        )

        // ── Add cross-signal reasons ─────────────────────────────────────────
        addCrossSignalReasons(trendSummary, patternSummary, reasons)

        // ── Handle no-data case ──────────────────────────────────────────────
        if (trendSummary == null && detectedPatternCount == 0) {
            reasons.add("Insufficient historical data for high-confidence prediction.")
        }

        // ── Confidence ───────────────────────────────────────────────────────
        val confidence = calculateConfidence(
            hasTrends = trendSummary != null,
            negativeTrendCount = negativeTrendCount,
            detectedPatternCount = detectedPatternCount
        )

        // ── Recommendation ───────────────────────────────────────────────────
        val recommendation = recommendationFor(riskLevel)

        // Ensure at least one reason exists
        if (reasons.isEmpty()) {
            reasons.add("All metrics are stable or improving.")
        }

        return RelapsePrediction(
            riskLevel = riskLevel,
            confidenceScore = confidence,
            reasons = reasons.toList(),
            recommendation = recommendation
        )
    }

    // ── Risk classification ──────────────────────────────────────────────────

    private fun classifyRisk(
        negativeTrendCount: Int,
        detectedPatternCount: Int,
        focusDeclining: Boolean,
        riskDeclining: Boolean,
        compulsiveDetected: Boolean,
        focusDecayDetected: Boolean
    ): RelapseRiskLevel {

        // CRITICAL: focus declining AND risk declining AND compulsive switching,
        //           OR 3+ high-risk patterns simultaneously.
        val criticalByTrends = focusDeclining && riskDeclining && compulsiveDetected
        val criticalByPatterns = detectedPatternCount >= 3
        if (criticalByTrends || criticalByPatterns) {
            return RelapseRiskLevel.CRITICAL
        }

        // HIGH: 2+ negative trends, OR focusDecay detected, OR compulsive switching detected.
        if (negativeTrendCount >= 2 || focusDecayDetected || compulsiveDetected) {
            return RelapseRiskLevel.HIGH
        }

        // MEDIUM: exactly 1 negative trend OR exactly 1 detected pattern.
        if (negativeTrendCount == 1 || detectedPatternCount >= 1) {
            return RelapseRiskLevel.MEDIUM
        }

        // LOW: nothing flagged.
        return RelapseRiskLevel.LOW
    }

    // ── Signal counting ──────────────────────────────────────────────────────

    /**
     * Counts how many of the 4 trend directions are DECLINING and appends reasons.
     */
    private fun countNegativeTrends(
        trendSummary: BehaviorTrendSummary?,
        reasons: MutableList<String>
    ): Int {
        if (trendSummary == null) return 0

        var count = 0

        if (trendSummary.focusTrend.trendDirection == TrendDirection.DECLINING) {
            count++
            reasons.add("Focus score has declined over the past ${trendSummary.windowDays} days.")
        }
        if (trendSummary.riskTrend.trendDirection == TrendDirection.DECLINING) {
            count++
            reasons.add("Overall risk score is trending upward.")
        }
        if (trendSummary.screenTimeTrend.trendDirection == TrendDirection.DECLINING) {
            count++
            reasons.add("Screen time is increasing compared to the previous week.")
        }
        if (trendSummary.productivityTrend.trendDirection == TrendDirection.DECLINING) {
            count++
            reasons.add("Productivity ratio is declining.")
        }

        return count
    }

    /**
     * Counts how many behavior patterns are actively detected and appends reasons.
     */
    private fun countDetectedPatterns(
        patternSummary: BehaviorPatternSummary,
        reasons: MutableList<String>
    ): Int {
        var count = 0

        if (patternSummary.weekendUsage.detected) {
            count++
            reasons.add(
                "Weekend screen time is ${String.format("%.0f", patternSummary.weekendUsage.percentageIncrease)}% higher than weekdays."
            )
        }
        if (patternSummary.lateNight.detected) {
            count++
            reasons.add(
                "Late night usage detected on ${patternSummary.lateNight.daysDetected} of the last 7 days."
            )
        }
        if (patternSummary.focusDecay.detected) {
            count++
            reasons.add(
                "Focus score has declined for ${patternSummary.focusDecay.consecutiveDeclines} consecutive days."
            )
        }
        if (patternSummary.compulsiveSwitching.detected) {
            count++
            reasons.add(
                "Compulsive switching behavior detected on ${patternSummary.compulsiveSwitching.daysDetected} of the last 7 days."
            )
        }
        if (patternSummary.appAddiction.detected) {
            count++
            reasons.add(
                "Dominant app usage exceeds 2 hours daily (${patternSummary.appAddiction.dominantAppPackage ?: "unknown"})."
            )
        }

        return count
    }

    /**
     * Adds compound reasons when multiple correlated signals fire simultaneously.
     */
    private fun addCrossSignalReasons(
        trendSummary: BehaviorTrendSummary?,
        patternSummary: BehaviorPatternSummary,
        reasons: MutableList<String>
    ) {
        if (trendSummary == null) return

        // Screen time increasing while productivity is decreasing
        val screenTimeUp = trendSummary.screenTimeTrend.trendDirection == TrendDirection.DECLINING
        val productivityDown = trendSummary.productivityTrend.trendDirection == TrendDirection.DECLINING
        if (screenTimeUp && productivityDown) {
            reasons.add("Screen time is increasing while productivity is decreasing.")
        }

        // Focus declining with late night usage
        val focusDown = trendSummary.focusTrend.trendDirection == TrendDirection.DECLINING
        if (focusDown && patternSummary.lateNight.detected) {
            reasons.add("Focus decline may be linked to disrupted sleep from late night usage.")
        }
    }

    // ── Confidence calculation ───────────────────────────────────────────────

    /**
     * Computes a 0.0–1.0 confidence score.
     *
     * Base: 0.3 (always returns a prediction, even with no data).
     * +0.3 if trend data exists (sufficient historical snapshots).
     * +0.2 if at least 1 pattern was detected (pattern engine had enough data to evaluate).
     * +0.1 per additional corroborating signal (max +0.2 bonus).
     */
    private fun calculateConfidence(
        hasTrends: Boolean,
        negativeTrendCount: Int,
        detectedPatternCount: Int
    ): Float {
        var confidence = 0.3f

        if (hasTrends) confidence += 0.3f
        if (detectedPatternCount > 0) confidence += 0.2f

        // Corroboration bonus: each additional signal beyond the first adds certainty.
        val totalSignals = negativeTrendCount + detectedPatternCount
        val corroborationBonus = ((totalSignals - 1).coerceAtLeast(0) * 0.1f).coerceAtMost(0.2f)
        confidence += corroborationBonus

        return confidence.coerceIn(0f, 1f)
    }

    // ── Recommendations ──────────────────────────────────────────────────────

    private fun recommendationFor(riskLevel: RelapseRiskLevel): String = when (riskLevel) {
        RelapseRiskLevel.LOW      -> "Maintain current habits."
        RelapseRiskLevel.MEDIUM   -> "Monitor usage and reduce distractions."
        RelapseRiskLevel.HIGH     -> "Intervention recommended. Review top distracting apps."
        RelapseRiskLevel.CRITICAL -> "High relapse risk detected. Immediate action recommended."
    }
}
