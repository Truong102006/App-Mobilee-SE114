package com.soulmate.app.ui.chat

import android.net.Uri
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
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.soulmate.app.R
import com.soulmate.app.ui.social.CommunityViewModel

@OptIn(ExperimentalMaterialApi::class)
@Composable
fun ChatListScreen(
    communityViewModel: CommunityViewModel,
    onChatClick: (String, String?) -> Unit,
    onBackClick: () -> Unit
) {
    val posts by communityViewModel.posts
    var searchQuery by remember { mutableStateOf("") }
    
    // Lấy danh sách user duy nhất từ các bài đăng community
    val allUsers = remember(posts) {
        posts.distinctBy { it.userName }.map {
            ChatUser(it.userName, it.userAvatarUrl)
        }
    }

    val filteredUsers = remember(allUsers, searchQuery) {
        if (searchQuery.isEmpty()) {
            allUsers
        } else {
            allUsers.filter { it.name.contains(searchQuery, ignoreCase = true) }
        }
    }

    Scaffold(
        modifier = Modifier.statusBarsPadding(),
        topBar = {
            TopAppBar(
                backgroundColor = Color.White,
                elevation = 0.dp,
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = Color.Black
                        )
                    }
                },
                title = {
                    Text(
                        "Messenger",
                        fontSize = 24.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color.Black
                    )
                },
                actions = {
                    IconButton(onClick = {}) {
                        Icon(Icons.Default.Edit, contentDescription = null, tint = Color.Black)
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
            // Search Bar
            Box(
                modifier = Modifier
                    .padding(horizontal = 16.dp, vertical = 8.dp)
                    .fillMaxWidth()
                    .height(40.dp)
                    .clip(RoundedCornerShape(20.dp))
                    .background(Color(0xFFF0F2F5))
                    .padding(horizontal = 12.dp),
                contentAlignment = Alignment.CenterStart
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Search, contentDescription = null, tint = Color.Gray, modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    BasicTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        textStyle = TextStyle(fontSize = 15.sp, color = Color.Black),
                        modifier = Modifier.fillMaxWidth(),
                        decorationBox = { innerTextField ->
                            if (searchQuery.isEmpty()) {
                                Text("Tìm kiếm tài khoản", color = Color.Gray, fontSize = 15.sp)
                            }
                            innerTextField()
                        }
                    )
                }
            }

            LazyColumn(modifier = Modifier.fillMaxSize()) {
                // Stories/Active Users
                item {
                    LazyRow(
                        modifier = Modifier.padding(vertical = 8.dp),
                        contentPadding = PaddingValues(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        item {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Box(contentAlignment = Alignment.BottomEnd) {
                                    Box(
                                        modifier = Modifier
                                            .size(60.dp)
                                            .clip(CircleShape)
                                            .background(Color(0xFFF0F2F5)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(30.dp))
                                    }
                                    Box(
                                        modifier = Modifier
                                            .size(20.dp)
                                            .clip(CircleShape)
                                            .background(Color.White)
                                            .padding(2.dp)
                                    ) {
                                        Box(modifier = Modifier.fillMaxSize().clip(CircleShape).background(Color.LightGray), contentAlignment = Alignment.Center) {
                                            Text("+", color = Color.White, fontSize = 12.sp)
                                        }
                                    }
                                }
                                Text("Tạo tin", fontSize = 12.sp, modifier = Modifier.padding(top = 4.dp))
                            }
                        }
                        items(allUsers) { user ->
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Box(contentAlignment = Alignment.BottomEnd) {
                                    AsyncImage(
                                        model = user.avatarUrl ?: R.drawable.ava1,
                                        contentDescription = null,
                                        modifier = Modifier
                                            .size(60.dp)
                                            .clip(CircleShape),
                                        contentScale = ContentScale.Crop
                                    )
                                    Box(
                                        modifier = Modifier
                                            .size(15.dp)
                                            .clip(CircleShape)
                                            .background(Color.White)
                                            .padding(2.dp)
                                    ) {
                                        Box(modifier = Modifier.fillMaxSize().clip(CircleShape).background(Color(0xFF42B72A)))
                                    }
                                }
                                Text(
                                    user.name.split(" ").firstOrNull() ?: "",
                                    fontSize = 12.sp,
                                    modifier = Modifier.padding(top = 4.dp)
                                )
                            }
                        }
                    }
                }

                // Chat Items
                items(filteredUsers, key = { it.name }) { user ->
                    var isVisible by remember { mutableStateOf(true) }
                    
                    if (isVisible) {
                        val dismissState = rememberDismissState(
                            confirmStateChange = {
                                if (it == DismissValue.DismissedToStart) {
                                    isVisible = false
                                    true
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
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .background(color)
                                        .padding(horizontal = 20.dp),
                                    contentAlignment = Alignment.CenterEnd
                                ) {
                                    Icon(
                                        Icons.Default.Delete,
                                        contentDescription = "Delete",
                                        tint = Color.White
                                    )
                                }
                            },
                            dismissContent = {
                                ChatItem(
                                    name = user.name,
                                    avatarUrl = user.avatarUrl,
                                    lastMessage = "Chào bạn! Mình thấy bài viết của bạn trên community...",
                                    time = "12:48",
                                    hasUnread = true,
                                    onClick = { onChatClick(user.name, user.avatarUrl) }
                                )
                            }
                        )
                    }
                }
            }
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
        modifier = Modifier
            .fillMaxWidth()
            .background(Color.White)
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        AsyncImage(
            model = avatarUrl ?: R.drawable.ava1,
            contentDescription = null,
            modifier = Modifier
                .size(60.dp)
                .clip(CircleShape),
            contentScale = ContentScale.Crop,
            error = coil.compose.rememberAsyncImagePainter(R.drawable.ava1)
        )
        
        Spacer(modifier = Modifier.width(12.dp))
        
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = name,
                fontSize = 16.sp,
                fontWeight = if (hasUnread) FontWeight.Bold else FontWeight.Normal,
                color = Color.Black,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = lastMessage,
                    fontSize = 14.sp,
                    color = if (hasUnread) Color.Black else Color.Gray,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f, fill = false)
                )
                Text(
                    text = " • $time",
                    fontSize = 14.sp,
                    color = Color.Gray
                )
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

data class ChatUser(
    val name: String,
    val avatarUrl: String?
)
