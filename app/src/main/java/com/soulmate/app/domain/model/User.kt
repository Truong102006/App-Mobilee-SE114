package com.soulmate.app.domain.model

data class User(
    val userId: String = "",

    val email: String = "",
    val anonymousName: String = "SoulMate User",
    val avatarUrl: String? = null,
    val gender: String = "Secret",
    val phoneNumber: String = "",
    val socialMedias: List<String> = emptyList(),

    val role: String = "user", // "user" hoặc "admin"
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val lastLoginAt: Long? = null,
    val notificationEnabled: Boolean = true,
    val reminderEnabled: Boolean = false,
    val reminderTime: String? = null,
    val currentMood: String? = null,
    val totalDiaries: Int = 0,
    val status: String = "active",
    val isProfileCompleted: Boolean = false
) {
    // Hàm helper để convert sang Map khi update Firestore (nếu cần)
    fun toMap(): Map<String, Any?> {
        return mapOf(
            "userId" to userId,

            "email" to email,
            "anonymousName" to anonymousName,
            "avatarUrl" to avatarUrl,
            "gender" to gender,
            "phoneNumber" to phoneNumber,
            "socialMedias" to socialMedias,

            "role" to role,
            "createdAt" to createdAt,
            "updatedAt" to updatedAt,
            "lastLoginAt" to lastLoginAt,
            "notificationEnabled" to notificationEnabled,
            "reminderEnabled" to reminderEnabled,
            "reminderTime" to reminderTime,
            "currentMood" to currentMood,
            "totalDiaries" to totalDiaries,
            "status" to status,
            "isProfileCompleted" to isProfileCompleted
        )
    }
}
