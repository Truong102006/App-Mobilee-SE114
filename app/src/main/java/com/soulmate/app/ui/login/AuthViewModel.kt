package com.soulmate.app.ui.login

import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.soulmate.app.domain.repository.IAuthRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AuthViewModel @Inject constructor(
    private val authRepository: IAuthRepository // Đã đổi từ FirebaseAuth sang IAuthRepository
) : ViewModel() {

    private val _isLoading = mutableStateOf(false)
    val isLoading: State<Boolean> = _isLoading

    private val _error = MutableSharedFlow<String>()
    val error = _error.asSharedFlow()

    private val _authSuccess = MutableSharedFlow<Unit>()
    val authSuccess = _authSuccess.asSharedFlow()

    fun login(email: String, password: String) {
        if (email.isBlank() || password.isBlank()) {
            viewModelScope.launch { _error.emit("Vui lòng điền đầy đủ thông tin") }
            return
        }

        viewModelScope.launch {
            _isLoading.value = true
            // Sử dụng repository để đảm bảo có cập nhật lastLoginAt
            authRepository.login(email, password)
                .onSuccess {
                    _authSuccess.emit(Unit)
                }
                .onFailure {
                    _error.emit(it.localizedMessage ?: "Đăng nhập thất bại")
                }
            _isLoading.value = false
        }
    }

    fun register(name: String, email: String, password: String, confirmPass: String) {
        if (email.isBlank() || password.isBlank() || name.isBlank()) {
            viewModelScope.launch { _error.emit("Vui lòng điền đầy đủ thông tin") }
            return
        }
        if (password != confirmPass) {
            viewModelScope.launch { _error.emit("Mật khẩu xác nhận không khớp") }
            return
        }

        viewModelScope.launch {
            _isLoading.value = true
            // Sử dụng repository.register để lưu cả vào Auth và Firestore
            authRepository.register(email, password)
                .onSuccess {
                    _authSuccess.emit(Unit)
                }
                .onFailure {
                    _error.emit(it.localizedMessage ?: "Đăng ký thất bại")
                }
            _isLoading.value = false
        }
    }
}
