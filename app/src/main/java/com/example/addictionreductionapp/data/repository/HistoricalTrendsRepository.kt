package com.example.addictionreductionapp.data.repository

import com.example.addictionreductionapp.data.local.entities.DailyBehaviorSnapshotEntity
import com.example.addictionreductionapp.data.models.BehaviorTrendSummary
import com.example.addictionreductionapp.data.models.FocusTrend
import com.example.addictionreductionapp.data.models.ProductivityTrend
import com.example.addictionreductionapp.data.models.RiskTrend
import com.example.addictionreductionapp.data.models.ScreenTimeTrend
import com.example.addictionreductionapp.data.models.TrendDirection
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.flowOn
import javax.inject.Inject
import kotlin.math.abs

/**
 * HistoricalTrendsRepository
 *
 * Reads [DailyBehaviorSnapshotEntity] rows from [DailyBehaviorSnapshotRepository]
 * and transforms them into trend models for UI consumption and future ML ingestion.
 *
 * ## Trend Calculation Strategy
 * For a given window (7 or 30 days) we split the available snapshots into two halves:
 *   - Current window  = most recent N days
 *   - Previous window = N days immediately before that
 *
 * We compute the per-metric average for each window and derive:
 *   1. Percentage change  (signed; positive = metric increased)
 *   2. Trend direction    (IMPROVING / STABLE / DECLINING)
 *
 * The "positive" direction differs by metric:
 *   - Focus Score    : higher is IMPROVING
 *   - Screen Time    : lower  is IMPROVING
 *   - Risk Score     : lower  is IMPROVING
 *   - Productivity   : higher is IMPROVING
 *
 * // Future ML Dataset Source: All computed averages and deltas feed directly into feature vectors.
 * // Future Relapse Prediction Features: trend windows expose early warning signals before a relapse event.
 * // Future Training Inputs: percentageChange + trendDirection per metric form a 8-dimensional snapshot.
 */
class HistoricalTrendsRepository @Inject constructor(
    private val snapshotRepository: DailyBehaviorSnapshotRepository
) {

    // ── Stability threshold ──────────────────────────────────────────────────
    // A change of less than STABLE_THRESHOLD_PERCENT is considered STABLE (noise floor).
    private val STABLE_THRESHOLD_PERCENT = 5f

    // ── Focus Trend ──────────────────────────────────────────────────────────

    /**
     * Calculates focus score trend for [windowDays] vs the previous [windowDays].
     * Focus: higher score = IMPROVING.
     */
    fun calculateFocusTrend(windowDays: Int = 7): Flow<FocusTrend?> {
        val snapshots = when (windowDays) {
            30   -> snapshotRepository.getHistoricalSnapshots(30)
            else -> snapshotRepository.getHistoricalSnapshots(7)
        }

        return snapshots
            .map { rows -> computeFocusTrend(rows, windowDays) }
            .flowOn(Dispatchers.Default)
    }

    private fun computeFocusTrend(
        rows: List<DailyBehaviorSnapshotEntity>,
        windowDays: Int
    ): FocusTrend? {
        val (current, previous) = splitWindows(rows, windowDays) ?: return null

        val currentAvg  = current.map  { it.focusScore.toFloat() }.average().toFloat()
        val previousAvg = previous.map { it.focusScore.toFloat() }.average().toFloat()

        val pctChange   = percentageChange(previousAvg, currentAvg)
        // Focus: positive change = IMPROVING
        val direction   = higherIsBetterDirection(pctChange)

        return FocusTrend(
            currentAverage    = currentAvg,
            previousAverage   = previousAvg,
            percentageChange  = pctChange,
            trendDirection    = direction
        )
    }

    // ── Screen Time Trend ────────────────────────────────────────────────────

    /**
     * Calculates screen time trend for [windowDays] vs the previous [windowDays].
     * Screen Time: lower usage = IMPROVING.
     */
    fun calculateScreenTimeTrend(windowDays: Int = 7): Flow<ScreenTimeTrend?> {
        val snapshots = when (windowDays) {
            30   -> snapshotRepository.getHistoricalSnapshots(30)
            else -> snapshotRepository.getHistoricalSnapshots(7)
        }

        return snapshots
            .map { rows -> computeScreenTimeTrend(rows, windowDays) }
            .flowOn(Dispatchers.Default)
    }

    private fun computeScreenTimeTrend(
        rows: List<DailyBehaviorSnapshotEntity>,
        windowDays: Int
    ): ScreenTimeTrend? {
        val (current, previous) = splitWindows(rows, windowDays) ?: return null

        val currentAvg  = current.map  { it.totalScreenTimeMinutes.toFloat() }.average().toFloat()
        val previousAvg = previous.map { it.totalScreenTimeMinutes.toFloat() }.average().toFloat()

        val pctChange   = percentageChange(previousAvg, currentAvg)
        // Screen time: negative change (less usage) = IMPROVING
        val direction   = lowerIsBetterDirection(pctChange)

        return ScreenTimeTrend(
            currentAverageMinutes  = currentAvg,
            previousAverageMinutes = previousAvg,
            percentageChange       = pctChange,
            trendDirection         = direction
        )
    }

    // ── Risk Trend ───────────────────────────────────────────────────────────

    /**
     * Calculates overall risk score trend for [windowDays] vs the previous [windowDays].
     * Risk Score: lower risk = IMPROVING.
     *
     * // Future Relapse Prediction Feature: DECLINING risk trend (risk going up) triggers model inference.
     */
    fun calculateRiskTrend(windowDays: Int = 7): Flow<RiskTrend?> {
        val snapshots = when (windowDays) {
            30   -> snapshotRepository.getHistoricalSnapshots(30)
            else -> snapshotRepository.getHistoricalSnapshots(7)
        }

        return snapshots
            .map { rows -> computeRiskTrend(rows, windowDays) }
            .flowOn(Dispatchers.Default)
    }

    private fun computeRiskTrend(
        rows: List<DailyBehaviorSnapshotEntity>,
        windowDays: Int
    ): RiskTrend? {
        val (current, previous) = splitWindows(rows, windowDays) ?: return null

        val currentAvg  = current.map  { it.overallRiskScore }.average().toFloat()
        val previousAvg = previous.map { it.overallRiskScore }.average().toFloat()

        val pctChange   = percentageChange(previousAvg, currentAvg)
        // Risk: negative change (lower risk) = IMPROVING
        val direction   = lowerIsBetterDirection(pctChange)

        return RiskTrend(
            currentRiskAverage  = currentAvg,
            previousRiskAverage = previousAvg,
            percentageChange    = pctChange,
            trendDirection      = direction
        )
    }

    // ── Productivity Trend ───────────────────────────────────────────────────

    /**
     * Calculates productive ratio trend for [windowDays] vs the previous [windowDays].
     * Productivity: higher ratio = IMPROVING.
     *
     * // Future Training Input: productiveRatio delta over time is a strong engagement signal.
     */
    fun calculateProductivityTrend(windowDays: Int = 7): Flow<ProductivityTrend?> {
        val snapshots = when (windowDays) {
            30   -> snapshotRepository.getHistoricalSnapshots(30)
            else -> snapshotRepository.getHistoricalSnapshots(7)
        }

        return snapshots
            .map { rows -> computeProductivityTrend(rows, windowDays) }
            .flowOn(Dispatchers.Default)
    }

    private fun computeProductivityTrend(
        rows: List<DailyBehaviorSnapshotEntity>,
        windowDays: Int
    ): ProductivityTrend? {
        val (current, previous) = splitWindows(rows, windowDays) ?: return null

        val currentAvg  = current.map  { it.productiveRatio }.average().toFloat()
        val previousAvg = previous.map { it.productiveRatio }.average().toFloat()

        val pctChange   = percentageChange(previousAvg, currentAvg)
        // Productivity: positive change = IMPROVING
        val direction   = higherIsBetterDirection(pctChange)

        return ProductivityTrend(
            currentProductiveRatio  = currentAvg,
            previousProductiveRatio = previousAvg,
            percentageChange        = pctChange,
            trendDirection          = direction
        )
    }

    // ── Aggregate Summary ────────────────────────────────────────────────────

    /**
     * Generates a full [BehaviorTrendSummary] for [windowDays] vs the previous window.
     *
     * Returns null if there are insufficient snapshots (need at least 2 × windowDays rows).
     *
     * // Future ML Dataset Source: generateBehaviorTrendSummary output is the primary
     * //   feature extraction point; call once per day and serialize the result.
     */
    fun generateBehaviorTrendSummary(windowDays: Int = 7): Flow<BehaviorTrendSummary?> {
        val snapshots = when (windowDays) {
            30   -> snapshotRepository.getHistoricalSnapshots(30)
            else -> snapshotRepository.getHistoricalSnapshots(7)
        }

        return snapshots
            .map { rows ->
                val (current, previous) = splitWindows(rows, windowDays) ?: return@map null

                val focusTrend = computeFocusTrend(rows, windowDays) ?: return@map null
                val screenTrend = computeScreenTimeTrend(rows, windowDays) ?: return@map null
                val riskTrend = computeRiskTrend(rows, windowDays) ?: return@map null
                val productivityTrend = computeProductivityTrend(rows, windowDays) ?: return@map null

                BehaviorTrendSummary(
                    windowDays        = windowDays,
                    focusTrend        = focusTrend,
                    screenTimeTrend   = screenTrend,
                    riskTrend         = riskTrend,
                    productivityTrend = productivityTrend,
                    generatedAt       = System.currentTimeMillis()
                )
            }
            .flowOn(Dispatchers.Default)
    }

    // ── Internal helpers ─────────────────────────────────────────────────────

    /**
     * Splits [rows] (sorted descending by date — newest first from DAO) into two equal
     * windows of [windowDays].
     *
     * Returns Pair(current, previous) where:
     *   - current  = rows[0 ..< windowDays]          (most recent days)
     *   - previous = rows[windowDays ..< 2*windowDays] (the prior window)
     *
     * Returns null if there are fewer than 2 × windowDays rows.
     */
    private fun splitWindows(
        rows: List<DailyBehaviorSnapshotEntity>,
        windowDays: Int
    ): Pair<List<DailyBehaviorSnapshotEntity>, List<DailyBehaviorSnapshotEntity>>? {
        val needed = windowDays * 2
        if (rows.size < needed) return null

        val current  = rows.take(windowDays)
        val previous = rows.drop(windowDays).take(windowDays)
        return Pair(current, previous)
    }

    /**
     * Computes signed percentage change from [from] to [to].
     * Returns 0 when [from] is zero to avoid division by zero.
     */
    private fun percentageChange(from: Float, to: Float): Float {
        if (from == 0f) return 0f
        return ((to - from) / from) * 100f
    }

    /**
     * Maps a [percentageChange] to [TrendDirection] for metrics where HIGHER is better
     * (e.g. focus score, productivity ratio).
     */
    private fun higherIsBetterDirection(percentageChange: Float): TrendDirection {
        return when {
            percentageChange > STABLE_THRESHOLD_PERCENT  -> TrendDirection.IMPROVING
            percentageChange < -STABLE_THRESHOLD_PERCENT -> TrendDirection.DECLINING
            else                                          -> TrendDirection.STABLE
        }
    }

    /**
     * Maps a [percentageChange] to [TrendDirection] for metrics where LOWER is better
     * (e.g. screen time, risk score).
     */
    private fun lowerIsBetterDirection(percentageChange: Float): TrendDirection {
        return when {
            percentageChange < -STABLE_THRESHOLD_PERCENT -> TrendDirection.IMPROVING
            percentageChange > STABLE_THRESHOLD_PERCENT  -> TrendDirection.DECLINING
            else                                          -> TrendDirection.STABLE
        }
    }
}
