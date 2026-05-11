package com.soulmate.app.ui.chat

import android.net.Uri
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.soulmate.app.domain.model.ChatMessage
import com.soulmate.app.domain.repository.IChatRepository
import com.soulmate.app.utils.CloudinaryHelper
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ChatViewModel @Inject constructor(
    private val chatRepository: IChatRepository
) : ViewModel() {

    private val _uploadProgress = mutableStateOf(0.0)
    val uploadProgress: State<Double> = _uploadProgress

    private val _isUploading = mutableStateOf(false)
    val isUploading: State<Boolean> = _isUploading

    private val _messages = MutableStateFlow<List<ChatMessage>>(emptyList())
    val messages: StateFlow<List<ChatMessage>> = _messages.asStateFlow()

    fun loadMessages(senderId: String, receiverId: String) {
        viewModelScope.launch {
            chatRepository.getMessages(senderId, receiverId).collect { list ->
                // Sắp xếp tin nhắn từ cũ đến mới (từ trên xuống dưới)
                _messages.value = list.sortedBy { it.timestamp?.seconds ?: 0L }
            }
        }
    }

    fun sendMessage(
        senderId: String,
        receiverId: String,
        messageText: String,
        imageUrl: String? = null
    ) {
        viewModelScope.launch {
            val chatMessage = ChatMessage(
                senderId = senderId,
                receiverId = receiverId,
                messageText = messageText,
                imageUrl = imageUrl
            )
            chatRepository.sendMessage(chatMessage)
        }
    }

    fun sendImageMessage(
        senderId: String,
        receiverId: String,
        imageUri: Uri,
        messageText: String = ""
    ) {
        _isUploading.value = true
        CloudinaryHelper.uploadImage(
            uri = imageUri,
            onProgress = { progress ->
                _uploadProgress.value = progress
            },
            onSuccess = { imageUrl ->
                _isUploading.value = false
                sendMessage(senderId, receiverId, messageText, imageUrl)
            },
            onError = {
                _isUploading.value = false
            }
        )
    }
}
