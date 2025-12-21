package com.example.chatee2e.domain.repository

import com.example.chatee2e.common.Resource
import com.example.chatee2e.domain.model.Chat
import com.example.chatee2e.domain.model.User
import kotlinx.coroutines.flow.Flow

interface ChatRepository {
    suspend fun connect()
    fun getChats(): Flow<List<Chat>>
    suspend fun createGroup(name: String, participants: List<User>): Resource<String>
    suspend fun createDirectChat(targetUser: User): Resource<String>
    suspend fun addMembers(chatId: String, newMembers: List<User>): Resource<Unit>
    suspend fun leaveChat(chatId: String): Resource<Unit>

    suspend fun curentId(): String
}