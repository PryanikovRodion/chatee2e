package com.example.chatee2e.data.repository

import android.util.Log
import com.example.chatee2e.common.Resource
import com.example.chatee2e.data.local.SessionManager
import com.example.chatee2e.data.local.dao.ChatDao
import com.example.chatee2e.data.local.entity.ChatEntity
import com.example.chatee2e.data.mapper.toDomain
import com.example.chatee2e.data.mapper.toEntity
import com.example.chatee2e.data.remote.dto.ChatDto
import com.example.chatee2e.data.remote.dto.InviteDto
import com.example.chatee2e.data.remote.dto.UserDto
import com.example.chatee2e.domain.model.Chat
import com.example.chatee2e.domain.model.User
import com.example.chatee2e.domain.repository.ChatRepository
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ChatRepositoryImpl @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val chatDao: dagger.Lazy<ChatDao>,
    private val sessionManager: SessionManager,
    private val cryptoManager: com.example.chatee2e.data.crypto.CryptoManager,
    private val prefs: android.content.SharedPreferences
) : ChatRepository {

    private val gson = Gson()
    private var chatsListener: ListenerRegistration? = null
    private var invitesListener: ListenerRegistration? = null
    private val repoScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override suspend fun connect() {
        if (sessionManager.databasePassphrase == null) {
            setupDatabaseKey()
        }

        val myUserId = sessionManager.getUserId() ?: return

        setupListeners(myUserId)
    }

    private fun setupDatabaseKey() {
        try {
            val encryptedPassphraseBase64 = prefs.getString("encrypted_db_pass", null)
            val ivBase64 = prefs.getString("db_pass_iv", null)

            if (encryptedPassphraseBase64 != null && ivBase64 != null) {
                val encryptedData = android.util.Base64.decode(encryptedPassphraseBase64, android.util.Base64.NO_WRAP)
                val iv = android.util.Base64.decode(ivBase64, android.util.Base64.NO_WRAP)

                val decryptedKey = cryptoManager.decryptDatabasePassphrase(encryptedData, iv)
                sessionManager.setDatabaseKey(decryptedKey)
            } else {
                val (rawKey, result) = cryptoManager.generateAndEncryptDatabasePassphrase()

                prefs.edit().apply {
                    putString("encrypted_db_pass", android.util.Base64.encodeToString(result.encryptedData, android.util.Base64.NO_WRAP))
                    putString("db_pass_iv", android.util.Base64.encodeToString(result.iv, android.util.Base64.NO_WRAP))
                    commit()
                }

                sessionManager.setDatabaseKey(rawKey)
            }
        } catch (e: Exception) {
            android.util.Log.e("ChatRepo", "Critical error during DB key setup", e)
            throw IllegalStateException("Could not initialize database key")
        }
    }

    private fun setupListeners(myUserId: String) {
        invitesListener?.remove()
        invitesListener = firestore.collection("users")
            .document(myUserId)
            .collection("invites")
            .addSnapshotListener { snapshots, e ->
                if (e != null || snapshots == null) return@addSnapshotListener
                repoScope.launch {
                    for (doc in snapshots.documents) {
                        val invite = doc.toObject(InviteDto::class.java) ?: continue
                        fetchAndSaveChat(invite.chatId)
                        doc.reference.delete()
                    }
                }
            }

        chatsListener?.remove()
        chatsListener = firestore.collection("chats")
            .whereArrayContains("participantIds", myUserId)
            .addSnapshotListener { snapshots, e ->
                if (e != null || snapshots == null) return@addSnapshotListener
                repoScope.launch { processChatSnapshots(snapshots.documents) }
            }
    }

    private suspend fun fetchAndSaveChat(chatId: String) {
        try {
            val doc = firestore.collection("chats").document(chatId).get().await()
            val chatDto = doc.toObject(ChatDto::class.java) ?: return
            val participantsList = fetchParticipantsDetails(chatDto.participantIds)
            val participantsJson = gson.toJson(participantsList)
            chatDao.get().insertChats(listOf(chatDto.toEntity(doc.id, participantsJson)))
        } catch (e: Exception) {
            Log.e("ChatRepo", "Fetch failed", e)
        }
    }

    private suspend fun processChatSnapshots(documents: List<com.google.firebase.firestore.DocumentSnapshot>) {
        val chatsToInsert = mutableListOf<ChatEntity>()
        for (doc in documents) {
            val chatDto = doc.toObject(ChatDto::class.java) ?: continue
            val participantsList = fetchParticipantsDetails(chatDto.participantIds)
            val participantsJson = gson.toJson(participantsList)
            chatsToInsert.add(chatDto.toEntity(doc.id, participantsJson))
        }
        chatDao.get().insertChats(chatsToInsert)
    }

    private suspend fun fetchParticipantsDetails(ids: List<String>): List<User> {
        val users = mutableListOf<User>()
        for (id in ids) {
            try {
                val doc = firestore.collection("users").document(id).get().await()
                doc.toObject(UserDto::class.java)?.let { users.add(it.toDomain(doc.id)) }
            } catch (e: Exception) { }
        }
        return users
    }

    override fun getChats(): Flow<List<Chat>> {
        return chatDao.get().getAllChats().map { entities ->
            entities.map { entity ->
                val type = object : TypeToken<List<User>>() {}.type
                val participants: List<User> = gson.fromJson(entity.participantsInfo, type)
                entity.toDomain(participants)
            }
        }
    }

    override suspend fun createGroup(name: String, participants: List<User>): Resource<String> {
        return try {
            val myId = sessionManager.getUserId() ?: return Resource.Error("Unauthorized")
            val allIds = (participants.map { it.id } + myId).distinct()

            val chatDto = ChatDto(
                name = name,
                isGroup = true,
                participantIds = allIds,
                ownerId = myId
            )
            val ref = firestore.collection("chats").add(chatDto).await()

            for (user in participants) {
                sendInvite(user.id, ref.id, myId)
            }

            Resource.Success(ref.id)
        } catch (e: Exception) {
            Resource.Error(e.localizedMessage ?: "Failed")
        }
    }

    override suspend fun createDirectChat(targetUser: User): Resource<String> {
        return try {
            val myId = sessionManager.getUserId() ?: return Resource.Error("Unauthorized")
            val allIds = listOf(myId, targetUser.id).sorted()

            val chatDto = ChatDto(
                name = "",
                isGroup = false,
                participantIds = allIds,
                ownerId = null
            )

            val ref = firestore.collection("chats").add(chatDto).await()
            sendInvite(targetUser.id, ref.id, myId)

            Resource.Success(ref.id)
        } catch (e: Exception) {
            Resource.Error(e.localizedMessage ?: "Failed")
        }
    }

    override suspend fun addMembers(chatId: String, newMembers: List<User>): Resource<Unit> {
        return try {
            val myId = sessionManager.getUserId() ?: return Resource.Error("Unauthorized")
            val chatDoc = firestore.collection("chats").document(chatId).get().await()
            val chatDto = chatDoc.toObject(ChatDto::class.java) ?: return Resource.Error("Not found")

            if (chatDto.ownerId != null && chatDto.ownerId != myId) {
                return Resource.Error("Only admin can add members")
            }

            val newIds = newMembers.map { it.id }
            firestore.collection("chats").document(chatId)
                .update("participantIds", com.google.firebase.firestore.FieldValue.arrayUnion(*newIds.toTypedArray()))
                .await()

            for (user in newMembers) {
                sendInvite(user.id, chatId, myId)
            }

            Resource.Success(Unit)
        } catch (e: Exception) {
            Resource.Error(e.localizedMessage ?: "Failed")
        }
    }

    private suspend fun sendInvite(receiverId: String, chatId: String, senderId: String) {
        val invite = InviteDto(chatId = chatId, senderId = senderId, receiverId = receiverId)
        firestore.collection("users")
            .document(receiverId)
            .collection("invites")
            .add(invite)
            .await()
    }

    override suspend fun leaveChat(chatId: String): Resource<Unit> {
        return try {
            val myId = sessionManager.getUserId() ?: return Resource.Error("Unauthorized")
            firestore.collection("chats").document(chatId)
                .update("participantIds", com.google.firebase.firestore.FieldValue.arrayRemove(myId))
                .await()
            chatDao.get().deleteChatById(chatId)
            Resource.Success(Unit)
        } catch (e: Exception) {
            Resource.Error(e.localizedMessage ?: "Failed")
        }
    }

    override suspend fun curentId(): String {
        return sessionManager.getUserId()?:""
    }
}