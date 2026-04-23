package com.soulmate.app.domain.repository

import com.soulmate.app.domain.model.Diary
import kotlinx.coroutines.flow.Flow

interface IDiaryRepository {
    suspend fun saveDiary(diary: Diary): Result<Unit>
    fun getDiaries(userId: String): Flow<List<Diary>>
}
