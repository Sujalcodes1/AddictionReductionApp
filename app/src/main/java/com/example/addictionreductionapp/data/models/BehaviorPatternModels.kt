package com.example.addictionreductionapp.data.models

data class WeekendUsagePattern(
    val detected: Boolean,
    val weekendAverageMinutes: Float,
    val weekdayAverageMinutes: Float,
    val percentageIncrease: Float
)

data class LateNightPattern(
    val detected: Boolean,
    val daysDetected: Int // out of last 7
)

data class FocusDecayPattern(
    val detected: Boolean,
    val consecutiveDeclines: Int
)

data class RecoveryPattern(
    val detected: Boolean,
    val consecutiveImprovements: Int
)

data class CompulsiveSwitchingPattern(
    val detected: Boolean,
    val daysDetected: Int // out of last 7
)

data class AppAddictionPattern(
    val detected: Boolean,
    val dominantAppPackage: String?,
    val averageDailyMinutes: Float
)

data class BehaviorPatternSummary(
    val weekendUsage: WeekendUsagePattern,
    val lateNight: LateNightPattern,
    val focusDecay: FocusDecayPattern,
    val recovery: RecoveryPattern,
    val compulsiveSwitching: CompulsiveSwitchingPattern,
    val appAddiction: AppAddictionPattern,
    val generatedAt: Long = System.currentTimeMillis()
)
