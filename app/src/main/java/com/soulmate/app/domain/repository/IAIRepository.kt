package com.soulmate.app.domain.repository

interface IAIRepository {
    suspend fun predictMood(text: String): Result<String>
}
