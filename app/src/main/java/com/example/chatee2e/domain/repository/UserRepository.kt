package com.example.chatee2e.domain.repository

import com.example.chatee2e.common.Resource
import com.example.chatee2e.domain.model.User
import kotlinx.coroutines.flow.Flow

interface UserRepository {
    suspend fun blockUser(userId: String): Resource<Unit>
    suspend fun unblockUser(userId: String): Resource<Unit>
    fun getBlockedUsers(): Flow<List<User>>
}