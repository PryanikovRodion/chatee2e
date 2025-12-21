package com.example.chatee2e.domain.repository

import com.example.chatee2e.common.Resource
import com.example.chatee2e.domain.model.User
import kotlinx.coroutines.flow.Flow

interface AuthRepository {
    val currentUser: Flow<User?>
    suspend fun signIn(): Resource<Boolean>
    suspend fun signOut()
    fun isKeysSetUp(): Boolean
    suspend fun searchUserByEmail(email: String): Resource<User?>
    suspend fun getUserById(userId: String): Resource<User>
}