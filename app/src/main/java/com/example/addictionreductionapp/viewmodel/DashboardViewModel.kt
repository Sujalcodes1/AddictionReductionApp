package com.example.addictionreductionapp.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.addictionreductionapp.data.analytics.BehavioralIntelligenceEngine
import com.example.addictionreductionapp.data.analytics.FocusScoreEngine
import com.example.addictionreductionapp.data.analytics.StreakEngine
import com.example.addictionreductionapp.data.models.FocusScoreDetails
import com.example.addictionreductionapp.data.repository.AnalyticsRepository
import com.example.addictionreductionapp.data.repository.DailyBehaviorSnapshotRepository
import com.example.addictionreductionapp.data.local.entities.DailyBehaviorSnapshotEntity
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
class DashboardViewModel @Inject constructor(
    private val analyticsRepository: AnalyticsRepository,
    private val focusScoreEngine: FocusScoreEngine,
    private val streakEngine: StreakEngine,
    private val behaviorEngine: BehavioralIntelligenceEngine,
    private val dailyBehaviorSnapshotRepository: DailyBehaviorSnapshotRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(DashboardUiState(isLoading = false))
    val uiState: StateFlow<DashboardUiState> = _uiState.asStateFlow()

    init {
        android.util.Log.d("NavDebug", "DashboardViewModel INITIALIZED (hashCode=${hashCode()})")
        loadDashboardData()
    }

    private fun loadDashboardData() {
        val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")
        val today = LocalDate.now().format(formatter)
        val startDate7 = LocalDate.now().minusDays(7).format(formatter)
        val startDate30 = LocalDate.now().minusDays(30).format(formatter)

        viewModelScope.launch(kotlinx.coroutines.Dispatchers.IO) {
            // 1. Basic Overview (Screen Time)
            launch {
                analyticsRepository.getTotalScreenTimeToday(today)
                    .catch { }
                    .collect { time ->
                        _uiState.update { it.copy(todayScreenTime = time ?: 0) }
                    }
            }

            // 2. Top Apps
            launch {
                analyticsRepository.getMostUsedAppsToday(today)
                    .catch { }
                    .collect { apps ->
                        _uiState.update { it.copy(mostDistractingApp = apps.firstOrNull()?.appName) }
                    }
            }

            // 3. Focus Score
            launch {
                combine(
                    analyticsRepository.getTotalScreenTimeToday(today),
                    analyticsRepository.getTotalOpensToday(today),
                    analyticsRepository.getCategoryTotalsToday(today)
                ) { time, opens, categories ->
                    focusScoreEngine.calculateScore(time ?: 0, opens ?: 0, categories)
                }
                .catch { }
                .collect { scoreDetails ->
                    _uiState.update { it.copy(currentFocusScore = scoreDetails.score) }
                }
            }

            // 4. Streaks
            launch {
                analyticsRepository.getHistoricalFocusScoreList(startDate30, today)
                    .catch { }
                    .collect { scores ->
                        val streak = streakEngine.calculateStreak(scores)
                        _uiState.update { it.copy(currentStreak = streak.streakInfo.currentStreakDays) }
                    }
            }

            // 5. Behavioral Intelligence
            launch {
                val todayMetricsFlow = combine(
                    analyticsRepository.getTotalScreenTimeToday(today),
                    analyticsRepository.getTotalOpensToday(today),
                    analyticsRepository.getCategoryTotalsToday(today),
                    analyticsRepository.getMostUsedAppsToday(today)
                ) { time, opens, categories, topApps ->
                    DashboardTodayMetrics(time ?: 0, opens ?: 0, categories, topApps)
                }

                val supportFlow = combine(
                    analyticsRepository.getHourlyUsageToday(today),
                    analyticsRepository.getWeeklyUsageTotal(startDate7, today),
                    analyticsRepository.getHistoricalFocusScores(startDate7, today)
                ) { hourly, weeklyTotal, details ->
                    DashboardSupportData(hourly, weeklyTotal ?: 0, emptyList(), details)
                }

                combine(todayMetricsFlow, supportFlow) { metrics, support ->
                    val weeklyAvg = if (support.weeklyTotal > 0) support.weeklyTotal / 7 else 0

                    val focusScore = focusScoreEngine.calculateScore(
                        metrics.totalTime, metrics.totalOpens, metrics.categories
                    )
                    val recentScores = buildRecentScoreList(support.historicalDetails, focusScore)

                    val snapshot = behaviorEngine.generateSnapshot(
                        usageToday = metrics.topApps,
                        hourlyUsage = support.hourly,
                        totalOpens = metrics.totalOpens,
                        totalScreenTimeMinutes = metrics.totalTime,
                        weeklyAverageMinutes = weeklyAvg,
                        recentScores = recentScores,
                        timestamp = System.currentTimeMillis()
                    )

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
                            overallRiskScore = snapshot.overallRiskScore,
                            activeWarnings = warnings
                        )
                    }
                }
                .catch { }
                .collect { }
            }
        }
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

    override fun onCleared() {
        super.onCleared()
        android.util.Log.d("NavDebug", "DashboardViewModel CLEARED (hashCode=${hashCode()})")
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

