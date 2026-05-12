package com.soulmate.app.data.repository

import com.google.firebase.Timestamp
import com.google.firebase.firestore.FieldValue
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
            "timestamp" to FieldValue.serverTimestamp(),
            "read" to false
        )
        chatCollection.add(messageData).await()
        Result.success(Unit)
    } catch (e: Exception) {
        Result.failure(e)
    }

    override fun getMessages(senderId: String, receiverId: String): Flow<List<ChatMessage>> = callbackFlow {
        val subscription = chatCollection
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
                    }.sortedWith { m1, m2 ->
                        val t1 = m1.timestamp
                        val t2 = m2.timestamp
                        when {
                            t1 == null && t2 == null -> 0
                            t1 == null -> 1
                            t2 == null -> -1
                            else -> t1.compareTo(t2)
                        }
                    }
                    
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
            .get()
            .await()
            .documents
            .filter { doc ->
                val sId = doc.getString("senderId")
                val rId = doc.getString("receiverId")
                (sId == userId && rId == otherUserId) || (sId == otherUserId && rId == userId)
            }
        
        if (messages.isNotEmpty()) {
            firestore.runBatch { batch ->
                messages.forEach { batch.delete(it.reference) }
            }.await()
        }
        Result.success(Unit)
    } catch (e: Exception) {
        Result.failure(e)
    }

    override suspend fun markAsRead(userId: String, otherUserId: String): Result<Unit> = try {
        val unreadMessages = chatCollection
            .whereEqualTo("senderId", otherUserId)
            .whereEqualTo("receiverId", userId)
            .whereEqualTo("read", false)
            .get()
            .await()
        
        if (!unreadMessages.isEmpty) {
            firestore.runBatch { batch ->
                unreadMessages.documents.forEach { doc ->
                    batch.update(doc.reference, "read", true)
                }
            }.await()
        }
        Result.success(Unit)
    } catch (e: Exception) {
        Result.failure(e)
    }
}
