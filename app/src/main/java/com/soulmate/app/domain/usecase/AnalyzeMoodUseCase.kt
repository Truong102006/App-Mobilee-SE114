package com.soulmate.app.domain.usecase

import com.soulmate.app.domain.repository.IAIRepository
import javax.inject.Inject

class AnalyzeMoodUseCase @Inject constructor(
    private val aiRepository: IAIRepository
) {
    suspend operator fun invoke(text: String): Result<String> {
        if (text.isBlank()) return Result.failure(Exception("Nội dung trống"))
        return aiRepository.predictMood(text)
    }
}
