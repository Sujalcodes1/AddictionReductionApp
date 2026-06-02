package com.example.addictionreductionapp.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.addictionreductionapp.data.local.entities.AppLimitEntity
import com.example.addictionreductionapp.data.repository.AppLimitRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ViewModel for the App Blocker screen.
 *
 * Survives configuration changes and exposes app-limit state as [StateFlow].
 * All database writes run on the viewModelScope (Room dispatches them to its
 * internal IO thread pool automatically — no manual Dispatchers.IO needed).
 *
 * ## Compose usage
 * ```kotlin
 * @Composable
 * fun AppBlockerScreen(
 *     viewModel: AppBlockerViewModel = hiltViewModel()
 * ) {
 *     val apps by viewModel.allApps.collectAsStateWithLifecycle()
 *     ...
 * }
 * ```
 *
 * @param repository Injected by Hilt via [com.example.addictionreductionapp.data.local.database.DatabaseModule].
 */
@HiltViewModel
class AppBlockerViewModel @Inject constructor(
    private val repository: AppLimitRepository
) : ViewModel() {

    /**
     * All configured apps, A→Z.
     * [SharingStarted.WhileSubscribed] stops the upstream Flow 5 s after the
     * last subscriber disappears (e.g. screen is off), reducing DB overhead.
     */
    val allApps: StateFlow<List<AppLimitEntity>> = repository
        .getAllApps()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000L),
            initialValue = emptyList()
        )

    /** Only the apps the user has toggled on for monitoring. */
    val selectedApps: StateFlow<List<AppLimitEntity>> = repository
        .getSelectedApps()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000L),
            initialValue = emptyList()
        )

    /** Count of currently selected apps — useful for badge / limit checks. */
    val selectedAppCount: StateFlow<Int> = repository
        .getSelectedAppCount()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000L),
            initialValue = 0
        )

    // ── Write operations ──────────────────────────────────────────────────────

    /** Toggle the selected state of [app], then persist the change. */
    fun toggleSelection(app: AppLimitEntity) {
        viewModelScope.launch {
            repository.upsert(app.copy(isSelected = !app.isSelected))
        }
    }

    /** Update the daily limit for [app] to [minutes] minutes. */
    fun updateLimit(app: AppLimitEntity, minutes: Int) {
        viewModelScope.launch {
            repository.upsert(app.copy(limitMinutes = minutes))
        }
    }

    /** Toggle the hard-lock state of [app]. */
    fun toggleLock(app: AppLimitEntity) {
        viewModelScope.launch {
            repository.upsert(app.copy(isLocked = !app.isLocked))
        }
    }

    /** Toggle the whitelist state of [app]. */
    fun toggleWhitelist(app: AppLimitEntity) {
        viewModelScope.launch {
            repository.upsert(app.copy(isWhitelisted = !app.isWhitelisted))
        }
    }

    /** Set the scheduled block window for [app]. -1 in both fields disables it. */
    fun setSchedule(app: AppLimitEntity, startHour: Int, endHour: Int) {
        viewModelScope.launch {
            repository.upsert(app.copy(blockScheduleStart = startHour, blockScheduleEnd = endHour))
        }
    }

    /**
     * Seed the database with the default app list on first launch.
     * Uses REPLACE on-conflict, so re-seeding is idempotent.
     */
    fun seedDefaults(apps: List<AppLimitEntity>) {
        viewModelScope.launch {
            repository.upsertAll(apps)
        }
    }

    /** Permanently remove a single app configuration. */
    fun deleteApp(app: AppLimitEntity) {
        viewModelScope.launch {
            repository.delete(app)
        }
    }
}
