package com.soulmate.app.ui.setting

import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class ThemeViewModel @Inject constructor() : ViewModel() {
    private val _isDarkMode = mutableStateOf(false)
    val isDarkMode: State<Boolean> = _isDarkMode

    fun toggleDarkMode() {
        _isDarkMode.value = !_isDarkMode.value
    }
}
