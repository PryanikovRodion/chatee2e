package com.example.chatee2e.data.local

import android.content.SharedPreferences
import androidx.core.content.edit
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SessionManager @Inject constructor(
    private val prefs: SharedPreferences
) {

    companion object {
        private const val KEY_USER_ID = "current_user_id"
    }

    private val publicKeyCache = mutableMapOf<String, String>()


    fun saveUserId(id: String) {
        prefs.edit {
            putString(KEY_USER_ID, id)
        }
    }

    fun getUserId(): String? {
        return prefs.getString(KEY_USER_ID, null)
    }

    fun savePublicKey(userId: String, publicKey: String) {
        publicKeyCache[userId] = publicKey
    }

    fun getPublicKey(userId: String): String? {
        return publicKeyCache[userId]
    }

    fun savePublicKeys(keys: Map<String, String>) {
        publicKeyCache.putAll(keys)
    }

    fun clearSession() {
        publicKeyCache.clear()
        prefs.edit {
            remove(KEY_USER_ID)
        }
    }
}