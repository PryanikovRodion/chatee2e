package com.example.chatee2e.data.remote.dto

import com.google.firebase.firestore.ServerTimestamp
import java.util.Date

data class MessageDto(
    val chatId: String = "",
    val senderId: String = "",
    @ServerTimestamp
    val timestamp: Date? = null,
    val encryptedContent: String = "",
    val iv: String = "",
    val encryptedKeys: Map<String, String> = emptyMap()
)