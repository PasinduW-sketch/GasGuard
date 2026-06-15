package com.example.gasml.data

import android.util.Log
import com.example.gasml.model.ChatMessage
import com.example.gasml.model.Conversation
import com.example.gasml.model.User
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

class ChatRepository {
    private val firestore = FirebaseFirestore.getInstance()
    private val chatCollection = firestore.collection("chats")
    private val conversationCollection = firestore.collection("conversations")
    private val dealerCollection = firestore.collection("dealers")

    suspend fun sendMessage(message: ChatMessage, senderName: String, receiverName: String): Result<Unit> {
        return try {
            val docRef = chatCollection.document()
            val finalMessage = message.copy(id = docRef.id)
            
            docRef.set(finalMessage).await()
            
            val conversationData = mapOf(
                "chatId" to message.chatId,
                "participantIds" to listOf(message.senderId, message.receiverId),
                "participantNames" to mapOf(
                    message.senderId to senderName,
                    message.receiverId to receiverName
                ),
                "lastMessage" to message.text,
                "lastTimestamp" to message.timestamp,
                "lastSenderId" to message.senderId
            )
            
            conversationCollection.document(message.chatId)
                .set(conversationData, SetOptions.merge())
                .await()
            
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e("ChatRepository", "Failed to send message", e)
            Result.failure(e)
        }
    }

    fun getMessages(chatId: String): Flow<List<ChatMessage>> = callbackFlow {
        val subscription = chatCollection
            .whereEqualTo("chatId", chatId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e("ChatRepository", "Messages listener failed", error)
                    close()
                    return@addSnapshotListener
                }
                val messages = snapshot?.documents?.mapNotNull { doc ->
                    try {
                        doc.toObject(ChatMessage::class.java)?.copy(id = doc.id)
                    } catch (e: Exception) {
                        Log.e("ChatRepository", "Failed to parse message ${doc.id}", e)
                        null
                    }
                } ?: emptyList()
                trySend(messages.sortedBy { it.timestamp })
            }
        awaitClose { subscription.remove() }
    }

    fun getConversations(userId: String): Flow<List<Conversation>> = callbackFlow {
        val subscription = conversationCollection
            .whereArrayContains("participantIds", userId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e("ChatRepository", "Conversations listener failed", error)
                    close()
                    return@addSnapshotListener
                }
                val convos = snapshot?.documents?.mapNotNull { doc ->
                    try {
                        doc.toObject(Conversation::class.java)
                    } catch (e: Exception) {
                        Log.e("ChatRepository", "Failed to parse conversation ${doc.id}", e)
                        null
                    }
                } ?: emptyList()
                trySend(convos.sortedByDescending { it.lastTimestamp })
            }
        awaitClose { subscription.remove() }
    }

    suspend fun getDealers(): List<User> {
        return try {
            val snapshot = dealerCollection.get().await()
            snapshot.documents.mapNotNull { it.toObject(User::class.java) }
        } catch (e: Exception) {
            Log.e("ChatRepository", "Failed to fetch dealers", e)
            emptyList()
        }
    }

    fun generateChatId(uid1: String, uid2: String): String {
        return if (uid1 < uid2) "${uid1}_$uid2" else "${uid2}_$uid1"
    }
}
