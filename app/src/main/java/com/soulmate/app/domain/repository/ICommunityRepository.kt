package com.soulmate.app.domain.repository

import com.soulmate.app.ui.social.CommunityPost
import com.soulmate.app.ui.social.Comment
import kotlinx.coroutines.flow.Flow

interface ICommunityRepository {
    fun getPosts(): Flow<List<CommunityPost>>
    fun getComments(postId: String): Flow<List<Comment>>
    suspend fun addPost(post: CommunityPost): Result<Unit>
    suspend fun toggleLike(postId: String, userId: String): Result<Unit>
    suspend fun addComment(postId: String, comment: Comment): Result<Unit>
    suspend fun toggleCommentLike(postId: String, commentId: String, userId: String): Result<Unit>
    suspend fun deletePost(postId: String): Result<Unit>
    suspend fun updatePostContent(postId: String, newContent: String): Result<Unit>
}
