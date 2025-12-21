package com.example.chatee2e.data.repository

import com.example.chatee2e.common.Resource
import com.example.chatee2e.data.local.SessionManager
import com.example.chatee2e.data.mapper.toDomain
import com.example.chatee2e.data.remote.dto.UserDto
import com.example.chatee2e.domain.model.User
import com.example.chatee2e.domain.repository.UserRepository
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class UserRepositoryImpl @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val sessionManager: SessionManager
) : UserRepository {
    override suspend fun blockUser(userId: String): Resource<Unit> {
        return try {
            val myId = sessionManager.getUserId() ?: return Resource.Error("Not logged in")

            val blockData = mapOf("blockedAt" to com.google.firebase.firestore.FieldValue.serverTimestamp())

            firestore.collection("users")
                .document(myId)
                .collection("blockedUsers")
                .document(userId)
                .set(blockData)
                .await()

            Resource.Success(Unit)
        } catch (e: Exception) {
            Resource.Error(e.localizedMessage ?: "Failed to block user")
        }
    }
    override suspend fun unblockUser(userId: String): Resource<Unit> {
        return try {
            val myId = sessionManager.getUserId() ?: return Resource.Error("Not logged in")

            firestore.collection("users")
                .document(myId)
                .collection("blockedUsers")
                .document(userId)
                .delete()
                .await()

            Resource.Success(Unit)
        } catch (e: Exception) {
            Resource.Error(e.localizedMessage ?: "Failed to unblock user")
        }
    }

    override fun getBlockedUsers(): Flow<List<User>> = callbackFlow {
        val myId = sessionManager.getUserId()
        if (myId == null) {
            trySend(emptyList())
            close()
            return@callbackFlow
        }
        val listener = firestore.collection("users")
            .document(myId)
            .collection("blockedUsers")
            .addSnapshotListener { snapshots, e ->
                if (e != null) return@addSnapshotListener

                val blockedIds = snapshots?.documents?.map { it.id } ?: emptyList()

                if (blockedIds.isEmpty()) {
                    trySend(emptyList())
                    return@addSnapshotListener
                }
                firestore.collection("users")
                    .whereIn(com.google.firebase.firestore.FieldPath.documentId(), blockedIds.take(10))
                    .get()
                    .addOnSuccessListener { userDocs ->
                        val users = userDocs.documents.mapNotNull { doc ->
                            doc.toObject(UserDto::class.java)?.toDomain(doc.id)
                        }
                        trySend(users)
                    }
            }
        awaitClose { listener.remove() }
    }
}