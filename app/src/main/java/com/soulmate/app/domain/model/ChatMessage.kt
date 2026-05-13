package com.soulmate.app.domain.model

import com.google.firebase.Timestamp

data class ChatMessage(
    val id: String = "",
    val senderId: String = "",
    val receiverId: String = "",
    val messageText: String = "",
    val imageUrl: String? = null,
    val timestamp: Timestamp? = null,
    val read: Boolean = false,
    val replyToId: String? = null,
    val replyToText: String? = null,
    val replyToName: String? = null,
    val replyToImageUrl: String? = null
)
