package com.example.chatee2e.data.repository

import android.util.Base64
import android.util.Log
import com.example.chatee2e.common.Resource
import com.example.chatee2e.data.crypto.CryptoManager
import com.example.chatee2e.data.local.SessionManager
import com.example.chatee2e.data.local.dao.ChatDao
import com.example.chatee2e.data.local.dao.MessageDao
import com.example.chatee2e.data.mapper.toDomain
import com.example.chatee2e.data.mapper.toEntity
import com.example.chatee2e.data.remote.dto.MessageDto
import com.example.chatee2e.data.remote.dto.UserDto
import com.example.chatee2e.domain.model.Message
import com.example.chatee2e.domain.model.User
import com.example.chatee2e.domain.repository.MessageRepository
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MessageRepositoryImpl @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val messageDao: dagger.Lazy<MessageDao>,
    private val chatDao: dagger.Lazy<ChatDao>,
    private val cryptoManager: CryptoManager,
    private val sessionManager: SessionManager
) : MessageRepository {

    private val gson = Gson()
    private val repoScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)


    override fun getMessages(chatId: String): Flow<List<Message>> {
        return channelFlow {
            val myId = sessionManager.getUserId()
            if (myId == null) {
                close()
                return@channelFlow
            }

            val listenerRegistration = firestore.collection("chats")
                .document(chatId)
                .collection("messages")
                .orderBy("timestamp", Query.Direction.ASCENDING)
                .addSnapshotListener { snapshots, e ->
                    if (e != null) {
                        Log.e("MessageRepo", "Error fetching messages", e)
                        return@addSnapshotListener
                    }

                    if (snapshots != null && !snapshots.isEmpty) {
                        repoScope.launch {
                            processIncomingMessages(chatId, myId, snapshots.documents)
                        }
                    }
                }

            val dbFlow = messageDao.get().getMessagesForChat(chatId).map { entities ->
                entities.map { it.toDomain() }
            }

            dbFlow.collectLatest { send(it) }

            awaitClose {
                listenerRegistration.remove()
            }
        }
    }

    private suspend fun processIncomingMessages(
        chatId: String,
        myId: String,
        documents: List<com.google.firebase.firestore.DocumentSnapshot>
    ) {
        val messagesToInsert = mutableListOf<com.example.chatee2e.data.local.entity.MessageEntity>()

        val chatEntity = chatDao.get().getChatById(chatId)
        val participantsType = object : TypeToken<List<User>>() {}.type
        val participants: List<User> = if (chatEntity != null) {
            gson.fromJson(chatEntity.participantsInfo, participantsType)
        } else {
            emptyList()
        }

        for (doc in documents) {
            try {
                val dto = doc.toObject(MessageDto::class.java) ?: continue

                if (messageDao.get().getMessageById(doc.id) != null) continue

                val myEncryptedAesKeyStr = dto.encryptedKeys[myId]
                if (myEncryptedAesKeyStr == null) {
                    Log.e("MessageRepo", "No key found for user $myId in msg ${doc.id}")
                    continue
                }


                val encryptedAesBytes = Base64.decode(myEncryptedAesKeyStr, Base64.NO_WRAP)

                val aesKey = cryptoManager.decryptAesKeyWithRsa(encryptedAesBytes)

                val iv = Base64.decode(dto.iv, Base64.NO_WRAP)
                val encryptedContent = Base64.decode(dto.encryptedContent, Base64.NO_WRAP)
                val decryptedText = cryptoManager.decryptMsg(encryptedContent, iv, aesKey)

                val senderName = participants.find { it.id == dto.senderId }?.username ?: "Unknown"
                val isMine = (dto.senderId == myId)

                messagesToInsert.add(dto.toEntity(doc.id, senderName, decryptedText, isMine))

            } catch (e: Exception) {
                Log.e("MessageRepo", "Failed to decrypt message ${doc.id}", e)
            }
        }

        if (messagesToInsert.isNotEmpty()) {
            messageDao.get().insertMessages(messagesToInsert)

            val lastMsg = messagesToInsert.maxByOrNull { it.timestamp }
            if (lastMsg != null) {
                chatDao.get().updateLastMessage(chatId, lastMsg.text, lastMsg.timestamp)
            }
        }
    }

    override suspend fun sendMessage(chatId: String, text: String): Resource<Unit> {
        return try {
            val myId = sessionManager.getUserId() ?: return Resource.Error("Not logged in")

            val chatDoc = firestore.collection("chats").document(chatId).get().await()
            val participantIds = chatDoc.get("participantIds") as? List<String> ?: return Resource.Error("No participants")

            val aesKey = cryptoManager.generateRandomAesKey()
            val encryptedKeysMap = mutableMapOf<String, String>()

            for (userId in participantIds) {
                try {
                    var publicKeyBase64 = sessionManager.getPublicKey(userId)
                    if (publicKeyBase64 == null) {
                        val userDoc = firestore.collection("users").document(userId).get().await()
                        publicKeyBase64 = userDoc.getString("publicKey")
                        if (publicKeyBase64 != null) {
                            sessionManager.savePublicKey(userId, publicKeyBase64)
                        } else continue
                    }
                    val publicKey = cryptoManager.getPublicKeyFromBase64(publicKeyBase64)
                    val encryptedAesBytes = cryptoManager.encryptAesKeyWithRsa(aesKey, publicKey)
                    encryptedKeysMap[userId] = Base64.encodeToString(encryptedAesBytes, Base64.NO_WRAP)
                } catch (e: Exception) {
                    Log.e("MessageRepo", "Failed to encrypt for $userId", e)
                }
            }

            val cryptoResult = cryptoManager.encryptMsg(text, aesKey)
            val messageDto = MessageDto(
                chatId = chatId,
                senderId = myId,
                timestamp = null,
                encryptedContent = Base64.encodeToString(cryptoResult.encryptedData, Base64.NO_WRAP),
                iv = Base64.encodeToString(cryptoResult.iv, Base64.NO_WRAP),
                encryptedKeys = encryptedKeysMap
            )

            firestore.collection("chats").document(chatId)
                .collection("messages")
                .add(messageDto)
                .await()

            Resource.Success(Unit)
        } catch (e: Exception) {
            Resource.Error(e.localizedMessage ?: "Failed to send")
        }
    }

    override suspend fun clearHistory(chatId: String): Resource<Unit> {
        return try {
            messageDao.get().deleteMessagesByChatId(chatId)
            Resource.Success(Unit)
        } catch (e: Exception) {
            Resource.Error("Failed to clear history")
        }
    }
}