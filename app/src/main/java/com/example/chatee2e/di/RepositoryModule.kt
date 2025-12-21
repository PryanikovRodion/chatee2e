package com.example.chatee2e.di

import com.example.chatee2e.data.repository.AuthRepositoryImpl
import com.example.chatee2e.data.repository.ChatRepositoryImpl
import com.example.chatee2e.data.repository.MessageRepositoryImpl
import com.example.chatee2e.data.repository.UserRepositoryImpl
import com.example.chatee2e.domain.repository.AuthRepository
import com.example.chatee2e.domain.repository.ChatRepository
import com.example.chatee2e.domain.repository.MessageRepository
import com.example.chatee2e.domain.repository.UserRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds
    @Singleton
    abstract fun bindAuthRepository(
        authRepositoryImpl: AuthRepositoryImpl
    ): AuthRepository

    @Binds
    @Singleton
    abstract fun bindChatRepository(
        chatRepositoryImpl: ChatRepositoryImpl
    ): ChatRepository

    @Binds
    @Singleton
    abstract fun bindMessageRepository(
        messageRepositoryImpl: MessageRepositoryImpl
    ): MessageRepository

    @Binds
    @Singleton
    abstract fun bindUserRepository(
        userRepositoryImpl: UserRepositoryImpl
    ): UserRepository
}