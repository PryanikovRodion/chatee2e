package com.example.chatee2e.ui.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.chatee2e.ui.auth.AuthScreen
import com.example.chatee2e.ui.chat.MessageScreen
import com.example.chatee2e.ui.chat_list.ChatListScreen
import com.example.chatee2e.ui.create_group.CreateGroupScreen
import com.example.chatee2e.ui.search.SearchScreen

@Composable
fun Navigation(startDestination: String) {
    val navController = rememberNavController()

    NavHost(
        navController = navController,
        startDestination = startDestination
    ) {
        composable(route = Screen.Auth.route) {
            AuthScreen(
                onNavigateToChat = {
                    navController.navigate(Screen.ChatList.route) {
                        popUpTo(Screen.Auth.route) { inclusive = true }
                    }
                }
            )
        }

        composable(route = Screen.ChatList.route) {
            ChatListScreen(
                onNavigateToChat = { chatId, chatName ->
                    navController.navigate(Screen.ChatDetail.createRoute(chatId, chatName))
                },
                onNavigateToSearch = {
                    navController.navigate(Screen.Search.route)
                },
                // НОВОЕ: Переход к созданию группы
                onNavigateToCreateGroup = {
                    navController.navigate(Screen.CreateGroup.route)
                }
            )
        }

        // НОВОЕ: Регистрация экрана создания группы
        composable(route = Screen.CreateGroup.route) {
            CreateGroupScreen(
                onNavigateBack = { navController.popBackStack() },
                onSuccess = {
                    navController.popBackStack() // Возврат в список чатов после создания
                }
            )
        }

        composable(route = Screen.Search.route) {
            SearchScreen(
                onNavigateBack = { navController.popBackStack() },
                onNavigateToChat = { chatId ->
                    navController.navigate(Screen.ChatDetail.createRoute(chatId, "Chat")) {
                        popUpTo(Screen.Search.route) { inclusive = true }
                    }
                }
            )
        }

        composable(
            route = Screen.ChatDetail.route,
            arguments = listOf(
                navArgument("chatId") { type = NavType.StringType },
                navArgument("chatName") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val chatName = backStackEntry.arguments?.getString("chatName") ?: "Chat"
            MessageScreen(chatName = chatName)
        }
    }
}