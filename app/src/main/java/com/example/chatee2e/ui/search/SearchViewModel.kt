package com.example.chatee2e.ui.search

import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.chatee2e.common.Resource
import com.example.chatee2e.domain.model.User
import com.example.chatee2e.domain.repository.UserRepository
import com.example.chatee2e.domain.repository.ChatRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SearchState(
    val query: String = "",
    val users: List<User> = emptyList(),
    val filteredUsers: List<User> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null
)

@HiltViewModel
class SearchViewModel @Inject constructor(
    private val userRepository: UserRepository,
    private val chatRepository: ChatRepository
) : ViewModel() {

    private val _state = mutableStateOf(SearchState())
    val state: State<SearchState> = _state

    private val _eventFlow = MutableSharedFlow<UiEvent>()
    val eventFlow = _eventFlow.asSharedFlow()

    init {
        loadInitialUsers()
    }

    private fun loadInitialUsers() {
        viewModelScope.launch {
            _state.value = state.value.copy(isLoading = true)
            when (val result = userRepository.getAllUsers()) {
                is Resource.Success -> {
                    _state.value = state.value.copy(
                        users = result.data ?: emptyList(),
                        isLoading = false
                    )
                }
                is Resource.Error -> {
                    _state.value = state.value.copy(error = result.message, isLoading = false)
                }
                else -> {}
            }
        }
    }

    fun onQueryChanged(newQuery: String) {
        val filtered = if (newQuery.isBlank()) {
            emptyList()
        } else {
            state.value.users.filter {
                it.username.contains(newQuery, ignoreCase = true) ||
                        it.email.contains(newQuery, ignoreCase = true)
            }
        }
        _state.value = state.value.copy(query = newQuery, filteredUsers = filtered)
    }

    fun onUserClicked(user: User) {
        viewModelScope.launch {
            _state.value = state.value.copy(isLoading = true)
            val result = chatRepository.createDirectChat(user)
            _state.value = state.value.copy(isLoading = false)

            when (result) {
                is Resource.Success -> {
                    result.data?.let { chatId ->
                        _eventFlow.emit(UiEvent.NavigateToChat(chatId))
                    }
                }
                is Resource.Error -> {
                    _eventFlow.emit(UiEvent.ShowSnackbar(result.message ?: "Error starting chat"))
                }
                else -> {}
            }
        }
    }

    sealed class UiEvent {
        data class ShowSnackbar(val message: String) : UiEvent()
        data class NavigateToChat(val chatId: String) : UiEvent()
    }
}