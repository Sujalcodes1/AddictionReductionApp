package com.example.addictionreductionapp.data.analytics

import com.example.addictionreductionapp.data.models.CategoryAnalytics
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class FocusScoreEngineTest {
    private val engine = FocusScoreEngine()

    @Test
    fun `empty usage returns perfect score`() {
        val result = engine.calculateScore(0, 0, emptyList())
        assertEquals(100, result.score)
        assertEquals("No screen time recorded. Perfect focus!", result.explanation)
    }

    @Test
    fun `fully productive usage gives high score`() {
        val categories = listOf(CategoryAnalytics("Productivity", 120))
        val result = engine.calculateScore(120, 5, categories)
        assertTrue(result.score >= 80)
        assertTrue(result.productiveRatio > 0.9f)
    }

    @Test
    fun `fully distracting usage penalizes heavily`() {
        val categories = listOf(CategoryAnalytics("Social", 120))
        val result = engine.calculateScore(120, 5, categories)
        assertTrue(result.score <= 60)
        assertTrue(result.distractionRatio > 0.9f)
    }

    @Test
    fun `high app switching penalizes score`() {
        val categories = listOf(CategoryAnalytics("Other", 60))
        val result = engine.calculateScore(60, 100, categories)
        assertTrue(result.score < 100)
        assertTrue(result.totalAppSwitches == 100)
    }

    @Test
    fun `score is clamped between 0 and 100`() {
        // Extreme distraction + extreme switching
        val categories = listOf(CategoryAnalytics("Social", 500))
        val result = engine.calculateScore(500, 1000, categories)
        assertTrue(result.score in 0..100)
    }
}
