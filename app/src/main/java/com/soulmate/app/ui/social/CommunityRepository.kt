package com.soulmate.app.ui.social

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CommunityRepository @Inject constructor() {
    private val _posts = MutableStateFlow<List<CommunityPost>>(getInitialMockPosts())
    val posts: StateFlow<List<CommunityPost>> = _posts.asStateFlow()

    fun addPost(newPost: CommunityPost) {
        val currentList = _posts.value
        _posts.value = listOf(newPost) + currentList
    }

    private fun getInitialMockPosts(): List<CommunityPost> {
        return listOf(
            CommunityPost(
                id = "post_1",
                userName = "Marvin McKinney",
                userAvatarUrl = null,
                mood = "Happy",
                timeAgo = "Today at 6:41",
                textContent = "<h3>10 Tips for Beginners in Stock Market Investing</h3><p>Start your journey today with these simple steps. Don't let the market scare you! 📈💰</p>",
                imageUrls = listOf(
                    "https://dummyimage.com/600x400/4caf50/ffffff.png&text=Investing+101",
                    "https://dummyimage.com/600x400/2196f3/ffffff.png&text=Stock+Market"
                ),
                likeCount = 2321,
                commentCount = 5321,
                viewCount = 8900
            ),
            CommunityPost(
                id = "post_2",
                userName = "Nguyễn Khánh",
                userAvatarUrl = null,
                mood = "Peaceful",
                timeAgo = "Yesterday at 14:30",
                textContent = "<h3>Hoàn thành xong Demo</h3><p>Hôm nay thời tiết thật đẹp, mình đã hoàn thành xong đồ án môn học. Một ngày thật năng suất và ý nghĩa! Cảm giác code chạy mượt mà không lỗi (crash) thật là <b>tuyệt vời</b> ✨</p>",
                imageUrls = emptyList(),
                likeCount = 128,
                commentCount = 12,
                viewCount = 450
            ),
            CommunityPost(
                id = "post_3",
                userName = "Sarah Jenkins",
                userAvatarUrl = null,
                mood = "Sad",
                timeAgo = "2 days ago",
                textContent = "<p>Sometimes things don't go as planned. Taking a step back to breathe and reflect today. Tomorrow is a new start. 🌧️</p>",
                imageUrls = listOf(
                    "https://dummyimage.com/600x400/9e9e9e/ffffff.png&text=Rainy+Day"
                ),
                likeCount = 890,
                commentCount = 145,
                viewCount = 3200
            )
        )
    }
}