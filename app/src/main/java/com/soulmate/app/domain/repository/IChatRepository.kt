package com.soulmate.app.domain.repository

import com.soulmate.app.domain.model.ChatMessage
import kotlinx.coroutines.flow.Flow

interface IChatRepository {
    suspend fun sendMessage(message: ChatMessage): Result<Unit>
    fun getMessages(senderId: String, receiverId: String): Flow<List<ChatMessage>>
    fun getLastMessages(userId: String): Flow<List<ChatMessage>>
    suspend fun deleteConversation(userId: String, otherUserId: String): Result<Unit>
    suspend fun markAsRead(userId: String, otherUserId: String): Result<Unit>
    suspend fun deleteMessage(messageId: String): Result<Unit>
}
