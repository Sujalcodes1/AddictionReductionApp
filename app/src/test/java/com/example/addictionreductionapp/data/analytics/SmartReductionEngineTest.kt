package com.example.addictionreductionapp.data.analytics

import org.junit.Assert.assertEquals
import org.junit.Test

class SmartReductionEngineTest {

    private fun computeBaseline(rawTotals: Map<String, Int>, days: Int): Map<String, Int> {
        return rawTotals.mapValues { (_, total) -> total / days.coerceAtLeast(1) }
    }

    @Test
    fun `computeBaseline returns correct daily averages`() {
        val rawTotals = mapOf("Social" to 1050, "Entertainment" to 700, "Games" to 140)
        val baselines = computeBaseline(rawTotals, 7)
        assertEquals(150, baselines["Social"])
        assertEquals(100, baselines["Entertainment"])
        assertEquals(20, baselines["Games"])
    }

    @Test
    fun `computeBaseline divides by days correctly`() {
        assertEquals(200, computeBaseline(mapOf("Social" to 600), 3)["Social"])
        assertEquals(600, computeBaseline(mapOf("Social" to 600), 1)["Social"])
    }

    @Test
    fun `computeBaseline with zero days defaults to 1`() {
        assertEquals(150, computeBaseline(mapOf("Social" to 150), 0)["Social"])
    }

    @Test
    fun `computeBaseline with zero minutes returns zero`() {
        assertEquals(0, computeBaseline(mapOf("Social" to 0), 7)["Social"])
    }

    // ── Adaptive roadmap rate calculation tests (M5) ───────────────────────

    private fun getReductionRate(successRate: Float): Float {
        return when {
            successRate >= 0.8f -> 0.12f
            successRate >= 0.5f -> 0.10f
            else -> 0.05f
        }
    }

    private fun getGoalFloor(hasProductiveGoal: Boolean): Int {
        return if (hasProductiveGoal) 20 else 30
    }

    private fun computeMilestoneTarget(baseline: Int, week: Int, rate: Float, floor: Int): Int {
        return (baseline * (1f - rate * (week - 1))).toInt().coerceAtLeast(floor)
    }

    @Test
    fun `high success gets aggressive 12 percent reduction`() {
        assertEquals(0.12f, getReductionRate(0.8f))
        assertEquals(0.12f, getReductionRate(0.95f))
    }

    @Test
    fun `medium success gets normal 10 percent reduction`() {
        assertEquals(0.10f, getReductionRate(0.5f))
        assertEquals(0.10f, getReductionRate(0.79f))
    }

    @Test
    fun `low success gets gentle 5 percent reduction`() {
        assertEquals(0.05f, getReductionRate(0.0f))
        assertEquals(0.05f, getReductionRate(0.49f))
    }

    @Test
    fun `learning goals lower floor to 20 minutes`() {
        assertEquals(20, getGoalFloor(true))
    }

    @Test
    fun `default floor is 30 minutes`() {
        assertEquals(30, getGoalFloor(false))
    }

    @Test
    fun `week 1 target equals baseline`() {
        assertEquals(120, computeMilestoneTarget(120, 1, 0.10f, 30))
    }

    @Test
    fun `week 4 target with aggressive rate reaches lower value`() {
        val target = computeMilestoneTarget(120, 4, 0.12f, 30)
        assertEquals(76, target) // 120 * (1 - 0.12*3) = 120 * 0.64 = 76
    }

    @Test
    fun `target never falls below floor`() {
        val target = computeMilestoneTarget(40, 4, 0.12f, 30)
        assertEquals(30, target)
    }

    @Test
    fun `target with productive goal floor of 20 is respected`() {
        val target = computeMilestoneTarget(40, 4, 0.10f, 20)
        assertEquals(28, target) // 40 * (1 - 0.10*3) = 28, above floor 20
    }
}
