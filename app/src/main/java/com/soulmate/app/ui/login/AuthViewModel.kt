package com.soulmate.app.ui.login

import android.net.Uri
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.soulmate.app.domain.model.User
import com.soulmate.app.domain.repository.IAuthRepository
import com.soulmate.app.utils.CloudinaryHelper
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AuthViewModel @Inject constructor(
    private val authRepository: IAuthRepository
) : ViewModel() {

    private val _isLoading = mutableStateOf(false)
    val isLoading: State<Boolean> = _isLoading

    private val _error = MutableSharedFlow<String>()
    val error = _error.asSharedFlow()

    private val _authSuccess = MutableSharedFlow<Unit>()
    val authSuccess = _authSuccess.asSharedFlow()
    
    private val _currentUser = mutableStateOf<User?>(null)
    val currentUser: State<User?> = _currentUser

    init {
        _currentUser.value = authRepository.getCurrentUser()
    }

    fun login(email: String, password: String) {
        if (email.isBlank() || password.isBlank()) {
            viewModelScope.launch { _error.emit("Vui lòng điền đầy đủ thông tin") }
            return
        }

        viewModelScope.launch {
            _isLoading.value = true
            authRepository.login(email, password)
                .onSuccess {
                    _currentUser.value = it
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
            authRepository.register(name = name.trim(), email = email, password = password)
                .onSuccess {
                    _currentUser.value = it
                    _authSuccess.emit(Unit)
                }
                .onFailure {
                    _error.emit(it.localizedMessage ?: "Đăng ký thất bại")
                }
            _isLoading.value = false
        }
    }

    fun signInWithGoogle(idToken: String) {
        viewModelScope.launch {
            _isLoading.value = true
            authRepository.signInWithGoogle(idToken)
                .onSuccess {
                    _currentUser.value = it
                    _authSuccess.emit(Unit)
                }
                .onFailure {
                    _error.emit(it.localizedMessage ?: "Đăng nhập Google thất bại")
                }
            _isLoading.value = false
        }
    }
    
    fun logout() {
        authRepository.logout()
        _currentUser.value = null
    }

    // Thêm hàm này vào trong AuthViewModel.kt
    fun updateProfile(updatedUser: User) {
        viewModelScope.launch {
            _isLoading.value = true

            // Gọi xuống Repository để lưu lên Firebase/Database
            authRepository.updateUserProfile(updatedUser)
                .onSuccess {
                    _currentUser.value = updatedUser
                }
                .onFailure {
                    _error.emit(it.localizedMessage ?: "Cập nhật thất bại")
                }

            _isLoading.value = false
        }
    }

    fun updateProfileWithImage(updatedUser: User, imageUri: Uri?) {
        viewModelScope.launch {
            _isLoading.value = true

            try {
                var finalUser = updatedUser

                if (imageUri != null) {
                    // GỌI HÀM CLOUDINARY (Chờ cho đến khi upload xong và lấy URL)
                    val downloadUrl = CloudinaryHelper.uploadImageSuspend(imageUri)
                    finalUser = finalUser.copy(avatarUrl = downloadUrl)
                }

                // 2. Gọi hàm update Database (Lưu đống text và link ảnh lên Firestore)
                authRepository.updateUserProfile(finalUser)
                    .onSuccess {
                        _currentUser.value = finalUser
                    }
                    .onFailure {
                        _error.emit(it.localizedMessage ?: "Cập nhật Database thất bại")
                    }

            } catch (e: Exception) {
                _error.emit(e.localizedMessage ?: "Lỗi tải ảnh lên Cloudinary. Vui lòng thử lại.")
            } finally {
                _isLoading.value = false
            }
        }
    }
}
