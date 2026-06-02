package com.example.addictionreductionapp.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.addictionreductionapp.data.local.entities.AppLimitEntity
import kotlinx.coroutines.flow.Flow

/**
 * Data Access Object for [AppLimitEntity].
 *
 * All query methods return [Flow] so that the UI layer can observe database
 * changes reactively without any manual refresh calls.
 *
 * Suspend functions are safe to call from any coroutine (Room dispatches them
 * to an internal IO thread pool automatically).
 */
@Dao
interface AppLimitDao {

    // ── Reads ─────────────────────────────────────────────────────────────────

    /**
     * Observe the full list of configured app limits.
     * The Flow re-emits whenever ANY row in "app_limits" changes.
     */
    @Query("SELECT * FROM app_limits ORDER BY app_name ASC")
    fun getAllApps(): Flow<List<AppLimitEntity>>

    /**
     * Observe only apps that the user has selected for monitoring.
     */
    @Query("SELECT * FROM app_limits WHERE is_selected = 1 ORDER BY app_name ASC")
    fun getSelectedApps(): Flow<List<AppLimitEntity>>

    /**
     * Fetch a single app by package name, or null if it doesn't exist.
     * Returns Flow so the UI can react to updates on that specific row.
     */
    @Query("SELECT * FROM app_limits WHERE package_name = :packageName LIMIT 1")
    fun getAppByPackage(packageName: String): Flow<AppLimitEntity?>

    /**
     * One-shot (non-Flow) fetch for use inside Workers or background tasks.
     */
    @Query("SELECT * FROM app_limits WHERE package_name = :packageName LIMIT 1")
    suspend fun getAppByPackageOnce(packageName: String): AppLimitEntity?

    /**
     * One-shot (non-Flow) fetch for use inside Workers or background tasks
     * where you need a direct result rather than a stream.
     */
    @Query("SELECT * FROM app_limits WHERE is_selected = 1")
    suspend fun getSelectedAppsOnce(): List<AppLimitEntity>

    // ── Writes ────────────────────────────────────────────────────────────────

    /**
     * Insert or replace an app-limit row.
     * REPLACE strategy: if a row with the same packageName already exists,
     * it is deleted and the new row is inserted — effectively an upsert.
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(app: AppLimitEntity)

    /**
     * Bulk upsert — useful for seeding the database on first launch
     * with the default app list.
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(apps: List<AppLimitEntity>)

    /** Partial update: changes only the columns tracked by [AppLimitEntity]. */
    @Update
    suspend fun update(app: AppLimitEntity)

    /** Remove a specific app's configuration permanently. */
    @Delete
    suspend fun delete(app: AppLimitEntity)

    /** Remove everything — useful for testing or "reset all" feature. */
    @Query("DELETE FROM app_limits")
    suspend fun deleteAll()

    // ── Aggregates ────────────────────────────────────────────────────────────

    /** Observe count of currently selected (monitored) apps. */
    @Query("SELECT COUNT(*) FROM app_limits WHERE is_selected = 1")
    fun getSelectedAppCount(): Flow<Int>
}
