package com.example.chatee2e.ui.navigation

sealed class Screen(val route: String) {
    object Auth : Screen("auth")
    object ChatList : Screen("chat_list")
    object Search : Screen("search")

    object CreateGroup : Screen("create_group")
    object ChatDetail : Screen("chat_detail/{chatId}/{chatName}") {
        fun createRoute(chatId: String, chatName: String) = "chat_detail/$chatId/$chatName"
    }
}