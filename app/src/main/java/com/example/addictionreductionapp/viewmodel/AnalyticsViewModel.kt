package com.example.addictionreductionapp.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.addictionreductionapp.data.analytics.BehavioralIntelligenceEngine
import com.example.addictionreductionapp.data.analytics.FocusScoreEngine
import com.example.addictionreductionapp.data.analytics.StreakEngine
import com.example.addictionreductionapp.data.models.AppUsageSummary
import com.example.addictionreductionapp.data.models.CategoryAnalytics
import com.example.addictionreductionapp.data.models.FocusScore
import com.example.addictionreductionapp.data.models.FocusScoreDetails
import com.example.addictionreductionapp.data.models.HourlyUsagePoint
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
class AnalyticsViewModel @Inject constructor(
    private val analyticsRepository: AnalyticsRepository,
    private val focusScoreEngine: FocusScoreEngine,
    private val streakEngine: StreakEngine,
    private val behaviorEngine: BehavioralIntelligenceEngine
) : ViewModel() {

    private val _uiState = MutableStateFlow(AnalyticsUiState(isLoading = true))
    val uiState: StateFlow<AnalyticsUiState> = _uiState.asStateFlow()

    init {
        loadRealtimeAnalytics()
    }

    private fun loadRealtimeAnalytics() {
        val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")
        val today = LocalDate.now().format(formatter)
        val startDate7 = LocalDate.now().minusDays(7).format(formatter)
        val startDate30 = LocalDate.now().minusDays(30).format(formatter)

        // ── Today's metrics ─────────────────────────────────────────────────
        val screenTimeFlow = analyticsRepository.getTotalScreenTimeToday(today)
        val opensFlow = analyticsRepository.getTotalOpensToday(today)
        val categoryFlow = analyticsRepository.getCategoryTotalsToday(today)
        val hourlyFlow = analyticsRepository.getHourlyUsageToday(today)
        val topAppsFlow = analyticsRepository.getMostUsedAppsToday(today)
        val topOpenedFlow = analyticsRepository.getMostOpenedAppsToday(today)
        val weeklyFlow = analyticsRepository.getWeeklyUsageTotal(startDate7, today)

        // ── Historical flows (real data from Room) ──────────────────────────
        // 30-day FocusScore list for StreakEngine
        val historicalScoresFlow = analyticsRepository.getHistoricalFocusScoreList(startDate30, today)
        // 7-day FocusScoreDetails for BehavioralIntelligenceEngine (productivity decay)
        val historicalDetailsFlow = analyticsRepository.getHistoricalFocusScores(startDate7, today)

        // ── Stage 1: Today's summary ────────────────────────────────────────
        val summaryFlow = combine(
            screenTimeFlow,
            opensFlow,
            categoryFlow,
            hourlyFlow
        ) { time, opens, categories, hourly ->
            SummaryData(time ?: 0, opens ?: 0, categories, hourly)
        }

        // ── Stage 2: App lists + weekly total ───────────────────────────────
        val appsAndWeeklyFlow = combine(
            topAppsFlow,
            topOpenedFlow,
            weeklyFlow
        ) { topApps, topOpened, weeklyTime ->
            AppsAndWeeklyData(topApps, topOpened, weeklyTime ?: 0)
        }

        // ── Stage 3: Historical data ────────────────────────────────────────
        val historyFlow = combine(
            historicalScoresFlow,
            historicalDetailsFlow
        ) { scores, details ->
            HistoricalData(scores, details)
        }

        // ── Final combination ───────────────────────────────────────────────
        combine(summaryFlow, appsAndWeeklyFlow, historyFlow) { summary, apps, history ->
            val weeklyAvg = if (apps.weeklyTime > 0) apps.weeklyTime / 7 else 0

            // Compute today's focus score from live data
            val todayFocusScore = focusScoreEngine.calculateScore(
                totalScreenTimeMinutes = summary.time,
                totalOpens = summary.opens,
                categoryTotals = summary.categories
            )

            // Build the historical FocusScoreDetails list for behavioral engine.
            // Use historical scores from previous days + today's live score as the latest entry.
            // This ensures productivityDecay always compares today vs historical average.
            val recentScores = buildRecentScoreList(history.focusScoreDetails, todayFocusScore)

            // Build behavioral snapshot with real historical data
            val snapshot = behaviorEngine.generateSnapshot(
                usageToday = apps.topApps,
                hourlyUsage = summary.hourly,
                totalOpens = summary.opens,
                totalScreenTimeMinutes = summary.time,
                weeklyAverageMinutes = weeklyAvg,
                recentScores = recentScores,
                timestamp = System.currentTimeMillis()
            )

            // Calculate streak from real 30-day historical FocusScore data
            val streak = streakEngine.calculateStreak(history.focusScores)

            _uiState.update { current ->
                current.copy(
                    isLoading = false,
                    error = null,
                    dailyScreenTimeMinutes = summary.time,
                    totalOpens = summary.opens,
                    categoryUsage = summary.categories,
                    hourlyUsage = summary.hourly,
                    topUsedApps = apps.topApps,
                    topOpenedApps = apps.topOpened,
                    weeklyTotalMinutes = apps.weeklyTime,
                    weeklyAverageMinutes = weeklyAvg,
                    focusScoreDetails = todayFocusScore,
                    behavioralSnapshot = snapshot,
                    streakAnalysis = streak
                )
            }
        }.catch { e ->
            _uiState.update { it.copy(isLoading = false, error = e.message ?: "Unknown error") }
        }.launchIn(viewModelScope)
    }

    /**
     * Builds a list of FocusScoreDetails for the behavioral engine.
     * Historical scores (from previous days) come first, followed by today's live score.
     * If today's score is already present in the historical list (same day), it replaces it.
     */
    private fun buildRecentScoreList(
        historical: List<FocusScoreDetails>,
        todayScore: FocusScoreDetails
    ): List<FocusScoreDetails> {
        // Historical data may already contain today if the DB has been written to.
        // Drop the last entry if it would be today's, and append the live computation.
        val previousDays = if (historical.isNotEmpty()) {
            // The last element in the historical list is from the most recent date with data.
            // If that's today, drop it so we use the live computation instead.
            historical.dropLast(1)
        } else {
            emptyList()
        }
        return previousDays + todayScore
    }
}

// Internal holder classes for Flow combine operators
internal data class SummaryData(
    val time: Int,
    val opens: Int,
    val categories: List<CategoryAnalytics>,
    val hourly: List<HourlyUsagePoint>
)

internal data class AppsAndWeeklyData(
    val topApps: List<AppUsageSummary>,
    val topOpened: List<AppUsageSummary>,
    val weeklyTime: Int
)

internal data class HistoricalData(
    val focusScores: List<FocusScore>,
    val focusScoreDetails: List<FocusScoreDetails>
)

