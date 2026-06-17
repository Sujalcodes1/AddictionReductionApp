package com.example.addictionreductionapp.data.repository

import com.example.addictionreductionapp.data.local.dao.DailyBehaviorSnapshotDao
import com.example.addictionreductionapp.data.local.entities.DailyBehaviorSnapshotEntity
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class DailyBehaviorSnapshotRepository @Inject constructor(
    private val snapshotDao: DailyBehaviorSnapshotDao
) {
    suspend fun saveDailySnapshot(snapshot: DailyBehaviorSnapshotEntity) {
        snapshotDao.insertSnapshot(snapshot)
    }

    fun getLatestSnapshot(): Flow<DailyBehaviorSnapshotEntity?> {
        return snapshotDao.getLatestSnapshot()
    }

    fun getHistoricalSnapshots(days: Int): Flow<List<DailyBehaviorSnapshotEntity>> {
        return when (days) {
            7 -> snapshotDao.getSnapshotsForLast7Days()
            30 -> snapshotDao.getSnapshotsForLast30Days()
            else -> snapshotDao.getAllSnapshots()
        }
    }
}
