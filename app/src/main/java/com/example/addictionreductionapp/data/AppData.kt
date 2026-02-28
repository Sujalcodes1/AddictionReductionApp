package com.example.addictionreductionapp.data

import android.content.Context
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf

// --- Data Models ---
data class AppTarget(
    val name: String,
    val packageName: String,
    val isSelected: Boolean = false,
    val limitMinutes: Int = 60,
    val isLocked: Boolean = false,
    val blockScheduleStart: Int = -1, // hour of day, -1 = no schedule
    val blockScheduleEnd: Int = -1,
    val isWhitelisted: Boolean = false
)

data class RealTimeUsage(
    val name: String,
    val packageName: String,
    val timeSpentMillis: Long,
    val limitMinutes: Int
)

data class DailyUsage(val dayLabel: String, val totalMillis: Long)

data class FocusSession(
    val durationMinutes: Int,
    val completedAt: Long,
    val soundType: String = "silence"
)

data class Achievement(
    val id: String,
    val title: String,
    val description: String,
    val icon: String,
    val isUnlocked: Boolean = false,
    val progress: Float = 0f
)

// --- Storage Helper ---
object AppDataStore {
    val apps = mutableStateListOf(
        AppTarget("Instagram", "com.instagram.android"),
        AppTarget("TikTok", "com.zhiliaoapp.musically"),
        AppTarget("YouTube", "com.google.android.youtube"),
        AppTarget("Twitter", "com.twitter.android"),
        AppTarget("Netflix", "com.netflix.mediaclient"),
        AppTarget("Snapchat", "com.snapchat.android"),
        AppTarget("Facebook", "com.facebook.katana"),
        AppTarget("Reddit", "com.reddit.frontpage"),
        AppTarget("WhatsApp", "com.whatsapp"),
        AppTarget("Telegram", "org.telegram.messenger")
    )

    var streakCount = mutableIntStateOf(0)
    var totalFocusMinutes = mutableIntStateOf(0)
    var sessionsCompleted = mutableIntStateOf(0)
    var userName = mutableStateOf("User")
    var hasCompletedOnboarding = mutableStateOf(false)
    var longestStreak = mutableIntStateOf(0)
    var isFocusModeActive = mutableStateOf(false)

    val achievements = mutableStateListOf(
        Achievement("first_focus", "First Focus", "Complete your first focus session", "🎯"),
        Achievement("streak_3", "On Fire!", "Maintain a 3-day streak", "🔥"),
        Achievement("streak_7", "Week Warrior", "Maintain a 7-day streak", "⚡"),
        Achievement("streak_30", "Monthly Master", "Maintain a 30-day streak", "👑"),
        Achievement("focus_60", "Hour Hero", "Complete 60 minutes of focus in one day", "⏰"),
        Achievement("focus_300", "Deep Diver", "Complete 300 total minutes of focus", "🌊"),
        Achievement("block_5", "App Tamer", "Block 5 apps simultaneously", "🛡️"),
        Achievement("sessions_10", "Consistent", "Complete 10 focus sessions", "💪"),
        Achievement("sessions_50", "Dedicated", "Complete 50 focus sessions", "🏆"),
        Achievement("no_phone", "Zen Mode", "Stay off blocked apps for a full day", "🧘")
    )

    fun saveToPrefs(context: Context) {
        val prefs = context.getSharedPreferences("regain_prefs", Context.MODE_PRIVATE)
        val editor = prefs.edit()
        apps.forEach { app ->
            editor.putBoolean("${app.packageName}_selected", app.isSelected)
            editor.putInt("${app.packageName}_limit", app.limitMinutes)
            editor.putBoolean("${app.packageName}_locked", app.isLocked)
            editor.putInt("${app.packageName}_schedule_start", app.blockScheduleStart)
            editor.putInt("${app.packageName}_schedule_end", app.blockScheduleEnd)
            editor.putBoolean("${app.packageName}_whitelisted", app.isWhitelisted)
        }
        editor.putInt("streak_count", streakCount.intValue)
        editor.putInt("total_focus_minutes", totalFocusMinutes.intValue)
        editor.putInt("sessions_completed", sessionsCompleted.intValue)
        editor.putString("user_name", userName.value)
        editor.putBoolean("onboarding_done", hasCompletedOnboarding.value)
        editor.putInt("longest_streak", longestStreak.intValue)
        editor.putBoolean("focus_mode_active", isFocusModeActive.value)
        editor.apply()
    }

    fun loadFromPrefs(context: Context) {
        val prefs = context.getSharedPreferences("regain_prefs", Context.MODE_PRIVATE)
        for (i in apps.indices) {
            val app = apps[i]
            apps[i] = app.copy(
                isSelected = prefs.getBoolean("${app.packageName}_selected", false),
                limitMinutes = prefs.getInt("${app.packageName}_limit", 60),
                isLocked = prefs.getBoolean("${app.packageName}_locked", false),
                blockScheduleStart = prefs.getInt("${app.packageName}_schedule_start", -1),
                blockScheduleEnd = prefs.getInt("${app.packageName}_schedule_end", -1),
                isWhitelisted = prefs.getBoolean("${app.packageName}_whitelisted", false)
            )
        }
        streakCount.intValue = prefs.getInt("streak_count", 0)
        totalFocusMinutes.intValue = prefs.getInt("total_focus_minutes", 0)
        sessionsCompleted.intValue = prefs.getInt("sessions_completed", 0)
        userName.value = prefs.getString("user_name", "User") ?: "User"
        hasCompletedOnboarding.value = prefs.getBoolean("onboarding_done", false)
        longestStreak.intValue = prefs.getInt("longest_streak", 0)
        isFocusModeActive.value = prefs.getBoolean("focus_mode_active", false)
        updateAchievements()
    }

    fun completeFocusSession(context: Context, durationMinutes: Int) {
        totalFocusMinutes.intValue += durationMinutes
        sessionsCompleted.intValue += 1
        saveToPrefs(context)
        updateAchievements()
    }

    fun incrementStreak(context: Context) {
        streakCount.intValue += 1
        if (streakCount.intValue > longestStreak.intValue) {
            longestStreak.intValue = streakCount.intValue
        }
        saveToPrefs(context)
        updateAchievements()
    }

    private fun updateAchievements() {
        for (i in achievements.indices) {
            val a = achievements[i]
            val unlocked = when (a.id) {
                "first_focus" -> sessionsCompleted.intValue >= 1
                "streak_3" -> streakCount.intValue >= 3
                "streak_7" -> streakCount.intValue >= 7
                "streak_30" -> streakCount.intValue >= 30
                "focus_60" -> totalFocusMinutes.intValue >= 60
                "focus_300" -> totalFocusMinutes.intValue >= 300
                "block_5" -> apps.count { it.isSelected } >= 5
                "sessions_10" -> sessionsCompleted.intValue >= 10
                "sessions_50" -> sessionsCompleted.intValue >= 50
                "no_phone" -> streakCount.intValue >= 1
                else -> false
            }
            val progress = when (a.id) {
                "first_focus" -> (sessionsCompleted.intValue.toFloat() / 1f).coerceAtMost(1f)
                "streak_3" -> (streakCount.intValue.toFloat() / 3f).coerceAtMost(1f)
                "streak_7" -> (streakCount.intValue.toFloat() / 7f).coerceAtMost(1f)
                "streak_30" -> (streakCount.intValue.toFloat() / 30f).coerceAtMost(1f)
                "focus_60" -> (totalFocusMinutes.intValue.toFloat() / 60f).coerceAtMost(1f)
                "focus_300" -> (totalFocusMinutes.intValue.toFloat() / 300f).coerceAtMost(1f)
                "block_5" -> (apps.count { it.isSelected }.toFloat() / 5f).coerceAtMost(1f)
                "sessions_10" -> (sessionsCompleted.intValue.toFloat() / 10f).coerceAtMost(1f)
                "sessions_50" -> (sessionsCompleted.intValue.toFloat() / 50f).coerceAtMost(1f)
                "no_phone" -> if (streakCount.intValue >= 1) 1f else 0f
                else -> 0f
            }
            achievements[i] = a.copy(isUnlocked = unlocked, progress = progress)
        }
    }
}
