package com.example.chatee2e.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import com.example.chatee2e.data.local.dao.ChatDao
import com.example.chatee2e.data.local.dao.MessageDao
import com.example.chatee2e.data.local.entity.ChatEntity
import com.example.chatee2e.data.local.entity.MessageEntity

@Database(
    entities = [ChatEntity::class, MessageEntity::class],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract val chatDao: ChatDao
    abstract val messageDao: MessageDao
}