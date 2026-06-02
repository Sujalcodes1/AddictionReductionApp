package com.example.addictionreductionapp.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.addictionreductionapp.data.analytics.BehavioralIntelligenceEngine
import com.example.addictionreductionapp.data.analytics.FocusScoreEngine
import com.example.addictionreductionapp.data.analytics.StreakEngine
import com.example.addictionreductionapp.data.models.FocusScoreDetails
import com.example.addictionreductionapp.data.repository.AnalyticsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.update
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import javax.inject.Inject

@HiltViewModel
class DashboardViewModel @Inject constructor(
    private val analyticsRepository: AnalyticsRepository,
    private val focusScoreEngine: FocusScoreEngine,
    private val streakEngine: StreakEngine,
    private val behaviorEngine: BehavioralIntelligenceEngine
) : ViewModel() {

    private val _uiState = MutableStateFlow(DashboardUiState(isLoading = true))
    val uiState: StateFlow<DashboardUiState> = _uiState.asStateFlow()

    init {
        loadDashboardData()
    }

    private fun loadDashboardData() {
        val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")
        val today = LocalDate.now().format(formatter)
        val startDate7 = LocalDate.now().minusDays(7).format(formatter)
        val startDate30 = LocalDate.now().minusDays(30).format(formatter)

        // ── Today's metrics ─────────────────────────────────────────────────
        val screenTimeFlow = analyticsRepository.getTotalScreenTimeToday(today)
        val opensFlow = analyticsRepository.getTotalOpensToday(today)
        val categoryFlow = analyticsRepository.getCategoryTotalsToday(today)
        val topAppsFlow = analyticsRepository.getMostUsedAppsToday(today)
        val hourlyFlow = analyticsRepository.getHourlyUsageToday(today)
        val weeklyFlow = analyticsRepository.getWeeklyUsageTotal(startDate7, today)

        // ── Historical flows (real data from Room) ──────────────────────────
        val historicalScoresFlow = analyticsRepository.getHistoricalFocusScoreList(startDate30, today)
        val historicalDetailsFlow = analyticsRepository.getHistoricalFocusScores(startDate7, today)

        // ── Stage 1: Today's core metrics ───────────────────────────────────
        val todayMetricsFlow = combine(
            screenTimeFlow,
            opensFlow,
            categoryFlow,
            topAppsFlow
        ) { time, opens, categories, topApps ->
            DashboardTodayMetrics(
                totalTime = time ?: 0,
                totalOpens = opens ?: 0,
                categories = categories,
                topApps = topApps
            )
        }

        // ── Stage 2: Hourly + weekly + historical ───────────────────────────
        val supportFlow = combine(
            hourlyFlow,
            weeklyFlow,
            historicalScoresFlow,
            historicalDetailsFlow
        ) { hourly, weeklyTotal, scores, details ->
            DashboardSupportData(
                hourly = hourly,
                weeklyTotal = weeklyTotal ?: 0,
                historicalScores = scores,
                historicalDetails = details
            )
        }

        // ── Final combination ───────────────────────────────────────────────
        combine(todayMetricsFlow, supportFlow) { metrics, support ->
            val weeklyAvg = if (support.weeklyTotal > 0) support.weeklyTotal / 7 else 0

            val focusScore = focusScoreEngine.calculateScore(
                metrics.totalTime, metrics.totalOpens, metrics.categories
            )

            // Build historical FocusScoreDetails: previous days + today's live score
            val recentScores = buildRecentScoreList(support.historicalDetails, focusScore)

            // Build full snapshot with real historical data
            val snapshot = behaviorEngine.generateSnapshot(
                usageToday = metrics.topApps,
                hourlyUsage = support.hourly,
                totalOpens = metrics.totalOpens,
                totalScreenTimeMinutes = metrics.totalTime,
                weeklyAverageMinutes = weeklyAvg,
                recentScores = recentScores,
                timestamp = System.currentTimeMillis()
            )

            // Calculate streak from real 30-day historical data
            val streak = streakEngine.calculateStreak(support.historicalScores)

            val mostDistracting = metrics.topApps.firstOrNull()?.appName

            // Build active warnings deterministically
            val warnings = mutableListOf<String>()
            if (focusScore.score < 50) {
                warnings.add("Focus score is low.")
            }
            if (snapshot.doomscroll.detected) {
                warnings.add("Doomscrolling detected in ${snapshot.doomscroll.appPackage}.")
            }
            if (snapshot.relapse.detected) {
                warnings.add("High risk of relapse detected.")
            }
            if (snapshot.lateNightUsage.detected) {
                warnings.add("Late night usage disrupting sleep.")
            }

            _uiState.update {
                it.copy(
                    isLoading = false,
                    error = null,
                    currentFocusScore = focusScore.score,
                    currentStreak = streak.streakInfo.currentStreakDays,
                    todayScreenTime = metrics.totalTime,
                    overallRiskScore = snapshot.overallRiskScore,
                    activeWarnings = warnings,
                    mostDistractingApp = mostDistracting
                )
            }
        }.catch { e ->
            _uiState.update { it.copy(isLoading = false, error = e.message ?: "Unknown error") }
        }.launchIn(viewModelScope)
    }

    /**
     * Builds a list of FocusScoreDetails for the behavioral engine.
     * Historical scores (from previous days) come first, followed by today's live score.
     */
    private fun buildRecentScoreList(
        historical: List<FocusScoreDetails>,
        todayScore: FocusScoreDetails
    ): List<FocusScoreDetails> {
        val previousDays = if (historical.isNotEmpty()) {
            historical.dropLast(1)
        } else {
            emptyList()
        }
        return previousDays + todayScore
    }
}

// ── Internal holder classes for type-safe Flow combine stages ────────────────

internal data class DashboardTodayMetrics(
    val totalTime: Int,
    val totalOpens: Int,
    val categories: List<com.example.addictionreductionapp.data.models.CategoryAnalytics>,
    val topApps: List<com.example.addictionreductionapp.data.models.AppUsageSummary>
)

internal data class DashboardSupportData(
    val hourly: List<com.example.addictionreductionapp.data.models.HourlyUsagePoint>,
    val weeklyTotal: Int,
    val historicalScores: List<com.example.addictionreductionapp.data.models.FocusScore>,
    val historicalDetails: List<FocusScoreDetails>
)

