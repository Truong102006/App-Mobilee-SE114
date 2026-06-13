package com.soulmate.app.domain.repository

import com.soulmate.app.domain.model.SoulPet

interface IPetRepository {
    suspend fun getPet(userId: String): Result<SoulPet?>
    suspend fun createPet(userId: String, petName: String): Result<SoulPet>
    suspend fun addXP(userId: String, xpAmount: Int, source: String): Result<SoulPet>
    suspend fun updatePetMood(userId: String, mood: String): Result<Unit>
    suspend fun updateStreak(userId: String): Result<SoulPet>
}
