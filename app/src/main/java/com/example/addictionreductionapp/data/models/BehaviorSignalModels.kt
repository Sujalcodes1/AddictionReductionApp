package com.example.addictionreductionapp.data.models

data class RelapseSignal(
    val detected: Boolean,
    val severityScore: Float, // 0.0 to 1.0
    val indicator: String,
    val timestamp: Long
)

data class DoomscrollSignal(
    val detected: Boolean,
    val appPackage: String?,
    val continuousMinutes: Int,
    val severityScore: Float, // 0.0 to 1.0
    val timestamp: Long
)

data class CompulsiveSwitchingSignal(
    val detected: Boolean,
    val switchesPerHour: Float,
    val severityScore: Float, // 0.0 to 1.0
    val timestamp: Long
)

data class LateNightUsageSignal(
    val detected: Boolean,
    val minutesUsed: Int,
    val severityScore: Float, // 0.0 to 1.0
    val timestamp: Long
)

data class AddictionSpikeSignal(
    val detected: Boolean,
    val baselineAverageMinutes: Int,
    val todayMinutes: Int,
    val percentageIncrease: Float,
    val severityScore: Float, // 0.0 to 1.0
    val timestamp: Long
)

data class ProductivityDecaySignal(
    val detected: Boolean,
    val previousRatio: Float,
    val currentRatio: Float,
    val severityScore: Float, // 0.0 to 1.0
    val timestamp: Long
)

data class BehavioralIntelligenceSnapshot(
    val relapse: RelapseSignal,
    val doomscroll: DoomscrollSignal,
    val compulsiveSwitching: CompulsiveSwitchingSignal,
    val lateNightUsage: LateNightUsageSignal,
    val addictionSpike: AddictionSpikeSignal,
    val productivityDecay: ProductivityDecaySignal,
    val overallRiskScore: Float, // 0.0 to 1.0, ML friendly aggregate
    val timestamp: Long
)
