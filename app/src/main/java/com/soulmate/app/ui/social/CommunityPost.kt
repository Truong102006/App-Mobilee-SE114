package com.soulmate.app.ui.social

import com.google.firebase.Timestamp

data class Comment(
    val id: String = "",
    val userId: String = "",
    val userName: String = "",
    val userAvatarUrl: String? = null,
    val content: String = "",
    val timeAgo: String = "",
    val likeCount: Int = 0,
    val isLiked: Boolean = false,
    val likedBy: List<String> = emptyList(),
    val parentId: String? = null, // ID của bình luận gốc
    val replyToUserName: String? = null // Tên người được trả lời
)

data class CommunityPost(
    val id: String = "",
    val userId: String = "",
    val userName: String = "",
    val userAvatarUrl: String? = null,
    val isVerified: Boolean = false,
    val mood: String = "",
    val timeAgo: String = "",
    val textContent: String = "",
    val imageUrls: List<String> = emptyList(),
    val likeCount: Int = 0,
    val commentCount: Int = 0,
    val viewCount: Int = 0,
    val isLiked: Boolean = false,
    val likedBy: List<String> = emptyList(),
    val comments: List<Comment> = emptyList(),
    val timestamp: Timestamp? = null
)
