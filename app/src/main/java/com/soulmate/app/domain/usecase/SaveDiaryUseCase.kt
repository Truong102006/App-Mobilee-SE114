package com.soulmate.app.domain.usecase

import com.soulmate.app.domain.model.Diary
import com.soulmate.app.domain.repository.IDiaryRepository
import javax.inject.Inject

class SaveDiaryUseCase @Inject constructor(
    private val diaryRepository: IDiaryRepository
) {
    suspend operator fun invoke(diary: Diary): Result<Unit> {
        return diaryRepository.saveDiary(diary)
    }
}
