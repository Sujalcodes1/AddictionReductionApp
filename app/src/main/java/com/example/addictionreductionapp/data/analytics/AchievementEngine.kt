package com.example.addictionreductionapp.data.analytics

import com.example.addictionreductionapp.data.local.entities.AchievementEntity
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AchievementEngine @Inject constructor() {

    fun compute(
        existing: List<AchievementEntity>,
        streak: Int,
        sessions: Int,
        focusMinutes: Int,
        blockedAppCount: Int
    ): List<AchievementEntity> {
        return existing.map { a ->
            val unlocked = when (a.id) {
                "first_focus" -> sessions >= 1
                "streak_3" -> streak >= 3
                "streak_7" -> streak >= 7
                "streak_30" -> streak >= 30
                "focus_60" -> focusMinutes >= 60
                "focus_300" -> focusMinutes >= 300
                "block_5" -> blockedAppCount >= 5
                "sessions_10" -> sessions >= 10
                "sessions_50" -> sessions >= 50
                "no_phone" -> streak >= 1
                else -> false
            }
            val progress = when (a.id) {
                "first_focus" -> (sessions.toFloat() / 1f).coerceAtMost(1f)
                "streak_3" -> (streak.toFloat() / 3f).coerceAtMost(1f)
                "streak_7" -> (streak.toFloat() / 7f).coerceAtMost(1f)
                "streak_30" -> (streak.toFloat() / 30f).coerceAtMost(1f)
                "focus_60" -> (focusMinutes.toFloat() / 60f).coerceAtMost(1f)
                "focus_300" -> (focusMinutes.toFloat() / 300f).coerceAtMost(1f)
                "block_5" -> (blockedAppCount.toFloat() / 5f).coerceAtMost(1f)
                "sessions_10" -> (sessions.toFloat() / 10f).coerceAtMost(1f)
                "sessions_50" -> (sessions.toFloat() / 50f).coerceAtMost(1f)
                "no_phone" -> if (streak >= 1) 1f else 0f
                else -> 0f
            }
            a.copy(isUnlocked = unlocked, progress = progress)
        }
    }
}
