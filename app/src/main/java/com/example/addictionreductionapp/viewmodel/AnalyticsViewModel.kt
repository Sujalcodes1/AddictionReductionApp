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
import com.example.addictionreductionapp.data.repository.HistoricalTrendsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import javax.inject.Inject

@HiltViewModel
class AnalyticsViewModel @Inject constructor(
    private val analyticsRepository: AnalyticsRepository,
    private val focusScoreEngine: FocusScoreEngine,
    private val streakEngine: StreakEngine,
    private val behaviorEngine: BehavioralIntelligenceEngine,
    private val historicalTrendsRepository: HistoricalTrendsRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(AnalyticsUiState(isLoading = false))
    val uiState: StateFlow<AnalyticsUiState> = _uiState.asStateFlow()

    init {
        android.util.Log.d("NavDebug", "AnalyticsViewModel INITIALIZED (hashCode=${hashCode()})")
        loadRealtimeAnalytics()
    }

    private fun loadRealtimeAnalytics() {
        val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")
        val today = LocalDate.now().format(formatter)
        val startDate7 = LocalDate.now().minusDays(7).format(formatter)
        val startDate30 = LocalDate.now().minusDays(30).format(formatter)

        viewModelScope.launch(kotlinx.coroutines.Dispatchers.IO) {
            // 1. Basic Stats
            launch {
                combine(
                    analyticsRepository.getTotalScreenTimeToday(today),
                    analyticsRepository.getTotalOpensToday(today)
                ) { time, opens ->
                    _uiState.update { it.copy(dailyScreenTimeMinutes = time ?: 0, totalOpens = opens ?: 0) }
                }.catch { }.collect { }
            }

            // 2. Categories
            launch {
                analyticsRepository.getCategoryTotalsToday(today).catch { }.collect { categories ->
                    _uiState.update { it.copy(categoryUsage = categories) }
                }
            }

            // 3. Hourly
            launch {
                analyticsRepository.getHourlyUsageToday(today).catch { }.collect { hourly ->
                    _uiState.update { it.copy(hourlyUsage = hourly) }
                }
            }

            // 4. Apps & Weekly
            launch {
                combine(
                    analyticsRepository.getMostUsedAppsToday(today),
                    analyticsRepository.getMostOpenedAppsToday(today),
                    analyticsRepository.getWeeklyUsageTotal(startDate7, today)
                ) { used, opened, weekly ->
                    val avg = if ((weekly ?: 0) > 0) (weekly ?: 0) / 7 else 0
                    _uiState.update { 
                        it.copy(
                            topUsedApps = used, 
                            topOpenedApps = opened, 
                            weeklyTotalMinutes = weekly ?: 0,
                            weeklyAverageMinutes = avg
                        ) 
                    }
                }.catch { }.collect { }
            }

            // 5. Streaks
            launch {
                analyticsRepository.getHistoricalFocusScoreList(startDate30, today).catch { }.collect { scores ->
                    val streak = streakEngine.calculateStreak(scores)
                    _uiState.update { it.copy(streakAnalysis = streak) }
                }
            }

            // 6. Historical Trends (7-day window)
            // Future ML Dataset Source: subscribe to trend summaries to populate training data.
            launch {
                historicalTrendsRepository.generateBehaviorTrendSummary(windowDays = 7)
                    .catch { }
                    .collect { summary ->
                        _uiState.update { it.copy(weeklyTrendSummary = summary) }
                    }
            }

            // 7. Historical Trends (30-day window)
            // Future ML Training Input: 30-day window exposes longer-term behavioral drift.
            launch {
                historicalTrendsRepository.generateBehaviorTrendSummary(windowDays = 30)
                    .catch { }
                    .collect { summary ->
                        _uiState.update { it.copy(monthlyTrendSummary = summary) }
                    }
            }

            // 8. Focus Score & Behavioral
            launch {
                val summaryFlow = combine(
                    analyticsRepository.getTotalScreenTimeToday(today),
                    analyticsRepository.getTotalOpensToday(today),
                    analyticsRepository.getCategoryTotalsToday(today),
                    analyticsRepository.getHourlyUsageToday(today)
                ) { time, opens, categories, hourly ->
                    SummaryData(time ?: 0, opens ?: 0, categories, hourly)
                }

                val appsAndWeeklyFlow = combine(
                    analyticsRepository.getMostUsedAppsToday(today),
                    analyticsRepository.getMostOpenedAppsToday(today),
                    analyticsRepository.getWeeklyUsageTotal(startDate7, today)
                ) { topApps, topOpened, weeklyTime ->
                    AppsAndWeeklyData(topApps, topOpened, weeklyTime ?: 0)
                }

                val historyFlow = combine(
                    analyticsRepository.getHistoricalFocusScoreList(startDate30, today),
                    analyticsRepository.getHistoricalFocusScores(startDate7, today)
                ) { scores, details ->
                    HistoricalData(scores, details)
                }

                combine(summaryFlow, appsAndWeeklyFlow, historyFlow) { summary, apps, history ->
                    val weeklyAvg = if (apps.weeklyTime > 0) apps.weeklyTime / 7 else 0

                    val todayFocusScore = focusScoreEngine.calculateScore(
                        totalScreenTimeMinutes = summary.time,
                        totalOpens = summary.opens,
                        categoryTotals = summary.categories
                    )

                    val recentScores = buildRecentScoreList(history.focusScoreDetails, todayFocusScore)

                    val snapshot = behaviorEngine.generateSnapshot(
                        usageToday = apps.topApps,
                        hourlyUsage = summary.hourly,
                        totalOpens = summary.opens,
                        totalScreenTimeMinutes = summary.time,
                        weeklyAverageMinutes = weeklyAvg,
                        recentScores = recentScores,
                        timestamp = System.currentTimeMillis()
                    )

                    _uiState.update {
                        it.copy(
                            focusScoreDetails = todayFocusScore,
                            behavioralSnapshot = snapshot
                        )
                    }
                }.catch { }.collect { }
            }
        }
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
    override fun onCleared() {
        super.onCleared()
        android.util.Log.d("NavDebug", "AnalyticsViewModel CLEARED (hashCode=${hashCode()})")
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

