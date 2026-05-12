package com.soulmate.app.data.repository

import android.util.Log
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.Filter
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
            "read" to false,
            "replyToId" to message.replyToId,
            "replyToText" to message.replyToText,
            "replyToName" to message.replyToName
        )
        chatCollection.add(messageData).await()
        Result.success(Unit)
    } catch (e: Exception) {
        Result.failure(e)
    }

    override fun getMessages(senderId: String, receiverId: String): Flow<List<ChatMessage>> = callbackFlow {
        val query = chatCollection.where(
            Filter.or(
                Filter.and(
                    Filter.equalTo("senderId", senderId),
                    Filter.equalTo("receiverId", receiverId)
                ),
                Filter.and(
                    Filter.equalTo("senderId", receiverId),
                    Filter.equalTo("receiverId", senderId)
                )
            )
        )

        val subscription = query.addSnapshotListener { snapshot, error ->
            if (error != null) {
                Log.e("ChatRepo", "Snapshot error: ${error.message}")
                trySend(emptyList())
                return@addSnapshotListener
            }
            if (snapshot != null) {
                val messages = snapshot.documents.mapNotNull { doc ->
                    doc.toObject(ChatMessage::class.java)?.copy(id = doc.id)
                }.sortedWith { m1, m2 ->
                    val t1 = m1.timestamp
                    val t2 = m2.timestamp
                    when {
                        t1 == null && t2 == null -> 0
                        t1 == null -> 1
                        t2 == null -> -1
                        else -> {
                            if (t1.seconds != t2.seconds) {
                                t1.seconds.compareTo(t2.seconds)
                            } else {
                                t1.nanoseconds.compareTo(t2.nanoseconds)
                            }
                        }
                    }
                }
                
                trySend(messages)
            }
        }
        awaitClose { subscription.remove() }
    }

    override fun getLastMessages(userId: String): Flow<List<ChatMessage>> = callbackFlow {
        val query = chatCollection.where(
            Filter.or(
                Filter.equalTo("senderId", userId),
                Filter.equalTo("receiverId", userId)
            )
        )

        val subscription = query.addSnapshotListener { snapshot, error ->
            if (error != null) {
                trySend(emptyList())
                return@addSnapshotListener
            }
            if (snapshot != null) {
                val allMessages = snapshot.documents.mapNotNull { doc ->
                    doc.toObject(ChatMessage::class.java)?.copy(id = doc.id)
                }

                val lastMessages = allMessages.groupBy { 
                    if (it.senderId == userId) it.receiverId else it.senderId 
                }.map { entry ->
                    entry.value.sortedWith { m1, m2 ->
                        val t1 = m1.timestamp
                        val t2 = m2.timestamp
                        when {
                            t1 == null && t2 == null -> 0
                            t1 == null -> -1
                            t2 == null -> 1
                            else -> {
                                if (t2.seconds != t1.seconds) {
                                    t2.seconds.compareTo(t1.seconds)
                                } else {
                                    t2.nanoseconds.compareTo(t1.nanoseconds)
                                }
                            }
                        }
                    }.first()
                }.sortedWith { m1, m2 ->
                    val t1 = m1.timestamp
                    val t2 = m2.timestamp
                    when {
                        t1 == null && t2 == null -> 0
                        t1 == null -> -1
                        t2 == null -> 1
                        else -> {
                            if (t2.seconds != t1.seconds) {
                                t2.seconds.compareTo(t1.seconds)
                            } else {
                                t2.nanoseconds.compareTo(t1.nanoseconds)
                            }
                        }
                    }
                }
                
                trySend(lastMessages)
            }
        }
        awaitClose { subscription.remove() }
    }

    override suspend fun deleteConversation(userId: String, otherUserId: String): Result<Unit> = try {
        val messages = chatCollection.where(
            Filter.or(
                Filter.and(Filter.equalTo("senderId", userId), Filter.equalTo("receiverId", otherUserId)),
                Filter.and(Filter.equalTo("senderId", otherUserId), Filter.equalTo("receiverId", userId))
            )
        ).get().await()
        
        if (!messages.isEmpty) {
            firestore.runBatch { batch ->
                messages.documents.forEach { batch.delete(it.reference) }
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

    override suspend fun deleteMessage(messageId: String): Result<Unit> = try {
        chatCollection.document(messageId).delete().await()
        Result.success(Unit)
    } catch (e: Exception) {
        Result.failure(e)
    }
}
