package com.example.chatee2e.data.repository

import android.util.Log
import com.example.chatee2e.common.Resource
import com.example.chatee2e.data.local.SessionManager
import com.example.chatee2e.data.local.dao.ChatDao
import com.example.chatee2e.data.local.entity.ChatEntity
import com.example.chatee2e.data.mapper.toDomain
import com.example.chatee2e.data.mapper.toEntity
import com.example.chatee2e.data.remote.dto.ChatDto
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
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ChatRepositoryImpl @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val chatDao: ChatDao,
    private val sessionManager: SessionManager
) : ChatRepository {

    private val gson = Gson()
    private var snapshotListener: ListenerRegistration? = null

    private val repoScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override suspend fun connect() {
        val myUserId = sessionManager.getUserId() ?: return
        snapshotListener?.remove()

        snapshotListener = firestore.collection("chats")
            .whereArrayContains("participantIds", myUserId)
            .addSnapshotListener { snapshots, e ->
                if (e != null) {
                    Log.e("ChatRepo", "Listen failed.", e)
                    return@addSnapshotListener
                }

                if (snapshots != null) {
                    repoScope.launch {
                        processChatSnapshots(snapshots.documents)
                    }
                }
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
        chatDao.insertChats(chatsToInsert)
    }

    private suspend fun fetchParticipantsDetails(ids: List<String>): List<User> {
        val users = mutableListOf<User>()
        for (id in ids) {
            try {
                val doc = firestore.collection("users").document(id).get().await()
                val dto = doc.toObject(UserDto::class.java)
                if (dto != null) {
                    users.add(dto.toDomain(doc.id))
                }
            } catch (e: Exception) {
            }
        }
        return users
    }
    override fun getChats(): Flow<List<Chat>> {
        return chatDao.getAllChats().map { entities ->
            entities.map { entity ->
                val type = object : TypeToken<List<User>>() {}.type
                val participants: List<User> = gson.fromJson(entity.participantsInfo, type)
                entity.toDomain(participants)
            }
        }
    }

    override suspend fun createGroup(name: String, participants: List<User>): Resource<String> {
        return try {
            val myId = sessionManager.getUserId() ?: return Resource.Error("Not logged in")
            val allIds = (participants.map { it.id } + myId).distinct()

            val chatDto = ChatDto(
                name = name,
                isGroup = true,
                participantIds = allIds
            )
            val ref = firestore.collection("chats").add(chatDto).await()
            Resource.Success(ref.id)
        } catch (e: Exception) {
            Resource.Error(e.localizedMessage ?: "Failed to create group")
        }
    }

    override suspend fun createDirectChat(targetUser: User): Resource<String> {
        return try {
            val myId = sessionManager.getUserId() ?: return Resource.Error("Not logged in")
            val allIds = listOf(myId, targetUser.id).sorted()
            val chatDto = ChatDto(
                name = "",
                isGroup = false,
                participantIds = allIds
            )

            val ref = firestore.collection("chats").add(chatDto).await()
            Resource.Success(ref.id)
        } catch (e: Exception) {
            Resource.Error(e.localizedMessage ?: "Failed to create chat")
        }
    }

    override suspend fun addMembers(chatId: String, newMembers: List<User>): Resource<Unit> {
        return try {
            val newIds = newMembers.map { it.id }
            firestore.collection("chats").document(chatId)
                .update("participantIds", com.google.firebase.firestore.FieldValue.arrayUnion(*newIds.toTypedArray()))
                .await()
            Resource.Success(Unit)
        } catch (e: Exception) {
            Resource.Error(e.localizedMessage ?: "Failed to add members")
        }
    }

    override suspend fun leaveChat(chatId: String): Resource<Unit> {
        return try {
            val myId = sessionManager.getUserId() ?: return Resource.Error("Not logged in")
            firestore.collection("chats").document(chatId)
                .update("participantIds", com.google.firebase.firestore.FieldValue.arrayRemove(myId))
                .await()
            chatDao.deleteChatById(chatId)
            Resource.Success(Unit)
        } catch (e: Exception) {
            Resource.Error(e.localizedMessage ?: "Failed to leave chat")
        }
    }
}