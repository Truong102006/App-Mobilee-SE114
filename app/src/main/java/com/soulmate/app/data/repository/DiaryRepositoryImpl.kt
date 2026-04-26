package com.soulmate.app.data.repository

import android.app.Application
import android.net.Uri
import android.util.Log
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.storage.FirebaseStorage
import com.soulmate.app.data.remote.dto.DiaryDto
import com.soulmate.app.domain.model.Diary
import com.soulmate.app.domain.repository.IDiaryRepository
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import java.util.Date
import java.util.UUID
import javax.inject.Inject

class DiaryRepositoryImpl @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val storage: FirebaseStorage,
    private val application: Application // Inject Application để dùng contentResolver
) : IDiaryRepository {

    override suspend fun saveDiary(diary: Diary): Result<Unit> {
        return try {
            Log.d("DiaryRepository", "Bắt đầu lưu nhật ký với ${diary.imageUrls.size} ảnh")
            
            // 1. Tải ảnh lên Storage trước để lấy URL thật
            val uploadedUrls = diary.imageUrls.map { uriString ->
                if (uriString.startsWith("http")) {
                    uriString 
                } else {
                    uploadImage(Uri.parse(uriString))
                }
            }

            val diaryRef = if (diary.id.isEmpty()) {
                firestore.collection("diaries").document()
            } else {
                firestore.collection("diaries").document(diary.id)
            }

            val diaryDto = DiaryDto(
                diary_id = diaryRef.id,
                user_id = diary.userId,
                text = diary.text,
                image_urls = uploadedUrls,
                audio_url = diary.audioUrl,
                mood_tag = diary.moodTag,
                timestamp = Timestamp(Date(diary.timestamp))
            )

            diaryRef.set(diaryDto.toMap()).await()
            Log.d("DiaryRepository", "Lưu Firestore thành công: ${diaryRef.id}")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e("DiaryRepository", "Lỗi saveDiary: ${e.message}", e)
            Result.failure(e)
        }
    }

    private suspend fun uploadImage(uri: Uri): String {
        val fileName = "diary_images/${UUID.randomUUID()}.jpg"
        val ref = storage.reference.child(fileName)
        
        return try {
            Log.d("DiaryRepository", "Đang tải ảnh lên: $uri")
            
            // Sử dụng Stream để đọc dữ liệu từ Uri một cách an toàn hơn
            val inputStream = application.contentResolver.openInputStream(uri) 
                ?: throw Exception("Không thể mở stream từ Uri: $uri")
            
            val bytes = inputStream.readBytes()
            inputStream.close()

            // Thực hiện upload
            ref.putBytes(bytes).await()
            
            // Lấy URL sau khi đã upload thành công
            val downloadUrl = ref.downloadUrl.await().toString()
            Log.d("DiaryRepository", "Tải lên thành công: $downloadUrl")
            downloadUrl
        } catch (e: Exception) {
            Log.e("DiaryRepository", "Lỗi uploadImage ($uri): ${e.message}")
            throw e
        }
    }

    override fun getDiaries(userId: String): Flow<List<Diary>> = callbackFlow {
        val subscription = firestore.collection("diaries")
            .whereEqualTo("user_id", userId)
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }

                val diaries = snapshot?.documents?.mapNotNull { doc ->
                    doc.toObject(DiaryDto::class.java)?.let { dto ->
                        Diary(
                            id = dto.diary_id,
                            userId = dto.user_id,
                            text = dto.text,
                            imageUrls = dto.image_urls,
                            audioUrl = dto.audio_url,
                            moodTag = dto.mood_tag,
                            timestamp = dto.timestamp.toDate().time
                        )
                    }
                } ?: emptyList()

                trySend(diaries)
            }

        awaitClose { subscription.remove() }
    }
}
