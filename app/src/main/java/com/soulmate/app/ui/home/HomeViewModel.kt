package com.soulmate.app.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.soulmate.app.domain.repository.IAuthRepository
import com.soulmate.app.domain.repository.IDiaryRepository
import com.soulmate.app.domain.repository.IUserRepository
import com.soulmate.app.domain.repository.IPetRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.Calendar
import javax.inject.Inject

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val authRepository: IAuthRepository,
    private val diaryRepository: IDiaryRepository,
    private val userRepository: IUserRepository,
    private val petRepository: IPetRepository
) : ViewModel() {

    data class HomeUiState(
        val userName: String = "",
        val greeting: String = "",
        val currentMood: String? = null,
        val recentDiaryCount: Int = 0,
        val petLevel: Int = 1,
        val streak: Int = 0,
        val isLoading: Boolean = true,
        val errorMessage: String? = null
    )

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    init {
        loadHomeData()
    }

    fun loadHomeData() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)
            val currentUserId = authRepository.getCurrentUserId() ?: ""
            if (currentUserId.isBlank()) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    errorMessage = "Bạn chưa đăng nhập"
                )
                return@launch
            }

            try {
                // Time based greeting
                val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
                val greeting = when (hour) {
                    in 5..11 -> "Chào buổi sáng"
                    in 12..17 -> "Chào buổi chiều"
                    else -> "Chào buổi tối"
                }

                // Fetch user data
                val userResult = userRepository.getCurrentUser(currentUserId)
                val user = userResult.getOrNull()
                val userName = user?.anonymousName ?: "Bạn"

                // Fetch diaries count
                var diaryCount = 0
                val diariesResult = diaryRepository.loadDiaries()
                if (diariesResult.isSuccess) {
                    diaryCount = diariesResult.getOrNull()?.size ?: 0
                }

                // Fetch pet stats
                var petLevel = 1
                var petStreak = 0
                val petResult = petRepository.getPet(currentUserId)
                if (petResult.isSuccess) {
                    val pet = petResult.getOrNull()
                    if (pet != null) {
                        petLevel = pet.level
                        petStreak = pet.currentStreak
                    }
                }

                _uiState.value = _uiState.value.copy(
                    userName = userName,
                    greeting = greeting,
                    currentMood = user?.currentMood,
                    recentDiaryCount = diaryCount,
                    petLevel = petLevel,
                    streak = petStreak,
                    isLoading = false
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    errorMessage = e.localizedMessage ?: "Tải dữ liệu trang chủ thất bại"
                )
            }
        }
    }
}