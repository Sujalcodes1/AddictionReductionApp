package com.example.addictionreductionapp.data.models

/**
 * Represents the direction a metric is moving over time.
 *
 * - IMPROVING: The metric is moving in a positive direction (e.g. focus score rising).
 * - STABLE: The metric is within a noise threshold — no significant change.
 * - DECLINING: The metric is moving in a negative direction (e.g. risk score rising).
 *
 * // Future ML Feature: TrendDirection serves as a categorical label for supervised learning.
 * // Future Training Input: Can be encoded as an ordinal feature (0=DECLINING, 1=STABLE, 2=IMPROVING).
 */
enum class TrendDirection {
    IMPROVING,
    STABLE,
    DECLINING;

    /**
     * Returns a UI-ready arrow symbol for rendering trend cards and charts.
     * // Future UI Feature: Replace with animated Compose icons.
     */
    fun toArrow(): String = when (this) {
        IMPROVING -> "↑"
        STABLE    -> "→"
        DECLINING -> "↓"
    }

    /**
     * Returns a UI-ready label for accessibility and trend cards.
     */
    fun toLabel(): String = when (this) {
        IMPROVING -> "Improving"
        STABLE    -> "Stable"
        DECLINING -> "Declining"
    }
}

/**
 * Focus score trend comparing two consecutive time windows.
 *
 * // Future ML Feature: Consecutive DECLINING focus trends are a leading indicator of relapse.
 * // Future Training Input: percentageChange is a continuous numerical feature for regression models.
 *
 * @param currentAverage  Average focus score (0–100) in the current window.
 * @param previousAverage Average focus score (0–100) in the previous window.
 * @param percentageChange Signed percentage change; positive = improving.
 * @param trendDirection  Categorical direction derived from the change magnitude.
 */
data class FocusTrend(
    val currentAverage: Float,
    val previousAverage: Float,
    val percentageChange: Float,
    val trendDirection: TrendDirection
)

/**
 * Screen time trend comparing two consecutive time windows.
 *
 * Note: For screen time, IMPROVING means usage is going DOWN (less distraction).
 * DECLINING means usage is going UP (more distraction).
 *
 * // Future ML Feature: Increasing screen time coupled with DECLINING focus = high relapse risk signal.
 * // Future Training Input: currentAverageMinutes is a key regression input for daily usage prediction.
 *
 * @param currentAverageMinutes  Average daily screen time (minutes) in the current window.
 * @param previousAverageMinutes Average daily screen time (minutes) in the previous window.
 * @param percentageChange Signed percentage change; negative = improving (less usage).
 * @param trendDirection  Categorical direction (IMPROVING = less usage, DECLINING = more usage).
 */
data class ScreenTimeTrend(
    val currentAverageMinutes: Float,
    val previousAverageMinutes: Float,
    val percentageChange: Float,
    val trendDirection: TrendDirection
)

/**
 * Risk score trend comparing two consecutive time windows.
 *
 * Note: For risk, IMPROVING means risk is going DOWN.
 * DECLINING means risk is going UP (more dangerous).
 *
 * // Future Relapse Prediction Feature: overallRiskScore trajectory is the primary target variable.
 * // Future Training Input: risk delta over 7-day windows is a key short-term relapse predictor.
 *
 * @param currentRiskAverage  Average overall risk score (0.0–1.0) in the current window.
 * @param previousRiskAverage Average overall risk score (0.0–1.0) in the previous window.
 * @param percentageChange Signed percentage change; negative = improving (less risk).
 * @param trendDirection  Categorical direction (IMPROVING = less risk, DECLINING = more risk).
 */
data class RiskTrend(
    val currentRiskAverage: Float,
    val previousRiskAverage: Float,
    val percentageChange: Float,
    val trendDirection: TrendDirection
)

/**
 * Productivity ratio trend comparing two consecutive time windows.
 *
 * // Future ML Feature: productiveRatio trend helps distinguish intentional usage from compulsive usage.
 * // Future Training Input: productiveRatio is a direct feature for behavioral classification models.
 *
 * @param currentProductiveRatio  Average productive ratio (0.0–1.0) in the current window.
 * @param previousProductiveRatio Average productive ratio (0.0–1.0) in the previous window.
 * @param percentageChange Signed percentage change; positive = improving.
 * @param trendDirection  Categorical direction.
 */
data class ProductivityTrend(
    val currentProductiveRatio: Float,
    val previousProductiveRatio: Float,
    val percentageChange: Float,
    val trendDirection: TrendDirection
)

/**
 * An aggregated summary of all behavioral trends for a specific time window.
 *
 * This is the primary UI-ready model for trend cards, trend arrows, and future dashboards.
 *
 * // Future ML Dataset Source: BehaviorTrendSummary fields map directly to feature vectors.
 * // Future Relapse Prediction Features: All four trend directions encoded together form a
 * //   multi-dimensional behavioral signature usable as a classifier input.
 * // Future Training Inputs: Serialize as a fixed-length float array for model ingestion.
 *
 * @param windowDays     The size of each comparison window (e.g. 7 or 30 days).
 * @param focusTrend       Focus score trend for this window.
 * @param screenTimeTrend  Screen time trend for this window.
 * @param riskTrend        Risk score trend for this window.
 * @param productivityTrend Productivity ratio trend for this window.
 * @param generatedAt      Epoch milliseconds when this summary was generated.
 */
data class BehaviorTrendSummary(
    val windowDays: Int,
    val focusTrend: FocusTrend,
    val screenTimeTrend: ScreenTimeTrend,
    val riskTrend: RiskTrend,
    val productivityTrend: ProductivityTrend,
    val generatedAt: Long = System.currentTimeMillis()
)
