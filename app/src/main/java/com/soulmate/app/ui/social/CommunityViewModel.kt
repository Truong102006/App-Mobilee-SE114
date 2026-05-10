package com.soulmate.app.ui.social

import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.soulmate.app.domain.repository.ICommunityRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject

@HiltViewModel
class CommunityViewModel @Inject constructor(
    private val repository: ICommunityRepository,
    private val auth: FirebaseAuth
) : ViewModel() {
    private val _posts = mutableStateOf<List<CommunityPost>>(emptyList())
    val posts: State<List<CommunityPost>> = _posts

    private val currentUserId: String?
        get() = auth.currentUser?.uid

    init {
        observePosts()
    }

    private fun observePosts() {
        viewModelScope.launch {
            repository.getPosts().collectLatest { allPosts ->
                val userId = currentUserId
                _posts.value = allPosts.map { post ->
                    // In a real scenario, 'liked_by' would be checked here or handled in Repo
                    // For now, Repo doesn't return liked_by in CommunityPost, but let's assume we handle it
                    post.copy(isLiked = false) // Logic for isLiked can be added if liked_by is exposed
                }
            }
        }
    }

    fun addPost(post: CommunityPost) {
        viewModelScope.launch {
            repository.addPost(post)
        }
    }

    fun toggleLike(postId: String) {
        val userId = currentUserId ?: return
        viewModelScope.launch {
            repository.toggleLike(postId, userId)
        }
    }

    fun addComment(postId: String, userName: String, userAvatarUrl: String?, content: String) {
        val newComment = Comment(
            id = UUID.randomUUID().toString(),
            userName = userName,
            userAvatarUrl = userAvatarUrl,
            content = content,
            timeAgo = "Vừa xong"
        )
        viewModelScope.launch {
            repository.addComment(postId, newComment)
        }
    }

    fun toggleCommentLike(postId: String, commentId: String) {
        val userId = currentUserId ?: return
        viewModelScope.launch {
            repository.toggleCommentLike(postId, commentId, userId)
        }
    }

    fun deletePost(postId: String) {
        viewModelScope.launch {
            repository.deletePost(postId)
        }
    }

    fun updatePostContent(postId: String, newContent: String) {
        viewModelScope.launch {
            repository.updatePostContent(postId, newContent)
        }
    }
}
