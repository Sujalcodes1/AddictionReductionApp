package com.example.addictionreductionapp.data.models

enum class CoachPriority {
    CRITICAL, HIGH, MEDIUM, LOW          // ordered high→low for sort key use
}

enum class CoachCategory {
    FOCUS, DISTRACTION, PRODUCTIVITY,
    RELAPSE, SLEEP, APP_USAGE
}

data class CoachInsight(
    val title: String,
    val description: String,
    val category: CoachCategory,
    val priority: CoachPriority,
    val createdAt: Long = System.currentTimeMillis()
)
