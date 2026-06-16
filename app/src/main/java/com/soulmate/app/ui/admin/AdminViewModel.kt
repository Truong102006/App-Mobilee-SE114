package com.soulmate.app.ui.admin

import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.soulmate.app.domain.model.User
import com.soulmate.app.domain.repository.IAuthRepository
import com.soulmate.app.domain.repository.ICommunityRepository
import com.soulmate.app.ui.social.CommunityPost
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AdminViewModel @Inject constructor(
    private val communityRepository: ICommunityRepository,
    private val authRepository: IAuthRepository
) : ViewModel() {

    private val _reportedPosts = mutableStateOf<List<CommunityPost>>(emptyList())
    val reportedPosts: State<List<CommunityPost>> = _reportedPosts

    private val _isLoadingPosts = mutableStateOf(false)
    val isLoadingPosts: State<Boolean> = _isLoadingPosts

    private val _searchQuery = mutableStateOf("")
    val searchQuery: State<String> = _searchQuery

    private val _searchResults = mutableStateOf<List<User>>(emptyList())
    val searchResults: State<List<User>> = _searchResults

    private val _isSearching = mutableStateOf(false)
    val isSearching: State<Boolean> = _isSearching

    init {
        loadReportedPosts()
    }

    fun loadReportedPosts() {
        viewModelScope.launch {
            _isLoadingPosts.value = true
            communityRepository.getReportedPosts().collect { posts ->
                _reportedPosts.value = posts
                _isLoadingPosts.value = false
            }
        }
    }

    fun resolveReport(postId: String, action: String) {
        viewModelScope.launch {
            communityRepository.resolveReport(postId, action).onSuccess {
                loadReportedPosts()
            }
        }
    }

    fun onSearchQueryChange(query: String) {
        _searchQuery.value = query
        if (query.length >= 2) {
            searchUsers(query)
        } else {
            _searchResults.value = emptyList()
        }
    }

    private fun searchUsers(query: String) {
        viewModelScope.launch {
            _isSearching.value = true
            authRepository.searchUsers(query).onSuccess { users ->
                _searchResults.value = users
            }
            _isSearching.value = false
        }
    }

    fun toggleSocialBan(targetUserId: String, isBanned: Boolean) {
        viewModelScope.launch {
            authRepository.toggleSocialBan(targetUserId, isBanned).onSuccess {
                _searchResults.value = _searchResults.value.map {
                    if (it.userId == targetUserId) it.copy(isSocialBanned = isBanned) else it
                }
            }
        }
    }
}