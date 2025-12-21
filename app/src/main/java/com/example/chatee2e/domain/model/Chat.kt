package com.example.chatee2e.domain.model

import java.util.Date

data class Chat(
    val id: String,
    val name: String,
    val isGroup: Boolean,
    val lastMessageText: String? = null,
    val lastMessageTime: Date? = null
)