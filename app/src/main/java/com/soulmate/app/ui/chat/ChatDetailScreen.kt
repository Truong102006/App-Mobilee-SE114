package com.soulmate.app.ui.chat

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.spring
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Reply
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.soulmate.app.R
import com.soulmate.app.domain.model.ChatMessage
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun ChatDetailScreen(
    userId: String,
    userName: String,
    userAvatarUrl: String?,
    chatViewModel: ChatViewModel,
    onBackClick: () -> Unit
) {
    val currentUserId = remember { FirebaseAuth.getInstance().currentUser?.uid ?: "" }
    val messages by chatViewModel.messages.collectAsState()
    val replyingTo by chatViewModel.replyingTo
    var messageText by remember { mutableStateOf("") }
    val listState = rememberLazyListState()

    var showDeleteDialog by remember { mutableStateOf(false) }
    var messageToDelete by remember { mutableStateOf<ChatMessage?>(null) }
    var showOptionsSheet by remember { mutableStateOf(false) }
    var selectedMessage by remember { mutableStateOf<ChatMessage?>(null) }

    LaunchedEffect(userId) {
        if (currentUserId.isNotEmpty() && userId.isNotEmpty()) {
            chatViewModel.loadMessages(currentUserId, userId)
            chatViewModel.markAsRead(currentUserId, userId)
        }
    }

    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.size - 1)
        }
    }
    
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
                                overflow = TextOverflow.Ellipsis
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
        Box(modifier = Modifier.fillMaxSize().padding(paddingValues)) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.White)
                    .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Bottom))
            ) {
                LazyColumn(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                    state = listState
                ) {
                    itemsIndexed(messages, key = { index, message -> 
                        message.id.ifEmpty { "msg_$index" } 
                    }) { index, message ->
                        val showHeader = remember(messages, index) {
                            if (index == 0) true
                            else {
                                val current = messages[index].timestamp?.seconds ?: 0L
                                val previous = messages[index - 1].timestamp?.seconds ?: 0L
                                (current - previous) > 10 * 60
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
                                    (nextTime - currentTime) > 3 * 60
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
                            otherUserName = userName,
                            showTime = isLastInBurst,
                            showAvatar = isLastInBurst && message.senderId != currentUserId,
                            onLongPress = {
                                selectedMessage = message
                                showOptionsSheet = true
                            },
                            onSwipeToReply = {
                                chatViewModel.setReplyingTo(message)
                            }
                        )
                    }
                }
                
                ChatBottomBar(
                    messageText = messageText,
                    onMessageChange = { messageText = it },
                    replyingTo = replyingTo,
                    onCancelReply = { chatViewModel.setReplyingTo(null) },
                    onSendClick = {
                        if (messageText.isNotBlank()) {
                            chatViewModel.sendMessage(currentUserId, userId, messageText, replyTo = replyingTo)
                            messageText = ""
                        }
                    },
                    onLikeClick = {
                        chatViewModel.sendMessage(currentUserId, userId, "👍")
                    }
                )
            }

            if (showOptionsSheet) {
                ModalOptions(
                    onDismiss = { showOptionsSheet = false },
                    onReply = {
                        chatViewModel.setReplyingTo(selectedMessage)
                        showOptionsSheet = false
                    },
                    onDelete = {
                        messageToDelete = selectedMessage
                        showDeleteDialog = true
                        showOptionsSheet = false
                    }
                )
            }
        }
    }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("Xóa tin nhắn?", fontWeight = FontWeight.Bold) },
            text = { Text("Bạn có chắc chắn muốn xóa vĩnh viễn tin nhắn này không?") },
            confirmButton = {
                TextButton(onClick = {
                    messageToDelete?.let { chatViewModel.deleteMessage(it.id) }
                    showDeleteDialog = false
                }) {
                    Text("Xóa", color = Color.Red, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) {
                    Text("Hủy")
                }
            },
            shape = RoundedCornerShape(16.dp)
        )
    }
}

@Composable
fun ModalOptions(onDismiss: () -> Unit, onReply: () -> Unit, onDelete: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.4f))
            .clickable(onClick = onDismiss),
        contentAlignment = Alignment.Center
    ) {
        Card(
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.width(200.dp).clickable(enabled = false) { },
            elevation = 8.dp
        ) {
            Column {
                TextButton(
                    onClick = onReply,
                    modifier = Modifier.fillMaxWidth().padding(8.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.AutoMirrored.Filled.Reply, contentDescription = null, tint = Color.Black)
                        ClarificationSpacer()
                        Text("Trả lời", color = Color.Black)
                    }
                }
                Divider()
                TextButton(
                    onClick = onDelete,
                    modifier = Modifier.fillMaxWidth().padding(8.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Delete, contentDescription = null, tint = Color.Red)
                        ClarificationSpacer()
                        Text("Xóa tin nhắn", color = Color.Red)
                    }
                }
            }
        }
    }
}

@Composable
private fun ClarificationSpacer() {
    Spacer(Modifier.width(12.dp))
}

private fun formatHeaderDate(timestamp: com.google.firebase.Timestamp?): String {
    if (timestamp == null) return ""
    val date = timestamp.toDate()
    val sdf = SimpleDateFormat("d 'THG' M 'LÚC' HH:mm", Locale("vi", "VN"))
    return sdf.format(date).uppercase()
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun MessageBubble(
    message: ChatMessage,
    isMine: Boolean,
    userAvatarUrl: String?,
    otherUserName: String,
    showTime: Boolean,
    showAvatar: Boolean,
    onLongPress: () -> Unit,
    onSwipeToReply: () -> Unit
) {
    val offsetX = remember { Animatable(0f) }
    val scope = rememberCoroutineScope()

    val timeString = remember(message.timestamp) {
        if (message.timestamp != null) {
            SimpleDateFormat("HH:mm", Locale.getDefault()).format(message.timestamp.toDate())
        } else ""
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .pointerInput(Unit) {
                detectHorizontalDragGestures(
                    onHorizontalDrag = { change, dragAmount ->
                        change.consume()
                        // Chỉ cho phép vuốt sang trái (dragAmount < 0)
                        val newOffset = (offsetX.value + dragAmount).coerceIn(-120f, 0f)
                        scope.launch {
                            offsetX.snapTo(newOffset)
                        }
                    },
                    onDragEnd = {
                        if (offsetX.value <= -90f) {
                            onSwipeToReply()
                        }
                        scope.launch {
                            offsetX.animateTo(0f, animationSpec = spring())
                        }
                    },
                    onDragCancel = {
                        scope.launch {
                            offsetX.animateTo(0f)
                        }
                    }
                )
            }
    ) {
        // Biểu tượng Reply hiện ra khi vuốt
        if (offsetX.value < 0) {
            Box(
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .padding(end = 16.dp)
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.Reply,
                    contentDescription = null,
                    tint = if (offsetX.value <= -90f) Color(0xFF0084FF) else Color.Gray.copy(alpha = 0.6f),
                    modifier = Modifier
                        .size(24.dp)
                        .graphicsLayer {
                            alpha = (offsetX.value / -90f).coerceIn(0f, 1f)
                            scaleX = (offsetX.value / -90f).coerceIn(0.5f, 1f)
                            scaleY = (offsetX.value / -90f).coerceIn(0.5f, 1f)
                        }
                )
            }
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .graphicsLayer { translationX = offsetX.value }
                .padding(vertical = 1.dp),
            horizontalAlignment = if (isMine) Alignment.End else Alignment.Start
        ) {
            // Reply Header
            if (message.replyToId != null) {
                val replyName = if (message.replyToName == "Bạn") "bạn" else otherUserName
                Row(
                    modifier = Modifier.padding(
                        start = if (isMine) 0.dp else 36.dp,
                        end = if (isMine) 8.dp else 0.dp,
                        bottom = 2.dp
                    ),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.Reply, 
                        contentDescription = null, 
                        modifier = Modifier.size(12.dp), 
                        tint = Color.Gray
                    )
                    Spacer(Modifier.width(4.dp))
                    Text(
                        text = if (isMine) "Bạn đã trả lời $replyName" else "$otherUserName đã trả lời bạn",
                        fontSize = 11.sp,
                        color = Color.Gray
                    )
                }
            }

            Row(
                verticalAlignment = Alignment.Bottom,
                horizontalArrangement = if (isMine) Arrangement.End else Arrangement.Start
            ) {
                if (!isMine) {
                    if (showAvatar) {
                        AsyncImage(
                            model = userAvatarUrl ?: R.drawable.ava1,
                            contentDescription = null,
                            modifier = Modifier.size(20.dp).clip(CircleShape),
                            contentScale = ContentScale.Crop,
                            error = painterResource(R.drawable.ava1)
                        )
                    } else {
                        Spacer(modifier = Modifier.size(28.dp))
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                }

                Column(horizontalAlignment = if (isMine) Alignment.End else Alignment.Start) {
                    // Replied Message Content
                    if (message.replyToText != null) {
                        Box(
                            modifier = Modifier
                                .padding(bottom = 2.dp)
                                .clip(RoundedCornerShape(14.dp))
                                .background(Color(0xFFF0F2F5))
                                .padding(horizontal = 10.dp, vertical = 6.dp)
                        ) {
                            Text(
                                text = message.replyToText,
                                fontSize = 13.sp,
                                color = Color.Gray,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }

                    // Main Message Content
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
                            .background(if (isMine) Color(0xFF0084FF) else Color(0xFFF0F2F5))
                            .combinedClickable(
                                onClick = {},
                                onLongClick = onLongPress
                            )
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
}

@Composable
fun ChatBottomBar(
    messageText: String,
    onMessageChange: (String) -> Unit,
    replyingTo: ChatMessage?,
    onCancelReply: () -> Unit,
    onSendClick: () -> Unit,
    onLikeClick: () -> Unit
) {
    Surface(
        elevation = 8.dp,
        color = Color.White
    ) {
        Column {
            if (replyingTo != null) {
                Row(
                    modifier = Modifier.fillMaxWidth().background(Color(0xFFF0F2F5)).padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Đang trả lời tin nhắn", fontSize = 12.sp, color = Color(0xFF0084FF), fontWeight = FontWeight.Bold)
                        Text(replyingTo.messageText, fontSize = 14.sp, color = Color.Gray, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                    IconButton(onClick = onCancelReply, modifier = Modifier.size(24.dp)) {
                        Icon(Icons.Default.Close, contentDescription = null, modifier = Modifier.size(16.dp))
                    }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp, vertical = 8.dp),
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
                    placeholder = { Text("Aa", color = Color.Gray, fontSize = 16.sp) },
                    textStyle = TextStyle(color = Color.Black, fontSize = 16.sp),
                    colors = TextFieldDefaults.textFieldColors(
                        backgroundColor = Color(0xFFF0F2F5),
                        focusedIndicatorColor = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent,
                        disabledIndicatorColor = Color.Transparent,
                        textColor = Color.Black,
                        cursorColor = Color.Black
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
                    IconButton(onClick = onLikeClick) {
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
}
