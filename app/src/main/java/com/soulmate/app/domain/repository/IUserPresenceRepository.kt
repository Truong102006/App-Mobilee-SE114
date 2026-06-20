package com.soulmate.app.domain.repository

import com.soulmate.app.domain.model.UserPresence
import kotlinx.coroutines.flow.Flow

interface IUserPresenceRepository {
    fun observePresences(userIds: Set<String>): Flow<Map<String, UserPresence>>
    fun observeOnlineUsersCount(excludedUserId: String? = null): Flow<Int>
}
