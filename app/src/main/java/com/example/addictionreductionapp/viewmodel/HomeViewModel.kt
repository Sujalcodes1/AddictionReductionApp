package com.example.addictionreductionapp.viewmodel

import android.app.Application
import android.app.usage.UsageStatsManager
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.addictionreductionapp.data.AppDataStore
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.Calendar
import javax.inject.Inject

data class HomeUiState(
    val totalUsedMillis: Long = 0L,
    val appsBlockedToday: Int = 0
)

@HiltViewModel
class HomeViewModel @Inject constructor(
    application: Application
) : AndroidViewModel(application) {
    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    private val usageStatsManager = application.getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager

    init {
        android.util.Log.d("NavDebug", "HomeViewModel INITIALIZED (hashCode=${hashCode()})")
        startPollingUsage()
    }

    private fun startPollingUsage() {
        viewModelScope.launch(Dispatchers.IO) {
            while (true) {
                val calendar = Calendar.getInstance().apply {
                    set(Calendar.HOUR_OF_DAY, 0)
                    set(Calendar.MINUTE, 0)
                    set(Calendar.SECOND, 0)
                    set(Calendar.MILLISECOND, 0)
                }

                // Execute the heavy query on IO dispatcher
                val stats = usageStatsManager.queryAndAggregateUsageStats(
                    calendar.timeInMillis,
                    System.currentTimeMillis()
                )

                var total = 0L
                var blockedCount = 0

                AppDataStore.apps.forEach { app ->
                    if (app.isSelected) {
                        val time = stats[app.packageName]?.totalTimeInForeground ?: 0L
                        total += time
                        if (time >= app.limitMinutes * 60 * 1000L) {
                            blockedCount++
                        }
                    }
                }

                _uiState.update { 
                    it.copy(
                        totalUsedMillis = total, 
                        appsBlockedToday = blockedCount
                    ) 
                }
                
                // Poll every 15 seconds
                delay(15000)
            }
        }
    }
    override fun onCleared() {
        super.onCleared()
        android.util.Log.d("NavDebug", "HomeViewModel CLEARED (hashCode=${hashCode()})")
    }
}
