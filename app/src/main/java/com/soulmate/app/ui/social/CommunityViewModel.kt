package com.soulmate.app.ui.social

import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import javax.inject.Singleton

@HiltViewModel
class CommunityViewModel @Inject constructor() : ViewModel() {
    // Sử dụng biến static hoặc một cách thức nào đó để giữ dữ liệu nếu không dùng Singleton Repository
    // Ở đây tôi sẽ giữ state bình thường, nhưng chúng ta sẽ cung cấp cùng 1 instance từ MainActivity
    private val _posts = mutableStateOf(getMockCommunityPosts())
    val posts: State<List<CommunityPost>> = _posts

    fun addPost(post: CommunityPost) {
        _posts.value = listOf(post) + _posts.value
    }
}
