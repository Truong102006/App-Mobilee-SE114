package com.soulmate.app.domain.repository

import com.soulmate.app.domain.model.ChatMessage
import kotlinx.coroutines.flow.Flow

interface IChatRepository {
    suspend fun sendMessage(message: ChatMessage): Result<Unit>
    fun getMessages(senderId: String, receiverId: String): Flow<List<ChatMessage>>
}
