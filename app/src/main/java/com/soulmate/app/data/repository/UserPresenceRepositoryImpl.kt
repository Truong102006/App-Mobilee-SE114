package com.soulmate.app.data.repository

import com.google.firebase.firestore.FieldPath
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.QuerySnapshot
import com.soulmate.app.domain.model.UserPresence
import com.soulmate.app.domain.repository.IUserPresenceRepository
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class UserPresenceRepositoryImpl @Inject constructor(
    firestore: FirebaseFirestore
) : IUserPresenceRepository {

    companion object {
        private const val PRESENCE_REFRESH_MS = 15_000L
        private const val WHERE_IN_LIMIT = 10
    }

    private val usersCollection = firestore.collection("users")

    override fun observePresences(userIds: Set<String>): Flow<Map<String, UserPresence>> = callbackFlow {
        val normalizedIds = userIds
            .map(String::trim)
            .filter(String::isNotEmpty)
            .toSet()

        if (normalizedIds.isEmpty()) {
            trySend(emptyMap())
            close()
            return@callbackFlow
        }

        val cache = linkedMapOf<String, UserPresence>()
        val listeners = mutableListOf<ListenerRegistration>()

        fun emitPresences() {
            val now = System.currentTimeMillis()
            val snapshot = normalizedIds.associateWith { userId ->
                cache[userId]?.copy(
                    isAppOnline = cache[userId]?.isOnlineNow(nowMillis = now) == true
                ) ?: UserPresence(userId = userId)
            }
            trySend(snapshot)
        }

        normalizedIds
            .chunked(WHERE_IN_LIMIT)
            .forEach { chunk ->
                listeners += usersCollection
                    .whereIn(FieldPath.documentId(), chunk)
                    .addSnapshotListener { snapshot, _ ->
                        mergeSnapshotChunk(cache, chunk, snapshot)
                        emitPresences()
                    }
            }

        val tickerJob = launch {
            while (isActive) {
                delay(PRESENCE_REFRESH_MS)
                emitPresences()
            }
        }

        emitPresences()

        awaitClose {
            tickerJob.cancel()
            listeners.forEach(ListenerRegistration::remove)
        }
    }

    override fun observeOnlineUsersCount(excludedUserId: String?): Flow<Int> = callbackFlow {
        val cache = linkedMapOf<String, UserPresence>()

        fun emitCount() {
            val now = System.currentTimeMillis()
            val count = cache.values.count { presence ->
                presence.userId != excludedUserId && presence.isOnlineNow(nowMillis = now)
            }
            trySend(count)
        }

        val listener = usersCollection
            .whereEqualTo("isAppOnline", true)
            .addSnapshotListener { snapshot, _ ->
                cache.clear()
                snapshot?.documents
                    ?.map(::toUserPresence)
                    ?.forEach { cache[it.userId] = it }
                emitCount()
            }

        val tickerJob = launch {
            while (isActive) {
                delay(PRESENCE_REFRESH_MS)
                emitCount()
            }
        }

        emitCount()

        awaitClose {
            tickerJob.cancel()
            listener.remove()
        }
    }

    private fun mergeSnapshotChunk(
        cache: MutableMap<String, UserPresence>,
        chunk: List<String>,
        snapshot: QuerySnapshot?
    ) {
        chunk.forEach(cache::remove)
        snapshot?.documents
            ?.map(::toUserPresence)
            ?.forEach { cache[it.userId] = it }
    }

    private fun toUserPresence(document: com.google.firebase.firestore.DocumentSnapshot): UserPresence {
        val rawLastActiveAt = document.getLong("lastActiveAt") ?: 0L
        val rawIsOnline = document.getBoolean("isAppOnline") ?: false
        return UserPresence(
            userId = document.id,
            userName = document.getString("anonymousName").orEmpty().ifBlank { "SoulMate User" },
            userAvatarUrl = document.getString("avatarUrl"),
            isAppOnline = rawIsOnline,
            lastActiveAt = rawLastActiveAt
        )
    }
}
