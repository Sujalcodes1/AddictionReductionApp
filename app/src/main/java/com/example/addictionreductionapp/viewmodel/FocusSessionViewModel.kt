package com.example.addictionreductionapp.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.addictionreductionapp.data.analytics.AchievementEngine
import com.example.addictionreductionapp.data.local.entities.FocusSessionEntity
import com.example.addictionreductionapp.data.local.entities.UserProfileEntity
import com.example.addictionreductionapp.data.repository.AchievementRepository
import com.example.addictionreductionapp.data.repository.FocusSessionRepository
import com.example.addictionreductionapp.data.repository.UserProfileRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ViewModel for the Focus Timer and Analytics screens.
 *
 * Owns all focus-session state and exposes it as lifecycle-aware [StateFlow].
 * Coordinates between [FocusSessionRepository] (session records) and
 * [UserProfileRepository] (aggregate profile stats) so both are updated
 * atomically when a session completes.
 *
 * ## Compose usage
 * ```kotlin
 * @Composable
 * fun FocusTimerScreen(
 *     viewModel: FocusSessionViewModel = hiltViewModel()
 * ) {
 *     val sessions by viewModel.allSessions.collectAsStateWithLifecycle()
 *     val totalMinutes by viewModel.totalFocusMinutes.collectAsStateWithLifecycle()
 *     ...
 * }
 * ```
 *
 * @param sessionRepository  Injected by Hilt — manages [FocusSessionEntity] persistence.
 * @param profileRepository  Injected by Hilt — updates aggregate stats after each session.
 */
@HiltViewModel
class FocusSessionViewModel @Inject constructor(
    private val sessionRepository: FocusSessionRepository,
    private val profileRepository: UserProfileRepository,
    private val achievementRepository: AchievementRepository,
    private val achievementEngine: AchievementEngine
) : ViewModel() {

    /** All sessions newest-first. Re-emits whenever a new row is inserted. */
    val allSessions: StateFlow<List<FocusSessionEntity>> = sessionRepository
        .getAllSessions()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000L),
            initialValue = emptyList()
        )

    /**
     * Running total of all focus minutes.
     * Null when the table is empty — treat as 0 in the UI layer.
     */
    val totalFocusMinutes: StateFlow<Int?> = sessionRepository
        .getTotalFocusMinutes()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000L),
            initialValue = null
        )

    /** Count of all completed sessions — drives achievement calculations. */
    val sessionCount: StateFlow<Int> = sessionRepository
        .getSessionCount()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000L),
            initialValue = 0
        )

    /** Live user profile for streak count and other aggregate stats. */
    val profile: StateFlow<UserProfileEntity?> = profileRepository
        .observeProfile()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000L),
            initialValue = null
        )

    // ── Timer state (survives configuration changes) ──────────────────────────

    private val _remainingSeconds = MutableStateFlow(0)
    val remainingSeconds: StateFlow<Int> = _remainingSeconds.asStateFlow()

    private val _isRunning = MutableStateFlow(false)
    val isRunning: StateFlow<Boolean> = _isRunning.asStateFlow()

    private val _selectedDuration = MutableStateFlow(25)
    val selectedDuration: StateFlow<Int> = _selectedDuration.asStateFlow()

    private val _showComplete = MutableStateFlow(false)
    val showComplete: StateFlow<Boolean> = _showComplete.asStateFlow()

    private val _isLockMode = MutableStateFlow(false)
    val isLockMode: StateFlow<Boolean> = _isLockMode.asStateFlow()

    private val _selectedSoundIndex = MutableStateFlow(4)
    val selectedSoundIndex: StateFlow<Int> = _selectedSoundIndex.asStateFlow()

    private var timerJob: Job? = null

    fun setDuration(minutes: Int) {
        if (_isRunning.value) return
        _selectedDuration.value = minutes
        _remainingSeconds.value = minutes * 60
    }

    fun setSoundIndex(index: Int) {
        if (_isRunning.value) return
        _selectedSoundIndex.value = index
    }

    fun toggleLockMode() {
        if (_isRunning.value) return
        _isLockMode.value = !_isLockMode.value
    }

    fun startTimer() {
        if (_isRunning.value) return
        if (_remainingSeconds.value <= 0) {
            _remainingSeconds.value = _selectedDuration.value * 60
        }
        _isRunning.value = true
        _showComplete.value = false
        timerJob = viewModelScope.launch {
            while (isActive && _remainingSeconds.value > 0) {
                delay(1000)
                _remainingSeconds.value--
            }
            if (_remainingSeconds.value <= 0 && _isRunning.value) {
                completeTimer()
            }
        }
    }

    fun pauseTimer() {
        _isRunning.value = false
        timerJob?.cancel()
    }

    fun resetTimer() {
        pauseTimer()
        _remainingSeconds.value = _selectedDuration.value * 60
    }

    private fun completeTimer() {
        _isRunning.value = false
        _showComplete.value = true
        recordSession(_selectedDuration.value, "silence")
    }

    fun dismissComplete() {
        _showComplete.value = false
        _remainingSeconds.value = _selectedDuration.value * 60
    }

    // ── Write operations ──────────────────────────────────────────────────────

    /**
     * Persist a completed focus session and atomically update the user profile.
     *
     * Both writes run in the same coroutine so they always succeed or fail
     * together within the same [viewModelScope] lifecycle.
     *
     * @param durationMinutes How long the session lasted.
     * @param soundType       Background audio used: "silence" | "white_noise" | "rain" | "binaural".
     */
    fun recordSession(durationMinutes: Int, soundType: String = "silence") {
        viewModelScope.launch {
            val session = FocusSessionEntity(
                durationMinutes = durationMinutes,
                completedAt = System.currentTimeMillis(),
                soundType = soundType
            )
            // Insert the session record first
            sessionRepository.insert(session)
            // Then atomically update aggregate profile stats via a single SQL UPDATE
            profileRepository.recordCompletedSession(durationMinutes)
            recomputeAchievements()
        }
    }

    private suspend fun recomputeAchievements() {
        val profile = profileRepository.getProfile() ?: return
        val existing = achievementRepository.getAll()
        val updated = achievementEngine.compute(
            existing = existing,
            streak = profile.streakCount,
            sessions = profile.sessionsCompleted,
            focusMinutes = profile.totalFocusMinutes,
            blockedAppCount = 0
        )
        achievementRepository.upsertAll(updated)
    }

    /**
     * Query sessions in a given time range (for chart rendering).
     * Returns a one-shot list — use [FocusSessionRepository.getSessionsInRange]
     * directly if you need a live Flow.
     */
    fun getSessionsInRange(fromEpochMs: Long, toEpochMs: Long) =
        sessionRepository.getSessionsInRange(fromEpochMs, toEpochMs)

    /** Wipe all session history — only for the "reset all data" flow. */
    fun deleteAllSessions() {
        viewModelScope.launch {
            sessionRepository.deleteAll()
        }
    }

    /**
     * Alias used by the debug screen. Delegates to [deleteAllSessions].
     * Safe to call from any coroutine context; Room dispatches the DELETE.
     */
    fun clearAllSessions() = deleteAllSessions()
}
