package com.soulmate.app.data.repository

import com.google.firebase.Timestamp
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.soulmate.app.domain.model.ChatMessage
import com.soulmate.app.domain.repository.IChatRepository
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ChatRepositoryImpl @Inject constructor(
    private val firestore: FirebaseFirestore
) : IChatRepository {

    private val chatCollection = firestore.collection("chats")

    override suspend fun sendMessage(message: ChatMessage): Result<Unit> = try {
        val messageData = hashMapOf(
            "senderId" to message.senderId,
            "receiverId" to message.receiverId,
            "messageText" to message.messageText,
            "imageUrl" to message.imageUrl,
            "timestamp" to (message.timestamp ?: Timestamp.now())
        )
        chatCollection.add(messageData).await()
        Result.success(Unit)
    } catch (e: Exception) {
        Result.failure(e)
    }

    override fun getMessages(senderId: String, receiverId: String): Flow<List<ChatMessage>> = callbackFlow {
        val subscription = chatCollection
            .whereIn("senderId", listOf(senderId, receiverId))
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    trySend(emptyList())
                    return@addSnapshotListener
                }
                if (snapshot != null) {
                    val messages = snapshot.documents.mapNotNull { doc ->
                        doc.toObject(ChatMessage::class.java)?.copy(id = doc.id)
                    }.filter { 
                        (it.senderId == senderId && it.receiverId == receiverId) ||
                        (it.senderId == receiverId && it.receiverId == senderId)
                    }.sortedBy { it.timestamp?.seconds ?: 0L }
                    
                    trySend(messages)
                }
            }
        awaitClose { subscription.remove() }
    }

    override fun getLastMessages(userId: String): Flow<List<ChatMessage>> = callbackFlow {
        val subscription = chatCollection
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    trySend(emptyList())
                    return@addSnapshotListener
                }
                if (snapshot != null) {
                    val allMessages = snapshot.documents.mapNotNull { doc ->
                        doc.toObject(ChatMessage::class.java)?.copy(id = doc.id)
                    }.filter { it.senderId == userId || it.receiverId == userId }

                    val lastMessages = allMessages.groupBy { 
                        if (it.senderId == userId) it.receiverId else it.senderId 
                    }.map { it.value.first() }
                    
                    trySend(lastMessages)
                }
            }
        awaitClose { subscription.remove() }
    }

    override suspend fun deleteConversation(userId: String, otherUserId: String): Result<Unit> = try {
        val messages = chatCollection
            .whereIn("senderId", listOf(userId, otherUserId))
            .get()
            .await()
            .documents
            .filter { doc ->
                val senderId = doc.getString("senderId")
                val receiverId = doc.getString("receiverId")
                (senderId == userId && receiverId == otherUserId) ||
                (senderId == otherUserId && receiverId == userId)
            }
        
        firestore.runBatch { batch ->
            messages.forEach { batch.delete(it.reference) }
        }.await()
        Result.success(Unit)
    } catch (e: Exception) {
        Result.failure(e)
    }
}
