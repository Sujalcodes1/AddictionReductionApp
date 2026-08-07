package com.example.addictionreductionapp.data.analytics

import com.example.addictionreductionapp.data.models.AppUsageSummary
import com.example.addictionreductionapp.data.models.FocusScoreDetails
import com.example.addictionreductionapp.data.models.HourlyUsagePoint
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class BehavioralIntelligenceEngineTest {
    private val engine = BehavioralIntelligenceEngine()

    @Test
    fun `doomscroll detected for long continuous sessions with few opens`() {
        val usage = listOf(
            AppUsageSummary("com.instagram.android", "Instagram", 90, 3)
        )
        val result = engine.analyzeDoomscroll(usage)
        assertTrue(result.detected)
        assertEquals("com.instagram.android", result.appPackage)
        assertTrue(result.severityScore > 0f)
    }

    @Test
    fun `no doomscroll for moderate usage`() {
        val usage = listOf(
            AppUsageSummary("com.instagram.android", "Instagram", 30, 15)
        )
        val result = engine.analyzeDoomscroll(usage)
        assertFalse(result.detected)
    }

    @Test
    fun `compulsive switching detected for high open counts`() {
        val result = engine.detectCompulsiveSwitching(
            totalOpens = 50,
            totalScreenTimeMinutes = 30
        )
        assertTrue(result.detected)
        assertTrue(result.switchesPerHour > 30f)
    }

    @Test
    fun `low switching not flagged as compulsive`() {
        val result = engine.detectCompulsiveSwitching(
            totalOpens = 15,
            totalScreenTimeMinutes = 60
        )
        assertFalse(result.detected)
    }

    @Test
    fun `late night usage detected after 10pm`() {
        val hourly = listOf(HourlyUsagePoint(23, 30))
        val result = engine.detectLateNightUsage(hourly)
        assertTrue(result.detected)
        assertEquals(30, result.minutesUsed)
    }

    @Test
    fun `no late night flag for minimal usage`() {
        val hourly = listOf(HourlyUsagePoint(23, 5))
        val result = engine.detectLateNightUsage(hourly)
        assertFalse(result.detected)
    }

    @Test
    fun `addiction spike detected when usage far above baseline`() {
        val result = engine.detectAddictionSpike(
            todayMinutes = 120,
            weeklyAverageMinutes = 60
        )
        assertTrue(result.detected)
        assertTrue(result.percentageIncrease > 50f)
    }

    @Test
    fun `no spike when usage near baseline`() {
        val result = engine.detectAddictionSpike(
            todayMinutes = 60,
            weeklyAverageMinutes = 50
        )
        assertFalse(result.detected)
    }

    @Test
    fun `productivity decay detected when ratio drops`() {
        val recent = listOf(
            FocusScoreDetails(90, 0.8f, 0.2f, 10, "high"),
            FocusScoreDetails(70, 0.4f, 0.6f, 15, "lower")
        )
        val result = engine.detectProductivityDecay(recent)
        assertTrue(result.detected)
    }

    @Test
    fun `no decay with stable productivity`() {
        val recent = listOf(
            FocusScoreDetails(90, 0.8f, 0.2f, 10, "high"),
            FocusScoreDetails(88, 0.78f, 0.22f, 12, "stable")
        )
        val result = engine.detectProductivityDecay(recent)
        assertFalse(result.detected)
    }

    @Test
    fun `generateSnapshot aggregates all signals`() {
        val usage = listOf(
            AppUsageSummary("com.instagram.android", "Instagram", 90, 3)
        )
        val hourly = listOf(HourlyUsagePoint(14, 40), HourlyUsagePoint(23, 25))
        val recent = listOf(
            FocusScoreDetails(90, 0.8f, 0.2f, 10, "high"),
            FocusScoreDetails(40, 0.3f, 0.7f, 20, "bad")
        )

        val snapshot = engine.generateSnapshot(
            usageToday = usage,
            hourlyUsage = hourly,
            totalOpens = 30,
            totalScreenTimeMinutes = 130,
            weeklyAverageMinutes = 60,
            recentScores = recent
        )

        assertTrue(snapshot.doomscroll.detected)
        assertTrue(snapshot.lateNightUsage.detected)
        assertTrue(snapshot.overallRiskScore in 0f..1f)
    }
}
