package com.example.addictionreductionapp.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.addictionreductionapp.data.local.entities.FocusSessionEntity
import kotlinx.coroutines.flow.Flow

/**
 * Data Access Object for [FocusSessionEntity].
 *
 * Sessions are append-only (no UPDATE / DELETE) because they form the
 * audit trail that drives analytics.  Deleting is only exposed via the
 * admin "wipe data" path.
 */
@Dao
interface FocusSessionDao {

    // ── Reads ─────────────────────────────────────────────────────────────────

    /**
     * All sessions, newest first.
     * Flow re-emits whenever a new session is inserted.
     */
    @Query("SELECT * FROM focus_sessions ORDER BY completed_at DESC")
    fun getAllSessions(): Flow<List<FocusSessionEntity>>

    /**
     * Sessions completed within the given epoch-ms window [from, to].
     * Use this to build daily / weekly / monthly charts.
     */
    @Query(
        """
        SELECT * FROM focus_sessions 
        WHERE completed_at BETWEEN :fromEpochMs AND :toEpochMs
        ORDER BY completed_at DESC
        """
    )
    fun getSessionsInRange(fromEpochMs: Long, toEpochMs: Long): Flow<List<FocusSessionEntity>>

    /**
     * Total focus minutes summed across ALL sessions.
     * Returns null when the table is empty (handled as 0 upstream).
     */
    @Query("SELECT SUM(duration_minutes) FROM focus_sessions")
    fun getTotalFocusMinutes(): Flow<Int?>

    /**
     * Count of completed sessions — drives achievements.
     */
    @Query("SELECT COUNT(*) FROM focus_sessions")
    fun getSessionCount(): Flow<Int>

    /**
     * Total minutes within a specific day (epoch start → end of that day).
     * Used by the accessibility service to enforce daily limits.
     */
    @Query(
        """
        SELECT COALESCE(SUM(duration_minutes), 0) FROM focus_sessions
        WHERE completed_at BETWEEN :dayStartMs AND :dayEndMs
        """
    )
    suspend fun getTotalMinutesOnDay(dayStartMs: Long, dayEndMs: Long): Int

    // ── Writes ────────────────────────────────────────────────────────────────

    /**
     * Insert a completed session.  IGNORE strategy means a duplicate id
     * (should never happen with autoGenerate) is silently skipped.
     */
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(session: FocusSessionEntity): Long

    /** Wipe entire history — exposed only for "reset all data" use-case. */
    @Query("DELETE FROM focus_sessions")
    suspend fun deleteAll()
}
