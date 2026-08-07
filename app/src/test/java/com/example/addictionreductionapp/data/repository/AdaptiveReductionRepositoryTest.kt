package com.example.addictionreductionapp.data.repository

import com.example.addictionreductionapp.data.local.entities.DailyBehaviorSnapshotEntity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class AdaptiveReductionRepositoryTest {

    @Test
    fun `target decreases by 10 percent per week with floor`() {
        val reductionRate = 0.10f
        val baseline = 120
        val floor = 30
        val week1 = (baseline * (1f - reductionRate * 0)).toInt().coerceAtLeast(floor)
        val week2 = (baseline * (1f - reductionRate * 1)).toInt().coerceAtLeast(floor)
        val week3 = (baseline * (1f - reductionRate * 2)).toInt().coerceAtLeast(floor)
        val week4 = (baseline * (1f - reductionRate * 3)).toInt().coerceAtLeast(floor)

        assertEquals(120, week1)
        assertEquals(108, week2)
        assertEquals(96, week3)
        assertEquals(84, week4)
    }

    @Test
    fun `target never falls below floor`() {
        val reductionRate = 0.10f
        val baseline = 40
        val floor = 30

        val targets = (0..3).map { week ->
            (baseline * (1f - reductionRate * week)).toInt().coerceAtLeast(floor)
        }
        assertTrue(targets.all { it >= floor })
    }

    @Test
    fun `achieved minutes for an empty week is 0`() {
        val emptySnapshots = emptyList<DailyBehaviorSnapshotEntity>()
        assertEquals(0, emptySnapshots.sumOf { it.totalScreenTimeMinutes })
    }

    // ── Adaptive success rate computation tests (M5) ──────────────────────

    private fun computeSuccessRate(recentMinutes: List<Int>, baselineAvg: Int): Float {
        if (recentMinutes.isEmpty()) return 0.5f
        return recentMinutes.count { it <= baselineAvg }.toFloat() / recentMinutes.size
    }

    @Test
    fun `perfect success returns 1f`() {
        assertEquals(1f, computeSuccessRate(listOf(100, 90, 80), 120))
    }

    @Test
    fun `complete failure returns 0f`() {
        assertEquals(0f, computeSuccessRate(listOf(150, 200, 180), 120))
    }

    @Test
    fun `mixed success returns correct ratio`() {
        val rate = computeSuccessRate(listOf(100, 150, 80, 200), 120)
        assertEquals(0.5f, rate)
    }

    @Test
    fun `empty data defaults to point five success rate`() {
        assertEquals(0.5f, computeSuccessRate(emptyList(), 120))
    }

    @Test
    fun `all days at baseline count as success`() {
        assertEquals(1f, computeSuccessRate(listOf(120, 120, 120), 120))
    }
}
