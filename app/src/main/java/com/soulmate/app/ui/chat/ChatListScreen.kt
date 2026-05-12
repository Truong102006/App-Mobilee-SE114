package com.soulmate.app.ui.chat

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Search
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.*
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import com.google.firebase.auth.FirebaseAuth
import com.soulmate.app.R
import com.soulmate.app.ui.social.CommunityViewModel
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterialApi::class)
@Composable
fun ChatListScreen(
    chatViewModel: ChatViewModel = hiltViewModel(),
    communityViewModel: CommunityViewModel,
    onChatClick: (String, String, String?) -> Unit,
    onBackClick: () -> Unit
) {
    val currentUserId = remember { FirebaseAuth.getInstance().currentUser?.uid ?: "" }
    val lastMessages by chatViewModel.lastMessages.collectAsState()
    val communityPosts by communityViewModel.posts
    
    var searchQuery by remember { mutableStateOf("") }
    var showDeleteDialog by remember { mutableStateOf(false) }
    var userToDeleteId by remember { mutableStateOf<String?>(null) }
    
    LaunchedEffect(currentUserId) {
        if (currentUserId.isNotEmpty()) {
            chatViewModel.loadLastMessages(currentUserId)
        }
    }

    // Extraction of all unique users who have posted in Community
    val communityUsers = remember(communityPosts) {
        communityPosts
            .filter { it.userId.isNotEmpty() && it.userId != currentUserId }
            .sortedByDescending { it.id } // Heuristic for latest info if multiple posts exist
            .distinctBy { it.userId }
            .map { ChatUser(it.userId, it.userName, it.userAvatarUrl) }
    }

    val chatDisplayItems = remember(lastMessages, communityUsers, searchQuery) {
        val userMap = communityUsers.associateBy { it.id }
        val items = mutableListOf<ChatDisplayItem>()
        val processedUserIds = mutableSetOf<String>()

        // 1. Existing conversations
        lastMessages.forEach { msg ->
            val otherUserId = if (msg.senderId == currentUserId) msg.receiverId else msg.senderId
            if (otherUserId != currentUserId) {
                // Prefer community user info (name/avatar) if available for better accuracy
                val user = userMap[otherUserId] ?: ChatUser(otherUserId, "Người dùng", null)
                items.add(ChatDisplayItem(
                    user = user,
                    lastMessage = msg.messageText,
                    isFromMe = msg.senderId == currentUserId,
                    timestamp = msg.timestamp,
                    hasUnread = msg.senderId != currentUserId && !msg.read,
                    messageId = msg.id
                ))
                processedUserIds.add(otherUserId)
            }
        }

        // 2. Community users who haven't chatted yet
        communityUsers.forEach { user ->
            if (!processedUserIds.contains(user.id)) {
                items.add(ChatDisplayItem(
                    user = user,
                    lastMessage = "Bắt đầu cuộc trò chuyện",
                    isFromMe = false,
                    timestamp = null,
                    hasUnread = false,
                    messageId = ""
                ))
            }
        }

        // Filter by user name only
        val filtered = if (searchQuery.isEmpty()) items 
        else items.filter { it.user.name.contains(searchQuery, ignoreCase = true) }

        // Sorting: Active chats first (by timestamp DESC), then "Start conversation" users
        filtered.sortedWith(
            compareByDescending<ChatDisplayItem> { it.timestamp != null }
                .thenByDescending { it.timestamp?.seconds ?: 0L }
        )
    }

    Scaffold(
        modifier = Modifier.statusBarsPadding(),
        topBar = {
            TopAppBar(
                backgroundColor = Color.White,
                elevation = 0.dp,
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color(0xFF0084FF))
                    }
                },
                title = {
                    Text(
                        "messenger",
                        fontSize = 32.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color(0xFF0084FF),
                        modifier = Modifier.padding(start = 0.dp)
                    )
                },
                actions = {
                    IconButton(onClick = {}) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(Color(0xFFF0F2F5)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.Edit, contentDescription = null, tint = Color.Black, modifier = Modifier.size(20.dp))
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
                .background(Color.White)
        ) {
            // Modern Search Bar
            Box(
                modifier = Modifier
                    .padding(horizontal = 16.dp, vertical = 8.dp)
                    .fillMaxWidth()
                    .height(44.dp)
                    .clip(RoundedCornerShape(22.dp))
                    .background(Color(0xFFF0F2F5))
                    .padding(horizontal = 16.dp),
                contentAlignment = Alignment.CenterStart
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Search, contentDescription = null, tint = Color.Gray, modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    BasicTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        textStyle = TextStyle(fontSize = 16.sp, color = Color.Black),
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
                // Active Row (Stories/Community Users)
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
                                        modifier = Modifier.size(64.dp).clip(CircleShape).background(Color(0xFFF0F2F5)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(28.dp))
                                    }
                                    Box(modifier = Modifier.size(20.dp).clip(CircleShape).background(Color.White).padding(2.dp)) {
                                        Box(modifier = Modifier.fillMaxSize().clip(CircleShape).background(Color.LightGray))
                                    }
                                }
                                Text("Tạo tin", fontSize = 12.sp, color = Color.Gray, modifier = Modifier.padding(top = 4.dp))
                            }
                        }
                        items(communityUsers) { user ->
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier.clickable { onChatClick(user.id, user.name, user.avatarUrl) }
                            ) {
                                Box(contentAlignment = Alignment.BottomEnd) {
                                    UserAvatar(user.name, user.avatarUrl, 64.dp)
                                    Box(modifier = Modifier.size(18.dp).clip(CircleShape).background(Color.White).padding(2.dp)) {
                                        Box(modifier = Modifier.fillMaxSize().clip(CircleShape).background(Color(0xFF42B72A)))
                                    }
                                }
                                Text(user.name.split(" ").firstOrNull() ?: "", fontSize = 12.sp, color = Color.Black, modifier = Modifier.padding(top = 4.dp), maxLines = 1, overflow = TextOverflow.Ellipsis)
                            }
                        }
                    }
                }

                // Chat Items
                items(chatDisplayItems, key = { it.user.id }) { item ->
                    val dismissState = rememberDismissState(
                        confirmStateChange = {
                            if (it == DismissValue.DismissedToStart) {
                                userToDeleteId = item.user.id
                                showDeleteDialog = true
                                false
                            } else false
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
                            Box(modifier = Modifier.fillMaxSize().background(color).padding(horizontal = 24.dp), contentAlignment = Alignment.CenterEnd) {
                                Icon(Icons.Default.Delete, contentDescription = "Xóa", tint = Color.White)
                            }
                        },
                        dismissContent = {
                            ChatItem(
                                name = item.user.name,
                                avatarUrl = item.user.avatarUrl,
                                lastMessage = if (item.timestamp != null) (if (item.isFromMe) "Bạn: ${item.lastMessage}" else item.lastMessage) else item.lastMessage,
                                time = formatChatTime(item.timestamp),
                                hasUnread = item.hasUnread,
                                onClick = { onChatClick(item.user.id, item.user.name, item.user.avatarUrl) }
                            )
                        }
                    )
                }
            }
        }
    }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false; userToDeleteId = null },
            title = { Text("Xóa đoạn chat?", fontWeight = FontWeight.Bold) },
            text = { Text("Bạn có chắc chắn muốn xóa vĩnh viễn đoạn hội thoại này không?") },
            confirmButton = {
                TextButton(onClick = {
                    userToDeleteId?.let { chatViewModel.deleteConversation(currentUserId, it) }
                    showDeleteDialog = false
                    userToDeleteId = null
                }) { Text("Xóa", color = Color.Red, fontWeight = FontWeight.Bold) }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false; userToDeleteId = null }) { Text("Hủy", color = Color.Black) }
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
            modifier = Modifier.size(size).clip(CircleShape),
            contentScale = ContentScale.Crop,
            error = painterResource(R.drawable.ava1)
        )
    } else {
        Box(
            modifier = Modifier.size(size).clip(CircleShape).background(Color(0xFFF0F2F5)),
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
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth().background(Color.White).clickable(onClick = onClick).padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(contentAlignment = Alignment.BottomEnd) {
            UserAvatar(name, avatarUrl, 60.dp)
            Box(modifier = Modifier.size(16.dp).clip(CircleShape).background(Color.White).padding(2.dp)) {
                Box(modifier = Modifier.fillMaxSize().clip(CircleShape).background(Color(0xFF42B72A)))
            }
        }
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(text = name, fontSize = 17.sp, fontWeight = if (hasUnread) FontWeight.Bold else FontWeight.Medium, color = Color.Black, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(text = lastMessage, fontSize = 14.sp, color = if (hasUnread) Color.Black else Color.Gray, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f, fill = false), fontWeight = if (hasUnread) FontWeight.Bold else FontWeight.Normal)
                if (time.isNotEmpty()) {
                    Text(text = " • $time", fontSize = 14.sp, color = Color.Gray)
                }
            }
        }
        if (hasUnread) {
            Box(modifier = Modifier.padding(start = 8.dp).size(12.dp).clip(CircleShape).background(Color(0xFF0084FF)))
        }
    }
}

data class ChatDisplayItem(
    val user: ChatUser,
    val lastMessage: String,
    val isFromMe: Boolean,
    val timestamp: com.google.firebase.Timestamp?,
    val hasUnread: Boolean,
    val messageId: String
)

private fun formatChatTime(timestamp: com.google.firebase.Timestamp?): String {
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
