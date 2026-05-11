package com.soulmate.app.ui.stats

import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.soulmate.app.domain.model.Diary
import com.soulmate.app.domain.repository.IDiaryRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import javax.inject.Inject

data class StatsUiState(
    val isLoading: Boolean = true,
    val diaries: List<Diary> = emptyList(),
    val errorMessage: String? = null
)

@HiltViewModel
class StatsViewModel @Inject constructor(
    private val diaryRepository: IDiaryRepository,
    private val firebaseAuth: FirebaseAuth
) : ViewModel() {

    private val _uiState = mutableStateOf(StatsUiState())
    val uiState: State<StatsUiState> = _uiState

    init {
        observeDiaries()
    }

    private fun observeDiaries() {
        viewModelScope.launch {
            val currentUid = firebaseAuth.currentUser?.uid
            if (currentUid.isNullOrBlank()) {
                _uiState.value = StatsUiState(
                    isLoading = false,
                    diaries = emptyList(),
                    errorMessage = "Người dùng chưa đăng nhập"
                )
                return@launch
            }

            diaryRepository.getDiaries(currentUid)
                .catch { throwable ->
                    _uiState.value = StatsUiState(
                        isLoading = false,
                        diaries = emptyList(),
                        errorMessage = throwable.message
                    )
                }
                .collectLatest { diaries ->
                    _uiState.value = StatsUiState(
                        isLoading = false,
                        diaries = diaries,
                        errorMessage = null
                    )
                }
        }
    }
}
