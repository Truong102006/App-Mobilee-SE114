package com.soulmate.app.domain.repository

import com.soulmate.app.domain.model.User
import kotlinx.coroutines.flow.Flow

interface IUserRepository {
    suspend fun createUserProfile(user: User): Result<Unit>
    suspend fun getCurrentUser(userId: String): Result<User?>
    suspend fun updateAnonymousProfile(userId: String, name: String, avatarUrl: String?): Result<Unit>
    suspend fun updateSettings(userId: String, notification: Boolean, reminder: Boolean, time: String?): Result<Unit>
    suspend fun updateCurrentMood(userId: String, mood: String): Result<Unit>
    suspend fun checkIsAdmin(userId: String): Boolean
    suspend fun updateLastLogin(userId: String): Result<Unit>
}
