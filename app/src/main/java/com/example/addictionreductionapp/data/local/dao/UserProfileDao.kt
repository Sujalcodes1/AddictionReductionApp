package com.example.addictionreductionapp.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.addictionreductionapp.data.local.entities.UserProfileEntity
import kotlinx.coroutines.flow.Flow

/**
 * Data Access Object for [UserProfileEntity].
 *
 * The "user_profile" table always has exactly one row (id = 1).
 * All writes go through [upsert] which uses REPLACE strategy.
 */
@Dao
interface UserProfileDao {

    // ── Reads ─────────────────────────────────────────────────────────────────

    /**
     * Observe the single user profile row.
     * Emits null until the first [upsert] is called (e.g. first launch).
     * The UI should treat null as "use defaults".
     */
    @Query("SELECT * FROM user_profile WHERE id = 1 LIMIT 1")
    fun observeProfile(): Flow<UserProfileEntity?>

    /**
     * Non-Flow one-shot fetch.  Use inside WorkManager or Service code
     * where you need a direct result on an IO coroutine.
     */
    @Query("SELECT * FROM user_profile WHERE id = 1 LIMIT 1")
    suspend fun getProfile(): UserProfileEntity?

    // ── Writes ────────────────────────────────────────────────────────────────

    /**
     * Create-or-replace the profile row.
     * Because the id is always 1, REPLACE effectively updates all fields
     * in a single atomic operation — no separate INSERT / UPDATE needed.
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(profile: UserProfileEntity)

    /**
     * Convenience: increment streak count by 1 in a single SQL statement.
     * More efficient than a full read-modify-write cycle for frequent updates.
     */
    @Query("UPDATE user_profile SET streak_count = streak_count + 1 WHERE id = 1")
    suspend fun incrementStreak()

    /**
     * Convenience: add [minutes] to total focus minutes and increment session count.
     */
    @Query(
        """
        UPDATE user_profile 
        SET total_focus_minutes = total_focus_minutes + :minutes,
            sessions_completed  = sessions_completed  + 1
        WHERE id = 1
        """
    )
    suspend fun recordCompletedSession(minutes: Int)
}
