package com.example.addictionreductionapp.data.ai

import com.example.addictionreductionapp.data.local.entities.ChatMessageEntity
import com.example.addictionreductionapp.data.local.entities.GoalEntity
import com.example.addictionreductionapp.data.local.entities.UserProfileEntity
import com.example.addictionreductionapp.data.repository.AICoachRepository
import com.example.addictionreductionapp.data.repository.AppLimitRepository
import com.example.addictionreductionapp.data.repository.ChatMessageRepository
import com.example.addictionreductionapp.data.repository.DailyBehaviorSnapshotRepository
import com.example.addictionreductionapp.data.repository.GoalRepository
import com.example.addictionreductionapp.data.repository.InterventionRepository
import com.example.addictionreductionapp.data.repository.UserProfileRepository
import kotlinx.coroutines.flow.first
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CoachContextBuilder @Inject constructor(
    private val userProfileRepository: UserProfileRepository,
    private val appLimitRepository: AppLimitRepository,
    private val interventionRepository: InterventionRepository,
    private val goalRepository: GoalRepository,
    private val dailyBehaviorSnapshotRepository: DailyBehaviorSnapshotRepository,
    private val chatMessageRepository: ChatMessageRepository
) {

    suspend fun buildFullContext(userMessage: String): String {
        val profile = userProfileRepository.getProfile()
        val userName = profile?.userName ?: "User"
        val streak = profile?.streakCount ?: 0
        val sessions = profile?.sessionsCompleted ?: 0
        val bestStreak = profile?.longestStreak ?: 0

        val interventionCount = interventionRepository.countToday(
            System.currentTimeMillis() - (System.currentTimeMillis() % (24L * 60 * 60 * 1000))
        )

        var topApp = "social media"
        try {
            val selectedApps = appLimitRepository.getSelectedAppsOnce()
            topApp = selectedApps.firstOrNull()?.appName ?: "social media"
        } catch (_: Exception) {}

        val activeGoals = goalRepository.observeActiveGoals().first()
        val goalsText = if (activeGoals.isNotEmpty()) {
            activeGoals.joinToString(" ") { g ->
                "- ${g.title}: ${(g.progress * 100).toInt()}% complete, ${g.savedHoursTotal}h invested. "
            }
        } else ""

        val today = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.US).format(java.util.Date())
        val snapshots = dailyBehaviorSnapshotRepository.getHistoricalSnapshots(7).first()
        val todaySnapshot = snapshots.find { it.date == today }
        val screenTime = todaySnapshot?.totalScreenTimeMinutes ?: 0
        val focusScore = todaySnapshot?.focusScore ?: 100
        val riskScore = todaySnapshot?.overallRiskScore ?: 0f
        val doomscrollDetected = todaySnapshot?.doomscrollDetected == true
        val lateNightDetected = todaySnapshot?.lateNightUsageDetected == true
        val compulsiveDetected = todaySnapshot?.compulsiveSwitchingDetected == true

        val behaviorText = buildString {
            append("Screen time: ${screenTime}min. Focus score: $focusScore/100. Risk: ${(riskScore * 100).toInt()}%. ")
            if (doomscrollDetected) append("Doomscrolling detected. ")
            if (lateNightDetected) append("Late-night usage detected. ")
            if (compulsiveDetected) append("Frequent app switching detected. ")
            append("Top app: $topApp.")
        }

        // Recent chat history (last 3 exchanges)
        val recentMessages = chatMessageRepository.getRecentOnce(6)
        val historyText = if (recentMessages.size > 2) {
            recentMessages.takeLast(6).joinToString(" ") { "[${it.sender}]: ${it.text}" }
        } else ""

        return buildString {
            append("You are Arjuna, a compassionate digital wellness coach. ")
            append("USER PROFILE: Name=$userName, Streak=$streak, Best=$bestStreak, Sessions=$sessions, Interventions today=$interventionCount. ")
            if (goalsText.isNotEmpty()) append("GOALS: $goalsText")
            append("BEHAVIOR: $behaviorText")
            if (historyText.isNotEmpty()) append("HISTORY: $historyText")
            append("Keep responses 2-4 sentences. Reference user data. Be encouraging. Never suggest harmful behavior. User: $userMessage")
        }
    }
}
