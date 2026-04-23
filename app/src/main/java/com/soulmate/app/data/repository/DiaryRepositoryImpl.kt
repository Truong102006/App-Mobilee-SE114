package com.soulmate.app.data.repository

import com.google.firebase.Timestamp
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.soulmate.app.data.remote.dto.DiaryDto
import com.soulmate.app.domain.model.Diary
import com.soulmate.app.domain.repository.IDiaryRepository
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import java.util.Date
import javax.inject.Inject

class DiaryRepositoryImpl @Inject constructor(
    private val firestore: FirebaseFirestore
) : IDiaryRepository {

    override suspend fun saveDiary(diary: Diary): Result<Unit> {
        return try {
            val diaryRef = if (diary.id.isEmpty()) {
                firestore.collection("diaries").document()
            } else {
                firestore.collection("diaries").document(diary.id)
            }

            val diaryDto = DiaryDto(
                diary_id = diaryRef.id,
                user_id = diary.userId,
                text = diary.text,
                image_urls = diary.imageUrls,
                audio_url = diary.audioUrl,
                mood_tag = diary.moodTag,
                timestamp = Timestamp(Date(diary.timestamp))
            )

            diaryRef.set(diaryDto.toMap()).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
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
