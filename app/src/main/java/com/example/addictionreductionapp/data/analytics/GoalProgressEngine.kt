package com.example.addictionreductionapp.data.analytics

import com.example.addictionreductionapp.data.local.entities.GoalEntity
import javax.inject.Inject
import javax.inject.Singleton

data class GoalProgressUpdate(
    val savedHoursTotal: Int,
    val progress: Float
)

@Singleton
class GoalProgressEngine @Inject constructor() {

    fun computeGoalProgress(
        activeGoals: List<GoalEntity>,
        baselineDailyMinutes: Int,
        actualDailyMinutes: Int
    ): Map<Long, GoalProgressUpdate> {
        val savedMinutes = (baselineDailyMinutes - actualDailyMinutes).coerceAtLeast(0)
        val savedHours = savedMinutes / 60f
        val hoursPerGoal = savedHours / activeGoals.size.coerceAtLeast(1)

        return activeGoals.associate { goal ->
            val addedHours = Math.round(hoursPerGoal).toInt()
            val newSaved = goal.savedHoursTotal + addedHours
            val progress = if (addedHours > 0 && goal.targetDate != null) {
                val totalTargetMinutes = ((goal.targetScreenTimePerDay - 30).coerceAtLeast(15) * 7)
                val totalTargetHours = totalTargetMinutes / 60f
                if (totalTargetHours > 0f) {
                    (newSaved.toFloat() / totalTargetHours).coerceIn(0f, 1f)
                } else {
                    1f
                }
            } else {
                goal.progress
            }
            goal.id to GoalProgressUpdate(savedHoursTotal = newSaved, progress = progress)
        }
    }

    fun generateGoalInsight(
        goals: List<GoalEntity>,
        savedHoursThisWeek: Float
    ): String {
        val perGoal = savedHoursThisWeek / goals.size.coerceAtLeast(1)
        return buildString {
            append("You saved ${savedHoursThisWeek.toInt()} hours this week. ")
            goals.take(3).forEach { goal ->
                append("${perGoal.toInt()}h toward '${goal.title}'. ")
            }
        }.trim()
    }
}
