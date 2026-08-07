package com.example.addictionreductionapp.data.analytics

import com.example.addictionreductionapp.data.local.entities.AchievementEntity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assert.assertFalse
import org.junit.Test

class AchievementEngineTest {

    private val engine = AchievementEngine()
    private val defaults = listOf(
        AchievementEntity(id = "first_focus", title = "First Focus", description = "...", icon = "Target"),
        AchievementEntity(id = "streak_3", title = "On Fire!", description = "...", icon = "Streak"),
        AchievementEntity(id = "streak_7", title = "Week Warrior", description = "...", icon = "Lightning"),
        AchievementEntity(id = "streak_30", title = "Monthly Master", description = "...", icon = "Crown"),
        AchievementEntity(id = "focus_60", title = "Hour Hero", description = "...", icon = "Clock"),
        AchievementEntity(id = "focus_300", title = "Deep Diver", description = "...", icon = "Ocean"),
        AchievementEntity(id = "block_5", title = "App Tamer", description = "...", icon = "Shield"),
        AchievementEntity(id = "sessions_10", title = "Consistent", description = "...", icon = "Strong"),
        AchievementEntity(id = "sessions_50", title = "Dedicated", description = "...", icon = "Trophy"),
        AchievementEntity(id = "no_phone", title = "Zen Mode", description = "...", icon = "Zen")
    )

    @Test
    fun `first focus unlocks with 1 session`() {
        val result = engine.compute(defaults, streak = 0, sessions = 1, focusMinutes = 0, blockedAppCount = 0)
        val achievement = result.find { it.id == "first_focus" }!!
        assertTrue(achievement.isUnlocked)
        assertEquals(1f, achievement.progress)
    }

    @Test
    fun `first focus stays locked with 0 sessions`() {
        val result = engine.compute(defaults, streak = 0, sessions = 0, focusMinutes = 0, blockedAppCount = 0)
        val achievement = result.find { it.id == "first_focus" }!!
        assertFalse(achievement.isUnlocked)
        assertEquals(0f, achievement.progress)
    }

    @Test
    fun `streak 3 unlocks at exactly 3 days`() {
        val result = engine.compute(defaults, streak = 3, sessions = 0, focusMinutes = 0, blockedAppCount = 0)
        val achievement = result.find { it.id == "streak_3" }!!
        assertTrue(achievement.isUnlocked)
        assertEquals(1f, achievement.progress)
    }

    @Test
    fun `streak 3 partial progress at 2 days`() {
        val result = engine.compute(defaults, streak = 2, sessions = 0, focusMinutes = 0, blockedAppCount = 0)
        val achievement = result.find { it.id == "streak_3" }!!
        assertFalse(achievement.isUnlocked)
        assertEquals(2f / 3f, achievement.progress, 0.01f)
    }

    @Test
    fun `streak 7 unlocks at 7 days`() {
        val result = engine.compute(defaults, streak = 7, sessions = 0, focusMinutes = 0, blockedAppCount = 0)
        val achievement = result.find { it.id == "streak_7" }!!
        assertTrue(achievement.isUnlocked)
    }

    @Test
    fun `streak 30 unlocks at 30 days`() {
        val result = engine.compute(defaults, streak = 30, sessions = 0, focusMinutes = 0, blockedAppCount = 0)
        val achievement = result.find { it.id == "streak_30" }!!
        assertTrue(achievement.isUnlocked)
    }

    @Test
    fun `focus_60 unlocks with 60 focus minutes`() {
        val result = engine.compute(defaults, streak = 0, sessions = 0, focusMinutes = 60, blockedAppCount = 0)
        val achievement = result.find { it.id == "focus_60" }!!
        assertTrue(achievement.isUnlocked)
        assertEquals(1f, achievement.progress)
    }

    @Test
    fun `focus_300 unlocks with 300 focus minutes`() {
        val result = engine.compute(defaults, streak = 0, sessions = 0, focusMinutes = 300, blockedAppCount = 0)
        val achievement = result.find { it.id == "focus_300" }!!
        assertTrue(achievement.isUnlocked)
    }

    @Test
    fun `block_5 unlocks with 5 blocked apps`() {
        val result = engine.compute(defaults, streak = 0, sessions = 0, focusMinutes = 0, blockedAppCount = 5)
        val achievement = result.find { it.id == "block_5" }!!
        assertTrue(achievement.isUnlocked)
    }

    @Test
    fun `sessions_10 unlocks with 10 sessions`() {
        val result = engine.compute(defaults, streak = 0, sessions = 10, focusMinutes = 0, blockedAppCount = 0)
        val achievement = result.find { it.id == "sessions_10" }!!
        assertTrue(achievement.isUnlocked)
    }

    @Test
    fun `sessions_50 unlocks with 50 sessions`() {
        val result = engine.compute(defaults, streak = 0, sessions = 50, focusMinutes = 0, blockedAppCount = 0)
        val achievement = result.find { it.id == "sessions_50" }!!
        assertTrue(achievement.isUnlocked)
    }

    @Test
    fun `all achievements unlocked with extreme values`() {
        val result = engine.compute(defaults, streak = 30, sessions = 50, focusMinutes = 300, blockedAppCount = 5)
        val unlockedCount = result.count { it.isUnlocked }
        assertEquals(10, unlockedCount)
    }

    @Test
    fun `progress never exceeds 1f`() {
        val result = engine.compute(defaults, streak = 100, sessions = 200, focusMinutes = 1000, blockedAppCount = 20)
        result.forEach { a ->
            assertTrue("${a.id} progress ${a.progress} > 1", a.progress <= 1f)
        }
    }

    @Test
    fun `unknown achievement id remains locked`() {
        val withUnknown = defaults + AchievementEntity(id = "xyz", title = "X", description = "...", icon = "?")
        val result = engine.compute(withUnknown, streak = 100, sessions = 200, focusMinutes = 1000, blockedAppCount = 20)
        val unknown = result.find { it.id == "xyz" }!!
        assertFalse(unknown.isUnlocked)
        assertEquals(0f, unknown.progress)
    }
}
