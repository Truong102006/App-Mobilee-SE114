package com.soulmate.app.ui.chat

import android.net.Uri
import android.util.Log
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.soulmate.app.domain.model.ChatMessage
import com.soulmate.app.domain.repository.IChatRepository
import com.soulmate.app.utils.CloudinaryHelper
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ChatViewModel @Inject constructor(
    private val chatRepository: IChatRepository
) : ViewModel() {
    companion object {
        private const val TAG = "ChatViewModel"
    }

    private val _uploadProgress = mutableStateOf(0.0)
    val uploadProgress: State<Double> = _uploadProgress

    private val _isUploading = mutableStateOf(false)
    val isUploading: State<Boolean> = _isUploading

    private val _messages = MutableStateFlow<List<ChatMessage>>(emptyList())
    val messages: StateFlow<List<ChatMessage>> = _messages.asStateFlow()

    private val _lastMessages = MutableStateFlow<List<ChatMessage>>(emptyList())
    val lastMessages: StateFlow<List<ChatMessage>> = _lastMessages.asStateFlow()

    private val _hasUnreadMessages = MutableStateFlow(false)

    private val _replyingTo = mutableStateOf<ChatMessage?>(null)
    val replyingTo: State<ChatMessage?> = _replyingTo

    private var messagesJob: Job? = null
    private var lastMessagesJob: Job? = null
    private var activeConversation: Pair<String, String>? = null
    private var activeInboxUserId: String? = null

    fun setReplyingTo(message: ChatMessage?) {
        _replyingTo.value = message
    }

    fun loadMessages(senderId: String, receiverId: String) {
        activeConversation = senderId to receiverId
        messagesJob?.cancel()
        messagesJob = viewModelScope.launch {
            chatRepository
                .getMessages(senderId, receiverId)
                .catch { error ->
                    Log.w(TAG, "loadMessages failed: senderId=$senderId receiverId=$receiverId", error)
                }
                .collect { list ->
                    _messages.value = list.sortedWith { left, right ->
                        val leftTime = left.timestamp
                        val rightTime = right.timestamp
                        when {
                            leftTime == null && rightTime == null -> 0
                            leftTime == null -> 1
                            rightTime == null -> -1
                            leftTime.seconds != rightTime.seconds -> leftTime.seconds.compareTo(rightTime.seconds)
                            else -> leftTime.nanoseconds.compareTo(rightTime.nanoseconds)
                        }
                    }
                }
        }
    }

    fun loadLastMessages(userId: String) {
        activeInboxUserId = userId
        lastMessagesJob?.cancel()
        lastMessagesJob = viewModelScope.launch {
            chatRepository
                .getLastMessages(userId)
                .catch { error ->
                    Log.w(TAG, "loadLastMessages failed: userId=$userId", error)
                }
                .collect { list ->
                    _lastMessages.value = list
                    _hasUnreadMessages.value = list.any { it.receiverId == userId && !it.read }
                }
        }
    }

    fun markAsRead(userId: String, otherUserId: String) {
        viewModelScope.launch {
            chatRepository.markAsRead(userId, otherUserId)
        }
    }

    fun hasUnreadMessages(userId: String): StateFlow<Boolean> {
        _hasUnreadMessages.value = _lastMessages.value.any { it.receiverId == userId && !it.read }
        return _hasUnreadMessages.asStateFlow()
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
                replyToName = if (replyTo?.senderId == senderId) "B\u1EA1n" else null,
                replyToImageUrl = replyTo?.imageUrl
            )

            chatRepository.sendMessage(chatMessage)
                .onSuccess {
                    _replyingTo.value = null
                    refreshConversation()
                    refreshInbox()
                }
                .onFailure { error ->
                    Log.w(TAG, "sendMessage failed: senderId=$senderId receiverId=$receiverId", error)
                }
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
                .onSuccess {
                    _messages.value = emptyList()
                    refreshConversation()
                    refreshInbox()
                }
                .onFailure { error ->
                    Log.w(TAG, "deleteConversation failed: userId=$userId otherUserId=$otherUserId", error)
                }
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
            onSuccess = { uploadedImageUrl ->
                _isUploading.value = false
                sendMessage(senderId, receiverId, messageText, uploadedImageUrl)
            },
            onError = {
                _isUploading.value = false
            }
        )
    }

    private fun refreshConversation() {
        activeConversation?.let { (senderId, receiverId) ->
            loadMessages(senderId, receiverId)
        }
    }

    private fun refreshInbox() {
        activeInboxUserId?.let(::loadLastMessages)
    }
}
