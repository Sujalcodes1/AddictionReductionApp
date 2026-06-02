package com.example.addictionreductionapp.data.local.entities

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Persists every completed focus (Pomodoro-style) session.
 *
 * One row per completed session — never updated, only inserted.
 * Use [FocusSessionDao] queries with Flow to drive the analytics screen.
 *
 * Table: "focus_sessions"
 */
@Entity(tableName = "focus_sessions")
data class FocusSessionEntity(

    /** Auto-generated surrogate key. */
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "id")
    val id: Long = 0L,

    /** How long the session ran, in minutes. */
    @ColumnInfo(name = "duration_minutes")
    val durationMinutes: Int,

    /**
     * Unix epoch milliseconds when the session ended.
     * Stored as Long; no TypeConverter needed.
     */
    @ColumnInfo(name = "completed_at")
    val completedAt: Long,

    /**
     * Background audio type chosen by the user:
     * "silence" | "white_noise" | "rain" | "binaural"
     */
    @ColumnInfo(name = "sound_type")
    val soundType: String = "silence"
)
