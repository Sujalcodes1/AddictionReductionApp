package com.example.addictionreductionapp.data.analytics

import com.example.addictionreductionapp.data.models.FocusScore
import com.example.addictionreductionapp.data.models.StreakAnalysis
import com.example.addictionreductionapp.data.models.StreakInfo
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import javax.inject.Inject

class StreakEngine @Inject constructor() {

    /**
     * Calculates focus streaks, productive-day streaks, and streak recovery logic.
     * @param historicalScores List of daily focus scores (e.g. from the past 30 days)
     * @param targetScore Minimum focus score required to count as a "productive day"
     */
    fun calculateStreak(
        historicalScores: List<FocusScore>,
        targetScore: Int = 50
    ): StreakAnalysis {
        if (historicalScores.isEmpty()) {
            return StreakAnalysis(
                streakInfo = StreakInfo(0, 0, null),
                isProductiveDayStreak = false,
                isRecovered = false,
                explanation = "No data available."
            )
        }

        // Sort ascending by date "YYYY-MM-DD"
        val sortedScores = historicalScores.sortedBy { it.date }
        
        var currentStreak = 0
        var longestStreak = 0
        var lastActiveDate: String? = null
        var isRecovered = false
        
        val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")

        for (i in sortedScores.indices) {
            val record = sortedScores[i]
            val isProductive = record.score >= targetScore

            if (isProductive) {
                if (lastActiveDate == null) {
                    currentStreak = 1
                } else {
                    val lastDate = LocalDate.parse(lastActiveDate, formatter)
                    val currDate = LocalDate.parse(record.date, formatter)
                    val daysBetween = ChronoUnit.DAYS.between(lastDate, currDate)

                    when (daysBetween) {
                        1L -> {
                            // Consecutive day
                            currentStreak++
                            isRecovered = false
                        }
                        2L -> {
                            // Missed exactly one day - Recovery logic activated
                            currentStreak++
                            isRecovered = true
                        }
                        else -> {
                            // Missed more than 1 day, streak reset
                            currentStreak = 1
                            isRecovered = false
                        }
                    }
                }
                lastActiveDate = record.date
                if (currentStreak > longestStreak) {
                    longestStreak = currentStreak
                }
            }
        }

        // Check if current streak is still alive compared to today
        val today = LocalDate.now()
        val streakAlive = lastActiveDate != null && 
                ChronoUnit.DAYS.between(LocalDate.parse(lastActiveDate, formatter), today) <= 1L

        if (!streakAlive) {
            currentStreak = 0
            isRecovered = false
        }

        val explanation = if (currentStreak > 0) {
            if (isRecovered) "Streak maintained via recovery logic (1 day missed)!" 
            else "Strong focus streak maintaining above $targetScore score!"
        } else {
            "Streak lost or not started yet. Try to score above $targetScore tomorrow!"
        }

        val info = StreakInfo(
            currentStreakDays = currentStreak,
            longestStreakDays = longestStreak,
            lastActiveDate = lastActiveDate
        )

        return StreakAnalysis(
            streakInfo = info,
            isProductiveDayStreak = currentStreak > 0,
            isRecovered = isRecovered,
            explanation = explanation
        )
    }

    /**
     * Flow-compatible engine method
     */
    fun getStreakFlow(
        historicalScoresFlow: Flow<List<FocusScore>>,
        targetScore: Int = 50
    ): Flow<StreakAnalysis> {
        return historicalScoresFlow.map { scores ->
            calculateStreak(scores, targetScore)
        }
    }
}
