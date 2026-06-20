package com.soulmate.app.ui.chat

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.AlertDialog
import androidx.compose.material.DismissDirection
import androidx.compose.material.DismissValue
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.Scaffold
import androidx.compose.material.SwipeToDismiss
import androidx.compose.material.Text
import androidx.compose.material.TextButton
import androidx.compose.material.TopAppBar
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.rememberDismissState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.soulmate.app.R
import com.soulmate.app.ui.components.OnlineStatusBadge
import com.soulmate.app.ui.presence.PresenceViewModel
import com.soulmate.app.ui.social.CommunityViewModel
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

@Suppress("UNUSED_PARAMETER")
@OptIn(ExperimentalMaterialApi::class)
@Composable
fun ChatListScreen(
    chatViewModel: ChatViewModel = hiltViewModel(),
    communityViewModel: CommunityViewModel,
    presenceViewModel: PresenceViewModel = hiltViewModel(),
    onChatClick: (String, String, String?) -> Unit,
    onBackClick: () -> Unit
) {
    val currentUserId = remember { FirebaseAuth.getInstance().currentUser?.uid ?: "" }
    val lastMessages by chatViewModel.lastMessages.collectAsState()
    val userPresences by presenceViewModel.userPresences.collectAsState()

    var searchQuery by remember { mutableStateOf("") }
    var showDeleteDialog by remember { mutableStateOf(false) }
    var userToDeleteId by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(currentUserId) {
        if (currentUserId.isNotEmpty()) {
            chatViewModel.loadLastMessages(currentUserId)
        }
    }

    val connectedUserIds = remember(lastMessages, currentUserId) {
        lastMessages.mapNotNull { message ->
            val otherUserId = if (message.senderId == currentUserId) {
                message.receiverId
            } else {
                message.senderId
            }
            otherUserId.takeIf { it.isNotBlank() && it != currentUserId }
        }.toSet()
    }

    LaunchedEffect(connectedUserIds) {
        presenceViewModel.observeUsers(connectedUserIds)
    }

    val chatDisplayItems = remember(lastMessages, userPresences, searchQuery, currentUserId) {
        val items = lastMessages
            .mapNotNull { message ->
                val otherUserId = if (message.senderId == currentUserId) {
                    message.receiverId
                } else {
                    message.senderId
                }

                if (otherUserId.isBlank() || otherUserId == currentUserId) {
                    return@mapNotNull null
                }

                val presence = userPresences[otherUserId]
                ChatDisplayItem(
                    user = ChatUser(
                        id = otherUserId,
                        name = presence?.userName?.ifBlank { "Người dùng" } ?: "Người dùng",
                        avatarUrl = presence?.userAvatarUrl
                    ),
                    lastMessage = message.messageText.ifBlank {
                        if (message.imageUrl != null) "Đã gửi một hình ảnh" else "Bắt đầu cuộc trò chuyện"
                    },
                    isFromMe = message.senderId == currentUserId,
                    timestamp = message.timestamp,
                    hasUnread = message.senderId != currentUserId && !message.read,
                    messageId = message.id,
                    isOnline = presence?.isOnlineNow() == true
                )
            }
            .distinctBy { it.user.id }

        val filtered = if (searchQuery.isEmpty()) {
            items
        } else {
            items.filter { it.user.name.contains(searchQuery, ignoreCase = true) }
        }

        filtered.sortedWith(
            compareByDescending<ChatDisplayItem> { it.timestamp != null }
                .thenByDescending { it.timestamp?.seconds ?: 0L }
        )
    }

    val onlineConnectedUsers = remember(chatDisplayItems) {
        chatDisplayItems.filter { it.isOnline }
    }

    Scaffold(
        modifier = Modifier.statusBarsPadding(),
        backgroundColor = Color.Black,
        topBar = {
            TopAppBar(
                backgroundColor = Color.Black,
                elevation = 0.dp,
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = Color(0xFF0084FF)
                        )
                    }
                },
                title = {
                    Text(
                        "messenger",
                        fontSize = 32.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color(0xFF0084FF)
                    )
                },
                actions = {
                    IconButton(onClick = {}) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(Color(0xFF242526)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Default.Edit,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(Color.Black)
        ) {
            Box(
                modifier = Modifier
                    .padding(horizontal = 16.dp, vertical = 8.dp)
                    .fillMaxWidth()
                    .height(44.dp)
                    .clip(RoundedCornerShape(22.dp))
                    .background(Color(0xFF242526))
                    .padding(horizontal = 16.dp),
                contentAlignment = Alignment.CenterStart
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.Search,
                        contentDescription = null,
                        tint = Color.Gray,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    BasicTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        textStyle = TextStyle(fontSize = 16.sp, color = Color.White),
                        cursorBrush = SolidColor(Color.White),
                        modifier = Modifier.fillMaxWidth(),
                        decorationBox = { innerTextField ->
                            if (searchQuery.isEmpty()) {
                                Text("Tìm kiếm theo tên", color = Color.Gray, fontSize = 16.sp)
                            }
                            innerTextField()
                        }
                    )
                }
            }

            LazyColumn(modifier = Modifier.fillMaxSize()) {
                item {
                    LazyRow(
                        modifier = Modifier.padding(vertical = 12.dp),
                        contentPadding = PaddingValues(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        item {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Box(contentAlignment = Alignment.BottomEnd) {
                                    Box(
                                        modifier = Modifier
                                            .size(64.dp)
                                            .clip(CircleShape)
                                            .background(Color(0xFF242526)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            Icons.Default.Edit,
                                            contentDescription = null,
                                            tint = Color.White,
                                            modifier = Modifier.size(28.dp)
                                        )
                                    }
                                    Box(
                                        modifier = Modifier
                                            .size(20.dp)
                                            .clip(CircleShape)
                                            .background(Color.Black)
                                            .padding(2.dp)
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .fillMaxSize()
                                                .clip(CircleShape)
                                                .background(Color.DarkGray)
                                        )
                                    }
                                }
                                Text(
                                    "Tạo tin",
                                    fontSize = 12.sp,
                                    color = Color.Gray,
                                    modifier = Modifier.padding(top = 4.dp)
                                )
                            }
                        }

                        items(onlineConnectedUsers, key = { it.user.id }) { item ->
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier.clickable {
                                    onChatClick(item.user.id, item.user.name, item.user.avatarUrl)
                                }
                            ) {
                                Box(contentAlignment = Alignment.BottomEnd) {
                                    UserAvatar(item.user.name, item.user.avatarUrl, 64.dp)
                                    Box(
                                        modifier = Modifier
                                            .size(18.dp)
                                            .clip(CircleShape)
                                            .background(Color.Black)
                                            .padding(2.dp)
                                    ) {
                                        OnlineStatusBadge(
                                            modifier = Modifier.fillMaxSize(),
                                            color = Color(0xFF42B72A)
                                        )
                                    }
                                }
                                Text(
                                    item.user.name.split(" ").firstOrNull() ?: "",
                                    fontSize = 12.sp,
                                    color = Color.White,
                                    modifier = Modifier.padding(top = 4.dp),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }
                }

                items(chatDisplayItems, key = { it.user.id }) { item ->
                    val dismissState = rememberDismissState(
                        confirmStateChange = {
                            if (it == DismissValue.DismissedToStart) {
                                userToDeleteId = item.user.id
                                showDeleteDialog = true
                                false
                            } else {
                                false
                            }
                        }
                    )

                    SwipeToDismiss(
                        state = dismissState,
                        directions = setOf(DismissDirection.EndToStart),
                        background = {
                            val color = when (dismissState.targetValue) {
                                DismissValue.Default -> Color.Transparent
                                else -> Color.Red
                            }
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(color)
                                    .padding(horizontal = 24.dp),
                                contentAlignment = Alignment.CenterEnd
                            ) {
                                Icon(Icons.Default.Delete, contentDescription = "Xóa", tint = Color.White)
                            }
                        },
                        dismissContent = {
                            ChatItem(
                                name = item.user.name,
                                avatarUrl = item.user.avatarUrl,
                                lastMessage = if (item.timestamp != null) {
                                    if (item.isFromMe) "Bạn: ${item.lastMessage}" else item.lastMessage
                                } else {
                                    item.lastMessage
                                },
                                time = formatChatTime(item.timestamp),
                                hasUnread = item.hasUnread,
                                isOnline = item.isOnline,
                                onClick = {
                                    onChatClick(item.user.id, item.user.name, item.user.avatarUrl)
                                }
                            )
                        }
                    )
                }
            }
        }
    }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = {
                showDeleteDialog = false
                userToDeleteId = null
            },
            backgroundColor = Color(0xFF242526),
            contentColor = Color.White,
            title = { Text("Xóa đoạn chat?", fontWeight = FontWeight.Bold) },
            text = { Text("Bạn có chắc chắn muốn xóa vĩnh viễn đoạn hội thoại này không?") },
            confirmButton = {
                TextButton(onClick = {
                    userToDeleteId?.let { chatViewModel.deleteConversation(currentUserId, it) }
                    showDeleteDialog = false
                    userToDeleteId = null
                }) {
                    Text("Xóa", color = Color.Red, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    showDeleteDialog = false
                    userToDeleteId = null
                }) {
                    Text("Hủy", color = Color.White)
                }
            },
            shape = RoundedCornerShape(16.dp)
        )
    }
}

@Composable
fun UserAvatar(name: String, avatarUrl: String?, size: Dp) {
    if (!avatarUrl.isNullOrBlank()) {
        AsyncImage(
            model = avatarUrl,
            contentDescription = null,
            modifier = Modifier
                .size(size)
                .clip(CircleShape),
            contentScale = ContentScale.Crop,
            error = painterResource(R.drawable.ava1)
        )
    } else {
        Box(
            modifier = Modifier
                .size(size)
                .clip(CircleShape)
                .background(Color(0xFF242526)),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = name.take(1).uppercase(),
                fontWeight = FontWeight.Bold,
                color = Color(0xFF0084FF),
                fontSize = (size.value * 0.4).sp
            )
        }
    }
}

@Composable
fun ChatItem(
    name: String,
    avatarUrl: String?,
    lastMessage: String,
    time: String,
    hasUnread: Boolean,
    isOnline: Boolean,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color.Black)
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(contentAlignment = Alignment.BottomEnd) {
            UserAvatar(name, avatarUrl, 60.dp)
            if (isOnline) {
                Box(
                    modifier = Modifier
                        .size(16.dp)
                        .clip(CircleShape)
                        .background(Color.Black)
                        .padding(2.dp)
                ) {
                    OnlineStatusBadge(
                        modifier = Modifier.fillMaxSize(),
                        color = Color(0xFF42B72A)
                    )
                }
            }
        }
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = name,
                fontSize = 17.sp,
                fontWeight = if (hasUnread) FontWeight.Bold else FontWeight.Medium,
                color = Color.White,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = lastMessage,
                    fontSize = 14.sp,
                    color = if (hasUnread) Color.White else Color.Gray,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f, fill = false),
                    fontWeight = if (hasUnread) FontWeight.Bold else FontWeight.Normal
                )
                if (time.isNotEmpty()) {
                    Text(text = " • $time", fontSize = 14.sp, color = Color.Gray)
                }
            }
        }
        if (hasUnread) {
            Box(
                modifier = Modifier
                    .padding(start = 8.dp)
                    .size(12.dp)
                    .clip(CircleShape)
                    .background(Color(0xFF0084FF))
            )
        }
    }
}

data class ChatDisplayItem(
    val user: ChatUser,
    val lastMessage: String,
    val isFromMe: Boolean,
    val timestamp: Timestamp?,
    val hasUnread: Boolean,
    val messageId: String,
    val isOnline: Boolean
)

private fun formatChatTime(timestamp: Timestamp?): String {
    if (timestamp == null) return ""
    val date = timestamp.toDate()
    val now = Calendar.getInstance()
    val chatDate = Calendar.getInstance().apply { time = date }
    return if (now.get(Calendar.DATE) == chatDate.get(Calendar.DATE)) {
        SimpleDateFormat("HH:mm", Locale.getDefault()).format(date)
    } else {
        SimpleDateFormat("d 'thg' M", Locale.getDefault()).format(date)
    }
}

data class ChatUser(val id: String, val name: String, val avatarUrl: String?)
