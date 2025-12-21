package com.example.chatee2e.di

import android.app.Application
import android.content.Context
import android.content.SharedPreferences
import com.example.chatee2e.data.crypto.CryptoManager
import com.example.chatee2e.data.crypto.CryptoManagerImpl
import com.example.chatee2e.data.local.AppDatabase
import com.example.chatee2e.data.local.SessionManager
import com.example.chatee2e.data.local.dao.ChatDao
import com.example.chatee2e.data.local.dao.MessageDao
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import net.zetetic.database.sqlcipher.SupportOpenHelperFactory
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {



    @Provides
    @Singleton
    fun provideFirebaseAuth(): FirebaseAuth {
        return FirebaseAuth.getInstance()
    }

    @Provides
    @Singleton
    fun provideFirebaseFirestore(): FirebaseFirestore {
        return FirebaseFirestore.getInstance()
    }



    @Provides
    @Singleton
    fun provideSharedPreferences(app: Application): SharedPreferences {
        return app.getSharedPreferences("chatee2e_prefs", Context.MODE_PRIVATE)
    }

    @Provides
    @Singleton
    fun provideSessionManager(prefs: SharedPreferences): SessionManager {
        return SessionManager(prefs)
    }

    @Provides
    @Singleton
    fun provideCryptoManager(): CryptoManager {
        return CryptoManagerImpl()
    }



    @Provides
    @Singleton
    fun provideDatabase(
        app: Application,
        cryptoManager: CryptoManager
    ): AppDatabase {
        val dbFile = app.getDatabasePath("chatee2e.db")
        val keyFile = File(app.filesDir, "db_key.dat")

        val passphrase: ByteArray

        if (!keyFile.exists()) {

            val (rawPass, cryptoResult) = cryptoManager.generateAndEncryptDatabasePassphrase()
            passphrase = rawPass

            FileOutputStream(keyFile).use { fos ->
                fos.write(cryptoResult.iv.size)
                fos.write(cryptoResult.iv)
                fos.write(cryptoResult.encryptedData)
            }
        } else {

            FileInputStream(keyFile).use { fis ->
                val ivSize = fis.read()
                val iv = ByteArray(ivSize)
                fis.read(iv)

                val encryptedData = fis.readBytes()


                passphrase = cryptoManager.decryptDatabasePassphrase(encryptedData, iv)
            }
        }


        val factory = SupportOpenHelperFactory(passphrase)

        return androidx.room.Room.databaseBuilder(
            app,
            AppDatabase::class.java,
            "chatee2e.db"
        )
            .openHelperFactory(factory)
            .fallbackToDestructiveMigration()
            .build()
    }



    @Provides
    @Singleton
    fun provideChatDao(db: AppDatabase): ChatDao {
        return db.chatDao
    }

    @Provides
    @Singleton
    fun provideMessageDao(db: AppDatabase): MessageDao {
        return db.messageDao
    }
}