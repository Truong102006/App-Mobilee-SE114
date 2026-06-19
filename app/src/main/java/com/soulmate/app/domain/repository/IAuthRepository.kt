package com.soulmate.app.domain.repository

import com.soulmate.app.domain.model.User

interface IAuthRepository {
    suspend fun register(name: String, email: String, password: String): Result<User>

    suspend fun login(email: String, password: String): Result<User>

    suspend fun signInWithGoogle(idToken: String): Result<User>

    fun logout(): Result<Unit>

    fun getCurrentUser(): User?

    fun getCurrentUserId(): String?

    suspend fun updateUserProfile(user: User): Result<Unit>
    
    suspend fun getUserProfile(uid: String): Result<User>

    // Gửi email khôi phục mật khẩu
    suspend fun sendPasswordResetEmail(email: String): Result<Unit>

    suspend fun searchUsers(query: String): Result<List<User>>

    suspend fun toggleSocialBan(targetUserId: String, isBanned: Boolean): Result<Unit>

    suspend fun hidePost(userId: String, postId: String): Result<Unit>
}
