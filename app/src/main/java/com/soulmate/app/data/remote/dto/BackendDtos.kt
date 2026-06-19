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

data class CreatePostRequestDto(
    val mood: String? = null,
    val textContent: String,
    val imageUrls: List<String> = emptyList()
)

data class CreatePostResponseDto(
    val id: String? = null,
    val userId: String? = null,
    val userName: String? = null,
    val userAvatarUrl: String? = null,
    val isVerified: Boolean? = false,
    val mood: String? = "Neutral",
    val textContent: String? = "",
    val imageUrls: List<String>? = emptyList(),
    val likeCount: Int? = 0,
    val commentCount: Int? = 0,
    val viewCount: Int? = 0,
    val likedBy: List<String>? = emptyList(),
    val timestamp: Long? = 0L
)

data class CreateCommentRequestDto(
    val content: String,
    val parentId: String? = null,
    val replyToUserName: String? = null
)

data class CreateCommentResponseDto(
    val id: String = "",
    val userId: String = "",
    val userName: String = "",
    val userAvatarUrl: String? = null,
    val content: String = "",
    val timestamp: Long = 0L,
    val likedBy: List<String> = emptyList(),
    val parentId: String? = null,
    val replyToUserName: String? = null
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

data class PremiumOfferResponseDto(
    val planCode: String = "",
    val priceVnd: Long = 0L,
    val durationDays: Int = 0,
    val orderExpireMinutes: Int = 15,
    val bankCode: String = "",
    val bankAccount: String = "",
    val accountHolder: String = "",
    val currentPremiumUntil: Long? = null
)

data class CreatePremiumOrderResponseDto(
    val orderId: String = "",
    val status: String = "",
    val planCode: String = "",
    val amountVnd: Long = 0L,
    val durationDays: Int = 0,
    val paymentCode: String = "",
    val expiresAt: Long = 0L,
    val qrImageUrl: String = "",
    val bankCode: String = "",
    val bankAccount: String = "",
    val accountHolder: String = ""
)

data class PaymentOrderStatusResponseDto(
    val orderId: String = "",
    val status: String = "",
    val planCode: String = "",
    val amountVnd: Long = 0L,
    val durationDays: Int = 0,
    val paymentCode: String = "",
    val createdAt: Long = 0L,
    val expiresAt: Long = 0L,
    val paidAt: Long? = null,
    val premiumGrantedUntil: Long? = null,
    val latePayment: Boolean = false
)

data class ReconcilePaymentOrderResponseDto(
    val matched: Boolean = false,
    val message: String = "",
    val order: PaymentOrderStatusResponseDto = PaymentOrderStatusResponseDto()
)
