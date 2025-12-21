package com.example.chatee2e

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.appcompat.app.AppCompatActivity
import androidx.biometric.BiometricPrompt
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.core.content.ContextCompat
import com.example.chatee2e.data.crypto.CryptoManager
import com.example.chatee2e.data.local.SessionManager
import com.example.chatee2e.domain.repository.ChatRepository
import com.example.chatee2e.ui.navigation.Navigation
import com.example.chatee2e.ui.navigation.Screen
import com.example.chatee2e.ui.theme.Chatee2eTheme
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import android.util.Base64
import android.content.SharedPreferences
import android.util.Log
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch

@AndroidEntryPoint
class MainActivity : AppCompatActivity() {

    @Inject
    lateinit var cryptoManager: CryptoManager

    @Inject
    lateinit var sessionManager: SessionManager

    @Inject
    lateinit var sharedPreferences: SharedPreferences

    @Inject
    lateinit var chatRepository: ChatRepository

    private var isUnlocked by mutableStateOf(false)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val userId = sessionManager.getUserId()

        if (userId == null) {
            isUnlocked = true
        } else {
            showBiometricPrompt()
        }

        setContent {
            Chatee2eTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    if (isUnlocked) {
                        val startDestination = if (userId != null) Screen.ChatList.route else Screen.Auth.route
                        Navigation(startDestination = startDestination)
                    } else {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator()
                        }
                    }
                }
            }
        }
    }

    private fun startChatSync() {
        lifecycleScope.launch {
            chatRepository.connect()
        }
    }

    private fun showBiometricPrompt() {
        val executor = ContextCompat.getMainExecutor(this)
        val callback = object : BiometricPrompt.AuthenticationCallback() {
            override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                super.onAuthenticationSucceeded(result)

                val encryptedKeyStr = sharedPreferences.getString("encrypted_db_key", null)
                val ivStr = sharedPreferences.getString("db_key_iv", null)

                if (encryptedKeyStr != null && ivStr != null) {
                    try {
                        val encryptedKey = Base64.decode(encryptedKeyStr, Base64.DEFAULT)
                        val iv = Base64.decode(ivStr, Base64.DEFAULT)
                        val rawKey = cryptoManager.decryptDatabasePassphrase(encryptedKey, iv)
                        sessionManager.setDatabaseKey(rawKey)

                        startChatSync()
                    } catch (e: Exception) {
                        Log.e("AUTH", "Decryption failed", e)
                    }
                }

                android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
                    isUnlocked = true
                }, 100)
            }

            override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                super.onAuthenticationError(errorCode, errString)
                finish()
            }
        }

        val promptInfo = BiometricPrompt.PromptInfo.Builder()
            .setTitle("Application Login")
            .setSubtitle("Confirm your identity to access secure chats")
            .setNegativeButtonText("Cancel")
            .build()

        BiometricPrompt(this, executor, callback).authenticate(promptInfo)
    }
}



