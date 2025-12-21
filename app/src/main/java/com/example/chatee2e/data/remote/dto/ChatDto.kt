package com.example.chatee2e.data.remote.dto

data class ChatDto(
    val name: String = "",
    val isGroup: Boolean = false,
    val participantIds: List<String> = emptyList()
)