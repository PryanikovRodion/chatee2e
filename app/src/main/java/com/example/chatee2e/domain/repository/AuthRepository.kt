package com.example.chatee2e.domain.repository

import com.example.chatee2e.common.Resource
import com.example.chatee2e.domain.model.User
import kotlinx.coroutines.flow.Flow

interface AuthRepository {
    val currentUser: Flow<User?>

    suspend fun signUp(email: String, password: String, username: String): Resource<Unit>

    suspend fun signIn(email: String, password: String): Resource<Boolean>

    suspend fun signOut()

    suspend fun deleteAccount(): Resource<Unit>

    fun isEmailVerified(): Boolean

    suspend fun sendVerificationEmail(): Resource<Unit>

    fun isKeysSetUp(): Boolean

    suspend fun searchUserByEmail(email: String): Resource<User?>

    suspend fun getUserById(userId: String): Resource<User>
}