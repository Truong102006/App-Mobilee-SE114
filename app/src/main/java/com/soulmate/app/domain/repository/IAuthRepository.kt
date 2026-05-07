package com.soulmate.app.domain.repository

import com.soulmate.app.domain.model.User

interface IAuthRepository {
    // Đăng ký tài khoản mới bằng Email và Password
    suspend fun register(email: String, password: String): Result<User>

    // Đăng nhập vào hệ thống
    suspend fun login(email: String, password: String): Result<User>

    // Đăng xuất khỏi hệ thống
    fun logout(): Result<Unit>

    // Lấy thông tin User hiện tại đang đăng nhập (nếu có)
    fun getCurrentUser(): User?

    // Lấy UID của User hiện tại đang đăng nhập
    fun getCurrentUserId(): String?
}