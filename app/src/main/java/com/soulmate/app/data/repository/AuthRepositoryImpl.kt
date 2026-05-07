package com.soulmate.app.data.repository

import com.google.firebase.auth.FirebaseAuth
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
        // 1. Tạo user trên Firebase Auth
        val authResult = auth.createUserWithEmailAndPassword(email, password).await()
        val uid = authResult.user?.uid ?: throw Exception("Không thể lấy UID sau khi đăng ký")

        // 2. Tạo đối tượng User model mới
        val newUser = User(
            userId = uid,
            email = email,
            anonymousName = "User_${uid.take(5)}",
            createdAt = System.currentTimeMillis(),
            updatedAt = System.currentTimeMillis()
        )

        // 3. Lưu vào Firestore
        usersCollection.document(uid).set(newUser).await()

        Result.success(newUser)
    } catch (e: Exception) {
        Result.failure(e)
    }

    override suspend fun login(email: String, password: String): Result<User> = try {
        // 1. Đăng nhập bằng Firebase Auth
        val authResult = auth.signInWithEmailAndPassword(email, password).await()
        val uid = authResult.user?.uid ?: throw Exception("Đăng nhập thất bại")

        // --- BỔ SUNG: Cập nhật thời gian đăng nhập cuối cùng ---
        usersCollection.document(uid).update("lastLoginAt", System.currentTimeMillis()).await()
        // ----------------------------------------------------

        // 2. Lấy thông tin chi tiết từ Firestore
        val snapshot = usersCollection.document(uid).get().await()
        val user = snapshot.toObject(User::class.java) ?: throw Exception("Không tìm thấy profile")

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
            // Lưu ý: Vì hàm này không phải suspend, nên chỉ trả về thông tin cơ bản
            // Để lấy full profile từ Firestore, bạn nên dùng IUserRepository.getCurrentUser(uid)
            User(
                userId = firebaseUser.uid,
                email = firebaseUser.email ?: ""
            )
        } else null
    }

    override fun getCurrentUserId(): String? {
        return auth.currentUser?.uid
    }
}