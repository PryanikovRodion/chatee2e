package com.example.chatee2e.data.repository

import android.util.Log
import com.example.chatee2e.common.Resource
import com.example.chatee2e.data.crypto.CryptoManager
import com.example.chatee2e.data.local.SessionManager
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
    private val sessionManager: SessionManager
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

    override suspend fun signIn(): Resource<Boolean> {
        return try {
            val authResult = auth.signInAnonymously().await()
            val firebaseUser = authResult.user ?: return Resource.Error("Firebase user is null")
            val uid = firebaseUser.uid
            cryptoManager.ensureKeyPairExists()
            val myPublicKey = cryptoManager.getMyPublicKeyBase64()
            val initialUsername = "User_${uid.takeLast(4)}"
            val userDto = UserDto(
                username = initialUsername,
                email = "", // У анонима нет email
                publicKey = myPublicKey // <--- Самое важное!
            )
            firestore.collection("users").document(uid)
                .set(userDto, SetOptions.merge())
                .await()
            sessionManager.saveUserId(uid)
            Resource.Success(true)
        } catch (e: Exception) {
            e.printStackTrace()
            Resource.Error(e.localizedMessage ?: "Unknown Login Error")
        }
    }

    override suspend fun signOut() {
        auth.signOut()
        sessionManager.clearSession()
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