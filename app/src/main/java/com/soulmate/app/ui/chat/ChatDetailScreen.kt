package com.soulmate.app.ui.chat

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.ThumbUp
import androidx.compose.material.icons.filled.VideoCall
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.soulmate.app.R
import com.soulmate.app.domain.model.ChatMessage
import com.google.firebase.auth.FirebaseAuth

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

    // Sử dụng userName làm receiverId tạm thời theo logic hiện tại
    val receiverId = userName

    LaunchedEffect(receiverId) {
        if (currentUserId.isNotEmpty()) {
            chatViewModel.loadMessages(currentUserId, receiverId)
        }
    }

    Scaffold(
        modifier = Modifier.fillMaxSize().statusBarsPadding(),
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
        bottomBar = {
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
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(Color.White),
            contentPadding = PaddingValues(16.dp),
            state = listState,
            reverseLayout = true
        ) {
            items(messages, key = { it.id.ifEmpty { it.timestamp?.toString() ?: it.hashCode().toString() } }) { message ->
                MessageBubble(
                    message = message,
                    isMine = message.senderId == currentUserId,
                    userAvatarUrl = userAvatarUrl
                )
            }
        }
    }
}

@Composable
fun MessageBubble(
    message: ChatMessage,
    isMine: Boolean,
    userAvatarUrl: String?
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp),
        horizontalArrangement = if (isMine) Arrangement.End else Arrangement.Start,
        verticalAlignment = Alignment.Bottom
    ) {
        if (!isMine) {
            AsyncImage(
                model = userAvatarUrl ?: R.drawable.ava1,
                contentDescription = null,
                modifier = Modifier
                    .size(28.dp)
                    .clip(CircleShape),
                contentScale = ContentScale.Crop,
                error = painterResource(R.drawable.ava1)
            )
            Spacer(modifier = Modifier.width(8.dp))
        }

        Box(
            modifier = Modifier
                .widthIn(max = 260.dp)
                .clip(
                    RoundedCornerShape(
                        topStart = 18.dp,
                        topEnd = 18.dp,
                        bottomStart = if (isMine) 18.dp else 4.dp,
                        bottomEnd = if (isMine) 4.dp else 18.dp
                    )
                )
                .background(if (isMine) Color(0xFF0084FF) else Color(0xFFF0F0F0))
                .padding(horizontal = 12.dp, vertical = 8.dp)
        ) {
            Text(
                text = message.messageText,
                color = if (isMine) Color.White else Color.Black,
                fontSize = 15.sp
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
                .padding(horizontal = 4.dp, vertical = 8.dp)
                .navigationBarsPadding()
                .imePadding(),
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
