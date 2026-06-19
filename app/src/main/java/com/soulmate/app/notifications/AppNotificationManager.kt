package com.soulmate.app.notifications

import android.content.Context
import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.onesignal.OneSignal
import com.onesignal.notifications.INotificationClickEvent
import com.onesignal.notifications.INotificationClickListener
import com.soulmate.app.BuildConfig
import com.soulmate.app.domain.model.User
import com.soulmate.app.domain.repository.ISettingsRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import org.json.JSONObject
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AppNotificationManager @Inject constructor(
    @ApplicationContext private val appContext: Context,
    private val settingsRepository: ISettingsRepository
) {

    companion object {
        private const val TAG = "AppNotificationManager"
    }

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private val oneSignalAppId = BuildConfig.ONESIGNAL_APP_ID.trim()
    private var initialized = false
    private val notificationClickListener = object : INotificationClickListener {
        override fun onClick(event: INotificationClickEvent) {
            _pendingNavigation.value = parseDestination(event.notification.additionalData)
        }
    }

    private val _pendingNavigation = MutableStateFlow<NotificationDestination?>(null)
    val pendingNavigation: StateFlow<NotificationDestination?> = _pendingNavigation.asStateFlow()

    fun initialize() {
        if (initialized) {
            return
        }
        initialized = true

        if (!isConfigured()) {
            Log.w(TAG, "OneSignal App ID is empty. Push notifications are disabled until ONESIGNAL_APP_ID is configured.")
            return
        }

        OneSignal.initWithContext(appContext, oneSignalAppId)
        OneSignal.Notifications.addClickListener(notificationClickListener)

        FirebaseAuth.getInstance().currentUser?.uid
            ?.takeIf { it.isNotBlank() }
            ?.let(OneSignal::login)

        scope.launch {
            settingsRepository.notificationEnabled.collectLatest { enabled ->
                applyNotificationPreference(enabled = enabled, requestPermissionIfNeeded = false)
            }
        }
    }

    fun onUserAuthenticated(user: User, requestPermissionIfNeeded: Boolean) {
        if (!isConfigured() || user.userId.isBlank()) {
            return
        }

        OneSignal.login(user.userId)
        applyNotificationPreference(
            enabled = user.notificationEnabled,
            requestPermissionIfNeeded = requestPermissionIfNeeded
        )
    }

    fun applyNotificationPreference(enabled: Boolean, requestPermissionIfNeeded: Boolean) {
        if (!isConfigured()) {
            return
        }

        if (!enabled) {
            OneSignal.User.pushSubscription.optOut()
            return
        }

        if (requestPermissionIfNeeded) {
            OneSignal.User.pushSubscription.optIn()
            return
        }

        if (OneSignal.Notifications.permission) {
            OneSignal.User.pushSubscription.optIn()
        }
    }

    fun logout() {
        if (!isConfigured()) {
            return
        }
        OneSignal.logout()
    }

    fun consumePendingNavigation() {
        _pendingNavigation.value = null
    }

    private fun parseDestination(data: JSONObject?): NotificationDestination? {
        if (data == null) {
            return NotificationDestination.Home
        }

        return when (data.optString("screen")) {
            "chat" -> {
                val userId = data.optString("userId").trim()
                if (userId.isBlank()) {
                    null
                } else {
                    NotificationDestination.Chat(
                        userId = userId,
                        userName = data.optString("userName").ifBlank { "SoulMate User" },
                        avatarUrl = data.optString("avatarUrl").takeIf { it.isNotBlank() }
                    )
                }
            }

            "community" -> NotificationDestination.Home
            else -> NotificationDestination.Home
        }
    }

    private fun isConfigured(): Boolean = oneSignalAppId.isNotBlank()
}

sealed interface NotificationDestination {
    data object Home : NotificationDestination

    data class Chat(
        val userId: String,
        val userName: String,
        val avatarUrl: String?
    ) : NotificationDestination
}
