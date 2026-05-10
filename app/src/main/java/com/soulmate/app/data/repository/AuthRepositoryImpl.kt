package com.soulmate.app.data.repository

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
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

    override suspend fun register(email: String, password: String): Result<User> = try {
        val authResult = auth.createUserWithEmailAndPassword(email, password).await()
        val uid = authResult.user?.uid ?: throw Exception("Không thể lấy UID sau khi đăng ký")

        val newUser = User(
            userId = uid,
            email = email,
            anonymousName = "User_${uid.take(5)}",
            createdAt = System.currentTimeMillis(),
            updatedAt = System.currentTimeMillis()
        )

        usersCollection.document(uid).set(newUser).await()
        Result.success(newUser)
    } catch (e: Exception) {
        Result.failure(e)
    }

    override suspend fun login(email: String, password: String): Result<User> = try {
        val authResult = auth.signInWithEmailAndPassword(email, password).await()
        val uid = authResult.user?.uid ?: throw Exception("Đăng nhập thất bại")

        usersCollection.document(uid).update("lastLoginAt", System.currentTimeMillis()).await()

        val snapshot = usersCollection.document(uid).get().await()
        val user = snapshot.toObject(User::class.java) ?: throw Exception("Không tìm thấy profile")

        Result.success(user)
    } catch (e: Exception) {
        Result.failure(e)
    }

    override suspend fun signInWithGoogle(idToken: String): Result<User> = try {
        val credential = GoogleAuthProvider.getCredential(idToken, null)
        val authResult = auth.signInWithCredential(credential).await()
        val firebaseUser = authResult.user ?: throw Exception("Đăng nhập Google thất bại")
        val uid = firebaseUser.uid

        // Kiểm tra xem user đã tồn tại trong Firestore chưa
        val snapshot = usersCollection.document(uid).get().await()
        val existingUser = snapshot.toObject(User::class.java)

        val user = if (existingUser == null) {
            // Nếu là user mới, tạo profile mới với thông tin từ Google
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
            // Nếu user đã tồn tại, chỉ cập nhật name, avatar và lastLoginAt nếu cần
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
            User(
                userId = firebaseUser.uid,
                email = firebaseUser.email ?: "",
                anonymousName = firebaseUser.displayName ?: "SoulMate User",
                avatarUrl = firebaseUser.photoUrl?.toString()
            )
        } else null
    }

    override fun getCurrentUserId(): String? {
        return auth.currentUser?.uid
    }

    override suspend fun getUserProfile(uid: String): Result<User> = try {
        val snapshot = usersCollection.document(uid).get().await()
        val user = snapshot.toObject(User::class.java) ?: throw Exception("Không tìm thấy profile")
        Result.success(user)
    } catch (e: Exception) {
        Result.failure(e)
    }
}