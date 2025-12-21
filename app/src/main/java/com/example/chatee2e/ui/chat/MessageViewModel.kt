package com.example.chatee2e.ui.chat

import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.chatee2e.common.Resource
import com.example.chatee2e.domain.repository.MessageRepository
import com.example.chatee2e.domain.repository.ChatRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import javax.inject.Inject
import com.example.chatee2e.domain.model.Message

data class MessageState(
    val messages: List<Message> = emptyList(),
    val messageText: String = "",
    val isLoading: Boolean = false
)

@HiltViewModel
class MessageViewModel @Inject constructor(
    private val repository: MessageRepository,
    private val chatRepository: ChatRepository,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val _state = mutableStateOf(MessageState())
    val state: State<MessageState> = _state

    private val _eventFlow = MutableSharedFlow<UiEvent>()
    val eventFlow = _eventFlow.asSharedFlow()

    private val chatId: String = checkNotNull(savedStateHandle["chatId"])

    init {
        getMessages()
    }

    private fun getMessages() {
        repository.getMessages(chatId).onEach { messages ->
            _state.value = state.value.copy(messages = messages)
        }.launchIn(viewModelScope)
    }

    fun onMessageChange(text: String) {
        _state.value = state.value.copy(messageText = text)
    }

    fun sendMessage() {
        val text = state.value.messageText
        if (text.isBlank()) return

        viewModelScope.launch {
            _state.value = state.value.copy(messageText = "")
            val result = repository.sendMessage(chatId, text)
            if (result is Resource.Error) {
                _eventFlow.emit(UiEvent.ShowSnackbar(result.message ?: "Failed to send"))
            }
        }
    }

    fun leaveChat(chatId: String) {
        viewModelScope.launch {
            chatRepository.leaveChat(chatId)
        }
    }

    sealed class UiEvent {
        data class ShowSnackbar(val message: String) : UiEvent()
    }
}