package com.soulmate.app.data.repository

import android.util.Log
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.soulmate.app.data.remote.api.BackendApiService
import com.soulmate.app.data.remote.dto.ChatMessageItemDto
import com.soulmate.app.data.remote.dto.SendChatMessageRequestDto
import com.soulmate.app.domain.model.ChatMessage
import com.soulmate.app.domain.repository.IChatRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.tasks.await
import java.util.Date
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ChatRepositoryImpl @Inject constructor(
    private val backendApiService: BackendApiService,
    private val auth: FirebaseAuth
) : IChatRepository {

    companion object {
        private const val TAG = "ChatRepositoryImpl"
    }

    private suspend fun requireIdToken(): String {
        val currentUser = auth.currentUser ?: throw IllegalStateException("User not logged in")
        return currentUser.getIdToken(false).await().token
            ?: throw IllegalStateException("Cannot get Firebase ID token")
    }

    private fun mapMessage(item: ChatMessageItemDto): ChatMessage {
        val timestamp = item.timestamp?.let { Timestamp(Date(it)) }
        return ChatMessage(
            id = item.id,
            senderId = item.senderId,
            receiverId = item.receiverId,
            messageText = item.messageText,
            imageUrl = item.imageUrl,
            timestamp = timestamp
        )
    }

    override suspend fun sendMessage(message: ChatMessage): Result<Unit> = runCatching {
        require(message.receiverId.isNotBlank()) { "receiverId is required" }
        require(message.senderId.isNotBlank()) { "senderId is required" }
        require(message.messageText.isNotBlank() || !message.imageUrl.isNullOrBlank()) {
            "messageText or imageUrl is required"
        }

        val idToken = requireIdToken()
        backendApiService.sendChatMessage(
            authorization = "Bearer $idToken",
            request = SendChatMessageRequestDto(
                receiverId = message.receiverId,
                messageText = message.messageText.ifBlank { null },
                imageUrl = message.imageUrl
            )
        )
    }

    override fun getMessages(senderId: String, receiverId: String): Flow<List<ChatMessage>> = flow {
        val idToken = requireIdToken()
        val currentUid = auth.currentUser?.uid.orEmpty()
        val otherUserId = if (currentUid == senderId) receiverId else senderId

        val messages = backendApiService
            .listConversation(
                authorization = "Bearer $idToken",
                otherUserId = otherUserId,
                limit = 200
            )
            .messages
            .map(::mapMessage)
            .sortedBy { it.timestamp?.seconds ?: 0L }

        emit(messages)
    }

    override fun getLastMessages(userId: String): Flow<List<ChatMessage>> = flow {
        val idToken = requireIdToken()
        val messages = backendApiService
            .listInbox(
                authorization = "Bearer $idToken",
                limit = 100
            )
            .messages
            .map(::mapMessage)
            .sortedByDescending { it.timestamp?.seconds ?: 0L }

        emit(messages)
    }

    override suspend fun deleteConversation(userId: String, otherUserId: String): Result<Unit> = runCatching {
        val idToken = requireIdToken()
        backendApiService.deleteConversation(
            authorization = "Bearer $idToken",
            otherUserId = otherUserId
        )
    }

    override suspend fun markAsRead(userId: String, otherUserId: String): Result<Unit> = runCatching {
        // TODO: add backend endpoint to mark conversation messages as read.
        Log.d(TAG, "markAsRead is not implemented on backend yet: userId=$userId otherUserId=$otherUserId")
    }

    override suspend fun deleteMessage(messageId: String): Result<Unit> = runCatching {
        // TODO: add backend endpoint to delete a single message by id.
        Log.d(TAG, "deleteMessage is not implemented on backend yet: messageId=$messageId")
    }
}
