package com.example.addictionreductionapp.data.analytics

import com.example.addictionreductionapp.data.local.entities.GoalEntity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class GoalProgressEngineTest {

    private val engine = GoalProgressEngine()

    @Test
    fun `empty active goals returns empty map`() {
        val result = engine.computeGoalProgress(
            activeGoals = emptyList(),
            baselineDailyMinutes = 180,
            actualDailyMinutes = 120
        )
        assertTrue(result.isEmpty())
    }

    @Test
    fun `no savings returns unchanged progress and savedHoursTotal`() {
        val goals = listOf(
            GoalEntity(
                id = 1L,
                title = "Study",
                targetScreenTimePerDay = 120,
                savedHoursTotal = 10,
                progress = 0.5f,
                targetDate = "2026-09-01"
            )
        )

        val result = engine.computeGoalProgress(
            activeGoals = goals,
            baselineDailyMinutes = 120,
            actualDailyMinutes = 120
        )

        val update = result[1L]
        assertEquals(10, update?.savedHoursTotal)
        assertEquals(0.5f, update?.progress ?: 0f, 0.001f)
    }

    @Test
    fun `positive savings increments saved hours and progress correctly`() {
        val goals = listOf(
            GoalEntity(
                id = 1L,
                title = "Study",
                targetScreenTimePerDay = 150, // weekly target hours = ((150 - 30) * 7) / 60 = 840 / 60 = 14 hours
                savedHoursTotal = 0,
                progress = 0.0f,
                targetDate = "2026-09-01"
            )
        )

        // Baseline = 270 mins, Actual = 150 mins -> Saved = 120 mins = 2 hours
        val result = engine.computeGoalProgress(
            activeGoals = goals,
            baselineDailyMinutes = 270,
            actualDailyMinutes = 150
        )

        val update = result[1L]
        assertEquals(2, update?.savedHoursTotal)
        // Weekly target hours = 14. 2 / 14 = 0.1428f
        assertEquals(0.1428f, update?.progress ?: 0f, 0.001f)
    }

    @Test
    fun `goal with null target date retains its progress but still accumulates savedHoursTotal`() {
        val goals = listOf(
            GoalEntity(
                id = 1L,
                title = "Reduce Screen Time",
                targetScreenTimePerDay = 120,
                savedHoursTotal = 5,
                progress = 0.3f,
                targetDate = null
            )
        )

        // Baseline = 240 mins, Actual = 120 mins -> Saved = 120 mins = 2 hours
        val result = engine.computeGoalProgress(
            activeGoals = goals,
            baselineDailyMinutes = 240,
            actualDailyMinutes = 120
        )

        val update = result[1L]
        assertEquals(7, update?.savedHoursTotal)
        assertEquals(0.3f, update?.progress ?: 0f, 0.001f)
    }

    @Test
    fun `goal with target date bounds progress between 0f and 1f`() {
        val goals = listOf(
            GoalEntity(
                id = 1L,
                title = "Study",
                targetScreenTimePerDay = 60, // weekly target hours = ((60 - 30) * 7) / 60 = 210 / 60 = 3.5 hours
                savedHoursTotal = 3,
                progress = 0.85f,
                targetDate = "2026-09-01"
            )
        )

        // Baseline = 180 mins, Actual = 60 mins -> Saved = 120 mins = 2 hours
        // newSaved = 3 + 2 = 5 hours
        // 5 hours / 3.5 hours > 1f -> should clamp to 1f
        val result = engine.computeGoalProgress(
            activeGoals = goals,
            baselineDailyMinutes = 180,
            actualDailyMinutes = 60
        )

        val update = result[1L]
        assertEquals(5, update?.savedHoursTotal)
        assertEquals(1.0f, update?.progress ?: 0f, 0.001f)
    }

    @Test
    fun `generateGoalInsight generates string with correct breakdown`() {
        val goals = listOf(
            GoalEntity(id = 1L, title = "Reading", targetScreenTimePerDay = 120),
            GoalEntity(id = 2L, title = "Exercise", targetScreenTimePerDay = 120)
        )

        val insight = engine.generateGoalInsight(goals, 10f)
        assertEquals("You saved 10 hours this week. 5h toward 'Reading'. 5h toward 'Exercise'.", insight)
    }
}
