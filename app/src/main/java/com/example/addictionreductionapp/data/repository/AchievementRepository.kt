package com.example.addictionreductionapp.data.repository

import com.example.addictionreductionapp.data.local.dao.AchievementDao
import com.example.addictionreductionapp.data.local.entities.AchievementEntity
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AchievementRepository @Inject constructor(
    private val achievementDao: AchievementDao
) {
    fun observeAll(): Flow<List<AchievementEntity>> = achievementDao.observeAll()

    suspend fun getAll(): List<AchievementEntity> = achievementDao.getAll()

    suspend fun upsertAll(achievements: List<AchievementEntity>) =
        achievementDao.upsertAll(achievements)

    suspend fun seedDefaults() {
        val existing = achievementDao.getAll()
        if (existing.isNotEmpty()) return

        val defaults = listOf(
            AchievementEntity("first_focus", "First Focus", "Complete your first focus session", "Target"),
            AchievementEntity("streak_3", "On Fire!", "Maintain a 3-day streak", "Streak"),
            AchievementEntity("streak_7", "Week Warrior", "Maintain a 7-day streak", "Lightning"),
            AchievementEntity("streak_30", "Monthly Master", "Maintain a 30-day streak", "Crown"),
            AchievementEntity("focus_60", "Hour Hero", "Complete 60 minutes of focus in one day", "Clock"),
            AchievementEntity("focus_300", "Deep Diver", "Complete 300 total minutes of focus", "Ocean"),
            AchievementEntity("block_5", "App Tamer", "Block 5 apps simultaneously", "Shield"),
            AchievementEntity("sessions_10", "Consistent", "Complete 10 focus sessions", "Strong"),
            AchievementEntity("sessions_50", "Dedicated", "Complete 50 focus sessions", "Trophy"),
            AchievementEntity("no_phone", "Zen Mode", "Stay off blocked apps for a full day", "Zen")
        )
        achievementDao.upsertAll(defaults)
    }
}
