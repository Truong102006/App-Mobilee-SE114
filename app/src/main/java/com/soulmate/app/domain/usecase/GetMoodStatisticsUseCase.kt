package com.soulmate.app.domain.usecase

import com.soulmate.app.domain.model.MoodStatistic
import com.soulmate.app.domain.repository.IDiaryRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class GetMoodStatisticsUseCase @Inject constructor(
    private val diaryRepository: IDiaryRepository
) {
    operator fun invoke(userId: String): Flow<List<MoodStatistic>> {
        if (userId.isBlank()) return flowOf(emptyList())

        return diaryRepository.getDiaries(userId).map { diaries ->
            diaries
                .groupingBy { diary -> normalizeMood(diary.moodTag) }
                .eachCount()
                .map { (mood, count) -> MoodStatistic(mood = mood, count = count) }
                .sortedWith(compareByDescending<MoodStatistic> { it.count }.thenBy { it.mood })
        }
    }

    private fun normalizeMood(mood: String?): String {
        val normalizedMood = mood?.trim() ?: ""
        if (normalizedMood.isEmpty()) return UNKNOWN_MOOD

        return normalizedMood
            .lowercase()
            .replaceFirstChar { firstChar -> firstChar.titlecase() }
    }

    private companion object {
        const val UNKNOWN_MOOD = "Unknown"
    }
}
