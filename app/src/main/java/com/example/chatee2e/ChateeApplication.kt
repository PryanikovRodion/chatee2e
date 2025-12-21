package com.example.chatee2e

import android.app.Application
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class ChateeApplication : Application() {
    override fun onCreate() {
        try {
            System.loadLibrary("sqlcipher")
        } catch (e: UnsatisfiedLinkError) {
            e.printStackTrace()
        }
        super.onCreate()
    }
}