package com.example.addictionreductionapp.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.addictionreductionapp.data.local.entities.GoalEntity
import com.example.addictionreductionapp.data.repository.GoalRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject

data class GoalsUiState(
    val showCreateDialog: Boolean = false,
    val editingGoal: GoalEntity? = null
)

@HiltViewModel
class GoalsViewModel @Inject constructor(
    private val goalRepository: GoalRepository
) : ViewModel() {

    val activeGoals: StateFlow<List<GoalEntity>> = goalRepository.observeActiveGoals()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyList())

    private val _uiState = MutableStateFlow(GoalsUiState())
    val uiState: StateFlow<GoalsUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch { goalRepository.seedDefaultIfEmpty() }
    }

    fun showCreateDialog() {
        _uiState.value = _uiState.value.copy(showCreateDialog = true)
    }

    fun dismissCreateDialog() {
        _uiState.value = _uiState.value.copy(showCreateDialog = false, editingGoal = null)
    }

    fun createGoal(title: String, description: String, goalType: String, targetMinutes: Int, targetDate: String?) {
        viewModelScope.launch {
            val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.US)
            goalRepository.upsert(
                GoalEntity(
                    title = title,
                    description = description,
                    goalType = goalType,
                    targetScreenTimePerDay = targetMinutes,
                    category = categoriesForType(goalType),
                    startDate = sdf.format(Date()),
                    targetDate = targetDate,
                    createdAt = System.currentTimeMillis(),
                    updatedAt = System.currentTimeMillis()
                )
            )
            _uiState.value = _uiState.value.copy(showCreateDialog = false)
        }
    }

    fun completeGoal(goal: GoalEntity) {
        viewModelScope.launch {
            goalRepository.upsert(goal.copy(isActive = false, completedAt = System.currentTimeMillis(), updatedAt = System.currentTimeMillis(), progress = 1f))
        }
    }

    fun deleteGoal(goal: GoalEntity) {
        viewModelScope.launch { goalRepository.delete(goal) }
    }

    private fun categoriesForType(goalType: String): String? = when (goalType) {
        "LEARNING", "CAREER" -> "Productivity"
        "FITNESS" -> "Health"
        "CREATIVE" -> "Productivity"
        else -> null
    }
}
