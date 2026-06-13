package com.soulmate.app.domain.usecase

import com.soulmate.app.domain.model.SoulPet
import com.soulmate.app.domain.repository.IPetRepository
import javax.inject.Inject

class CalculatePetXPUseCase @Inject constructor(
    private val petRepository: IPetRepository
) {
    companion object {
        const val XP_WRITE_DIARY = 10
        const val XP_COMMUNITY_POST = 8
        const val XP_COMMUNITY_COMMENT = 3
        const val XP_COMMUNITY_LIKE = 1
        const val XP_DAILY_STREAK_BONUS = 5
        const val XP_WEEKLY_STREAK_BONUS = 20
    }

    suspend fun addDiaryXP(userId: String): Result<SoulPet> {
        return petRepository.addXP(userId, XP_WRITE_DIARY, "diary")
    }

    suspend fun addCommunityXP(userId: String, action: String): Result<SoulPet> {
        val xp = when(action) {
            "post" -> XP_COMMUNITY_POST
            "comment" -> XP_COMMUNITY_COMMENT
            "like" -> XP_COMMUNITY_LIKE
            else -> 0
        }
        return petRepository.addXP(userId, xp, "community_$action")
    }

    fun calculateLevel(totalXp: Int): Pair<Int, Int> {
        var level = 1
        var remaining = totalXp
        var threshold = 100
        while (remaining >= threshold) {
            remaining -= threshold
            level++
            threshold = (threshold * 1.3).toInt()
        }
        return Pair(level, remaining)
    }
}
