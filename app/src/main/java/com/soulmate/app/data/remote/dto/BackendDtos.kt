package com.soulmate.app.data.remote.dto

import com.soulmate.app.ui.social.CommunityPost

data class SaveDiaryRequestDto(
    val diaryId: String? = null,
    val title: String? = null,
    val text: String,
    val moodTag: String? = null,
    val imageUrls: List<String> = emptyList(),
    val audioUrl: String? = null
)

data class SaveDiaryResponseDto(
    val diaryId: String = "",
    val updatedAt: Long = 0L
)

data class DiaryItemDto(
    val diaryId: String = "",
    val userId: String = "",
    val title: String = "",
    val text: String = "",
    val moodTag: String = "Neutral",
    val imageUrls: List<String> = emptyList(),
    val audioUrl: String? = null,
    val createdAt: Long? = null,
    val updatedAt: Long? = null
)

data class ListDiariesResponseDto(
    val diaries: List<DiaryItemDto> = emptyList()
)

data class DeleteDiaryResponseDto(
    val deleted: Boolean = false,
    val diaryId: String = ""
)

data class SendChatMessageRequestDto(
    val receiverId: String,
    val messageText: String? = null,
    val imageUrl: String? = null
)

data class SendChatMessageResponseDto(
    val messageId: String = "",
    val conversationId: String = ""
)

data class ChatMessageItemDto(
    val id: String = "",
    val senderId: String = "",
    val receiverId: String = "",
    val messageText: String = "",
    val imageUrl: String? = null,
    val timestamp: Long? = null
)

data class ListConversationResponseDto(
    val conversationId: String = "",
    val messages: List<ChatMessageItemDto> = emptyList()
)

data class ListInboxResponseDto(
    val messages: List<ChatMessageItemDto> = emptyList()
)

data class DeleteConversationResponseDto(
    val conversationId: String = "",
    val deletedCount: Int = 0
)

data class SignUploadRequestDto(
    val publicId: String? = null,
    val context: String? = null
)

data class SignUploadResponseDto(
    val cloudName: String = "",
    val apiKey: String = "",
    val folder: String = "",
    val timestamp: Long = 0L,
    val signature: String = "",
    val publicId: String? = null,
    val context: String? = null,
    val uploadUrl: String = ""
)

data class CommonResponseDto(
    val success: Boolean = true,
    val message: String = ""
)

data class ListReportedPostsResponseDto(
    val posts: List<CommunityPost> = emptyList()
)

data class ResolveReportRequestDto(
    val postId: String,
    val action: String // "delete" or "ignore"
)