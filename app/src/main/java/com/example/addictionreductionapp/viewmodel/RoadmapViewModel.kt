package com.example.addictionreductionapp.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.addictionreductionapp.data.local.entities.ReductionPlanEntity
import com.example.addictionreductionapp.data.models.RoadmapPlan
import com.example.addictionreductionapp.data.repository.AdaptiveReductionRepository
import com.example.addictionreductionapp.data.repository.ReductionPlanRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class RoadmapViewModel @Inject constructor(
    private val adaptiveReductionRepository: AdaptiveReductionRepository,
    private val reductionPlanRepository: ReductionPlanRepository
) : ViewModel() {

    private val _plan = MutableStateFlow<RoadmapPlan?>(null)
    val plan: StateFlow<RoadmapPlan?> = _plan.asStateFlow()

    val reductionPlans: StateFlow<List<ReductionPlanEntity>> = reductionPlanRepository
        .observeActive()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000L),
            initialValue = emptyList()
        )

    init {
        viewModelScope.launch {
            try {
                adaptiveReductionRepository.generateRoadmap()
                    .catch { e ->
                        android.util.Log.e("Roadmap", "Error generating roadmap: ${e.message}", e)
                    }
                    .collect { plan ->
                        _plan.value = plan
                    }
            } catch (e: Exception) {
                android.util.Log.e("Roadmap", "Fatal error in roadmap subscription: ${e.message}", e)
            }
        }
    }
}
