package com.soulmate.app.ui.setting

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.soulmate.app.domain.repository.ISettingsRepository
import com.soulmate.app.domain.repository.IUserRepository
import com.soulmate.app.notifications.AppNotificationManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val settingsRepository: ISettingsRepository,
    private val userRepository: IUserRepository,
    private val firebaseAuth: FirebaseAuth,
    private val notificationManager: AppNotificationManager
) : ViewModel() {

    companion object {
        private const val TAG = "SettingsViewModel"
    }

    val notificationEnabled: StateFlow<Boolean> = settingsRepository.notificationEnabled
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = true
        )

    fun toggleNotification(isEnabled: Boolean) {
        viewModelScope.launch {
            settingsRepository.toggleNotification(isEnabled)
            notificationManager.applyNotificationPreference(
                enabled = isEnabled,
                requestPermissionIfNeeded = isEnabled
            )

            val currentUserId = firebaseAuth.currentUser?.uid
            if (!currentUserId.isNullOrBlank()) {
                userRepository.updateNotificationPreference(currentUserId, isEnabled)
                    .onFailure { error ->
                        Log.w(TAG, "Failed to sync notification preference for user=$currentUserId", error)
                    }
            }
        }
    }
}
