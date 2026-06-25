package com.example.addictionreductionapp.data.models

// ── AddictionLevel ────────────────────────────────────────────────────────────
// Ordered LOW → CRITICAL; maps addictionScore bands defined in
// AddictionIntelligenceRepository.buildProfile().
enum class AddictionLevel {
    LOW,      // addictionScore  0–34
    MEDIUM,   // addictionScore 35–59
    HIGH,     // addictionScore 60–79
    CRITICAL  // addictionScore 80–100
}

// ── RecoveryTrend ─────────────────────────────────────────────────────────────
// Derived in AddictionIntelligenceRepository from BehaviorPatternSummary.recovery
// and the count of DECLINING trends in BehaviorTrendSummary.
enum class RecoveryTrend {
    IMPROVING,
    STABLE,
    WORSENING,
    UNKNOWN
}

// ── AddictionProfile ──────────────────────────────────────────────────────────
// UI-facing data class consumed by DashboardViewModel._addictionProfile.
// No Room annotations — this is a pure presentation model.
//
// @param addictionScore     Weighted composite 0–100. Higher = more concerning.
//                           Formula: 40% relapse severity + 30% inverse focus +
//                           20% declining trends + 10% pattern flags.
// @param addictionLevel     Categorical band derived from addictionScore.
// @param focusScore         Pass-through of FocusScoreEngine.calculateScore().score
//                           for the current day (0–100).
// @param relapseRiskLevel   Pass-through of RelapsePrediction.riskLevel from
//                           RelapsePredictionRepository.generateRelapsePrediction().
//                           Type: RelapseRiskLevel (imported, not redefined here).
// @param recoveryTrend      Derived from recovery pattern + declining-trend count.
// @param mostUsedApp        appName of the top app by totalMinutes today; null when
//                           AnalyticsRepository.getMostUsedAppsToday() returns empty.
// @param mostUsedCategory   category string of the top CategoryAnalytics by totalMinutes
//                           today; null when getCategoryTotalsToday() returns empty.
// @param warnings           Human-readable alert strings, reusing phrasing from
//                           AICoachRepository rules.
// @param generatedAt        Epoch ms timestamp at object construction.
data class AddictionProfile(
    val addictionScore: Int,
    val addictionLevel: AddictionLevel,
    val focusScore: Int,
    val relapseRiskLevel: RelapseRiskLevel,  // imported from RelapsePredictionModels.kt
    val recoveryTrend: RecoveryTrend,
    val mostUsedApp: String?,
    val mostUsedCategory: String?,
    val warnings: List<String>,
    val generatedAt: Long = System.currentTimeMillis()
)
