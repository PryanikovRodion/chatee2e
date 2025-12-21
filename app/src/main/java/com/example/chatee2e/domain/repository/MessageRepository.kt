package com.example.chatee2e.domain.repository

import com.example.chatee2e.common.Resource
import com.example.chatee2e.domain.model.Message
import kotlinx.coroutines.flow.Flow

interface MessageRepository {
    fun getMessages(chatId: String): Flow<List<Message>>
    suspend fun sendMessage(chatId: String, text: String): Resource<Unit>
    suspend fun clearHistory(chatId: String): Resource<Unit>
}