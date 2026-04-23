package com.soulmate.app.data.repository

import com.google.ai.client.generativeai.GenerativeModel
import com.soulmate.app.domain.repository.IAIRepository
import javax.inject.Inject

class AIRepositoryImpl @Inject constructor(
    private val generativeModel: GenerativeModel
) : IAIRepository {

    override suspend fun predictMood(text: String): Result<String> {
        return try {
            val prompt = """
                Bạn là một chuyên gia tâm lý. Hãy phân tích đoạn nhật ký sau và trả về duy nhất 1 từ tiếng Anh mô tả tâm trạng (ví dụ: Happy, Sad, Angry, Neutral, Excited, Tired).
                Nếu không xác định được, hãy trả về 'Neutral'.
                
                Nội dung nhật ký: "$text"
            """.trimIndent()

            val response = generativeModel.generateContent(prompt)
            val mood = response.text?.trim() ?: "Neutral"
            Result.success(mood)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
