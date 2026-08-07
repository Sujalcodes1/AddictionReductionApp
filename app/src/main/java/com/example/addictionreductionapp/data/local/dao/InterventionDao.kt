package com.example.addictionreductionapp.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.example.addictionreductionapp.data.local.entities.InterventionEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface InterventionDao {
    @Insert
    suspend fun insert(intervention: InterventionEntity)

    @Query("SELECT * FROM interventions ORDER BY timestamp DESC")
    fun getAll(): Flow<List<InterventionEntity>>

    @Query("SELECT COUNT(*) FROM interventions WHERE type = :type")
    suspend fun countByType(type: String): Int

    @Query("SELECT COUNT(*) FROM interventions WHERE timestamp >= :startOfDay")
    suspend fun countToday(startOfDay: Long): Int

    @Query("SELECT * FROM interventions ORDER BY timestamp DESC LIMIT :limit")
    fun getRecent(limit: Int): Flow<List<InterventionEntity>>
}
