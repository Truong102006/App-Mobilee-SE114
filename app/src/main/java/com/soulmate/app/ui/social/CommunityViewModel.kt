package com.soulmate.app.ui.social

import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import java.util.UUID
import javax.inject.Inject

@HiltViewModel
class CommunityViewModel @Inject constructor() : ViewModel() {
    private val _posts = mutableStateOf(getMockCommunityPosts())
    val posts: State<List<CommunityPost>> = _posts

    fun addPost(post: CommunityPost) {
        _posts.value = listOf(post) + _posts.value
    }

    fun toggleLike(postId: String) {
        _posts.value = _posts.value.map { post ->
            if (post.id == postId) {
                val newIsLiked = !post.isLiked
                post.copy(
                    isLiked = newIsLiked,
                    likeCount = if (newIsLiked) post.likeCount + 1 else post.likeCount - 1
                )
            } else {
                post
            }
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
        _posts.value = _posts.value.map { post ->
            if (post.id == postId) {
                post.copy(
                    comments = post.comments + newComment,
                    commentCount = post.commentCount + 1
                )
            } else {
                post
            }
        }
    }

    fun toggleCommentLike(postId: String, commentId: String) {
        _posts.value = _posts.value.map { post ->
            if (post.id == postId) {
                val updatedComments = post.comments.map { comment ->
                    if (comment.id == commentId) {
                        val newIsLiked = !comment.isLiked
                        comment.copy(
                            isLiked = newIsLiked,
                            likeCount = if (newIsLiked) comment.likeCount + 1 else comment.likeCount - 1
                        )
                    } else {
                        comment
                    }
                }
                post.copy(comments = updatedComments)
            } else {
                post
            }
        }
    }

    fun deletePost(postId: String) {
        _posts.value = _posts.value.filter { it.id != postId }
    }

    fun updatePostContent(postId: String, newContent: String) {
        _posts.value = _posts.value.map { post ->
            if (post.id == postId) {
                post.copy(textContent = newContent)
            } else {
                post
            }
        }
    }
}
