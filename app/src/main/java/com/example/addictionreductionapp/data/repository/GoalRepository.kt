package com.example.addictionreductionapp.data.repository

import com.example.addictionreductionapp.data.local.dao.GoalDao
import com.example.addictionreductionapp.data.local.entities.GoalEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class GoalRepository @Inject constructor(
    private val goalDao: GoalDao
) {
    fun observeActiveGoals(): Flow<List<GoalEntity>> = goalDao.getActiveGoals()

    fun observeAllGoals(): Flow<List<GoalEntity>> = goalDao.getAllGoals()

    suspend fun getById(id: Long): GoalEntity? = goalDao.getById(id)

    suspend fun upsert(goal: GoalEntity): Long = goalDao.upsert(goal)

    suspend fun delete(goal: GoalEntity) = goalDao.delete(goal.id)

    suspend fun deleteAll() = goalDao.deleteAll()

    suspend fun seedDefaultIfEmpty() {
        val all = goalDao.getAllGoals().first()
        if (all.isEmpty()) {
            val today = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.US).format(java.util.Date())
            upsert(
                GoalEntity(
                    title = "Reduce Screen Time",
                    description = "Reduce daily screen time to a healthier level",
                    goalType = "CUSTOM",
                    targetScreenTimePerDay = 120,
                    category = null,
                    startDate = today,
                    targetDate = null,
                    isActive = true,
                    createdAt = System.currentTimeMillis(),
                    updatedAt = System.currentTimeMillis()
                )
            )
        }
    }
}
