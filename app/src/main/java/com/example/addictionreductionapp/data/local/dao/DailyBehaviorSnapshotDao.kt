package com.example.addictionreductionapp.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.addictionreductionapp.data.local.entities.DailyBehaviorSnapshotEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface DailyBehaviorSnapshotDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSnapshot(snapshot: DailyBehaviorSnapshotEntity)

    @Query("SELECT * FROM daily_behavior_snapshots WHERE date = :date")
    fun getSnapshotByDate(date: String): Flow<DailyBehaviorSnapshotEntity?>

    @Query("SELECT * FROM daily_behavior_snapshots ORDER BY date DESC LIMIT 1")
    fun getLatestSnapshot(): Flow<DailyBehaviorSnapshotEntity?>

    @Query("SELECT * FROM daily_behavior_snapshots ORDER BY date DESC LIMIT 7")
    fun getSnapshotsForLast7Days(): Flow<List<DailyBehaviorSnapshotEntity>>

    @Query("SELECT * FROM daily_behavior_snapshots ORDER BY date DESC LIMIT 30")
    fun getSnapshotsForLast30Days(): Flow<List<DailyBehaviorSnapshotEntity>>

    @Query("SELECT * FROM daily_behavior_snapshots ORDER BY date DESC")
    fun getAllSnapshots(): Flow<List<DailyBehaviorSnapshotEntity>>

    @Query("SELECT DISTINCT usage_date FROM app_usage WHERE usage_date NOT IN (SELECT date FROM daily_behavior_snapshots) ORDER BY usage_date ASC")
    suspend fun getMissingSnapshotDates(): List<String>

    @Query("SELECT date FROM daily_behavior_snapshots ORDER BY date ASC")
    suspend fun getAllSnapshotDates(): List<String>

    @Query("DELETE FROM daily_behavior_snapshots WHERE date = :date")
    suspend fun deleteSnapshotByDate(date: String)
}
