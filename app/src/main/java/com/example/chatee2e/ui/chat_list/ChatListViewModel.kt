package com.example.chatee2e.ui.chat_list

import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.chatee2e.common.Resource
import com.example.chatee2e.domain.model.Chat
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
    data class DeleteAccount(val password: String) : ChatListEvent()
    object Refresh : ChatListEvent()
}

@HiltViewModel
class ChatListViewModel @Inject constructor(
    private val repository: ChatRepository,
    private val auth: AuthRepository
) : ViewModel() {

    private val _state = mutableStateOf(ChatListState())
    val state: State<ChatListState> = _state

    var currentUserId by mutableStateOf("")
        private set

    private val _eventFlow = MutableSharedFlow<UiEvent>()
    val eventFlow = _eventFlow.asSharedFlow()

    init {
        initializeData()
    }

    private fun initializeData() {
        viewModelScope.launch {
            _state.value = state.value.copy(isLoading = true)
            try {
                repository.connect()
                currentUserId = repository.curentId()
                getChats()
            } catch (e: Exception) {
                _eventFlow.emit(UiEvent.NavigateToAuth)
            } finally {
                _state.value = state.value.copy(isLoading = false)
            }
        }
    }

    fun onEvent(event: ChatListEvent) {
        when (event) {
            is ChatListEvent.DeleteAccount -> deleteAccount(event.password)
            is ChatListEvent.Refresh -> viewModelScope.launch { repository.connect() }
        }
    }

    private fun getChats() {
        repository.getChats().onEach { chats ->
            _state.value = state.value.copy(chats = chats)
        }.launchIn(viewModelScope)
    }

    private fun deleteAccount(password: String) {
        viewModelScope.launch {
            _state.value = state.value.copy(isLoading = true)
            val result = auth.deleteAccount(password)
            _state.value = state.value.copy(isLoading = false)

            when (result) {
                is Resource.Success -> _eventFlow.emit(UiEvent.NavigateToAuth)
                is Resource.Error -> _eventFlow.emit(
                    UiEvent.ShowSnackbar(
                        result.message ?: "Error"
                    )
                )
                else -> Unit
            }
        }
    }

    sealed class UiEvent {
        data class ShowSnackbar(val message: String) : UiEvent()
        data class NavigateToChat(val chatId: String) : UiEvent()
        object NavigateToAuth : UiEvent()
    }
}