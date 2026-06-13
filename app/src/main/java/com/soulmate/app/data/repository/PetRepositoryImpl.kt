package com.soulmate.app.data.repository

import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.soulmate.app.domain.model.SoulPet
import com.soulmate.app.domain.repository.IPetRepository
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PetRepositoryImpl @Inject constructor(
    private val firestore: FirebaseFirestore
) : IPetRepository {

    private val petsCollection = firestore.collection("pets")

    override suspend fun getPet(userId: String): Result<SoulPet?> = try {
        val doc = petsCollection.document(userId).get().await()
        if (doc.exists()) {
            Result.success(doc.toObject(SoulPet::class.java))
        } else {
            Result.success(null)
        }
    } catch (e: Exception) {
        Result.failure(e)
    }

    override suspend fun createPet(userId: String, petName: String): Result<SoulPet> = try {
        val pet = SoulPet(
            id = userId,
            userId = userId,
            name = petName,
            createdAt = System.currentTimeMillis(),
            updatedAt = System.currentTimeMillis()
        )
        petsCollection.document(userId).set(pet).await()
        Result.success(pet)
    } catch (e: Exception) {
        Result.failure(e)
    }

    override suspend fun addXP(userId: String, xpAmount: Int, source: String): Result<SoulPet> = try {
        val docRef = petsCollection.document(userId)
        docRef.update(
            mapOf(
                "xp" to FieldValue.increment(xpAmount.toLong()),
                "updatedAt" to System.currentTimeMillis()
            )
        ).await()
        val updated = docRef.get().await().toObject(SoulPet::class.java)
            ?: throw Exception("Pet not found after XP update")
        
        var level = 1
        var rem = updated.xp
        var threshold = 100
        while (rem >= threshold) {
            rem -= threshold
            level++
            threshold = (threshold * 1.3).toInt()
        }
        val finalPet = updated.copy(level = level, maxXp = threshold)
        docRef.update("level", level, "maxXp", threshold).await()
        Result.success(finalPet)
    } catch (e: Exception) {
        Result.failure(e)
    }

    override suspend fun updatePetMood(userId: String, mood: String): Result<Unit> = try {
        petsCollection.document(userId).update(
            "mood", mood,
            "updatedAt", System.currentTimeMillis()
        ).await()
        Result.success(Unit)
    } catch (e: Exception) {
        Result.failure(e)
    }

    override suspend fun updateStreak(userId: String): Result<SoulPet> = try {
        val docRef = petsCollection.document(userId)
        docRef.update(
            mapOf(
                "currentStreak" to FieldValue.increment(1),
                "updatedAt" to System.currentTimeMillis()
            )
        ).await()
        val updated = docRef.get().await().toObject(SoulPet::class.java)
            ?: throw Exception("Pet not found after streak update")
        Result.success(updated)
    } catch (e: Exception) {
        Result.failure(e)
    }
}
