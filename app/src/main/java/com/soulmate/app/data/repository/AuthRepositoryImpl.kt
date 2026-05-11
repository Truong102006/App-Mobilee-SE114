package com.soulmate.app.data.repository

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.UserProfileChangeRequest
import com.google.firebase.firestore.FirebaseFirestore
import com.soulmate.app.domain.model.User
import com.soulmate.app.domain.repository.IAuthRepository
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AuthRepositoryImpl @Inject constructor(
    private val auth: FirebaseAuth,
    private val firestore: FirebaseFirestore
) : IAuthRepository {

    private val usersCollection = firestore.collection("users")

    override suspend fun register(name: String, email: String, password: String): Result<User> = try {
        val authResult = auth.createUserWithEmailAndPassword(email, password).await()
        val firebaseUser = authResult.user ?: throw Exception("Khong the lay user sau khi dang ky")
        val uid = firebaseUser.uid
        val normalizedName = name.trim()
        val displayName = normalizedName.ifBlank { "User_${uid.take(5)}" }

        val profileUpdateRequest = UserProfileChangeRequest.Builder()
            .setDisplayName(displayName)
            .build()
        firebaseUser.updateProfile(profileUpdateRequest).await()

        val now = System.currentTimeMillis()
        val newUser = User(
            userId = uid,
            email = email,
            anonymousName = displayName,
            createdAt = now,
            updatedAt = now,
            lastLoginAt = now
        )

        usersCollection.document(uid).set(newUser).await()
        Result.success(newUser)
    } catch (e: Exception) {
        Result.failure(e)
    }

    override suspend fun login(email: String, password: String): Result<User> = try {
        val authResult = auth.signInWithEmailAndPassword(email, password).await()
        val firebaseUser = authResult.user ?: throw Exception("Dang nhap that bai")
        val uid = firebaseUser.uid
        val now = System.currentTimeMillis()

        usersCollection.document(uid).update("lastLoginAt", now).await()

        val snapshot = usersCollection.document(uid).get().await()
        val user = snapshot.toObject(User::class.java) ?: throw Exception("Khong tim thay profile")

        if (firebaseUser.displayName.isNullOrBlank() && user.anonymousName.isNotBlank()) {
            val profileUpdateRequest = UserProfileChangeRequest.Builder()
                .setDisplayName(user.anonymousName)
                .build()
            firebaseUser.updateProfile(profileUpdateRequest).await()
        }

        Result.success(user)
    } catch (e: Exception) {
        Result.failure(e)
    }

    override suspend fun signInWithGoogle(idToken: String): Result<User> = try {
        val credential = GoogleAuthProvider.getCredential(idToken, null)
        val authResult = auth.signInWithCredential(credential).await()
        val firebaseUser = authResult.user ?: throw Exception("Dang nhap Google that bai")
        val uid = firebaseUser.uid

        val snapshot = usersCollection.document(uid).get().await()
        val existingUser = snapshot.toObject(User::class.java)

        val user = if (existingUser == null) {
            User(
                userId = uid,
                email = firebaseUser.email ?: "",
                anonymousName = firebaseUser.displayName ?: "SoulMate User",
                avatarUrl = firebaseUser.photoUrl?.toString(),
                createdAt = System.currentTimeMillis(),
                updatedAt = System.currentTimeMillis(),
                lastLoginAt = System.currentTimeMillis()
            ).also {
                usersCollection.document(uid).set(it).await()
            }
        } else {
            val updatedUser = existingUser.copy(
                anonymousName = firebaseUser.displayName ?: existingUser.anonymousName,
                avatarUrl = firebaseUser.photoUrl?.toString() ?: existingUser.avatarUrl,
                lastLoginAt = System.currentTimeMillis(),
                updatedAt = System.currentTimeMillis()
            )
            usersCollection.document(uid).set(updatedUser).await()
            updatedUser
        }

        Result.success(user)
    } catch (e: Exception) {
        Result.failure(e)
    }

    override fun logout(): Result<Unit> = try {
        auth.signOut()
        Result.success(Unit)
    } catch (e: Exception) {
        Result.failure(e)
    }

    override fun getCurrentUser(): User? {
        val firebaseUser = auth.currentUser
        return if (firebaseUser != null) {
            val fallbackName = "User_${firebaseUser.uid.take(5)}"
            User(
                userId = firebaseUser.uid,
                email = firebaseUser.email ?: "",
                anonymousName = firebaseUser.displayName?.ifBlank { null } ?: fallbackName,
                avatarUrl = firebaseUser.photoUrl?.toString()
            )
        } else {
            null
        }
    }

    override fun getCurrentUserId(): String? {
        return auth.currentUser?.uid
    }

    override suspend fun getUserProfile(uid: String): Result<User> = try {
        val snapshot = usersCollection.document(uid).get().await()
        val user = snapshot.toObject(User::class.java) ?: throw Exception("Khong tim thay profile")
        Result.success(user)
    } catch (e: Exception) {
        Result.failure(e)
    }

    override suspend fun updateUserProfile(user: User): Result<Unit> {
        TODO("Not yet implemented")
    }
}