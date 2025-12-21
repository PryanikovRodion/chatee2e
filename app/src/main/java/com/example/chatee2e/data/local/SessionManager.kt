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

    fun saveUserId(id: String) {
        prefs.edit {
            putString(KEY_USER_ID, id)
        }
    }

    fun getUserId(): String? {
        return prefs.getString(KEY_USER_ID, null)
    }
    fun clearSession() {
        prefs.edit {
            remove(KEY_USER_ID)
        }
    }
}