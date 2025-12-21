package com.example.chatee2e.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "chats")
data class ChatEntity(
    @PrimaryKey val id: String,
    val name: String,
    val isGroup: Boolean,
    val participantsInfo: String,
    val lastMessageText: String? = null,
    val lastMessageTime: Long? = null
)