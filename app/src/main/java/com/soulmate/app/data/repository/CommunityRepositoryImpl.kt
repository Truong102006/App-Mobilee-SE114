package com.soulmate.app.data.repository

import android.net.Uri
import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.soulmate.app.data.remote.api.BackendApiService
import com.soulmate.app.data.remote.dto.CreateCommentRequestDto
import com.soulmate.app.data.remote.dto.CreatePostRequestDto
import com.soulmate.app.domain.repository.ICommunityRepository
import com.soulmate.app.ui.social.Comment
import com.soulmate.app.ui.social.CommunityPost
import com.soulmate.app.utils.CloudinaryHelper
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.tasks.await
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CommunityRepositoryImpl @Inject constructor(
    private val backendApiService: BackendApiService,
    private val auth: FirebaseAuth
) : ICommunityRepository {

    companion object {
        private const val TAG = "CommunityRepositoryImpl"
        private const val POLL_INTERVAL_MS = 3000L
    }

    private fun formatTimeAgo(timestampMs: Long): String {
        val now = System.currentTimeMillis()
        val diff = now - timestampMs

        return when {
            diff < 60000 -> "Vừa xong"
            diff < 3600000 -> "${diff / 60000} phút trước"
            diff < 86400000 -> "${diff / 3600000} giờ trước"
            else -> {
                val sdf = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
                sdf.format(Date(timestampMs))
            }
        }
    }

    private fun isRemoteHttpUrl(value: String): Boolean {
        return value.startsWith("http://", ignoreCase = true) ||
            value.startsWith("https://", ignoreCase = true)
    }

    private suspend fun uploadToCloudinary(imagePath: String): String {
        return if (isRemoteHttpUrl(imagePath)) {
            imagePath
        } else {
            try {
                CloudinaryHelper.uploadImageSuspend(Uri.parse(imagePath))
            } catch (e: Exception) {
                Log.e(TAG, "Cloudinary upload failed for $imagePath", e)
                imagePath
            }
        }
    }

    private suspend fun requireIdToken(): String {
        val currentUser = auth.currentUser ?: throw IllegalStateException("User not logged in")
        return currentUser.getIdToken(true).await().token
            ?: throw IllegalStateException("Cannot get Firebase ID token")
    }

    override fun getPosts(): Flow<List<CommunityPost>> = flow {
        while (currentCoroutineContext().isActive) {
            val posts = try {
                val idToken = requireIdToken()
                backendApiService.listCommunityPosts("Bearer $idToken").map { doc ->
                    CommunityPost(
                        id = doc.id,
                        userId = doc.userId,
                        userName = doc.userName,
                        userAvatarUrl = doc.userAvatarUrl,
                        isVerified = doc.isVerified ?: false,
                        mood = doc.mood,
                        timeAgo = formatTimeAgo(doc.timestamp),
                        textContent = doc.textContent,
                        imageUrls = doc.imageUrls,
                        likeCount = doc.likeCount,
                        commentCount = doc.commentCount,
                        viewCount = doc.viewCount,
                        likedBy = doc.likedBy
                    )
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to load posts", e)
                emptyList()
            }
            emit(posts)
            delay(POLL_INTERVAL_MS)
        }
    }.distinctUntilChanged()

    override fun getComments(postId: String): Flow<List<Comment>> = flow {
        while (currentCoroutineContext().isActive) {
            val comments = try {
                val idToken = requireIdToken()
                backendApiService.listCommunityComments("Bearer $idToken", postId).map { doc ->
                    Comment(
                        id = doc.id,
                        userId = doc.userId,
                        userName = doc.userName,
                        userAvatarUrl = doc.userAvatarUrl,
                        content = doc.content,
                        timeAgo = formatTimeAgo(doc.timestamp),
                        likeCount = doc.likedBy.size,
                        likedBy = doc.likedBy,
                        parentId = doc.parentId,
                        replyToUserName = doc.replyToUserName
                    )
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to load comments for $postId", e)
                emptyList()
            }
            emit(comments)
            delay(POLL_INTERVAL_MS)
        }
    }.distinctUntilChanged()

    override suspend fun addPost(post: CommunityPost): Result<Unit> = runCatching {
        val idToken = requireIdToken()
        val uploadedUrls = post.imageUrls.map { path ->
            uploadToCloudinary(path)
        }
        backendApiService.createCommunityPost(
            authorization = "Bearer $idToken",
            request = CreatePostRequestDto(
                mood = post.mood,
                textContent = post.textContent,
                imageUrls = uploadedUrls
            )
        )
    }

    override suspend fun toggleLike(postId: String, userId: String): Result<Unit> = runCatching {
        val idToken = requireIdToken()
        backendApiService.toggleCommunityPostLike("Bearer $idToken", postId)
    }

    override suspend fun addComment(postId: String, comment: Comment): Result<Unit> = runCatching {
        val idToken = requireIdToken()
        backendApiService.addCommunityComment(
            authorization = "Bearer $idToken",
            id = postId,
            request = CreateCommentRequestDto(
                content = comment.content,
                parentId = comment.parentId,
                replyToUserName = comment.replyToUserName
            )
        )
    }

    override suspend fun toggleCommentLike(postId: String, commentId: String, userId: String): Result<Unit> = runCatching {
        val idToken = requireIdToken()
        backendApiService.toggleCommunityCommentLike("Bearer $idToken", postId, commentId)
    }

    override suspend fun deletePost(postId: String): Result<Unit> = runCatching {
        val idToken = requireIdToken()
        backendApiService.deleteCommunityPost("Bearer $idToken", postId)
    }

    override suspend fun updatePostContent(postId: String, newContent: String): Result<Unit> = runCatching {
        val idToken = requireIdToken()
        backendApiService.updateCommunityPost(
            authorization = "Bearer $idToken",
            id = postId,
            request = CreatePostRequestDto(
                textContent = newContent
            )
        )
    }
}
