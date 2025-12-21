package com.example.chatee2e.domain

import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SessionManager @Inject constructor() {

    @Volatile
    var databasePassphrase: ByteArray? = null
        private set

    fun setKey(key: ByteArray) {
        databasePassphrase = key
    }

    fun isSessionActive(): Boolean {
        return databasePassphrase != null
    }

    fun clear() {
        databasePassphrase?.fill(0)
        databasePassphrase = null
    }
}