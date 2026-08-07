package com.example.addictionreductionapp.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.addictionreductionapp.data.local.entities.ReductionPlanEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ReductionPlanDao {

    @Query("SELECT * FROM reduction_plans WHERE is_active = 1")
    fun observeActive(): Flow<List<ReductionPlanEntity>>

    @Query("SELECT * FROM reduction_plans WHERE is_active = 1")
    suspend fun getActive(): List<ReductionPlanEntity>

    @Query("SELECT * FROM reduction_plans")
    suspend fun getAll(): List<ReductionPlanEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(plan: ReductionPlanEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(plans: List<ReductionPlanEntity>)

    @Query("DELETE FROM reduction_plans")
    suspend fun deleteAll()

    @Query("SELECT * FROM reduction_plans WHERE id = :id LIMIT 1")
    suspend fun getById(id: String): ReductionPlanEntity?
}
