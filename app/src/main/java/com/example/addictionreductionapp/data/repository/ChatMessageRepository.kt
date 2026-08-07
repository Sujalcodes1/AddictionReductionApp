package com.example.addictionreductionapp.data.repository

import com.example.addictionreductionapp.data.local.dao.ChatMessageDao
import com.example.addictionreductionapp.data.local.entities.ChatMessageEntity
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ChatMessageRepository @Inject constructor(
    private val chatMessageDao: ChatMessageDao
) {
    fun getRecent(limit: Int): Flow<List<ChatMessageEntity>> = chatMessageDao.getRecent(limit)

    suspend fun getRecentOnce(limit: Int): List<ChatMessageEntity> = chatMessageDao.getRecentOnce(limit)

    suspend fun insert(message: ChatMessageEntity) = chatMessageDao.insert(message)

    suspend fun deleteAll() = chatMessageDao.deleteAll()
}
