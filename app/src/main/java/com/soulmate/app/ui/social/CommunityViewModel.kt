package com.soulmate.app.ui.social

import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.snapshots.SnapshotStateMap
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.soulmate.app.domain.repository.ICommunityRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
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

    // Lưu trữ bình luận theo postId: Map<PostId, List<Comment>>
    private val _commentsMap = mutableStateMapOf<String, List<Comment>>()
    val commentsMap: Map<String, List<Comment>> = _commentsMap

    private val commentJobs = mutableMapOf<String, Job>()

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
                    post.copy(isLiked = post.likedBy.contains(userId))
                }
            }
        }
    }

    // Quan sát bình luận của một bài viết cụ thể
    fun observeCommentsForPost(postId: String) {
        if (commentJobs.containsKey(postId)) return

        val job = viewModelScope.launch {
            repository.getComments(postId).collectLatest { comments ->
                val userId = currentUserId
                _commentsMap[postId] = comments.map { comment ->
                    comment.copy(isLiked = comment.likedBy.contains(userId))
                }
            }
        }
        commentJobs[postId] = job
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

    override fun onCleared() {
        super.onCleared()
        commentJobs.values.forEach { it.cancel() }
    }
}
