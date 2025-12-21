package com.example.chatee2e.ui.create_group


import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.chatee2e.common.Resource
import com.example.chatee2e.domain.model.User
import com.example.chatee2e.domain.repository.ChatRepository
import com.example.chatee2e.domain.repository.UserRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class CreateGroupViewModel @Inject constructor(
    private val chatRepository: ChatRepository,
    private val userRepository: UserRepository
) : ViewModel() {

    private val _state = mutableStateOf(CreateGroupState())
    val state: State<CreateGroupState> = _state

    init {
        loadUsers()
    }

    private fun loadUsers() {
        viewModelScope.launch {
            userRepository.getAllUsers().let { result ->
                _state.value = when (result) {
                    is Resource.Loading -> {
                        _state.value.copy(isLoading = true, error = null)
                    }
                    is Resource.Success -> {
                        _state.value.copy(
                            users = result.data ?: emptyList(),
                            isLoading = false,
                            error = null
                        )
                    }
                    is Resource.Error -> {
                        _state.value.copy(
                            error = result.message,
                            isLoading = false
                        )
                    }
                }
            }
        }
    }

    fun onUserSelected(user: User) {
        val currentSelected = _state.value.selectedUsers.toMutableList()
        if (currentSelected.contains(user)) {
            currentSelected.remove(user)
        } else {
            currentSelected.add(user)
        }
        _state.value = _state.value.copy(selectedUsers = currentSelected)
    }

    fun createGroup(name: String) {
        if (name.isBlank() || _state.value.selectedUsers.isEmpty()) return

        viewModelScope.launch {
            _state.value = _state.value.copy(isLoading = true)
            val result = chatRepository.createGroup(name, _state.value.selectedUsers)
            if (result is Resource.Success) {
                _state.value = _state.value.copy(isSuccess = true, isLoading = false)
            } else {
                _state.value = _state.value.copy(
                    error = result.message,
                    isLoading = false
                )
            }
        }
    }
}

data class CreateGroupState(
    val users: List<User> = emptyList(),
    val selectedUsers: List<User> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null,
    val isSuccess: Boolean = false
)