package com.example.addictionreductionapp.data.repository

import com.example.addictionreductionapp.data.local.dao.AppLimitDao
import com.example.addictionreductionapp.data.local.entities.AppLimitEntity
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Repository for app-limit (blocking) data.
 *
 * Acts as the single source of truth for [AppLimitEntity] records.
 * The UI / ViewModel layer must NEVER talk directly to [AppLimitDao].
 *
 * ## MVVM wiring
 * ```
 * AppLimitDao  ──injected by Hilt──►  AppLimitRepository
 *                                           │
 *                                    @Inject constructor
 *                                           │
 *                                    AppBlockerViewModel (@HiltViewModel)
 *                                           │
 *                                    hiltViewModel() in Compose
 * ```
 *
 * @param appLimitDao Provided by [com.example.addictionreductionapp.data.local.database.DatabaseModule].
 */
@Singleton
class AppLimitRepository @Inject constructor(
    private val appLimitDao: AppLimitDao
) {

    // ── Reads ─────────────────────────────────────────────────────────────────

    /** Live stream of all configured apps, sorted A→Z. */
    fun getAllApps(): Flow<List<AppLimitEntity>> = appLimitDao.getAllApps()

    /** Live stream of only the apps the user has marked as monitored. */
    fun getSelectedApps(): Flow<List<AppLimitEntity>> = appLimitDao.getSelectedApps()

    /** Live stream for a specific package, or null if not in the DB. */
    fun getAppByPackage(packageName: String): Flow<AppLimitEntity?> =
        appLimitDao.getAppByPackage(packageName)

    /** One-shot profile fetch for Workers / Services. */
    suspend fun getAppByPackageOnce(packageName: String): AppLimitEntity? =
        appLimitDao.getAppByPackageOnce(packageName)

    /**
     * One-shot snapshot for Workers / Services that run outside Compose.
     * Must be called from a coroutine on an IO dispatcher.
     */
    suspend fun getSelectedAppsOnce(): List<AppLimitEntity> =
        appLimitDao.getSelectedAppsOnce()

    /** Live count of selected (monitored) apps. */
    fun getSelectedAppCount(): Flow<Int> = appLimitDao.getSelectedAppCount()

    // ── Writes ────────────────────────────────────────────────────────────────

    /** Create or fully replace a single app-limit record. */
    suspend fun upsert(app: AppLimitEntity) = appLimitDao.upsert(app)

    /** Bulk create-or-replace — use on first launch to seed defaults. */
    suspend fun upsertAll(apps: List<AppLimitEntity>) = appLimitDao.upsertAll(apps)

    /** Full-column update on an already-existing row. */
    suspend fun update(app: AppLimitEntity) = appLimitDao.update(app)

    /** Permanently remove a single app's configuration. */
    suspend fun delete(app: AppLimitEntity) = appLimitDao.delete(app)

    /** Wipe ALL app-limit records — only for "reset all" / testing. */
    suspend fun deleteAll() = appLimitDao.deleteAll()
}
