package com.example.addictionreductionapp.data.local.entities

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Persists the user's profile and global streak / progress stats.
 *
 * This is a single-row table (id is always 1).  Use [UserProfileDao.upsert]
 * to create-or-replace the single record.
 *
 * Table: "user_profile"
 */
@Entity(tableName = "user_profile")
data class UserProfileEntity(

    /**
     * Always 1 — enforced by the default value.
     * Using a fixed PK makes upsert semantics trivial.
     */
    @PrimaryKey
    @ColumnInfo(name = "id")
    val id: Int = 1,

    /** The name shown in the profile / home screen greeting. */
    @ColumnInfo(name = "user_name")
    val userName: String = "User",

    /** Consecutive days without breaking an app-limit. */
    @ColumnInfo(name = "streak_count")
    val streakCount: Int = 0,

    /** Historical maximum streak, never decremented. */
    @ColumnInfo(name = "longest_streak")
    val longestStreak: Int = 0,

    /** Cumulative focus minutes across all sessions. */
    @ColumnInfo(name = "total_focus_minutes")
    val totalFocusMinutes: Int = 0,

    /** Count of fully completed focus sessions. */
    @ColumnInfo(name = "sessions_completed")
    val sessionsCompleted: Int = 0,

    /** True once the user has finished the onboarding flow. */
    @ColumnInfo(name = "has_completed_onboarding")
    val hasCompletedOnboarding: Boolean = false,

    /** True while focus mode is currently active. */
    @ColumnInfo(name = "is_focus_mode_active")
    val isFocusModeActive: Boolean = false,

    /** True after successful login. */
    @ColumnInfo(name = "is_logged_in")
    val isLoggedIn: Boolean = false
)
