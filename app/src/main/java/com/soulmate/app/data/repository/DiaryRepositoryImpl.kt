package com.soulmate.app.data.repository

import android.net.Uri
import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.soulmate.app.data.remote.api.BackendApiService
import com.soulmate.app.data.remote.dto.SaveDiaryRequestDto
import com.soulmate.app.domain.model.Diary
import com.soulmate.app.domain.repository.IDiaryRepository
import com.soulmate.app.utils.CloudinaryHelper
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DiaryRepositoryImpl @Inject constructor(
    private val backendApiService: BackendApiService,
    private val auth: FirebaseAuth
) : IDiaryRepository {

    companion object {
        private const val TAG = "DiaryRepositoryImpl"
    }

    private val diaryRefreshes = MutableSharedFlow<Unit>(replay = 1, extraBufferCapacity = 1)

    init {
        diaryRefreshes.tryEmit(Unit)
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    override fun getDiaries(userId: String): Flow<List<Diary>> =
        diaryRefreshes
            .mapLatest {
                loadDiaries().getOrElse { error ->
                    Log.w(TAG, "Unable to load diaries for userId=$userId", error)
                    emptyList()
                }
            }
            .distinctUntilChanged()

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
        return currentUser.getIdToken(false).await().token
            ?: throw IllegalStateException("Cannot get Firebase ID token")
    }

    private fun mapDiary(diary: com.soulmate.app.data.remote.dto.DiaryItemDto): Diary {
        return Diary(
            diaryId = diary.diaryId,
            userId = diary.userId,
            title = diary.title,
            content = diary.text,
            imageUrls = diary.imageUrls,
            audioUrl = diary.audioUrl,
            moodTag = diary.moodTag,
            createdAt = diary.createdAt ?: diary.updatedAt ?: System.currentTimeMillis(),
            updatedAt = diary.updatedAt ?: diary.createdAt ?: System.currentTimeMillis()
        )
    }

    override suspend fun saveDiary(diary: Diary): Result<Unit> = runCatching {
        val idToken = requireIdToken()

        val uploadedUrls = diary.imageUrls.map { path ->
            uploadToCloudinary(path)
        }

        backendApiService.saveDiary(
            authorization = "Bearer $idToken",
            request = SaveDiaryRequestDto(
                diaryId = diary.diaryId.ifBlank { null },
                title = diary.title,
                text = diary.content,
                moodTag = diary.moodTag,
                imageUrls = uploadedUrls,
                audioUrl = diary.audioUrl
            )
        )
        notifyDiariesChanged()
    }

    override suspend fun loadDiaries(): Result<List<Diary>> = runCatching {
        val idToken = requireIdToken()
        backendApiService
            .listMyDiaries(authorization = "Bearer $idToken")
            .diaries
            .map(::mapDiary)
            .sortedByDescending { it.createdAt }
    }

    override suspend fun loadDiaryById(diaryId: String): Result<Diary?> = runCatching {
        if (diaryId.isBlank()) return@runCatching null
        val diaries = loadDiaries().getOrThrow()
        diaries.firstOrNull { it.diaryId == diaryId }
    }

    override suspend fun patchDiary(diaryId: String, updates: Map<String, Any?>): Result<Unit> = runCatching {
        val existingDiary = loadDiaryById(diaryId).getOrThrow()
            ?: throw IllegalStateException("Diary not found")

        val mergedTitle = updates["title"] as? String ?: existingDiary.title
        val mergedText = updates["text"] as? String ?: existingDiary.content

        val mergedMood = when {
            updates.containsKey("moodTag") -> updates["moodTag"] as? String
            updates.containsKey("mood_tag") -> updates["mood_tag"] as? String
            else -> existingDiary.moodTag
        }

        val mergedImageUrls = when {
            updates.containsKey("imageUrls") -> extractStringList(updates["imageUrls"])
            updates.containsKey("image_urls") -> extractStringList(updates["image_urls"])
            else -> existingDiary.imageUrls
        }

        val mergedAudioUrl = when {
            updates.containsKey("audioUrl") -> updates["audioUrl"] as? String
            updates.containsKey("audio_url") -> updates["audio_url"] as? String
            else -> existingDiary.audioUrl
        }

        saveDiary(
            existingDiary.copy(
                diaryId = diaryId,
                title = mergedTitle,
                content = mergedText,
                moodTag = mergedMood,
                imageUrls = mergedImageUrls,
                audioUrl = mergedAudioUrl,
                updatedAt = System.currentTimeMillis()
            )
        ).getOrThrow()
    }

    override suspend fun deleteDiary(diaryId: String): Result<Unit> = runCatching {
        val idToken = requireIdToken()
        backendApiService.deleteDiary(
            authorization = "Bearer $idToken",
            diaryId = diaryId
        )
        notifyDiariesChanged()
    }

    private fun extractStringList(value: Any?): List<String> {
        if (value !is List<*>) return emptyList()
        return value.filterIsInstance<String>().map { it.trim() }.filter { it.isNotBlank() }
    }

    private fun notifyDiariesChanged() {
        diaryRefreshes.tryEmit(Unit)
    }
}
