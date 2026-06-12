package com.soulmate.app.domain.usecase

import com.soulmate.app.domain.model.SoulPet
import com.soulmate.app.domain.repository.IPetRepository
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CalculatePetXPUseCaseTest {

    @Test
    fun testLevelCalculationAtBoundary() {
        val useCase = CalculatePetXPUseCase(FakePetRepository())
        
        // XP = 0 -> level 1, remaining XP = 0
        var result = useCase.calculateLevel(0)
        assertEquals(1, result.first)
        assertEquals(0, result.second)

        // XP = 50 -> level 1, remaining XP = 50 (below threshold of 100)
        result = useCase.calculateLevel(50)
        assertEquals(1, result.first)
        assertEquals(50, result.second)

        // XP = 100 -> level 2, remaining XP = 0 (exact threshold of level 1)
        result = useCase.calculateLevel(100)
        assertEquals(2, result.first)
        assertEquals(0, result.second)

        // XP = 229 -> level 2, remaining XP = 129
        // Leve1 -> Level 2: 100 XP
        // Level 2 -> Level 3: 130 XP
        result = useCase.calculateLevel(229)
        assertEquals(2, result.first)
        assertEquals(129, result.second)

        // XP = 230 -> level 3, remaining XP = 0
        result = useCase.calculateLevel(230)
        assertEquals(3, result.first)
        assertEquals(0, result.second)
    }

    @Test
    fun testAddDiaryXPIncreasesCorrectAmount() = runBlocking {
        val fakeRepo = FakePetRepository()
        val useCase = CalculatePetXPUseCase(fakeRepo)

        val petResult = useCase.addDiaryXP("user123")
        assertTrue(petResult.isSuccess)

        val pet = petResult.getOrThrow()
        assertEquals(CalculatePetXPUseCase.XP_WRITE_DIARY, pet.xp)
        assertEquals("diary", fakeRepo.lastSource)
    }

    @Test
    fun testAddCommunityActionXP() = runBlocking {
        val fakeRepo = FakePetRepository()
        val useCase = CalculatePetXPUseCase(fakeRepo)

        // Test post
        var petResult = useCase.addCommunityXP("user123", "post")
        assertEquals(CalculatePetXPUseCase.XP_COMMUNITY_POST, petResult.getOrThrow().xp)

        // Test comment
        fakeRepo.resetXP()
        petResult = useCase.addCommunityXP("user123", "comment")
        assertEquals(CalculatePetXPUseCase.XP_COMMUNITY_COMMENT, petResult.getOrThrow().xp)

        // Test like
        fakeRepo.resetXP()
        petResult = useCase.addCommunityXP("user123", "like")
        assertEquals(CalculatePetXPUseCase.XP_COMMUNITY_LIKE, petResult.getOrThrow().xp)
    }

    private class FakePetRepository : IPetRepository {
        var currentXP = 0
        var lastSource = ""

        fun resetXP() {
            currentXP = 0
            lastSource = ""
        }

        override suspend fun getPet(userId: String): Result<SoulPet?> {
            return Result.success(SoulPet(userId = userId, name = "FakePet", xp = currentXP))
        }

        override suspend fun createPet(userId: String, petName: String): Result<SoulPet> {
            return Result.success(SoulPet(userId = userId, name = petName, xp = currentXP))
        }

        override suspend fun addXP(userId: String, xpAmount: Int, source: String): Result<SoulPet> {
            currentXP += xpAmount
            lastSource = source
            return Result.success(SoulPet(userId = userId, name = "FakePet", xp = currentXP))
        }

        override suspend fun updatePetMood(userId: String, mood: String): Result<Unit> {
            return Result.success(Unit)
        }

        override suspend fun updateStreak(userId: String): Result<SoulPet> {
            return Result.success(SoulPet(userId = userId, name = "FakePet", xp = currentXP))
        }
    }
}
