package com.example.addictionreductionapp.viewmodel

import android.app.Application
import android.app.usage.UsageStatsManager
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.addictionreductionapp.data.local.entities.AchievementEntity
import com.example.addictionreductionapp.data.local.entities.InterventionEntity
import com.example.addictionreductionapp.data.local.entities.ReductionPlanEntity
import com.example.addictionreductionapp.data.local.entities.UserProfileEntity
import com.example.addictionreductionapp.data.repository.AchievementRepository
import com.example.addictionreductionapp.data.repository.AppLimitRepository
import com.example.addictionreductionapp.data.repository.InterventionRepository
import com.example.addictionreductionapp.data.repository.ReductionPlanRepository
import com.example.addictionreductionapp.data.repository.UserProfileRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.util.Calendar
import javax.inject.Inject

data class HomeUiState(
    val totalUsedMillis: Long = 0L,
    val appsBlockedToday: Int = 0,
    val totalLimitMins: Long = 1L,
    val hasSelectedApps: Boolean = false
)

@HiltViewModel
class HomeViewModel @Inject constructor(
    application: Application,
    private val appLimitRepository: AppLimitRepository,
    private val userProfileRepository: UserProfileRepository,
    private val achievementRepository: AchievementRepository,
    private val interventionRepository: InterventionRepository,
    private val reductionPlanRepository: ReductionPlanRepository
) : AndroidViewModel(application) {
    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    val profile: StateFlow<UserProfileEntity?> = userProfileRepository
        .observeProfile()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000L),
            initialValue = null
        )

    val achievements: StateFlow<List<AchievementEntity>> = achievementRepository
        .observeAll()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000L),
            initialValue = emptyList()
        )

    val recentInterventions: StateFlow<List<InterventionEntity>> = interventionRepository
        .getRecent(5)
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000L),
            initialValue = emptyList()
        )

    val reductionPlans: StateFlow<List<ReductionPlanEntity>> = reductionPlanRepository
        .observeActive()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000L),
            initialValue = emptyList()
        )

    init {
        viewModelScope.launch {
            achievementRepository.seedDefaults()
        }
    }

    private val usageStatsManager = application.getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager

    init {
        startPollingUsage()
    }

    private fun startPollingUsage() {
        viewModelScope.launch {
            flow {
                while (isActive) {
                    emit(Unit)
                    delay(15_000L)
                }
            }
            .flowOn(Dispatchers.IO)
            .collect {
                val calendar = Calendar.getInstance().apply {
                    set(Calendar.HOUR_OF_DAY, 0)
                    set(Calendar.MINUTE, 0)
                    set(Calendar.SECOND, 0)
                    set(Calendar.MILLISECOND, 0)
                }

                val stats = usageStatsManager.queryAndAggregateUsageStats(
                    calendar.timeInMillis,
                    System.currentTimeMillis()
                )

                var total = 0L
                var blockedCount = 0

                val selectedApps = appLimitRepository.getSelectedAppsOnce()
                var totalLimitMins = 0L
                selectedApps.forEach { app ->
                    val time = stats[app.packageName]?.totalTimeInForeground ?: 0L
                    total += time
                    totalLimitMins += app.limitMinutes
                    if (time >= app.limitMinutes * 60 * 1000L) {
                        blockedCount++
                    }
                }

                _uiState.update {
                    it.copy(
                        totalUsedMillis = total,
                        appsBlockedToday = blockedCount,
                        totalLimitMins = totalLimitMins.coerceAtLeast(1L),
                        hasSelectedApps = selectedApps.isNotEmpty()
                    )
                }
            }
        }
    }
    override fun onCleared() {
        super.onCleared()
    }

    fun toggleFocusMode() {
        viewModelScope.launch {
            val current = userProfileRepository.getProfile() ?: UserProfileEntity()
            userProfileRepository.upsert(current.copy(isFocusModeActive = !current.isFocusModeActive))
        }
    }

    fun updateUserName(name: String) {
        viewModelScope.launch {
            val current = userProfileRepository.getProfile() ?: UserProfileEntity()
            userProfileRepository.upsert(current.copy(userName = name))
        }
    }

    fun completeOnboarding() {
        viewModelScope.launch {
            val current = userProfileRepository.getProfile() ?: UserProfileEntity()
            userProfileRepository.upsert(current.copy(hasCompletedOnboarding = true))
        }
    }
}
