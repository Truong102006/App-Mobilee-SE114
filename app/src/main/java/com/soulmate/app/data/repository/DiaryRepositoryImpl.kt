package com.soulmate.app.data.repository

import android.net.Uri
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.soulmate.app.domain.model.Diary
import com.soulmate.app.domain.repository.IDiaryRepository
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton
import com.google.firebase.firestore.Query
import com.google.firebase.Timestamp
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import java.util.UUID

@Singleton
class DiaryRepositoryImpl @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val auth: FirebaseAuth,
    private val storage: FirebaseStorage
) : IDiaryRepository {

    private val diariesCollection = firestore.collection("diaries")

    private val FIELD_USER_ID = "user_id"
    private val FIELD_TIMESTAMP = "timestamp"

    override fun getDiaries(userId: String): Flow<List<Diary>> = callbackFlow {
        val subscription = diariesCollection
            .whereEqualTo(FIELD_USER_ID, userId)
            .orderBy(FIELD_TIMESTAMP, Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }
                if (snapshot != null) {
                    val diaries = snapshot.documents.mapNotNull { doc ->
                        try {
                            val diary = doc.toObject(Diary::class.java)
                            val firebaseTimestamp = doc.get(FIELD_TIMESTAMP) as? Timestamp
                            diary?.copy(
                                diaryId = doc.id,
                                createdAt = firebaseTimestamp?.toDate()?.time ?: System.currentTimeMillis()
                            )
                        } catch (e: Exception) {
                            null
                        }
                    }
                    trySend(diaries)
                }
            }
        awaitClose { subscription.remove() }
    }

    private suspend fun uploadImages(imageUrls: List<String>): List<String> {
        val currentUserUid = auth.currentUser?.uid ?: return imageUrls
        val uploadedUrls = mutableListOf<String>()

        for (url in imageUrls) {
            if (url.startsWith("http")) {
                uploadedUrls.add(url)
                continue
            }

            try {
                val uri = Uri.parse(url)
                val fileName = UUID.randomUUID().toString()
                val storageRef = storage.reference.child("diaries/$currentUserUid/$fileName")
                
                storageRef.putFile(uri).await()
                val downloadUrl = storageRef.downloadUrl.await().toString()
                uploadedUrls.add(downloadUrl)
            } catch (e: Exception) {
                // If upload fails, we might want to skip or handle it. For now, skip.
            }
        }
        return uploadedUrls
    }

    override suspend fun saveDiary(diary: Diary): Result<Unit> = try {
        val currentUserUid = auth.currentUser?.uid ?: throw Exception("User not logged in")
        
        // 1. Upload images to Firebase Storage first
        val firebaseImageUrls = uploadImages(diary.imageUrls)
        
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
            "image_urls" to firebaseImageUrls,
            "audio_url" to diary.audioUrl,
            "timestamp" to Timestamp.now(),
            "updated_at" to now
        )
        
        docRef.set(diaryData).await()
        Result.success(Unit)
    } catch (e: Exception) {
        Result.failure(e)
    }

    override suspend fun loadDiaries(): Result<List<Diary>> = try {
        val uid = auth.currentUser?.uid ?: throw Exception("User not logged in")

        val snapshot = diariesCollection
            .whereEqualTo(FIELD_USER_ID, uid)
            .orderBy(FIELD_TIMESTAMP, Query.Direction.DESCENDING)
            .get()
            .await()

        val listDiary = snapshot.documents.mapNotNull { doc ->
            val diary = doc.toObject(Diary::class.java)
            val firebaseTimestamp = doc.get(FIELD_TIMESTAMP) as? Timestamp
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
            val firebaseTimestamp = snapshot.get(FIELD_TIMESTAMP) as? Timestamp
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
