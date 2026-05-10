package com.soulmate.app.ui.social

data class Comment(
    val id: String,
    val userName: String,
    val userAvatarUrl: String?,
    val content: String,
    val timeAgo: String,
    val likeCount: Int = 0,
    val isLiked: Boolean = false
)

data class CommunityPost(
    val id: String,
    val userName: String,
    val userAvatarUrl: String?,
    val isVerified: Boolean,
    val mood: String,
    val timeAgo: String,
    val textContent: String,
    val imageUrls: List<String>,
    val likeCount: Int,
    val commentCount: Int,
    val viewCount: Int,
    val isLiked: Boolean = false,
    val comments: List<Comment> = emptyList()
)
