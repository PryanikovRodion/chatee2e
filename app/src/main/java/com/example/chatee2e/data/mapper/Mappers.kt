package com.example.chatee2e.data.mapper

import com.example.chatee2e.data.local.entity.ChatEntity
import com.example.chatee2e.data.local.entity.MessageEntity
import com.example.chatee2e.data.remote.dto.ChatDto
import com.example.chatee2e.data.remote.dto.MessageDto
import com.example.chatee2e.data.remote.dto.UserDto
import com.example.chatee2e.domain.model.Chat
import com.example.chatee2e.domain.model.Message
import com.example.chatee2e.domain.model.User
import java.util.Date

fun UserDto.toDomain(id: String): User {
    return User(
        id = id,
        username = this.username,
        email = this.email
    )
}

fun ChatDto.toEntity(id: String, participantsJson: String): ChatEntity {
    return ChatEntity(
        id = id,
        ownerId = ownerId,
        name = this.name,
        isGroup = this.isGroup,
        participantsInfo = participantsJson,
        lastMessageText = null,
        lastMessageTime = null
    )
}

fun ChatEntity.toDomain(participants: List<User>): Chat {
    return Chat(
        id = this.id,
        ownerId = ownerId,
        name = this.name,
        isGroup = this.isGroup,
        participants = participants,
        lastMessageText = this.lastMessageText,
        lastMessageTime = this.lastMessageTime?.let { Date(it) }
    )
}

fun MessageDto.toEntity(
    id: String,
    senderName: String,
    decryptedText: String,
    isMine: Boolean
): MessageEntity {
    return MessageEntity(
        id = id,
        chatId = this.chatId,
        senderId = this.senderId,
        senderName = senderName,
        text = decryptedText,
        timestamp = this.timestamp?.time ?: System.currentTimeMillis(),
        isMine = isMine,
        status = 1
    )
}

fun MessageEntity.toDomain(): Message {
    return Message(
        id = this.id,
        chatId = this.chatId,
        senderId = this.senderId,
        senderName = this.senderName ?: "Unknown",
        text = this.text,
        timestamp = Date(this.timestamp),
        isMine = this.isMine
    )
}
