package com.example.addictionreductionapp.data

// ── Data Models ─────────────────────────────────────────────────────────────

data class AppTarget(
    val name: String,
    val packageName: String,
    val isSelected: Boolean = false,
    val limitMinutes: Int = 60,
    val isLocked: Boolean = false,
    val blockScheduleStart: Int = -1,
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

// ── Static Data (seed on first launch) ─────────────────────────────────────

val DEFAULT_APPS = listOf(
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

val DEFAULT_ACHIEVEMENTS = listOf(
    Achievement("first_focus", "First Focus", "Complete your first focus session", "Target"),
    Achievement("streak_3", "On Fire!", "Maintain a 3-day streak", "Streak"),
    Achievement("streak_7", "Week Warrior", "Maintain a 7-day streak", "Lightning"),
    Achievement("streak_30", "Monthly Master", "Maintain a 30-day streak", "Crown"),
    Achievement("focus_60", "Hour Hero", "Complete 60 minutes of focus in one day", "Clock"),
    Achievement("focus_300", "Deep Diver", "Complete 300 total minutes of focus", "Ocean"),
    Achievement("block_5", "App Tamer", "Block 5 apps simultaneously", "Shield"),
    Achievement("sessions_10", "Consistent", "Complete 10 focus sessions", "Strong"),
    Achievement("sessions_50", "Dedicated", "Complete 50 focus sessions", "Trophy"),
    Achievement("no_phone", "Zen Mode", "Zen Mode", "Zen")
)
