package com.soulmate.app.data.repository

import com.google.firebase.auth.FirebaseAuth
import com.soulmate.app.data.remote.api.BackendApiService
import com.soulmate.app.data.remote.dto.PredictMoodRequestDto
import com.soulmate.app.domain.repository.IAIRepository
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

class AIRepositoryImpl @Inject constructor(
    private val backendApiService: BackendApiService,
    private val firebaseAuth: FirebaseAuth
) : IAIRepository {

    private suspend fun <T> executeWithToken(forceRefresh: Boolean = false, block: suspend (String) -> T): T {
        val currentUser = firebaseAuth.currentUser ?: throw IllegalStateException("Ban can dang nhap de su dung AI.")
        val token = currentUser.getIdToken(forceRefresh).await().token
            ?: throw IllegalStateException("Khong the lay Firebase ID token.")
        return try {
            block(token)
        } catch (e: retrofit2.HttpException) {
            if (e.code() == 401 && !forceRefresh) {
                executeWithToken(forceRefresh = true, block)
            } else {
                throw e
            }
        }
    }

    override suspend fun predictMood(text: String): Result<String> = runCatching {
        val normalizedText = text.trim()
        require(normalizedText.isNotBlank()) { "Noi dung khong duoc de trong." }

        val response = executeWithToken { idToken ->
            backendApiService.predictMood(
                authorization = "Bearer $idToken",
                request = PredictMoodRequestDto(text = normalizedText)
            )
        }

        response.mood.trim().ifBlank { "Neutral" }
    }
}
