package com.example.addictionreductionapp.data.repository

import com.example.addictionreductionapp.data.analytics.AchievementEngine
import com.example.addictionreductionapp.data.local.dao.DailyBehaviorSnapshotDao
import com.example.addictionreductionapp.data.local.entities.UserProfileEntity
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.first
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class StreakSyncManager @Inject constructor(
    private val snapshotDao: DailyBehaviorSnapshotDao,
    private val userProfileRepository: UserProfileRepository,
    private val achievementRepository: AchievementRepository,
    private val achievementEngine: AchievementEngine
) {
    companion object {
        private const val STREAK_THRESHOLD = 50
    }

    suspend fun syncTodayStreak() {
        val today = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())
        val profile = userProfileRepository.getProfile() ?: return
        val lastStreakDate = profile.lastStreakDate

        if (lastStreakDate == today) return

        val snapshot = snapshotDao.getSnapshotByDate(today).first()
        if (snapshot == null) return

        if (snapshot.focusScore >= STREAK_THRESHOLD) {
            userProfileRepository.incrementStreak()
            val updated = userProfileRepository.getProfile()
            if (updated != null && updated.streakCount > updated.longestStreak) {
                userProfileRepository.upsert(updated.copy(longestStreak = updated.streakCount))
            }
            userProfileRepository.upsert(profile.copy(lastStreakDate = today))
        } else {
            userProfileRepository.resetStreak()
            userProfileRepository.upsert(profile.copy(lastStreakDate = today))
        }

        recomputeAchievements()
    }

    private suspend fun recomputeAchievements() {
        val profile = userProfileRepository.getProfile() ?: return
        val existing = achievementRepository.getAll()
        val updated = achievementEngine.compute(
            existing = existing,
            streak = profile.streakCount,
            sessions = profile.sessionsCompleted,
            focusMinutes = profile.totalFocusMinutes,
            blockedAppCount = 0
        )
        achievementRepository.upsertAll(updated)
    }
}
