package com.example.addictionreductionapp.data.repository

import com.example.addictionreductionapp.data.local.dao.FocusSessionDao
import com.example.addictionreductionapp.data.local.entities.FocusSessionEntity
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Repository for focus-session analytics data.
 *
 * Sessions are append-only — no UPDATE or DELETE is exposed here
 * (by design: sessions form the immutable audit trail for analytics).
 *
 * ## MVVM wiring
 * ```
 * FocusSessionDao  ──injected by Hilt──►  FocusSessionRepository
 *                                                │
 *                                         @Inject constructor
 *                                                │
 *                                         FocusSessionViewModel (@HiltViewModel)
 *                                                │
 *                                         hiltViewModel() in Compose
 * ```
 *
 * @param focusSessionDao Provided by [com.example.addictionreductionapp.data.local.database.DatabaseModule].
 */
@Singleton
class FocusSessionRepository @Inject constructor(
    private val focusSessionDao: FocusSessionDao
) {

    // ── Reads ─────────────────────────────────────────────────────────────────

    /** Live stream of all sessions, newest first. */
    fun getAllSessions(): Flow<List<FocusSessionEntity>> = focusSessionDao.getAllSessions()

    /**
     * Live stream of sessions completed within [fromEpochMs]..[toEpochMs].
     * Use this to populate daily / weekly / monthly chart data.
     */
    fun getSessionsInRange(fromEpochMs: Long, toEpochMs: Long): Flow<List<FocusSessionEntity>> =
        focusSessionDao.getSessionsInRange(fromEpochMs, toEpochMs)

    /**
     * Live total focus minutes across ALL sessions.
     * Emits null when the table is empty — treat as 0 in the UI.
     */
    fun getTotalFocusMinutes(): Flow<Int?> = focusSessionDao.getTotalFocusMinutes()

    /** Live count of completed sessions — drives achievement unlocking. */
    fun getSessionCount(): Flow<Int> = focusSessionDao.getSessionCount()

    /**
     * One-shot total minutes on a specific day.
     * Intended for the Accessibility Service to enforce daily time limits.
     * Must be called from an IO coroutine.
     */
    suspend fun getTotalMinutesOnDay(dayStartMs: Long, dayEndMs: Long): Int =
        focusSessionDao.getTotalMinutesOnDay(dayStartMs, dayEndMs)

    // ── Writes ────────────────────────────────────────────────────────────────

    /**
     * Record a completed focus session.
     * Returns the newly inserted row ID, or -1 if a duplicate id was ignored
     * (should never happen with autoGenerate = true).
     */
    suspend fun insert(session: FocusSessionEntity): Long = focusSessionDao.insert(session)

    /** Wipe entire session history — use ONLY for the "reset all data" flow. */
    suspend fun deleteAll() = focusSessionDao.deleteAll()
}
