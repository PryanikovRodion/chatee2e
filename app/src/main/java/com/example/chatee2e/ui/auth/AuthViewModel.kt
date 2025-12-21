package com.example.chatee2e.ui.auth

import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.chatee2e.common.Resource
import com.example.chatee2e.domain.repository.AuthRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed class AuthEvent {
    data class EmailChanged(val value: String) : AuthEvent()
    data class PasswordChanged(val value: String) : AuthEvent()
    data class UsernameChanged(val value: String) : AuthEvent()
    object Authenticate : AuthEvent()
    object ToggleMode : AuthEvent()
    object CheckVerification : AuthEvent()
}
data class AuthState(
    val email: String = "",
    val password: String = "",
    val username: String = "",
    val isLoginMode: Boolean = true,
    val isLoading: Boolean = false,
    val isAwaitingVerification: Boolean = false
)
@HiltViewModel
class AuthViewModel @Inject constructor(
    private val repository: AuthRepository
) : ViewModel() {

    private val _state = mutableStateOf(AuthState())
    val state: State<AuthState> = _state

    private val _eventFlow = MutableSharedFlow<UiEvent>()
    val eventFlow = _eventFlow.asSharedFlow()

    fun onEvent(event: AuthEvent) {
        when (event) {
            is AuthEvent.EmailChanged -> {
                _state.value = state.value.copy(email = event.value)
            }
            is AuthEvent.PasswordChanged -> {
                _state.value = state.value.copy(password = event.value)
            }
            is AuthEvent.UsernameChanged -> {
                _state.value = state.value.copy(username = event.value)
            }
            is AuthEvent.ToggleMode -> {
                _state.value = state.value.copy(
                    isLoginMode = !state.value.isLoginMode,
                    isAwaitingVerification = false
                )
            }
            is AuthEvent.Authenticate -> {
                if (state.value.isLoginMode) signIn() else signUp()
            }
            is AuthEvent.CheckVerification -> {
                checkVerification()
            }
        }
    }

    private fun signUp() {
        viewModelScope.launch {
            _state.value = state.value.copy(isLoading = true)
            val result = repository.signUp(
                state.value.email,
                state.value.password,
                state.value.username
            )
            _state.value = state.value.copy(isLoading = false)

            when (result) {
                is Resource.Success -> {
                    _state.value = state.value.copy(isAwaitingVerification = true)
                    _eventFlow.emit(UiEvent.ShowSnackbar("Verification email sent!"))
                }
                is Resource.Error -> {
                    _eventFlow.emit(UiEvent.ShowSnackbar(result.message ?: "Registration failed"))
                }
                is Resource.Loading -> {
                    _state.value = state.value.copy(isLoading = true)
                }
            }
        }
    }

    private fun signIn() {
        viewModelScope.launch {
            _state.value = state.value.copy(isLoading = true)
            val result = repository.signIn(state.value.email, state.value.password)
            _state.value = state.value.copy(isLoading = false)

            when (result) {
                is Resource.Success -> {
                    _eventFlow.emit(UiEvent.AuthSuccess)
                }
                is Resource.Error -> {
                    if (result.message?.contains("verify", ignoreCase = true) == true) {
                        _state.value = state.value.copy(isAwaitingVerification = true)
                    }
                    _eventFlow.emit(UiEvent.ShowSnackbar(result.message ?: "Login failed"))
                }
                is Resource.Loading -> {
                    _state.value = state.value.copy(isLoading = true)
                }
            }
        }
    }

    private fun checkVerification() {
        if (repository.isEmailVerified()) {
            _state.value = state.value.copy(isAwaitingVerification = false)
            viewModelScope.launch {
                _eventFlow.emit(UiEvent.ShowSnackbar("Email verified! You can now login."))
            }
            if (!state.value.isLoginMode) {
                _state.value = state.value.copy(isLoginMode = true)
            }
        } else {
            viewModelScope.launch {
                _eventFlow.emit(UiEvent.ShowSnackbar("Email not verified yet. Check your inbox."))
            }
        }
    }

    sealed class UiEvent {
        data class ShowSnackbar(val message: String) : UiEvent()
        object AuthSuccess : UiEvent()
    }
}