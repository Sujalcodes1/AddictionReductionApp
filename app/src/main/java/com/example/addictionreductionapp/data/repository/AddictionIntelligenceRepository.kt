package com.example.addictionreductionapp.data.repository

import com.example.addictionreductionapp.data.analytics.FocusScoreEngine
import com.example.addictionreductionapp.data.models.AddictionLevel
import com.example.addictionreductionapp.data.models.AddictionProfile
import com.example.addictionreductionapp.data.models.BehaviorPatternSummary
import com.example.addictionreductionapp.data.models.BehaviorTrendSummary
import com.example.addictionreductionapp.data.models.RelapsePrediction
import com.example.addictionreductionapp.data.models.RelapseRiskLevel
import com.example.addictionreductionapp.data.models.RecoveryTrend
import com.example.addictionreductionapp.data.models.TrendDirection
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import javax.inject.Inject
import javax.inject.Singleton

/**
 * AddictionIntelligenceRepository
 *
 * Thin aggregation layer — reads outputs from existing engines/repositories and
 * combines them into a single [AddictionProfile] for dashboard consumption.
 *
 * ## Step 0 Audit result — addictionScore path
 *
 * BehavioralIntelligenceEngine EXISTS but does NOT expose a standalone composite
 * risk score as a Flow.  Its [BehavioralIntelligenceEngine.generateSnapshot] requires
 * live daily metrics (hourlyUsage, weeklyAverageMinutes, etc.) that are outside this
 * repository's data contract.  Injecting it here would duplicate DashboardViewModel's
 * existing pipeline.
 *
 * Decision: use the MANUAL weighted formula.
 *
 * ## addictionScore weighted formula (pending product sign-off on weights)
 *
 *   40% → relapse severity   (CRITICAL=100, HIGH=75, MEDIUM=45, LOW=15)
 *   30% → inverse focus      (100 − focusScore)
 *   20% → declining trends   (DECLINING focus/screenTime/productivity ÷ 3 × 100)
 *   10% → pattern flags      (lateNight + compulsiveSwitching + weekendUsage ÷ 3 × 100)
 *
 * ## Constraints (not violated)
 *  - No Room @Entity, @Database, or @Migration.
 *  - FocusScoreEngine internals untouched.
 *  - All 7 existing repos/engines unmodified.
 *  - No blocking calls on Main thread (flowOn(Dispatchers.Default)).
 */
@Singleton
class AddictionIntelligenceRepository @Inject constructor(
    private val analyticsRepository: AnalyticsRepository,
    private val focusScoreEngine: FocusScoreEngine,
    private val historicalTrendsRepository: HistoricalTrendsRepository,
    private val behaviorPatternRepository: BehaviorPatternRepository,
    private val relapsePredictionRepository: RelapsePredictionRepository
) {

    /**
     * Emits a new [AddictionProfile] whenever any upstream flow emits a new value.
     * Computation is kept off the Main thread via [flowOn(Dispatchers.Default)].
     */
    fun generateAddictionProfile(): Flow<AddictionProfile> {
        val today = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd"))

        // ── Focus score ───────────────────────────────────────────────────────
        // FocusScoreEngine.calculateScore() is a pure function, not a Flow.
        // Combine the three AnalyticsRepository flows that feed it, then call
        // calculateScore() inside the combine lambda.
        val focusScoreFlow: Flow<Int> = combine(
            analyticsRepository.getTotalScreenTimeToday(today),   // Flow<Int?>
            analyticsRepository.getTotalOpensToday(today),        // Flow<Int?>
            analyticsRepository.getCategoryTotalsToday(today)     // Flow<List<CategoryAnalytics>>
        ) { screenTime, opens, categories ->
            focusScoreEngine.calculateScore(
                totalScreenTimeMinutes = screenTime ?: 0,
                totalOpens             = opens ?: 0,
                categoryTotals         = categories
            ).score  // Int, 0–100
        }

        // ── Trend summary ─────────────────────────────────────────────────────
        // Nullable: returns null when < 14 snapshot rows exist.
        val trendsFlow: Flow<BehaviorTrendSummary?> =
            historicalTrendsRepository.generateBehaviorTrendSummary(windowDays = 7)

        // ── Behavior patterns ─────────────────────────────────────────────────
        // Always non-null.
        val patternsFlow: Flow<BehaviorPatternSummary> =
            behaviorPatternRepository.generateBehaviorPatternSummary()

        // ── Relapse prediction ────────────────────────────────────────────────
        // Always non-null (returns LOW risk + low confidence when data is insufficient).
        val predictionFlow: Flow<RelapsePrediction> =
            relapsePredictionRepository.generateRelapsePrediction()

        // ── Most-used app ─────────────────────────────────────────────────────
        // AnalyticsRepository.getMostUsedAppsToday() returns List<AppUsageSummary>
        // ordered by totalMinutes descending (DAO ORDER BY clause).
        // We take the first element's appName; null if the list is empty.
        val mostUsedAppFlow: Flow<String?> =
            analyticsRepository.getMostUsedAppsToday(today)
                .map { apps -> apps.firstOrNull()?.appName }

        // ── Most-used category ────────────────────────────────────────────────
        // AnalyticsRepository.getCategoryTotalsToday() returns List<CategoryAnalytics>.
        // "Most used" = highest totalMinutes — computed here by max(), which is
        // aggregation of an existing list, not duplicate scoring logic.
        val mostUsedCategoryFlow: Flow<String?> =
            analyticsRepository.getCategoryTotalsToday(today)
                .map { cats -> cats.maxByOrNull { it.totalMinutes }?.category }

        // ── Combine all 5 upstreams into a sealed holder, then map to profile ─
        // kotlinx combine() has overloads up to 5 typed params; beyond that we
        // use a two-stage combine to stay within the typed API.
        val stage1: Flow<Stage1Data> = combine(
            focusScoreFlow,
            trendsFlow,
            patternsFlow,
            predictionFlow,
            mostUsedAppFlow
        ) { focus, trends, patterns, prediction, mostApp ->
            Stage1Data(focus, trends, patterns, prediction, mostApp)
        }

        return combine(stage1, mostUsedCategoryFlow) { s1, mostCat ->
            buildProfile(
                focus         = s1.focus,
                trends        = s1.trends,
                patterns      = s1.patterns,
                prediction    = s1.prediction,
                mostUsedApp   = s1.mostUsedApp,
                mostUsedCategory = mostCat
            )
        }.flowOn(Dispatchers.Default)
    }

    // ── Profile builder ───────────────────────────────────────────────────────

    private fun buildProfile(
        focus: Int,
        trends: BehaviorTrendSummary?,
        patterns: BehaviorPatternSummary,
        prediction: RelapsePrediction,
        mostUsedApp: String?,
        mostUsedCategory: String?
    ): AddictionProfile {

        // ── 40%: relapse severity component ──────────────────────────────────
        val relapseComponent = when (prediction.riskLevel) {
            RelapseRiskLevel.CRITICAL -> 100
            RelapseRiskLevel.HIGH     -> 75
            RelapseRiskLevel.MEDIUM   -> 45
            RelapseRiskLevel.LOW      -> 15
        }

        // ── 30%: inverse focus component ─────────────────────────────────────
        val focusComponent = 100 - focus.coerceIn(0, 100)

        // ── 20%: negative trend count (null-safe; 0 when no trend data yet) ──
        // TrendDirection.DECLINING = the outcome is worsening for each metric.
        val decliningCount: Int = if (trends == null) 0 else listOf(
            trends.focusTrend.trendDirection,
            trends.screenTimeTrend.trendDirection,
            trends.productivityTrend.trendDirection
        ).count { it == TrendDirection.DECLINING }
        val trendComponent = (decliningCount / 3.0 * 100).toInt()

        // ── 10%: pattern severity component ──────────────────────────────────
        val patternFlags = listOf(
            patterns.lateNight.detected,
            patterns.compulsiveSwitching.detected,
            patterns.weekendUsage.detected
        ).count { it }
        val patternComponent = (patternFlags / 3.0 * 100).toInt()

        // ── Weighted composite — weights are assumptions pending product sign-off ──
        val addictionScore = (
            relapseComponent * 0.40 +
            focusComponent   * 0.30 +
            trendComponent   * 0.20 +
            patternComponent * 0.10
        ).toInt().coerceIn(0, 100)

        val addictionLevel = when {
            addictionScore >= 80 -> AddictionLevel.CRITICAL
            addictionScore >= 60 -> AddictionLevel.HIGH
            addictionScore >= 35 -> AddictionLevel.MEDIUM
            else                 -> AddictionLevel.LOW
        }

        // ── recoveryTrend ─────────────────────────────────────────────────────
        val recoveryTrend = when {
            patterns.recovery.detected && decliningCount == 0 -> RecoveryTrend.IMPROVING
            decliningCount >= 2                               -> RecoveryTrend.WORSENING
            decliningCount == 0                               -> RecoveryTrend.STABLE
            else                                              -> RecoveryTrend.UNKNOWN
        }

        // ── Warnings: reuse exact phrasing from AICoachRepository rules ───────
        val warnings = buildList {
            if (prediction.riskLevel == RelapseRiskLevel.CRITICAL)
                add("High relapse risk detected")
            if (patterns.lateNight.detected)
                add("Late-night usage may affect sleep")
            if (patterns.compulsiveSwitching.detected)
                add("Frequent app switching detected")
            if (trends != null && trends.screenTimeTrend.trendDirection == TrendDirection.DECLINING)
                add("Screen time is increasing")
        }

        return AddictionProfile(
            addictionScore   = addictionScore,
            addictionLevel   = addictionLevel,
            focusScore       = focus,
            relapseRiskLevel = prediction.riskLevel,
            recoveryTrend    = recoveryTrend,
            mostUsedApp      = mostUsedApp,
            mostUsedCategory = mostUsedCategory,
            warnings         = warnings
        )
    }

    // ── Internal holder to work around combine()-5-param limit ───────────────

    private data class Stage1Data(
        val focus: Int,
        val trends: BehaviorTrendSummary?,
        val patterns: BehaviorPatternSummary,
        val prediction: RelapsePrediction,
        val mostUsedApp: String?
    )
}
