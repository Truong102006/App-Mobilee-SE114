package com.soulmate.app.presence

import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ProcessLifecycleOwner
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PresenceSyncManager @Inject constructor(
    private val auth: FirebaseAuth,
    firestore: FirebaseFirestore
) : DefaultLifecycleObserver {

    companion object {
        private const val HEARTBEAT_INTERVAL_MS = 30_000L
    }

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val usersCollection = firestore.collection("users")

    @Volatile
    private var isForeground = false

    private var heartbeatJob: Job? = null

    fun start() {
        ProcessLifecycleOwner.get().lifecycle.addObserver(this)
    }

    fun onUserAuthenticated() {
        if (isForeground) {
            markOnline()
            startHeartbeat()
        }
    }

    fun markOfflineForLogout() {
        heartbeatJob?.cancel()
        heartbeatJob = null
        runBlocking(Dispatchers.IO) {
            updatePresence(isOnline = false)
        }
    }

    override fun onStart(owner: LifecycleOwner) {
        isForeground = true
        markOnline()
        startHeartbeat()
    }

    override fun onStop(owner: LifecycleOwner) {
        isForeground = false
        heartbeatJob?.cancel()
        heartbeatJob = null
        scope.launch {
            updatePresence(isOnline = false)
        }
    }

    private fun markOnline() {
        scope.launch {
            updatePresence(isOnline = true)
        }
    }

    private fun startHeartbeat() {
        heartbeatJob?.cancel()
        heartbeatJob = scope.launch {
            while (isActive && isForeground) {
                updatePresence(isOnline = true)
                delay(HEARTBEAT_INTERVAL_MS)
            }
        }
    }

    private suspend fun updatePresence(isOnline: Boolean) {
        val uid = auth.currentUser?.uid ?: return
        val now = System.currentTimeMillis()

        usersCollection.document(uid)
            .set(
                mapOf(
                    "isAppOnline" to isOnline,
                    "lastActiveAt" to now,
                    "updatedAt" to now
                ),
                SetOptions.merge()
            )
            .await()
    }
}
