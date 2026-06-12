package com.soulmate.app.ui.pet

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.soulmate.app.domain.model.SoulPet
import com.soulmate.app.domain.repository.IAuthRepository
import com.soulmate.app.domain.repository.IPetRepository
import com.soulmate.app.domain.usecase.CalculatePetXPUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class PetViewModel @Inject constructor(
    private val petRepository: IPetRepository,
    private val calculateXP: CalculatePetXPUseCase,
    private val authRepository: IAuthRepository
) : ViewModel() {

    data class PetUiState(
        val pet: SoulPet? = null,
        val isLoading: Boolean = true,
        val showNameDialog: Boolean = false,
        val xpProgress: Float = 0f,
        val levelUpAnimation: Boolean = false,
        val errorMessage: String? = null
    )

    private val _uiState = MutableStateFlow(PetUiState())
    val uiState: StateFlow<PetUiState> = _uiState.asStateFlow()

    init {
        loadPet()
    }

    fun loadPet() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)
            val currentUserId = authRepository.getCurrentUserId() ?: ""
            if (currentUserId.isBlank()) {
                _uiState.value = _uiState.value.copy(isLoading = false, errorMessage = "Bạn chưa đăng nhập")
                return@launch
            }
            petRepository.getPet(currentUserId).onSuccess { pet ->
                if (pet == null) {
                    _uiState.value = _uiState.value.copy(isLoading = false, showNameDialog = true)
                } else {
                    val progress = if (pet.maxXp > 0) pet.xp.toFloat() / pet.maxXp else 0f
                    val oldPet = _uiState.value.pet
                    val triggeredLevelUp = oldPet != null && pet.level > oldPet.level
                    _uiState.value = _uiState.value.copy(
                        pet = pet,
                        isLoading = false,
                        showNameDialog = false,
                        xpProgress = progress,
                        levelUpAnimation = triggeredLevelUp
                    )
                }
            }.onFailure { e ->
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    errorMessage = e.localizedMessage ?: "Không thể tải thông tin thú cưng"
                )
            }
        }
    }

    fun createPet(name: String) {
        if (name.isBlank()) return
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)
            val currentUserId = authRepository.getCurrentUserId() ?: ""
            if (currentUserId.isBlank()) {
                _uiState.value = _uiState.value.copy(isLoading = false, errorMessage = "Bạn chưa đăng nhập")
                return@launch
            }
            petRepository.createPet(currentUserId, name).onSuccess { pet ->
                _uiState.value = _uiState.value.copy(
                    pet = pet,
                    isLoading = false,
                    showNameDialog = false,
                    xpProgress = 0f
                )
            }.onFailure { e ->
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    errorMessage = e.localizedMessage ?: "Tạo thú cưng thất bại"
                )
            }
        }
    }

    fun onLevelUpAnimationShown() {
        _uiState.value = _uiState.value.copy(levelUpAnimation = false)
    }

    fun onErrorHandled() {
        _uiState.value = _uiState.value.copy(errorMessage = null)
    }
}
