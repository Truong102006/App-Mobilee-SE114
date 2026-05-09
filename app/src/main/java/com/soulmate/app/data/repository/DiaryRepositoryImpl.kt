package com.soulmate.app.data.repository

import android.net.Uri
import android.util.Log
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageException
import com.soulmate.app.domain.model.Diary
import com.soulmate.app.domain.repository.IDiaryRepository
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DiaryRepositoryImpl @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val auth: FirebaseAuth,
    private val storage: FirebaseStorage
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

    private fun shouldFallbackToLocalImage(errorCode: Int): Boolean {
        return when (errorCode) {
            StorageException.ERROR_OBJECT_NOT_FOUND,
            StorageException.ERROR_BUCKET_NOT_FOUND,
            StorageException.ERROR_PROJECT_NOT_FOUND,
            StorageException.ERROR_NOT_AUTHENTICATED,
            StorageException.ERROR_NOT_AUTHORIZED,
            StorageException.ERROR_QUOTA_EXCEEDED,
            StorageException.ERROR_RETRY_LIMIT_EXCEEDED,
            StorageException.ERROR_UNKNOWN -> true
            else -> false
        }
    }

    private fun logStorageFallback(exception: StorageException, imagePath: String) {
        val bucket = storage.app.options.storageBucket ?: "(null)"
        Log.w(
            TAG,
            "Storage upload failed, fallback to local URI. bucket=$bucket path=$imagePath code=${exception.errorCode} http=${exception.httpResultCode}",
            exception
        )
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

    private suspend fun uploadImage(uri: Uri): String {
        val fileName = "diary_images/${UUID.randomUUID()}.jpg"
        val ref = storage.reference.child(fileName)

        ref.putFile(uri).await()
        return ref.downloadUrl.await().toString()
    }

    private fun mapStorageErrorToUserMessage(e: StorageException): String {
        return when (e.errorCode) {
            StorageException.ERROR_BUCKET_NOT_FOUND,
            StorageException.ERROR_PROJECT_NOT_FOUND -> {
                "Firebase Storage chua duoc cau hinh. Vao Firebase Console > Storage > Get started de tao bucket."
            }
            StorageException.ERROR_OBJECT_NOT_FOUND -> {
                "Khong upload duoc anh (HTTP ${e.httpResultCode}). Thuong la bucket chua ton tai hoac du an chua bat Blaze cho Cloud Storage."
            }
            StorageException.ERROR_NOT_AUTHENTICATED -> {
                "Ban chua dang nhap, vui long dang nhap lai roi thu luu anh."
            }
            StorageException.ERROR_NOT_AUTHORIZED -> {
                "Khong du quyen ghi Storage. Kiem tra Firebase Storage Rules."
            }
            StorageException.ERROR_QUOTA_EXCEEDED -> {
                "Storage vuot quota. Can nang cap/goi thanh toan Blaze de tiep tuc."
            }
            else -> e.message ?: "Loi upload anh tu Firebase Storage."
        }
    }

    override suspend fun saveDiary(diary: Diary): Result<Unit> = try {
        val currentUserUid = auth.currentUser?.uid ?: throw Exception("User not logged in")

        var canUploadToCloud = true
        val uploadedUrls = diary.imageUrls
            .mapNotNull { it.trim().takeIf(String::isNotEmpty) }
            .map { imagePath ->
                if (!canUploadToCloud) {
                    return@map imagePath
                }

                when {
                    isRemoteHttpUrl(imagePath) -> imagePath
                    imagePath.startsWith("gs://", ignoreCase = true) -> {
                        runCatching {
                            storage.getReferenceFromUrl(imagePath).downloadUrl.await().toString()
                        }.getOrElse { throwable ->
                            val storageException = throwable as? StorageException
                            if (storageException != null && shouldFallbackToLocalImage(storageException.errorCode)) {
                                canUploadToCloud = false
                                logStorageFallback(storageException, imagePath)
                                imagePath
                            } else {
                                throw throwable
                            }
                        }
                    }
                    else -> {
                        runCatching {
                            uploadImage(Uri.parse(imagePath))
                        }.getOrElse { throwable ->
                            val storageException = throwable as? StorageException
                            if (storageException != null && shouldFallbackToLocalImage(storageException.errorCode)) {
                                canUploadToCloud = false
                                logStorageFallback(storageException, imagePath)
                                imagePath
                            } else {
                                throw throwable
                            }
                        }
                    }
                }
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
        val storageException = e as? StorageException
        if (storageException != null) {
            val bucket = storage.app.options.storageBucket ?: "(null)"
            Log.e(
                TAG,
                "Upload failed. bucket=$bucket code=${storageException.errorCode} http=${storageException.httpResultCode}",
                storageException
            )
            Result.failure(Exception(mapStorageErrorToUserMessage(storageException)))
        } else {
            Result.failure(e)
        }
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
        auth.currentUser?.uid ?: throw Exception("User not logged in")
        val docRef = diariesCollection.document(diaryId)
        docRef.delete().await()
        Result.success(Unit)
    } catch (e: Exception) {
        Result.failure(e)
    }
}
