package com.example.addictionreductionapp.data.repository

import com.example.addictionreductionapp.data.local.dao.UserProfileDao
import com.example.addictionreductionapp.data.local.entities.UserProfileEntity
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Repository for the user profile / aggregate stats.
 *
 * The "user_profile" table is a single-row table (id = 1).
 * All writes use the REPLACE on-conflict strategy, making upsert semantics trivial.
 *
 * ## MVVM wiring
 * ```
 * UserProfileDao  ──injected by Hilt──►  UserProfileRepository
 *                                               │
 *                                        @Inject constructor
 *                                               │
 *                                        UserProfileViewModel (@HiltViewModel)
 *                                               │
 *                                        hiltViewModel() in Compose
 * ```
 *
 * @param userProfileDao Provided by [com.example.addictionreductionapp.data.local.database.DatabaseModule].
 */
@Singleton
class UserProfileRepository @Inject constructor(
    private val userProfileDao: UserProfileDao
) {

    // ── Reads ─────────────────────────────────────────────────────────────────

    /**
     * Live stream of the user profile.
     * Emits null until the first [upsert] — treat null as "use defaults".
     */
    fun observeProfile(): Flow<UserProfileEntity?> = userProfileDao.observeProfile()

    /**
     * One-shot profile fetch for Workers / Services.
     * Must be called from an IO coroutine.
     */
    suspend fun getProfile(): UserProfileEntity? = userProfileDao.getProfile()

    // ── Writes ────────────────────────────────────────────────────────────────

    /**
     * Create or fully replace the profile row.
     * Because the id is always 1, this is effectively an atomic upsert.
     */
    suspend fun upsert(profile: UserProfileEntity) = userProfileDao.upsert(profile)

    /**
     * Atomic streak increment — avoids a costly read-modify-write cycle.
     * Call this at the end of each day the user met their goal.
     */
    suspend fun incrementStreak() = userProfileDao.incrementStreak()

    /**
     * Atomically add [minutes] to total focus time and bump the session counter.
     * Call this immediately after a focus session is persisted.
     *
     * @param minutes Duration of the completed session in minutes.
     */
    suspend fun recordCompletedSession(minutes: Int) =
        userProfileDao.recordCompletedSession(minutes)
}
