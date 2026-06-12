package com.soulmate.app.domain.model

data class SoulPet(
    val id: String = "",
    val userId: String = "",
    val name: String = "Soul Buddy",
    val level: Int = 1,
    val xp: Int = 0,
    val maxXp: Int = 100,
    val mood: String = "happy",
    val totalDiaries: Int = 0,
    val currentStreak: Int = 0,
    val createdAt: Long = 0L,
    val updatedAt: Long = 0L
)
