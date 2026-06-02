package com.example.addictionreductionapp.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.addictionreductionapp.data.local.entities.UserProfileEntity
import com.example.addictionreductionapp.data.repository.UserProfileRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ViewModel for the Profile / Home screens that display user stats.
 *
 * Exposes the single [UserProfileEntity] row as a lifecycle-aware [StateFlow].
 * Write operations (save, incrementStreak) run on [viewModelScope] — Room
 * dispatches them to the appropriate IO thread automatically.
 *
 * ## Compose usage
 * ```kotlin
 * @Composable
 * fun ProfileScreen(
 *     viewModel: UserProfileViewModel = hiltViewModel()
 * ) {
 *     val profile by viewModel.profile.collectAsStateWithLifecycle()
 *     val userName = profile?.userName ?: "User"
 *     ...
 * }
 * ```
 *
 * @param repository Injected by Hilt via [com.example.addictionreductionapp.data.local.database.DatabaseModule].
 */
@HiltViewModel
class UserProfileViewModel @Inject constructor(
    private val repository: UserProfileRepository
) : ViewModel() {

    /**
     * Live user profile.
     * Emits null until the first [saveProfile] or [upsertDefaults] call.
     * UI should treat null as "first launch — show defaults".
     */
    val profile: StateFlow<UserProfileEntity?> = repository
        .observeProfile()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000L),
            initialValue = null
        )

    // ── Write operations ──────────────────────────────────────────────────────

    /**
     * Persist (create or fully replace) the user profile row.
     * The [UserProfileEntity] id is always 1, so this is an atomic upsert.
     */
    fun saveProfile(profile: UserProfileEntity) {
        viewModelScope.launch {
            repository.upsert(profile)
        }
    }

    /**
     * Seed the DB with default values on first launch.
     * Safe to call more than once — the REPLACE strategy makes it idempotent.
     */
    fun upsertDefaults() {
        viewModelScope.launch {
            // Only seed if no profile row exists yet
            if (repository.getProfile() == null) {
                repository.upsert(UserProfileEntity())
            }
        }
    }

    /**
     * Convenience: update just the user's display name without touching
     * any other profile field.
     *
     * @param name New display name entered by the user.
     */
    fun updateUserName(name: String) {
        viewModelScope.launch {
            val current = repository.getProfile() ?: UserProfileEntity()
            repository.upsert(current.copy(userName = name))
        }
    }

    /**
     * Mark onboarding as completed.
     * Call this when the user dismisses the onboarding flow.
     */
    fun completeOnboarding() {
        viewModelScope.launch {
            val current = repository.getProfile() ?: UserProfileEntity()
            repository.upsert(current.copy(hasCompletedOnboarding = true))
        }
    }

    /**
     * Toggle focus mode on/off and persist the new state.
     * The Accessibility Service reads this flag to decide whether to block apps.
     */
    fun setFocusModeActive(active: Boolean) {
        viewModelScope.launch {
            val current = repository.getProfile() ?: UserProfileEntity()
            repository.upsert(current.copy(isFocusModeActive = active))
        }
    }

    /**
     * Increment the streak counter by 1 via an atomic SQL UPDATE.
     * Also updates longestStreak if the new streak exceeds the current record.
     * Call this at the end of each day the user met their goal.
     */
    fun incrementStreak() {
        viewModelScope.launch {
            repository.incrementStreak()
            // Sync longestStreak if needed (requires a read-modify-write here)
            val updated = repository.getProfile() ?: return@launch
            if (updated.streakCount > updated.longestStreak) {
                repository.upsert(updated.copy(longestStreak = updated.streakCount))
            }
        }
    }

    /**
     * Reset the streak to zero (e.g., user broke their goal on a given day).
     */
    fun resetStreak() {
        viewModelScope.launch {
            val current = repository.getProfile() ?: UserProfileEntity()
            repository.upsert(current.copy(streakCount = 0))
        }
    }

    /**
     * Set login state. Call with true after successful authentication,
     * false on logout.
     */
    fun setLoggedIn(loggedIn: Boolean) {
        viewModelScope.launch {
            val current = repository.getProfile() ?: UserProfileEntity()
            repository.upsert(current.copy(isLoggedIn = loggedIn))
        }
    }
}
