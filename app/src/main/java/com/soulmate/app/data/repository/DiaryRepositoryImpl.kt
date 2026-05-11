package com.soulmate.app.data.repository

import android.net.Uri
import android.util.Log
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.soulmate.app.domain.model.Diary
import com.soulmate.app.domain.repository.IDiaryRepository
import com.soulmate.app.utils.CloudinaryHelper
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DiaryRepositoryImpl @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val auth: FirebaseAuth
) : IDiaryRepository {
    companion object {
        private const val TAG = "DiaryRepositoryImpl"
    }

    private val diariesCollection = firestore.collection("diaries")

    private val fieldUserId = "user_id"
    private val fieldTimestamp = "timestamp"

    private fun isRemoteHttpUrl(value: String): Boolean {
        return value.startsWith("http://", ignoreCase = true) ||
            value.startsWith("https://", ignoreCase = true)
    }

    override fun getDiaries(userId: String): Flow<List<Diary>> = callbackFlow {
        val subscription = diariesCollection
            .whereEqualTo(fieldUserId, userId)
            .orderBy(fieldTimestamp, Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }
                if (snapshot != null) {
                    val diaries = snapshot.documents.mapNotNull { doc ->
                        try {
                            val diary = doc.toObject(Diary::class.java)
                            val firebaseTimestamp = doc.get(fieldTimestamp) as? Timestamp
                            diary?.copy(
                                diaryId = doc.id,
                                createdAt = firebaseTimestamp?.toDate()?.time ?: System.currentTimeMillis()
                            )
                        } catch (_: Exception) {
                            null
                        }
                    }
                    trySend(diaries)
                }
            }
        awaitClose { subscription.remove() }
    }

    private suspend fun uploadToCloudinary(imagePath: String): String {
        return if (isRemoteHttpUrl(imagePath)) {
            imagePath
        } else {
            try {
                CloudinaryHelper.uploadImageSuspend(Uri.parse(imagePath))
            } catch (e: Exception) {
                Log.e(TAG, "Cloudinary upload failed for $imagePath", e)
                imagePath // Fallback to local path if upload fails
            }
        }
    }

    override suspend fun saveDiary(diary: Diary): Result<Unit> = try {
        val currentUserUid = auth.currentUser?.uid ?: throw Exception("User not logged in")

        // Upload images to Cloudinary and get secure_urls
        val uploadedUrls = diary.imageUrls.map { path ->
            uploadToCloudinary(path)
        }

        val docRef = if (diary.diaryId.isEmpty()) {
            diariesCollection.document()
        } else {
            diariesCollection.document(diary.diaryId)
        }

        val now = System.currentTimeMillis()

        val diaryData = hashMapOf(
            "diary_id" to docRef.id,
            "user_id" to currentUserUid,
            "text" to diary.content,
            "mood_tag" to (diary.moodTag ?: "Neutral"),
            "image_urls" to uploadedUrls,
            "audio_url" to diary.audioUrl,
            "timestamp" to Timestamp.now(),
            "updated_at" to now
        )

        docRef.set(diaryData).await()
        Result.success(Unit)
    } catch (e: Exception) {
        Log.e(TAG, "Save diary failed", e)
        Result.failure(e)
    }

    override suspend fun loadDiaries(): Result<List<Diary>> = try {
        val uid = auth.currentUser?.uid ?: throw Exception("User not logged in")

        val snapshot = diariesCollection
            .whereEqualTo(fieldUserId, uid)
            .orderBy(fieldTimestamp, Query.Direction.DESCENDING)
            .get()
            .await()

        val listDiary = snapshot.documents.mapNotNull { doc ->
            val diary = doc.toObject(Diary::class.java)
            val firebaseTimestamp = doc.get(fieldTimestamp) as? Timestamp
            diary?.copy(
                diaryId = doc.id,
                createdAt = firebaseTimestamp?.toDate()?.time ?: 0L
            )
        }
        Result.success(listDiary)
    } catch (e: Exception) {
        Result.failure(e)
    }

    override suspend fun loadDiaryById(diaryId: String): Result<Diary?> = try {
        val uid = auth.currentUser?.uid ?: throw Exception("User not logged in")
        val snapshot = diariesCollection.document(diaryId).get().await()

        if (!snapshot.exists()) {
            Result.success(null)
        } else {
            val diary = snapshot.toObject(Diary::class.java)
            val firebaseTimestamp = snapshot.get(fieldTimestamp) as? Timestamp
            val finalDiary = diary?.copy(
                diaryId = snapshot.id,
                createdAt = firebaseTimestamp?.toDate()?.time ?: 0L
            )

            if (finalDiary?.userId != uid) {
                Result.failure(Exception("Permission denied"))
            } else {
                Result.success(finalDiary)
            }
        }
    } catch (e: Exception) {
        Result.failure(e)
    }

    override suspend fun patchDiary(diaryId: String, updates: Map<String, Any?>): Result<Unit> = try {
        val uid = auth.currentUser?.uid ?: throw Exception("User not logged in")
        val docRef = diariesCollection.document(diaryId)
        val snapshot = docRef.get().await()

        if (!snapshot.exists()) {
            throw Exception("Diary not found")
        }

        val diary = snapshot.toObject(Diary::class.java)
        if (diary?.userId != uid) {
            throw Exception("Permission denied")
        }

        val safeUpdates = updates.toMutableMap().apply {
            remove("diaryId")
            remove("diary_id")
            remove("userId")
            remove("user_id")
            remove("createdAt")
            remove("timestamp")
            put("updated_at", System.currentTimeMillis())
        }

        docRef.update(safeUpdates).await()
        Result.success(Unit)
    } catch (e: Exception) {
        Result.failure(e)
    }

    override suspend fun deleteDiary(diaryId: String): Result<Unit> = try {
        val uid = auth.currentUser?.uid ?: throw Exception("User not logged in")
        val docRef = diariesCollection.document(diaryId)

        val snapshot = docRef.get().await()
        val diary = snapshot.toObject(Diary::class.java)
        if (diary?.userId != uid) {
            throw Exception("Permission denied")
        }

        docRef.delete().await()
        Result.success(Unit)
    } catch (e: Exception) {
        Result.failure(e)
    }
}
