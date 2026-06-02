package com.example.addictionreductionapp.data.analytics

import com.example.addictionreductionapp.data.models.*
import javax.inject.Inject

/**
 * Deterministic behavioral intelligence engine.
 * Generates structured, ML-friendly signals indicating various addictive behaviors.
 */
class BehavioralIntelligenceEngine @Inject constructor() {

    /**
     * Detects doomscrolling: long continuous sessions (high usage time, few opens)
     * in distracting apps.
     */
    fun analyzeDoomscroll(
        usageToday: List<AppUsageSummary>,
        timestamp: Long = System.currentTimeMillis()
    ): DoomscrollSignal {
        var maxDoomApp: AppUsageSummary? = null
        var maxMinutes = 0
        
        for (app in usageToday) {
            // Heuristic: > 60 minutes of usage with < 10 opens suggests long continuous scrolling
            if (app.totalMinutes > 60 && app.openCount < 10) {
                if (app.totalMinutes > maxMinutes) {
                    maxMinutes = app.totalMinutes
                    maxDoomApp = app
                }
            }
        }
        
        val detected = maxDoomApp != null
        val severity = if (detected) ((maxMinutes - 60f) / 180f).coerceIn(0f, 1f) else 0f
        
        return DoomscrollSignal(
            detected = detected,
            appPackage = maxDoomApp?.packageName,
            continuousMinutes = maxMinutes,
            severityScore = severity,
            timestamp = timestamp
        )
    }

    /**
     * Detects compulsive switching: frequently opening apps without spending much time in them.
     */
    fun detectCompulsiveSwitching(
        totalOpens: Int,
        totalScreenTimeMinutes: Int,
        timestamp: Long = System.currentTimeMillis()
    ): CompulsiveSwitchingSignal {
        // Calculate opens per hour of actual screen time
        val opensPerHour = if (totalScreenTimeMinutes > 0) {
            (totalOpens.toFloat() / totalScreenTimeMinutes.toFloat()) * 60f
        } else 0f

        // Threshold: > 30 opens per hour of screen time indicates compulsive checking
        val detected = opensPerHour > 30f && totalOpens > 20
        val severity = if (detected) ((opensPerHour - 30f) / 50f).coerceIn(0f, 1f) else 0f

        return CompulsiveSwitchingSignal(
            detected = detected,
            switchesPerHour = opensPerHour,
            severityScore = severity,
            timestamp = timestamp
        )
    }

    /**
     * Detects late-night usage indicating disrupted sleep patterns.
     */
    fun detectLateNightUsage(
        hourlyUsage: List<HourlyUsagePoint>,
        timestamp: Long = System.currentTimeMillis()
    ): LateNightUsageSignal {
        // Late night window: 22:00 to 05:00
        val lateNightHours = setOf(22, 23, 0, 1, 2, 3, 4, 5)
        var lateNightMinutes = 0

        for (point in hourlyUsage) {
            if (point.hourOfDay in lateNightHours) {
                lateNightMinutes += point.minutesUsed
            }
        }

        // Detected if more than 15 minutes of usage in the late night window
        val detected = lateNightMinutes > 15
        val severity = if (detected) ((lateNightMinutes - 15f) / 120f).coerceIn(0f, 1f) else 0f

        return LateNightUsageSignal(
            detected = detected,
            minutesUsed = lateNightMinutes,
            severityScore = severity,
            timestamp = timestamp
        )
    }

    /**
     * Detects sudden spikes in overall usage compared to the baseline.
     */
    fun detectAddictionSpike(
        todayMinutes: Int,
        weeklyAverageMinutes: Int,
        timestamp: Long = System.currentTimeMillis()
    ): AddictionSpikeSignal {
        if (weeklyAverageMinutes == 0) {
             return AddictionSpikeSignal(false, 0, todayMinutes, 0f, 0f, timestamp)
        }

        val increaseRatio = todayMinutes.toFloat() / weeklyAverageMinutes.toFloat()
        val percentageIncrease = (increaseRatio - 1f) * 100f
        
        // Spike is defined as > 50% increase over the baseline with at least 60 mins of total usage
        val detected = percentageIncrease > 50f && todayMinutes > 60
        val severity = if (detected) ((percentageIncrease - 50f) / 150f).coerceIn(0f, 1f) else 0f

        return AddictionSpikeSignal(
            detected = detected,
            baselineAverageMinutes = weeklyAverageMinutes,
            todayMinutes = todayMinutes,
            percentageIncrease = percentageIncrease,
            severityScore = severity,
            timestamp = timestamp
        )
    }

    /**
     * Detects gradual or sudden decay in productivity ratio over time.
     */
    fun detectProductivityDecay(
        recentScores: List<FocusScoreDetails>,
        timestamp: Long = System.currentTimeMillis()
    ): ProductivityDecaySignal {
        // Need at least 2 days of history to compare
        if (recentScores.size < 2) {
            return ProductivityDecaySignal(false, 0f, 0f, 0f, timestamp)
        }

        val todayRatio = recentScores.last().productiveRatio
        val previousRatios = recentScores.dropLast(1).map { it.productiveRatio }
        val avgPrevious = if (previousRatios.isNotEmpty()) previousRatios.average().toFloat() else 0f

        // Decay is flagged if today's ratio drops by 0.2 (20%) below the recent average
        val detected = todayRatio < (avgPrevious - 0.2f)
        val severity = if (detected) ((avgPrevious - todayRatio - 0.2f) / 0.5f).coerceIn(0f, 1f) else 0f

        return ProductivityDecaySignal(
            detected = detected,
            previousRatio = avgPrevious,
            currentRatio = todayRatio,
            severityScore = severity,
            timestamp = timestamp
        )
    }

    /**
     * Detects potential relapse by aggregating high-risk behavioral signals.
     */
    fun detectRelapse(
        addictionSpike: AddictionSpikeSignal,
        productivityDecay: ProductivityDecaySignal,
        compulsiveSwitching: CompulsiveSwitchingSignal,
        timestamp: Long = System.currentTimeMillis()
    ): RelapseSignal {
        // A relapse is flagged when multiple critical signals converge
        val detected = addictionSpike.detected && (productivityDecay.detected || compulsiveSwitching.detected)
        
        val severity = (addictionSpike.severityScore + productivityDecay.severityScore + compulsiveSwitching.severityScore) / 3f
        val indicator = if (detected) "High-risk behavior convergence" else "Stable"

        return RelapseSignal(
            detected = detected,
            severityScore = severity,
            indicator = indicator,
            timestamp = timestamp
        )
    }

    /**
     * Generates a complete structured ML-friendly snapshot of current user behavior.
     */
    fun generateSnapshot(
        usageToday: List<AppUsageSummary>,
        hourlyUsage: List<HourlyUsagePoint>,
        totalOpens: Int,
        totalScreenTimeMinutes: Int,
        weeklyAverageMinutes: Int,
        recentScores: List<FocusScoreDetails>,
        timestamp: Long = System.currentTimeMillis()
    ): BehavioralIntelligenceSnapshot {
        
        val doomscroll = analyzeDoomscroll(usageToday, timestamp)
        val compulsive = detectCompulsiveSwitching(totalOpens, totalScreenTimeMinutes, timestamp)
        val lateNight = detectLateNightUsage(hourlyUsage, timestamp)
        val spike = detectAddictionSpike(totalScreenTimeMinutes, weeklyAverageMinutes, timestamp)
        val decay = detectProductivityDecay(recentScores, timestamp)
        val relapse = detectRelapse(spike, decay, compulsive, timestamp)

        // ML friendly aggregate score reflecting overall behavioral risk
        val overallRiskScore = (
            doomscroll.severityScore +
            compulsive.severityScore +
            lateNight.severityScore +
            spike.severityScore +
            decay.severityScore +
            relapse.severityScore
        ) / 6f

        return BehavioralIntelligenceSnapshot(
            relapse = relapse,
            doomscroll = doomscroll,
            compulsiveSwitching = compulsive,
            lateNightUsage = lateNight,
            addictionSpike = spike,
            productivityDecay = decay,
            overallRiskScore = overallRiskScore.coerceIn(0f, 1f),
            timestamp = timestamp
        )
    }
}
