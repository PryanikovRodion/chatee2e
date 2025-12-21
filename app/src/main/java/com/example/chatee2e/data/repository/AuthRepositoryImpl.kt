package com.example.chatee2e.data.repository

import android.util.Log
import com.example.chatee2e.common.Resource
import com.example.chatee2e.data.crypto.CryptoManager
import com.example.chatee2e.data.local.SessionManager
import com.example.chatee2e.data.local.dao.ChatDao
import com.example.chatee2e.data.local.dao.MessageDao
import com.example.chatee2e.data.mapper.toDomain
import com.example.chatee2e.data.remote.dto.UserDto
import com.example.chatee2e.domain.model.User
import com.example.chatee2e.domain.repository.AuthRepository
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AuthRepositoryImpl @Inject constructor(
    private val auth: FirebaseAuth,
    private val firestore: FirebaseFirestore,
    private val cryptoManager: CryptoManager,
    private val sessionManager: SessionManager,
    private val chatDao: dagger.Lazy<ChatDao>,
    private val messageDao: dagger.Lazy<MessageDao>
) : AuthRepository {

    override val currentUser: Flow<User?> = callbackFlow {
        val listener = FirebaseAuth.AuthStateListener { firebaseAuth ->
            val uid = firebaseAuth.currentUser?.uid
            if (uid == null) {
                trySend(null)
            } else {
                firestore.collection("users").document(uid).get()
                    .addOnSuccessListener { document ->
                        val userDto = document.toObject(UserDto::class.java)
                        val user = userDto?.toDomain(document.id)
                        trySend(user)
                        sessionManager.saveUserId(uid)
                    }
                    .addOnFailureListener {
                        trySend(null)
                    }
            }
        }
        auth.addAuthStateListener(listener)
        awaitClose { auth.removeAuthStateListener(listener) }
    }

    override suspend fun signUp(email: String, password: String, username: String): Resource<Unit> {
        return try {
            auth.createUserWithEmailAndPassword(email, password).await()
            sendVerificationEmail()

            val uid = auth.currentUser?.uid ?: return Resource.Error("User ID not found")
            val userDto = UserDto(
                username = username,
                email = email,
                publicKey = ""
            )
            firestore.collection("users").document(uid).set(userDto).await()

            Resource.Success(Unit)
        } catch (e: Exception) {
            Resource.Error(e.localizedMessage ?: "Registration failed")
        }
    }

    override suspend fun signIn(email: String, password: String): Resource<Boolean> {
        return try {
            val result = auth.signInWithEmailAndPassword(email, password).await()
            val user = result.user ?: return Resource.Error("User not found")

            if (!user.isEmailVerified) {
                return Resource.Error("Please verify your email first")
            }

            val uid = user.uid
            cryptoManager.ensureKeyPairExists()
            val myPublicKey = cryptoManager.getMyPublicKeyBase64()

            firestore.collection("users").document(uid)
                .update("publicKey", myPublicKey)
                .await()

            sessionManager.saveUserId(uid)
            Resource.Success(true)
        } catch (e: Exception) {
            Resource.Error(e.localizedMessage ?: "Login failed")
        }
    }

    override suspend fun signOut() {
        auth.signOut()
        sessionManager.clearSession()
    }

    override suspend fun deleteAccount(): Resource<Unit> {
        return try {
            val user = auth.currentUser ?: return Resource.Error("Not logged in")
            val uid = user.uid

            firestore.collection("users").document(uid).delete().await()

            messageDao.get().clearAllMessages()
            chatDao.get().clearAllChats()

            cryptoManager.deleteKeys()

            user.delete().await()
            sessionManager.clearSession()

            Resource.Success(Unit)
        } catch (e: Exception) {
            Resource.Error(e.localizedMessage ?: "Failed to delete account")
        }
    }

    override fun isEmailVerified(): Boolean {
        auth.currentUser?.reload()
        return auth.currentUser?.isEmailVerified ?: false
    }

    override suspend fun sendVerificationEmail(): Resource<Unit> {
        return try {
            auth.currentUser?.sendEmailVerification()?.await()
            Resource.Success(Unit)
        } catch (e: Exception) {
            Resource.Error(e.localizedMessage ?: "Failed to send email")
        }
    }

    override fun isKeysSetUp(): Boolean {
        return try {
            cryptoManager.ensureKeyPairExists()
            true
        } catch (e: Exception) {
            false
        }
    }

    override suspend fun searchUserByEmail(email: String): Resource<User?> {
        return try {
            val snapshot = firestore.collection("users")
                .whereEqualTo("email", email)
                .limit(1)
                .get()
                .await()
            if (snapshot.isEmpty) {
                Resource.Success(null)
            } else {
                val doc = snapshot.documents.first()
                val userDto = doc.toObject(UserDto::class.java)
                Resource.Success(userDto?.toDomain(doc.id))
            }
        } catch (e: Exception) {
            Resource.Error(e.localizedMessage ?: "Search failed")
        }
    }

    override suspend fun getUserById(userId: String): Resource<User> {
        return try {
            val doc = firestore.collection("users").document(userId).get().await()
            val userDto = doc.toObject(UserDto::class.java)
            if (userDto != null) {
                Resource.Success(userDto.toDomain(doc.id))
            } else {
                Resource.Error("User not found")
            }
        } catch (e: Exception) {
            Resource.Error(e.localizedMessage ?: "Fetch failed")
        }
    }
}