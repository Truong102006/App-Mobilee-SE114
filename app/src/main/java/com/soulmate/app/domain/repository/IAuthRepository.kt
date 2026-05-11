package com.soulmate.app.domain.repository

import com.soulmate.app.domain.model.User

interface IAuthRepository {
    // Đăng ký tài khoản mới bằng Email và Password
    suspend fun register(name: String, email: String, password: String): Result<User>

    // Đăng nhập vào hệ thống
    suspend fun login(email: String, password: String): Result<User>

    // Đăng nhập bằng Google
    suspend fun signInWithGoogle(idToken: String): Result<User>

    // Đăng xuất khỏi hệ thống
    fun logout(): Result<Unit>

    // Lấy thông tin User hiện tại đang đăng nhập (nếu có)
    fun getCurrentUser(): User?

    // Lấy UID của User hiện tại đang đăng nhập
    fun getCurrentUserId(): String?

    // Cập nhật thông tin User
    suspend fun updateUserProfile(user: User): Result<Unit>
    
    // Lấy thông tin chi tiết User từ Firestore
    suspend fun getUserProfile(uid: String): Result<User>
}
