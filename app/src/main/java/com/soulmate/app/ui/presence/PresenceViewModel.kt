package com.soulmate.app.ui.presence

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.soulmate.app.domain.model.UserPresence
import com.soulmate.app.domain.repository.IUserPresenceRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

@HiltViewModel
@OptIn(ExperimentalCoroutinesApi::class)
class PresenceViewModel @Inject constructor(
    repository: IUserPresenceRepository,
    auth: FirebaseAuth
) : ViewModel() {

    private val observedUserIds = MutableStateFlow<Set<String>>(emptySet())

    val userPresences: StateFlow<Map<String, UserPresence>> = observedUserIds
        .flatMapLatest(repository::observePresences)
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000L),
            initialValue = emptyMap()
        )

    val onlineUsersCount: StateFlow<Int> = repository
        .observeOnlineUsersCount(auth.currentUser?.uid)
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000L),
            initialValue = 0
        )

    fun observeUsers(userIds: Set<String>) {
        observedUserIds.value = userIds
            .map(String::trim)
            .filter(String::isNotEmpty)
            .toSet()
    }
}
