package com.example.addictionreductionapp.data.models

data class ReductionMilestone(
    val weekNumber: Int,
    val targetMinutes: Int,
    val achievedMinutes: Int,
    val startDate: String,
    val endDate: String,
    val isComplete: Boolean = false
)

data class RoadmapPlan(
    val baselineDailyAverage: Int,
    val milestones: List<ReductionMilestone>,
    val currentWeek: Int,
    val generatedAt: Long = System.currentTimeMillis()
)
