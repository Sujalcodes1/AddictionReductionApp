package com.example.addictionreductionapp.data.repository

import com.example.addictionreductionapp.data.analytics.SmartReductionEngine
import com.example.addictionreductionapp.data.local.entities.DailyBehaviorSnapshotEntity
import com.example.addictionreductionapp.data.local.entities.GoalEntity
import com.example.addictionreductionapp.data.models.RoadmapPlan
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOn
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AdaptiveReductionRepository @Inject constructor(
    private val dailyBehaviorSnapshotRepository: DailyBehaviorSnapshotRepository,
    private val goalRepository: GoalRepository,
    private val smartReductionEngine: SmartReductionEngine
) {

    fun generateRoadmap(): Flow<RoadmapPlan?> {
        val snapshotsFlow = dailyBehaviorSnapshotRepository.getHistoricalSnapshots(14)
        val goalsFlow = goalRepository.observeActiveGoals()

        return combine(snapshotsFlow, goalsFlow) { snapshots, goals ->
            if (snapshots.size < 7) return@combine null

            val sorted = snapshots.sortedBy { it.date }
            val baselineAvg = sorted.takeLast(7).sumOf { it.totalScreenTimeMinutes } / sorted.takeLast(7).size

            // Compute success rate: % of past days where actual was <= baseline
            val successRate = if (baselineAvg > 0 && sorted.size >= 3) {
                val recent = sorted.takeLast(3)
                recent.count { it.totalScreenTimeMinutes <= baselineAvg }.toFloat() / recent.size
            } else 0.5f

            smartReductionEngine.generateAdaptiveRoadmap(sorted, goals, successRate)
        }.flowOn(Dispatchers.Default)
    }
}
