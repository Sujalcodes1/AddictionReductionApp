package com.example.addictionreductionapp.data.analytics

import com.example.addictionreductionapp.data.models.FocusScore
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.format.DateTimeFormatter

class StreakEngineTest {
    private val engine = StreakEngine()
    private val fmt = DateTimeFormatter.ofPattern("yyyy-MM-dd")
    private val today = LocalDate.now()

    private fun date(daysAgo: Long): String =
        today.minusDays(daysAgo).format(fmt)

    @Test
    fun `empty history returns zero streak`() {
        val result = engine.calculateStreak(emptyList(), targetScore = 50)
        assertEquals(0, result.streakInfo.currentStreakDays)
        assertFalse(result.isProductiveDayStreak)
    }

    @Test
    fun `single good day today gives streak of 1`() {
        val scores = listOf(FocusScore(date(0), 80))
        val result = engine.calculateStreak(scores, targetScore = 50)
        assertEquals(1, result.streakInfo.currentStreakDays)
        assertEquals(1, result.streakInfo.longestStreakDays)
        assertTrue(result.isProductiveDayStreak)
    }

    @Test
    fun `consecutive good days accumulate streak`() {
        val scores = listOf(
            FocusScore(date(2), 80),
            FocusScore(date(1), 75),
            FocusScore(date(0), 90)
        )
        val result = engine.calculateStreak(scores, targetScore = 50)
        assertEquals(3, result.streakInfo.currentStreakDays)
        assertEquals(3, result.streakInfo.longestStreakDays)
    }

    @Test
    fun `bad day below threshold does not extend streak`() {
        val scores = listOf(
            FocusScore(date(4), 80),
            FocusScore(date(3), 30),
            FocusScore(date(2), 30),
            FocusScore(date(1), 30),
            FocusScore(date(0), 90)
        )
        val result = engine.calculateStreak(scores, targetScore = 50)
        assertEquals(1, result.streakInfo.currentStreakDays)
        assertEquals(1, result.streakInfo.longestStreakDays)
    }

    @Test
    fun `gap of two or more days resets streak`() {
        val scores = listOf(
            FocusScore(date(3), 80),
            FocusScore(date(0), 90)
        )
        val result = engine.calculateStreak(scores, targetScore = 50)
        assertEquals(1, result.streakInfo.currentStreakDays)
    }

    @Test
    fun `one-day gap activates recovery logic`() {
        val scores = listOf(
            FocusScore(date(2), 80),
            FocusScore(date(0), 90)
        )
        val result = engine.calculateStreak(scores, targetScore = 50)
        assertEquals(2, result.streakInfo.currentStreakDays)
        assertTrue(result.isRecovered)
    }

    @Test
    fun `streak resets to zero if last active date is more than 1 day ago`() {
        val scores = listOf(
            FocusScore(date(3), 80)
        )
        val result = engine.calculateStreak(scores, targetScore = 50)
        assertEquals(0, result.streakInfo.currentStreakDays)
        assertFalse(result.isProductiveDayStreak)
    }

    @Test
    fun `longest streak is tracked across multiple segments`() {
        val scores = listOf(
            FocusScore(date(5), 80),
            FocusScore(date(4), 75),
            FocusScore(date(1), 90),
            FocusScore(date(0), 85)
        )
        val result = engine.calculateStreak(scores, targetScore = 50)
        assertEquals(2, result.streakInfo.longestStreakDays)
    }
}
