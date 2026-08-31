package com.example.smartfocus

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

data class AnalyticsUiState(
    val screenTimeMinutes: Int = 0,
    val totalOpens: Int = 0,
    val focusScore: Int = 0,
    val streakDays: Int = 0,
    val riskPct: Int = 0,
    val isLoading: Boolean = false,
    val error: String? = null
)

class AnalyticsViewModel : ViewModel() {

    private val _uiState = MutableLiveData(AnalyticsUiState(isLoading = true))
    val uiState: LiveData<AnalyticsUiState> = _uiState

    init {
        load()
    }

    fun load() {
        viewModelScope.launch {
            _uiState.value = AnalyticsUiState(isLoading = true)
            delay(800) // simulate repo fetch — replace with real AnalyticsRepository
            _uiState.value = AnalyticsUiState(
                screenTimeMinutes = 150,
                totalOpens = 48,
                focusScore = 78,
                streakDays = 5,
                riskPct = 22,
                isLoading = false
            )
        }
    }

    fun refresh() = load()
}
