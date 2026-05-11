package com.soulmate.app.data.repository

import com.google.firebase.Timestamp
import com.google.firebase.firestore.FirebaseFirestore
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
                    }
                    trySend(messages)
                }
            }
        awaitClose { subscription.remove() }
    }
}
