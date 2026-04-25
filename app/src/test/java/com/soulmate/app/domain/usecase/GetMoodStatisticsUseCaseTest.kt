package com.soulmate.app.domain.usecase

import com.soulmate.app.domain.model.Diary
import com.soulmate.app.domain.model.MoodStatistic
import com.soulmate.app.domain.repository.IDiaryRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Test

class GetMoodStatisticsUseCaseTest {

    @Test
    fun `groups moods case insensitively and sorts by count descending`() = runBlocking {
        val fakeRepository = FakeDiaryRepository(
            diaries = listOf(
                Diary(id = "1", userId = "u1", moodTag = "happy"),
                Diary(id = "2", userId = "u1", moodTag = "Happy"),
                Diary(id = "3", userId = "u1", moodTag = "stress"),
                Diary(id = "4", userId = "u1", moodTag = ""),
                Diary(id = "5", userId = "u1", moodTag = "stress")
            )
        )
        val useCase = GetMoodStatisticsUseCase(fakeRepository)

        val result = useCase("u1").first()

        assertEquals(
            listOf(
                MoodStatistic(mood = "Happy", count = 2),
                MoodStatistic(mood = "Stress", count = 2),
                MoodStatistic(mood = "Unknown", count = 1)
            ),
            result
        )
    }

    @Test
    fun `returns empty list when user id is blank`() = runBlocking {
        val useCase = GetMoodStatisticsUseCase(FakeDiaryRepository(emptyList()))

        val result = useCase("").first()

        assertEquals(emptyList<MoodStatistic>(), result)
    }

    private class FakeDiaryRepository(
        private val diaries: List<Diary>
    ) : IDiaryRepository {
        override suspend fun saveDiary(diary: Diary): Result<Unit> = Result.success(Unit)

        override fun getDiaries(userId: String): Flow<List<Diary>> = flowOf(
            diaries.filter { it.userId == userId }
        )
    }
}
