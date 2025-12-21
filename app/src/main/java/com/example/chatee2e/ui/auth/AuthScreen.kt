package com.example.chatee2e.ui.auth

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import kotlinx.coroutines.flow.collectLatest

@Composable
fun AuthScreen(
    onNavigateToChat: () -> Unit,
    viewModel: AuthViewModel = hiltViewModel()
) {
    val state = viewModel.state.value
    val snackbarHostState = androidx.compose.runtime.remember { SnackbarHostState() }

    LaunchedEffect(key1 = true) {
        viewModel.eventFlow.collectLatest { event ->
            when (event) {
                is AuthViewModel.UiEvent.AuthSuccess -> onNavigateToChat()
                is AuthViewModel.UiEvent.ShowSnackbar -> snackbarHostState.showSnackbar(event.message)
            }
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = if (state.isAwaitingVerification) "Verify Email" else if (state.isLoginMode) "Sign In" else "Sign Up",
                style = MaterialTheme.typography.headlineMedium
            )

            Spacer(modifier = Modifier.height(16.dp))

            if (state.isAwaitingVerification) {
                Text(
                    text = "We sent a link to ${state.email}. Please verify it to continue.",
                    style = MaterialTheme.typography.bodyMedium
                )
                Spacer(modifier = Modifier.height(24.dp))
                Button(onClick = { viewModel.onEvent(AuthEvent.CheckVerification) }) {
                    Text("I've Verified My Email")
                }
            } else {
                if (!state.isLoginMode) {
                    OutlinedTextField(
                        value = state.username,
                        onValueChange = { viewModel.onEvent(AuthEvent.UsernameChanged(it)) },
                        label = { Text("Username") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                OutlinedTextField(
                    value = state.email,
                    onValueChange = { viewModel.onEvent(AuthEvent.EmailChanged(it)) },
                    label = { Text("Email") },
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = state.password,
                    onValueChange = { viewModel.onEvent(AuthEvent.PasswordChanged(it)) },
                    label = { Text("Password") },
                    visualTransformation = PasswordVisualTransformation(),
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(24.dp))

                if (state.isLoading) {
                    CircularProgressIndicator()
                } else {
                    Button(
                        onClick = { viewModel.onEvent(AuthEvent.Authenticate) },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(if (state.isLoginMode) "Login" else "Register")
                    }

                    TextButton(onClick = { viewModel.onEvent(AuthEvent.ToggleMode) }) {
                        Text(if (state.isLoginMode) "Don't have an account? Sign Up" else "Already have an account? Sign In")
                    }
                }
            }
        }
    }
}