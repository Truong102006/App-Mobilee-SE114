package com.soulmate.app.data.repository

import android.net.Uri
import android.util.Log
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.soulmate.app.domain.repository.ICommunityRepository
import com.soulmate.app.ui.social.Comment
import com.soulmate.app.ui.social.CommunityPost
import com.soulmate.app.utils.CloudinaryHelper
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton
import java.text.SimpleDateFormat
import java.util.*

@Singleton
class CommunityRepositoryImpl @Inject constructor(
    private val firestore: FirebaseFirestore
) : ICommunityRepository {

    private val postsCollection = firestore.collection("community_posts")

    private fun formatTimeAgo(timestamp: Timestamp?): String {
        if (timestamp == null) return "Vừa xong"
        val now = System.currentTimeMillis()
        val diff = now - timestamp.toDate().time
        
        return when {
            diff < 60000 -> "Vừa xong"
            diff < 3600000 -> "${diff / 60000} phút trước"
            diff < 86400000 -> "${diff / 3600000} giờ trước"
            else -> {
                val sdf = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
                sdf.format(timestamp.toDate())
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
                Log.e("CommunityRepo", "Cloudinary upload failed for $imagePath", e)
                imagePath
            }
        }
    }

    override fun getPosts(): Flow<List<CommunityPost>> = callbackFlow {
        val subscription = postsCollection
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    trySend(emptyList())
                    return@addSnapshotListener
                }
                if (snapshot != null) {
                    val posts = snapshot.documents.mapNotNull { doc ->
                        try {
                            val likedBy = doc.get("liked_by") as? List<String> ?: emptyList()
                            val timestamp = try { doc.getTimestamp("timestamp") } catch (e: Exception) { null }
                            
                            CommunityPost(
                                id = doc.id,
                                userId = doc.getString("user_id") ?: "",
                                userName = doc.getString("user_name") ?: "Người dùng",
                                userAvatarUrl = doc.getString("user_avatar_url"),
                                isVerified = doc.getBoolean("is_verified") ?: false,
                                mood = doc.getString("mood") ?: "Neutral",
                                timeAgo = formatTimeAgo(timestamp),
                                textContent = doc.getString("text_content") ?: "",
                                imageUrls = doc.get("image_urls") as? List<String> ?: emptyList(),
                                likeCount = doc.getLong("like_count")?.toInt() ?: 0,
                                commentCount = doc.getLong("comment_count")?.toInt() ?: 0,
                                viewCount = doc.getLong("view_count")?.toInt() ?: 0,
                                likedBy = likedBy,
                                timestamp = timestamp
                            )
                        } catch (e: Exception) {
                            Log.e("CommunityRepo", "Error parsing post ${doc.id}", e)
                            null
                        }
                    }.sortedByDescending { it.timestamp?.seconds ?: 0L }
                    
                    trySend(posts)
                }
            }
        awaitClose { subscription.remove() }
    }

    override fun getComments(postId: String): Flow<List<Comment>> = callbackFlow {
        val subscription = postsCollection.document(postId).collection("comments")
            .orderBy("timestamp", Query.Direction.ASCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }
                if (snapshot != null) {
                    val comments = snapshot.documents.mapNotNull { doc ->
                        try {
                            val likedBy = doc.get("liked_by") as? List<String> ?: emptyList()
                            Comment(
                                id = doc.id,
                                userId = doc.getString("user_id") ?: "",
                                userName = doc.getString("user_name") ?: "Người dùng",
                                userAvatarUrl = doc.getString("user_avatar_url"),
                                content = doc.getString("content") ?: "",
                                timeAgo = formatTimeAgo(doc.getTimestamp("timestamp")),
                                likeCount = likedBy.size,
                                likedBy = likedBy,
                                parentId = doc.getString("parent_id"), // Lấy parent_id
                                replyToUserName = doc.getString("reply_to_user_name") // Lấy tên người được trả lời
                            )
                        } catch (e: Exception) {
                            null
                        }
                    }
                    trySend(comments)
                }
            }
        awaitClose { subscription.remove() }
    }

    override suspend fun addPost(post: CommunityPost): Result<Unit> = try {
        val uploadedUrls = post.imageUrls.map { path ->
            uploadToCloudinary(path)
        }

        val postData = hashMapOf(
            "user_id" to post.userId,
            "user_name" to post.userName,
            "user_avatar_url" to post.userAvatarUrl,
            "is_verified" to post.isVerified,
            "mood" to post.mood,
            "timestamp" to FieldValue.serverTimestamp(),
            "text_content" to post.textContent,
            "image_urls" to uploadedUrls,
            "like_count" to 0,
            "comment_count" to 0,
            "view_count" to 0,
            "liked_by" to emptyList<String>()
        )
        postsCollection.add(postData).await()
        Result.success(Unit)
    } catch (e: Exception) {
        Result.failure(e)
    }

    override suspend fun toggleLike(postId: String, userId: String): Result<Unit> = try {
        val docRef = postsCollection.document(postId)
        firestore.runTransaction { transaction ->
            val snapshot = transaction.get(docRef)
            val likedBy = snapshot.get("liked_by") as? List<String> ?: emptyList()
            val newLikedBy = if (likedBy.contains(userId)) {
                likedBy - userId
            } else {
                likedBy + userId
            }
            transaction.update(docRef, "liked_by", newLikedBy)
            transaction.update(docRef, "like_count", newLikedBy.size)
        }.await()
        Result.success(Unit)
    } catch (e: Exception) {
        Result.failure(e)
    }

    override suspend fun addComment(postId: String, comment: Comment): Result<Unit> = try {
        val commentData = hashMapOf(
            "user_id" to comment.userId,
            "user_name" to comment.userName,
            "user_avatar_url" to comment.userAvatarUrl,
            "content" to comment.content,
            "timestamp" to FieldValue.serverTimestamp(),
            "liked_by" to emptyList<String>(),
            "parent_id" to comment.parentId, // Lưu parent_id
            "reply_to_user_name" to comment.replyToUserName // Lưu tên người được trả lời
        )
        val postRef = postsCollection.document(postId)
        firestore.runBatch { batch ->
            val commentRef = postRef.collection("comments").document()
            batch.set(commentRef, commentData)
            batch.update(postRef, "comment_count", FieldValue.increment(1))
        }.await()
        Result.success(Unit)
    } catch (e: Exception) {
        Result.failure(e)
    }

    override suspend fun toggleCommentLike(postId: String, commentId: String, userId: String): Result<Unit> = try {
        val commentRef = postsCollection.document(postId).collection("comments").document(commentId)
        firestore.runTransaction { transaction ->
            val snapshot = transaction.get(commentRef)
            val likedBy = snapshot.get("liked_by") as? List<String> ?: emptyList()
            val newLikedBy = if (likedBy.contains(userId)) {
                likedBy - userId
            } else {
                likedBy + userId
            }
            transaction.update(commentRef, "liked_by", newLikedBy)
        }.await()
        Result.success(Unit)
    } catch (e: Exception) {
        Result.failure(e)
    }

    override suspend fun deletePost(postId: String): Result<Unit> = try {
        postsCollection.document(postId).delete().await()
        Result.success(Unit)
    } catch (e: Exception) {
        Result.failure(e)
    }

    override suspend fun updatePostContent(postId: String, newContent: String): Result<Unit> = try {
        postsCollection.document(postId).update("text_content", newContent).await()
        Result.success(Unit)
    } catch (e: Exception) {
        Result.failure(e)
    }
}
