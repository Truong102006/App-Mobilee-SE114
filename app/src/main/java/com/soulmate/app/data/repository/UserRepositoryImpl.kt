package com.soulmate.app.data.repository

import com.google.firebase.firestore.FirebaseFirestore
import com.soulmate.app.domain.model.User
import com.soulmate.app.domain.repository.IUserRepository
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class UserRepositoryImpl @Inject constructor(
    private val firestore: FirebaseFirestore
) : IUserRepository {

    private val usersCollection = firestore.collection("users")

    override suspend fun createUserProfile(user: User): Result<Unit> = try {
        usersCollection.document(user.userId).set(user).await()
        Result.success(Unit)
    } catch (e: Exception) {
        Result.failure(e)
    }

    override suspend fun getCurrentUser(userId: String): Result<User?> = try {
        val snapshot = usersCollection.document(userId).get().await()
        val user = snapshot.toObject(User::class.java)
        Result.success(user)
    } catch (e: Exception) {
        Result.failure(e)
    }

    override suspend fun updateAnonymousProfile(userId: String, name: String, avatarUrl: String?): Result<Unit> = try {
        val updates = mapOf(
            "anonymousName" to name,
            "avatarUrl" to avatarUrl,
            "updatedAt" to System.currentTimeMillis()
        )
        usersCollection.document(userId).update(updates).await()
        Result.success(Unit)
    } catch (e: Exception) {
        Result.failure(e)
    }

    override suspend fun updateSettings(userId: String, notification: Boolean, reminder: Boolean, time: String?): Result<Unit> = try {
        val updates = mapOf(
            "notificationEnabled" to notification,
            "reminderEnabled" to reminder,
            "reminderTime" to time,
            "updatedAt" to System.currentTimeMillis()
        )
        usersCollection.document(userId).update(updates).await()
        Result.success(Unit)
    } catch (e: Exception) {
        Result.failure(e)
    }

    override suspend fun updateCurrentMood(userId: String, mood: String): Result<Unit> = try {
        usersCollection.document(userId).update(
            "currentMood", mood,
            "updatedAt", System.currentTimeMillis()
        ).await()
        Result.success(Unit)
    } catch (e: Exception) {
        Result.failure(e)
    }

    override suspend fun checkIsAdmin(userId: String): Boolean = try {
        val snapshot = usersCollection.document(userId).get().await()
        snapshot.getString("role") == "admin"
    } catch (e: Exception) {
        false
    }

    override suspend fun updateLastLogin(userId: String): Result<Unit> = try {
        usersCollection.document(userId).update("lastLoginAt", System.currentTimeMillis()).await()
        Result.success(Unit)
    } catch (e: Exception) {
        Result.failure(e)
    }
}