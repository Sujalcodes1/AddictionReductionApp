package com.example.addictionreductionapp.data.analytics

import com.example.addictionreductionapp.data.models.CategoryAnalytics
import com.example.addictionreductionapp.data.models.FocusScoreDetails
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import javax.inject.Inject

class FocusScoreEngine @Inject constructor() {

    /**
     * Calculates a deterministic focus score (0-100) based on daily usage data.
     * 
     * Factors:
     * - Productive Time vs Distracting Time (Ratio analysis)
     * - App Switching Frequency (More opens = more distracted)
     */
    fun calculateScore(
        totalScreenTimeMinutes: Int,
        totalOpens: Int,
        categoryTotals: List<CategoryAnalytics>
    ): FocusScoreDetails {
        if (totalScreenTimeMinutes == 0) {
            return FocusScoreDetails(100, 1f, 0f, 0, "No screen time recorded. Perfect focus!")
        }

        var productiveMinutes = 0
        var distractingMinutes = 0
        var neutralMinutes = 0

        for (cat in categoryTotals) {
            when (cat.category.lowercase()) {
                "productivity", "education", "business", "reading", "focus" -> productiveMinutes += cat.totalMinutes
                "social", "entertainment", "games", "video" -> distractingMinutes += cat.totalMinutes
                else -> neutralMinutes += cat.totalMinutes
            }
        }

        val productiveRatio = productiveMinutes.toFloat() / totalScreenTimeMinutes.toFloat()
        val distractionRatio = distractingMinutes.toFloat() / totalScreenTimeMinutes.toFloat()

        // 1. Base Score starts at 100
        var score = 100f

        // 2. Penalize for distraction ratio (up to -50 points if 100% distracted)
        score -= (distractionRatio * 50f)

        // 3. Reward for productive ratio (up to +20 points, capped at 100)
        score += (productiveRatio * 20f)

        // 4. App switching penalty
        // For every open above 10, penalty of 0.2 points, max penalty of 30 points.
        val excessOpens = (totalOpens - 10).coerceAtLeast(0)
        val switchingPenalty = (excessOpens * 0.2f).coerceAtMost(30f)
        score -= switchingPenalty

        val finalScore = score.toInt().coerceIn(0, 100)

        val explanation = buildString {
            append("Base focus score is $finalScore. ")
            if (distractionRatio > 0.5f) append("High distraction ratio (${(distractionRatio*100).toInt()}%). ")
            if (productiveRatio > 0.5f) append("Great productive time (${(productiveRatio*100).toInt()}%). ")
            if (switchingPenalty > 10f) append("High app switching penalty (-${switchingPenalty.toInt()} pts). ")
        }.trim()

        return FocusScoreDetails(
            score = finalScore,
            productiveRatio = productiveRatio,
            distractionRatio = distractionRatio,
            totalAppSwitches = totalOpens,
            explanation = explanation
        )
    }

    /**
     * Flow-compatible engine method to transform real-time metrics into a focus score flow.
     */
    fun getFocusScoreFlow(
        totalScreenTimeFlow: Flow<Int?>,
        totalOpensFlow: Flow<Int?>,
        categoryTotalsFlow: Flow<List<CategoryAnalytics>>
    ): Flow<FocusScoreDetails> {
        return combine(
            totalScreenTimeFlow,
            totalOpensFlow,
            categoryTotalsFlow
        ) { time, opens, categories ->
            calculateScore(
                totalScreenTimeMinutes = time ?: 0,
                totalOpens = opens ?: 0,
                categoryTotals = categories
            )
        }
    }
}
