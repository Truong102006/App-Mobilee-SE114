package com.soulmate.app.ui.chat

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.soulmate.app.R
import com.soulmate.app.domain.model.ChatMessage
import com.google.firebase.auth.FirebaseAuth
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun ChatDetailScreen(
    userName: String,
    userAvatarUrl: String?,
    chatViewModel: ChatViewModel,
    onBackClick: () -> Unit
) {
    val currentUserId = remember { FirebaseAuth.getInstance().currentUser?.uid ?: "" }
    val messages by chatViewModel.messages.collectAsState()
    var messageText by remember { mutableStateOf("") }
    val listState = rememberLazyListState()

    val receiverId = userName

    LaunchedEffect(receiverId) {
        if (currentUserId.isNotEmpty()) {
            chatViewModel.loadMessages(currentUserId, receiverId)
        }
    }

    // Tự động cuộn xuống cuối khi có tin nhắn mới
    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.size - 1)
        }
    }
    
    // Tự động cuộn xuống khi bàn phím mở
    val isKeyboardVisible = WindowInsets.ime.asPaddingValues().calculateBottomPadding() > 0.dp
    LaunchedEffect(isKeyboardVisible) {
        if (isKeyboardVisible && messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.size - 1)
        }
    }

    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding(),
        topBar = {
            TopAppBar(
                backgroundColor = Color.White,
                elevation = 1.dp,
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = Color(0xFF0084FF)
                        )
                    }
                },
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(contentAlignment = Alignment.BottomEnd) {
                            AsyncImage(
                                model = userAvatarUrl ?: R.drawable.ava1,
                                contentDescription = null,
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape),
                                contentScale = ContentScale.Crop,
                                error = painterResource(R.drawable.ava1)
                            )
                            Box(
                                modifier = Modifier
                                    .size(10.dp)
                                    .clip(CircleShape)
                                    .background(Color.White)
                                    .padding(1.5.dp)
                            ) {
                                Box(modifier = Modifier.fillMaxSize().clip(CircleShape).background(Color(0xFF42B72A)))
                            }
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = userName,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.Black,
                                maxLines = 1,
                                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                            )
                            Text("Đang hoạt động", fontSize = 11.sp, color = Color.Gray)
                        }
                    }
                },
                actions = {
                    IconButton(onClick = {}) {
                        Icon(Icons.Default.Call, contentDescription = null, tint = Color(0xFF0084FF))
                    }
                    IconButton(onClick = {}) {
                        Icon(Icons.Default.VideoCall, contentDescription = null, tint = Color(0xFF0084FF))
                    }
                    IconButton(onClick = {}) {
                        Icon(Icons.Default.Info, contentDescription = null, tint = Color(0xFF0084FF))
                    }
                }
            )
        },
        contentWindowInsets = WindowInsets(0, 0, 0, 0)
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(Color.White)
                .navigationBarsPadding()
                .imePadding()
        ) {
            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                state = listState,
                reverseLayout = false
            ) {
                itemsIndexed(messages, key = { index, message -> 
                    message.id.ifEmpty { "msg_$index" } 
                }) { index, message ->
                    
                    val showHeader = remember(messages, index) {
                        if (index == 0) true
                        else {
                            val current = messages[index].timestamp?.seconds ?: 0L
                            val previous = messages[index - 1].timestamp?.seconds ?: 0L
                            (current - previous) > 10 * 60 // 10 mins
                        }
                    }

                    val isLastInBurst = remember(messages, index) {
                        if (index == messages.lastIndex) true
                        else {
                            val current = messages[index]
                            val next = messages[index + 1]
                            if (current.senderId != next.senderId) true
                            else {
                                val currentTime = current.timestamp?.seconds ?: 0L
                                val nextTime = next.timestamp?.seconds ?: 0L
                                (nextTime - currentTime) > 3 * 60 // 3 mins
                            }
                        }
                    }

                    if (showHeader) {
                        Text(
                            text = formatHeaderDate(message.timestamp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 12.dp),
                            textAlign = TextAlign.Center,
                            fontSize = 11.sp,
                            color = Color.Gray,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    MessageBubble(
                        message = message,
                        isMine = message.senderId == currentUserId,
                        userAvatarUrl = userAvatarUrl,
                        showTime = isLastInBurst,
                        showAvatar = isLastInBurst && message.senderId != currentUserId
                    )
                }
            }
            
            ChatBottomBar(
                messageText = messageText,
                onMessageChange = { messageText = it },
                onSendClick = {
                    if (messageText.isNotBlank()) {
                        chatViewModel.sendMessage(currentUserId, receiverId, messageText)
                        messageText = ""
                    }
                }
            )
        }
    }
}

private fun formatHeaderDate(timestamp: com.google.firebase.Timestamp?): String {
    if (timestamp == null) return ""
    val date = timestamp.toDate()
    val sdf = SimpleDateFormat("d 'THG' M 'LÚC' HH:mm", Locale("vi", "VN"))
    return sdf.format(date).uppercase()
}

@Composable
fun MessageBubble(
    message: ChatMessage,
    isMine: Boolean,
    userAvatarUrl: String?,
    showTime: Boolean,
    showAvatar: Boolean
) {
    val timeString = remember(message.timestamp) {
        if (message.timestamp != null) {
            SimpleDateFormat("HH:mm", Locale.getDefault()).format(message.timestamp.toDate())
        } else ""
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 1.dp),
        horizontalAlignment = if (isMine) Alignment.End else Alignment.Start
    ) {
        Row(
            verticalAlignment = Alignment.Bottom,
            horizontalArrangement = if (isMine) Arrangement.End else Arrangement.Start
        ) {
            if (!isMine) {
                if (showAvatar) {
                    AsyncImage(
                        model = userAvatarUrl ?: R.drawable.ava1,
                        contentDescription = null,
                        modifier = Modifier
                            .size(28.dp)
                            .clip(CircleShape),
                        contentScale = ContentScale.Crop,
                        error = painterResource(R.drawable.ava1)
                    )
                } else {
                    Spacer(modifier = Modifier.size(28.dp))
                }
                Spacer(modifier = Modifier.width(8.dp))
            }

            Box(
                modifier = Modifier
                    .widthIn(max = 260.dp)
                    .clip(
                        RoundedCornerShape(
                            topStart = 18.dp,
                            topEnd = 18.dp,
                            bottomStart = if (isMine) 18.dp else (if (showAvatar) 4.dp else 18.dp),
                            bottomEnd = if (isMine) (if (showTime) 4.dp else 18.dp) else 18.dp
                        )
                    )
                    .background(if (isMine) Color(0xFF0000AA) else Color(0xFFF0F2F5))
                    .padding(horizontal = 12.dp, vertical = 8.dp)
            ) {
                Text(
                    text = message.messageText,
                    color = if (isMine) Color.White else Color.Black,
                    fontSize = 15.sp
                )
            }
        }
        
        if (showTime && timeString.isNotEmpty()) {
            Text(
                text = timeString,
                fontSize = 10.sp,
                color = Color.Gray,
                modifier = Modifier.padding(
                    top = 2.dp,
                    start = if (isMine) 0.dp else 36.dp,
                    end = if (isMine) 4.dp else 0.dp
                )
            )
        }
    }
}

@Composable
fun ChatBottomBar(
    messageText: String,
    onMessageChange: (String) -> Unit,
    onSendClick: () -> Unit
) {
    Surface(
        elevation = 4.dp,
        color = Color.White
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 4.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = {}) {
                Icon(Icons.Default.Add, contentDescription = null, tint = Color(0xFF0084FF))
            }
            IconButton(onClick = {}) {
                Icon(Icons.Default.Image, contentDescription = null, tint = Color(0xFF0084FF))
            }
            
            TextField(
                value = messageText,
                onValueChange = onMessageChange,
                modifier = Modifier
                    .weight(1f)
                    .heightIn(min = 40.dp)
                    .clip(RoundedCornerShape(20.dp)),
                placeholder = { Text("Nhắn tin", color = Color.Gray, fontSize = 15.sp) },
                colors = TextFieldDefaults.textFieldColors(
                    backgroundColor = Color(0xFFF0F2F5),
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent,
                    disabledIndicatorColor = Color.Transparent
                ),
                trailingIcon = {
                    IconButton(onClick = {}) {
                        Icon(
                            imageVector = Icons.Default.SentimentSatisfiedAlt,
                            contentDescription = null,
                            tint = Color(0xFF0084FF),
                            modifier = Modifier.size(24.dp)
                        )
                    }
                },
                maxLines = 4
            )

            if (messageText.isBlank()) {
                IconButton(onClick = {}) {
                    Icon(Icons.Default.ThumbUp, contentDescription = null, tint = Color(0xFF0084FF))
                }
            } else {
                IconButton(onClick = onSendClick) {
                    Icon(Icons.AutoMirrored.Filled.Send, contentDescription = "Gửi", tint = Color(0xFF0084FF))
                }
            }
        }
    }
}
