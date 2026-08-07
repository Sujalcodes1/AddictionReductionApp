package com.example.addictionreductionapp.data.repository

import com.example.addictionreductionapp.data.local.dao.ReductionPlanDao
import com.example.addictionreductionapp.data.local.entities.ReductionPlanEntity
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ReductionPlanRepository @Inject constructor(
    private val dao: ReductionPlanDao
) {
    fun observeActive(): Flow<List<ReductionPlanEntity>> = dao.observeActive()

    suspend fun getActive(): List<ReductionPlanEntity> = dao.getActive()

    suspend fun getAll(): List<ReductionPlanEntity> = dao.getAll()

    suspend fun upsert(plan: ReductionPlanEntity) = dao.upsert(plan)

    suspend fun upsertAll(plans: List<ReductionPlanEntity>) = dao.upsertAll(plans)

    suspend fun deleteAll() = dao.deleteAll()

    suspend fun getById(id: String): ReductionPlanEntity? = dao.getById(id)
}
