package com.example.addictionreductionapp.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.addictionreductionapp.data.analytics.SmartReductionEngine
import com.example.addictionreductionapp.data.local.entities.ReductionPlanEntity
import com.example.addictionreductionapp.data.repository.ReductionPlanRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject
import com.example.addictionreductionapp.data.repository.AppLimitRepository

data class CategoryOption(
    val category: String,
    val averageMinutes: Int,
    val isSelected: Boolean = false,
    val stepDown: Int = SmartReductionEngine.DEFAULT_STEP_DOWN
)

@HiltViewModel
class SmartReductionSetupViewModel @Inject constructor(
    private val smartReductionEngine: SmartReductionEngine,
    private val reductionPlanRepository: ReductionPlanRepository,
    private val appLimitRepository: AppLimitRepository
) : ViewModel() {

    private val _categories = MutableStateFlow<List<CategoryOption>>(emptyList())
    val categories: StateFlow<List<CategoryOption>> = _categories.asStateFlow()

    private val _isLoading = MutableStateFlow(true)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _summary = MutableStateFlow("")
    val summary: StateFlow<String> = _summary.asStateFlow()

    private val _hasRealData = MutableStateFlow(false)
    val hasRealData: StateFlow<Boolean> = _hasRealData.asStateFlow()

    init {
        viewModelScope.launch {
            loadUsageData()
        }
    }

    private suspend fun loadUsageData() {
        withContext(Dispatchers.IO) {
            try {
                val rawTotals = smartReductionEngine.importHistoricalUsage()
                val baselines = smartReductionEngine.computeBaseline(rawTotals)

                val options = baselines
                    .filter { (category, avg) ->
                        category in smartReductionEngine.targetCategories && avg > 0
                    }
                    .map { (category, avg) ->
                        CategoryOption(
                            category = category,
                            averageMinutes = avg,
                            isSelected = category in listOf("Social", "Entertainment"),
                            stepDown = SmartReductionEngine.DEFAULT_STEP_DOWN
                        )
                    }
                    .sortedByDescending { it.averageMinutes }

                if (options.isEmpty()) {
                    setDefaultCategories()
                } else {
                    _categories.value = options
                    _hasRealData.value = true
                    updateSummary()
                }
            } catch (e: Exception) {
                android.util.Log.e("SmartReduction", "Failed to load usage data", e)
                setDefaultCategories()
            } finally {
                _isLoading.value = false
            }
        }
    }

    private fun setDefaultCategories() {
        _categories.value = smartReductionEngine.targetCategories.map { category ->
            CategoryOption(
                category = category,
                averageMinutes = 120,
                isSelected = category in listOf("Social", "Entertainment"),
                stepDown = SmartReductionEngine.DEFAULT_STEP_DOWN
            )
        }
        _hasRealData.value = false
    }

    fun toggleCategory(category: String) {
        _categories.update { list ->
            list.map { if (it.category == category) it.copy(isSelected = !it.isSelected) else it }
        }
        updateSummary()
    }

    fun setStepDown(category: String, minutes: Int) {
        _categories.update { list ->
            list.map { if (it.category == category) it.copy(stepDown = minutes) else it }
        }
        updateSummary()
    }

    fun confirmPlans(onComplete: () -> Unit) {
        viewModelScope.launch {
            val selected = _categories.value.filter { it.isSelected }
            if (selected.isEmpty()) {
                onComplete()
                return@launch
            }

            val plans = selected.map { opt ->
                ReductionPlanEntity(
                    id = opt.category.lowercase(),
                    category = opt.category,
                    baselineMinutes = opt.averageMinutes,
                    currentTarget = opt.averageMinutes,
                    dailyStepDown = opt.stepDown,
                    floorMinutes = SmartReductionEngine.DEFAULT_FLOOR,
                    isActive = true
                )
            }
            reductionPlanRepository.upsertAll(plans)

            // Update individual app limits in the database to sync with active plans
            val allApps = appLimitRepository.getAllAppsOnce()
            val updatedApps = mutableListOf<com.example.addictionreductionapp.data.local.entities.AppLimitEntity>()
            for (plan in plans) {
                val categoryApps = allApps.filter {
                    com.example.addictionreductionapp.utils.AppCategoryResolver.resolveCategory(it.packageName) == plan.category
                }
                for (app in categoryApps) {
                    updatedApps.add(app.copy(isSelected = true, limitMinutes = plan.currentTarget, isLocked = false))
                }
            }
            if (updatedApps.isNotEmpty()) {
                appLimitRepository.upsertAll(updatedApps)
            }

            onComplete()
        }
    }

    private fun updateSummary() {
        val selected = _categories.value.filter { it.isSelected }
        if (selected.isEmpty()) {
            _summary.value = "Select categories above to begin gradual reduction."
            return
        }

        val summaries = selected.map { opt ->
            val daysToFloor = if (opt.stepDown > 0) {
                (opt.averageMinutes - SmartReductionEngine.DEFAULT_FLOOR) / opt.stepDown
            } else 0
            "${opt.category}: ${opt.averageMinutes} min → ${SmartReductionEngine.DEFAULT_FLOOR} min in ~${daysToFloor} days (-${opt.stepDown} min/day)"
        }
        _summary.value = summaries.joinToString("\n")
    }
}
