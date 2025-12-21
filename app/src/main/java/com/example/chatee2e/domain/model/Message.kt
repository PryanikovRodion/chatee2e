package com.example.chatee2e.domain.model

import java.util.Date

data class Message(
    val id: String,
    val chatId: String,
    val senderId: String,
    val senderName: String?,
    val text: String,
    val timestamp: Date,
    val isMine: Boolean
)