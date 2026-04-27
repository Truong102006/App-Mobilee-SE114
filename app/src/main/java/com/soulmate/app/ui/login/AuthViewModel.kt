package com.soulmate.app.ui.login

import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

@HiltViewModel
class AuthViewModel @Inject constructor(
    private val auth: FirebaseAuth
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
            try {
                auth.signInWithEmailAndPassword(email, password).await()
                _authSuccess.emit(Unit)
            } catch (e: Exception) {
                _error.emit(e.localizedMessage ?: "Đăng nhập thất bại")
            } finally {
                _isLoading.value = false
            }
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
            try {
                auth.createUserWithEmailAndPassword(email, password).await()
                // Lưu tên người dùng vào profile nếu cần
                _authSuccess.emit(Unit)
            } catch (e: Exception) {
                _error.emit(e.localizedMessage ?: "Đăng ký thất bại")
            } finally {
                _isLoading.value = false
            }
        }
    }
}
