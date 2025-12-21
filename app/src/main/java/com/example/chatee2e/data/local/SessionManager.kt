package com.example.chatee2e.data.local

import android.content.Context
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

    @Volatile
    var databasePassphrase: ByteArray? = null
        private set

    private val publicKeyCache = mutableMapOf<String, String>()

    fun setDatabaseKey(key: ByteArray) {
        databasePassphrase = key
    }

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

    fun clearSession(context: Context) {
        publicKeyCache.clear()
        databasePassphrase?.fill(0)
        databasePassphrase = null

        prefs.edit {
            clear()
            commit()
        }

        val dbName = "chatee2e_db"
        context.deleteDatabase(dbName)
        context.deleteDatabase("$dbName-shm")
        context.deleteDatabase("$dbName-wal")
    }
}