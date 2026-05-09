package com.soulmate.app.ui.journal.editor

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.soulmate.app.domain.model.Diary
import com.soulmate.app.domain.usecase.AnalyzeMoodUseCase
import com.soulmate.app.domain.usecase.SaveDiaryUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class DiaryViewModel @Inject constructor(
    private val analyzeMoodUseCase: AnalyzeMoodUseCase,
    private val saveDiaryUseCase: SaveDiaryUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(DiaryUiState())
    val uiState = _uiState.asStateFlow()

    private var currentDiaryId: String? = null

    fun setDiaryId(id: String?) {
        currentDiaryId = id
    }

    fun onTextChanged(newText: String) {
        _uiState.value = _uiState.value.copy(text = newText)
    }

    fun onMoodSelected(mood: String?) {
        _uiState.value = _uiState.value.copy(selectedMood = mood ?: "Neutral")
    }

    fun onImagesChanged(images: List<String>) {
        _uiState.value = _uiState.value.copy(imageUrls = images)
    }

    fun analyzeMoodFromText(text: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            analyzeMoodUseCase(text).onSuccess { mood ->
                _uiState.value = _uiState.value.copy(
                    selectedMood = mood,
                    isLoading = false
                )
            }.onFailure {
                _uiState.value = _uiState.value.copy(isLoading = false, error = it.message)
            }
        }
    }

    fun saveDiary(diaryId: String? = null) {
        viewModelScope.launch {
            val currentState = _uiState.value
            
            if (currentState.text.isBlank()) {
                _uiState.value = _uiState.value.copy(error = "N\u1ED9i dung nh\u1EADt k\u00FD kh\u00F4ng \u0111\u01B0\u1EE3c \u0111\u1EC3 tr\u1ED1ng")
                return@launch
            }

            // Get UID of current signed-in user
            val currentUid = FirebaseAuth.getInstance().currentUser?.uid ?: ""

            val diary = Diary(
                diaryId = diaryId ?: currentDiaryId ?: "",
                userId = currentUid,
                content = currentState.text,
                moodTag = currentState.selectedMood,
                imageUrls = currentState.imageUrls,
                createdAt = System.currentTimeMillis()
            )
            
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            saveDiaryUseCase(diary).onSuccess {
                _uiState.value = _uiState.value.copy(isLoading = false, isSaved = true)
            }.onFailure {
                _uiState.value = _uiState.value.copy(isLoading = false, error = it.message)
            }
        }
    }

    fun onSaveCompleteHandled() {
        _uiState.value = _uiState.value.copy(isSaved = false)
    }

    fun onErrorHandled() {
        _uiState.value = _uiState.value.copy(error = null)
    }
}

data class DiaryUiState(
    val text: String = "",
    val selectedMood: String = "Neutral",
    val imageUrls: List<String> = emptyList(),
    val isLoading: Boolean = false,
    val isSaved: Boolean = false,
    val error: String? = null
)
