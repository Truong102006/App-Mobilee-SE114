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

    override suspend fun predictMood(text: String): Result<String> {
        return runCatching {
            val normalizedText = text.trim()
            require(normalizedText.isNotBlank()) { "Noi dung khong duoc de trong." }

            val currentUser = firebaseAuth.currentUser
                ?: throw IllegalStateException("Ban can dang nhap de su dung AI.")

            val idToken = currentUser.getIdToken(true).await().token
                ?: throw IllegalStateException("Khong the lay Firebase ID token.")

            val response = backendApiService.predictMood(
                authorization = "Bearer $idToken",
                request = PredictMoodRequestDto(text = normalizedText)
            )

            response.mood.trim().ifBlank { "Neutral" }
        }
    }
}
