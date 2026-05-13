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
import kotlinx.coroutines.flow.map
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

    private val _lastMessages = MutableStateFlow<List<ChatMessage>>(emptyList())
    val lastMessages: StateFlow<List<ChatMessage>> = _lastMessages.asStateFlow()

    private val _replyingTo = mutableStateOf<ChatMessage?>(null)
    val replyingTo: State<ChatMessage?> = _replyingTo

    fun setReplyingTo(message: ChatMessage?) {
        _replyingTo.value = message
    }

    fun loadMessages(senderId: String, receiverId: String) {
        viewModelScope.launch {
            chatRepository.getMessages(senderId, receiverId).collect { list ->
                _messages.value = list.sortedWith { m1, m2 ->
                    val t1 = m1.timestamp
                    val t2 = m2.timestamp
                    when {
                        t1 == null && t2 == null -> 0
                        t1 == null -> 1
                        t2 == null -> -1
                        else -> {
                            if (t1.seconds != t2.seconds) {
                                t1.seconds.compareTo(t2.seconds)
                            } else {
                                t1.nanoseconds.compareTo(t2.nanoseconds)
                            }
                        }
                    }
                }
            }
        }
    }

    fun loadLastMessages(userId: String) {
        viewModelScope.launch {
            chatRepository.getLastMessages(userId).collect { list ->
                _lastMessages.value = list
            }
        }
    }

    fun markAsRead(userId: String, otherUserId: String) {
        viewModelScope.launch {
            chatRepository.markAsRead(userId, otherUserId)
        }
    }

    fun hasUnreadMessages(userId: String): StateFlow<Boolean> {
        return lastMessages.map { messages ->
            messages.any { it.receiverId == userId && !it.read }
        }.let { flow ->
            val state = MutableStateFlow(false)
            viewModelScope.launch {
                flow.collect { state.value = it }
            }
            state.asStateFlow()
        }
    }

    fun sendMessage(
        senderId: String,
        receiverId: String,
        messageText: String,
        imageUrl: String? = null,
        replyTo: ChatMessage? = null
    ) {
        viewModelScope.launch {
            val chatMessage = ChatMessage(
                senderId = senderId,
                receiverId = receiverId,
                messageText = messageText,
                imageUrl = imageUrl,
                read = false,
                replyToId = replyTo?.id,
                replyToText = replyTo?.messageText,
                replyToName = if (replyTo?.senderId == senderId) "Bạn" else null,
                replyToImageUrl = replyTo?.imageUrl
            )
            chatRepository.sendMessage(chatMessage)
            _replyingTo.value = null
        }
    }

    fun deleteMessage(messageId: String) {
        viewModelScope.launch {
            chatRepository.deleteMessage(messageId)
        }
    }

    fun deleteConversation(userId: String, otherUserId: String) {
        viewModelScope.launch {
            chatRepository.deleteConversation(userId, otherUserId)
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
