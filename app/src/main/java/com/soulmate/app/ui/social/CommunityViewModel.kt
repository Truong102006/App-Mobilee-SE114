package com.soulmate.app.ui.social

import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateMapOf
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

    private val _postComments = mutableStateMapOf<String, List<Comment>>()
    val postComments: Map<String, List<Comment>> = _postComments

    private val currentUserId: String?
        get() = auth.currentUser?.uid

    init {
        observePosts()
    }

    private fun observePosts() {
        viewModelScope.launch {
            repository.getPosts().collectLatest { allPosts ->
                _posts.value = allPosts.map { post ->
                    val isLiked = currentUserId?.let { post.likedBy.contains(it) } ?: false
                    post.copy(isLiked = isLiked)
                }
            }
        }
    }

    fun loadComments(postId: String) {
        viewModelScope.launch {
            repository.getComments(postId).collectLatest { comments ->
                val userId = currentUserId
                _postComments[postId] = comments.map { comment ->
                    comment.copy(isLiked = userId?.let { comment.likedBy.contains(it) } ?: false)
                }
            }
        }
    }

    fun addPost(post: CommunityPost) {
        val userId = currentUserId ?: return
        viewModelScope.launch {
            repository.addPost(post.copy(userId = userId))
        }
    }

    fun toggleLike(postId: String) {
        val userId = currentUserId ?: return
        viewModelScope.launch {
            repository.toggleLike(postId, userId)
        }
    }

    fun addComment(
        postId: String, 
        userId: String, 
        userName: String, 
        userAvatarUrl: String?, 
        content: String,
        parentId: String? = null,
        replyToUserName: String? = null
    ) {
        val newComment = Comment(
            id = UUID.randomUUID().toString(),
            userId = userId,
            userName = userName,
            userAvatarUrl = userAvatarUrl,
            content = content,
            timeAgo = "Vừa xong",
            parentId = parentId,
            replyToUserName = replyToUserName
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
