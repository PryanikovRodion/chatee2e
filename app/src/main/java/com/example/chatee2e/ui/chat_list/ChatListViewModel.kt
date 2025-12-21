package com.example.chatee2e.ui.chat_list

import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.chatee2e.common.Resource
import com.example.chatee2e.domain.model.Chat
import com.example.chatee2e.domain.model.User
import com.example.chatee2e.domain.repository.AuthRepository
import com.example.chatee2e.domain.repository.ChatRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ChatListState(
    val chats: List<Chat> = emptyList(),
    val isLoading: Boolean = false
)

sealed class ChatListEvent {
    data class CreateDirectChat(val user: User) : ChatListEvent()
    object Refresh : ChatListEvent()
}
@HiltViewModel
class ChatListViewModel @Inject constructor(
    private val repository: ChatRepository,
    private val auth: AuthRepository
) : ViewModel() {

    private val _state = mutableStateOf(ChatListState())
    val state: State<ChatListState> = _state
    val currentUser = auth.currentUser
    private val _eventFlow = MutableSharedFlow<UiEvent>()
    val eventFlow = _eventFlow.asSharedFlow()

    init {
        viewModelScope.launch {
            repository.connect()
        }
        getChats()
    }

    fun onEvent(event: ChatListEvent) {
        when (event) {
            is ChatListEvent.CreateDirectChat -> {
                createDirectChat(event.user)
            }
            is ChatListEvent.Refresh -> {
                viewModelScope.launch { repository.connect() }
            }
        }
    }

    private fun getChats() {
        repository.getChats().onEach { chats ->
            _state.value = state.value.copy(
                chats = chats
            )
        }.launchIn(viewModelScope)
    }

    private fun createDirectChat(user: User) {
        viewModelScope.launch {
            val result = repository.createDirectChat(user)
            when (result) {
                is Resource.Success -> {
                    result.data?.let { chatId ->
                        _eventFlow.emit(UiEvent.NavigateToChat(chatId))
                    }
                }
                is Resource.Error -> {
                    _eventFlow.emit(UiEvent.ShowSnackbar(result.message ?: "Error"))
                }
                else -> Unit
            }
        }
    }
    fun logout() {
        viewModelScope.launch {
            auth.signOut()
        }
    }

    sealed class UiEvent {
        data class ShowSnackbar(val message: String) : UiEvent()
        data class NavigateToChat(val chatId: String) : UiEvent()
    }
}