package com.example.addictionreductionapp.data.repository

import com.example.addictionreductionapp.data.local.dao.InterventionDao
import com.example.addictionreductionapp.data.local.entities.InterventionEntity
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class InterventionRepository @Inject constructor(
    private val interventionDao: InterventionDao
) {
    suspend fun log(type: String, packageNameBlocked: String?, journalText: String? = null) {
        interventionDao.insert(
            InterventionEntity(
                type = type,
                packageNameBlocked = packageNameBlocked,
                journalText = journalText
            )
        )
    }

    suspend fun countToday(startOfDay: Long): Int = interventionDao.countToday(startOfDay)

    fun getRecent(limit: Int): Flow<List<InterventionEntity>> = interventionDao.getRecent(limit)
}
