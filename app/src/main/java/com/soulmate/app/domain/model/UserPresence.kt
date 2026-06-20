package com.soulmate.app.domain.model

const val USER_PRESENCE_TIMEOUT_MS = 75_000L

data class UserPresence(
    val userId: String = "",
    val userName: String = "SoulMate User",
    val userAvatarUrl: String? = null,
    val isAppOnline: Boolean = false,
    val lastActiveAt: Long = 0L
) {
    fun isOnlineNow(
        nowMillis: Long = System.currentTimeMillis(),
        timeoutMillis: Long = USER_PRESENCE_TIMEOUT_MS
    ): Boolean {
        return isAppOnline && lastActiveAt >= nowMillis - timeoutMillis
    }
}
