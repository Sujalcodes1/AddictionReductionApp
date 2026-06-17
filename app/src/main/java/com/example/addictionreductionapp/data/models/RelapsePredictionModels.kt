package com.example.addictionreductionapp.data.models

/**
 * Categorical risk level for relapse prediction.
 *
 * Ordinal severity: LOW (0) < MEDIUM (1) < HIGH (2) < CRITICAL (3).
 */
enum class RelapseRiskLevel {
    LOW,
    MEDIUM,
    HIGH,
    CRITICAL;

    /**
     * Returns a UI-ready label for trend cards and alerts.
     */
    fun toLabel(): String = when (this) {
        LOW      -> "Low Risk"
        MEDIUM   -> "Medium Risk"
        HIGH     -> "High Risk"
        CRITICAL -> "Critical Risk"
    }
}

/**
 * A deterministic relapse prediction derived from historical trends and behavior patterns.
 *
 * This model is the primary output of [RelapsePredictionRepository] and is designed
 * to be consumed directly by UI layers and future ML comparison benchmarks.
 *
 * @param riskLevel       Categorical severity classification.
 * @param confidenceScore 0.0–1.0 confidence based on data availability and signal consistency.
 * @param reasons         Human-readable explanations for the assigned risk level.
 * @param recommendation  Actionable guidance string mapped to the risk level.
 * @param generatedAt     Epoch milliseconds when this prediction was computed.
 */
data class RelapsePrediction(
    val riskLevel: RelapseRiskLevel,
    val confidenceScore: Float,
    val reasons: List<String>,
    val recommendation: String,
    val generatedAt: Long = System.currentTimeMillis()
)
