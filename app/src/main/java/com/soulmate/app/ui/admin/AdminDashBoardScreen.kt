package com.soulmate.app.ui.admin

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import androidx.compose.ui.tooling.preview.Preview

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminDashboardScreen(
    navController: NavController,
    viewModel: AdminViewModel = hiltViewModel()
) {
    var selectedTab by remember { mutableIntStateOf(0) }
    val tabs = listOf("Bài viết vi phạm", "Người dùng")

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "🛠️ Admin Control Panel",
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color(0xFF4A148C)
                )
            )
        }
    ) { paddingValues ->
        Column(modifier = Modifier.padding(paddingValues)) {
            TabRow(
                selectedTabIndex = selectedTab,
                containerColor = Color(0xFFF3E5F5),
                contentColor = Color(0xFF4A148C)
            ) {
                tabs.forEachIndexed { index, title ->
                    Tab(
                        selected = selectedTab == index,
                        onClick = { selectedTab = index },
                        text = { Text(title) }
                    )
                }
            }

            when (selectedTab) {
                0 -> ReportedPostsTab(viewModel)
                1 -> ManageUsersTab(viewModel)
            }
        }
    }
}

@Composable
fun ReportedPostsTab(viewModel: AdminViewModel) {
    val posts by viewModel.reportedPosts
    val isLoading by viewModel.isLoadingPosts

    if (isLoading) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = androidx.compose.ui.Alignment.Center) {
            CircularProgressIndicator(color = Color(0xFF4A148C))
        }
    } else if (posts.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = androidx.compose.ui.Alignment.Center) {
            Text("Không có bài viết nào bị báo cáo", color = Color.Gray)
        }
    } else {
        androidx.compose.foundation.lazy.LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(posts.size) { index ->
                val post = posts[index]
                ReportedPostCard(
                    post = post,
                    onDelete = { viewModel.resolveReport(post.id, "delete") },
                    onIgnore = { viewModel.resolveReport(post.id, "ignore") }
                )
            }
        }
    }
}

@Composable
fun ReportedPostCard(
    post: com.soulmate.app.ui.social.CommunityPost,
    onDelete: () -> Unit,
    onIgnore: () -> Unit
) {
    var showDeleteDialog by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                // Badge số lượng report
                Surface(
                    color = Color.Red.copy(alpha = 0.1f),
                    shape = androidx.compose.foundation.shape.RoundedCornerShape(4.dp)
                ) {
                    Text(
                        text = "🚩 ${post.reportCount} Reports",
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        color = Color.Red,
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Bởi: ${post.userName}",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = post.textContent,
                style = MaterialTheme.typography.bodyMedium,
                maxLines = 3,
                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(16.dp))

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                OutlinedButton(
                    onClick = onIgnore,
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.Gray)
                ) {
                    Text("Bỏ qua")
                }
                Spacer(modifier = Modifier.width(8.dp))
                Button(
                    onClick = { showDeleteDialog = true },
                    colors = ButtonDefaults.buttonColors(containerColor = Color.Red)
                ) {
                    Text("Xóa bài")
                }
            }
        }
    }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("Xác nhận xóa") },
            text = { Text("Bạn có chắc chắn muốn xóa bài viết này vĩnh viễn không?") },
            confirmButton = {
                TextButton(onClick = {
                    onDelete()
                    showDeleteDialog = false
                }) {
                    Text("Xác nhận", color = Color.Red)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) {
                    Text("Hủy")
                }
            }
        )
    }
}

@Composable
fun ManageUsersTab(viewModel: AdminViewModel) {val searchQuery by viewModel.searchQuery
    val searchResults by viewModel.searchResults
    val isSearching by viewModel.isSearching

    Column(modifier = Modifier
        .fillMaxSize()
        .padding(16.dp)) {
        OutlinedTextField(
            value = searchQuery,
            onValueChange = { viewModel.onSearchQueryChange(it) },
            modifier = Modifier.fillMaxWidth(),
            placeholder = { Text("Tìm theo tên hoặc Email...") },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
            singleLine = true,
            shape = androidx.compose.foundation.shape.RoundedCornerShape(12.dp)
        )

        Spacer(modifier = Modifier.height(16.dp))

        if (isSearching) {
            Box(modifier = Modifier.fillMaxWidth(), contentAlignment = androidx.compose.ui.Alignment.Center) {
                CircularProgressIndicator(modifier = Modifier.size(32.dp), color = Color(0xFF4A148C))
            }
        } else if (searchResults.isEmpty() && searchQuery.length >= 2) {
            Text("Không tìm thấy người dùng nào", color = Color.Gray, modifier = Modifier.padding(8.dp))
        }

        androidx.compose.foundation.lazy.LazyColumn(
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(searchResults.size) { index ->
                val user = searchResults[index]
                UserAdminCard(
                    user = user,
                    onToggleBan = { isBanned ->
                        viewModel.toggleSocialBan(user.userId, isBanned)
                    }
                )
            }
        }
    }
}

@Composable
fun UserAdminCard(
    user: com.soulmate.app.domain.model.User,
    onToggleBan: (Boolean) -> Unit
) {
    var showConfirmDialog by remember { mutableStateOf(false) }
    var pendingBanStatus by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
        ) {
            // Avatar giả định hoặc Icon
            Surface(
                modifier = Modifier.size(40.dp),
                shape = androidx.compose.foundation.shape.CircleShape,
                color = Color(0xFFE1BEE7)
            ) {
                Box(contentAlignment = androidx.compose.ui.Alignment.Center) {
                    Text(user.anonymousName.take(1).uppercase(), fontWeight = FontWeight.Bold)
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                    Text(user.anonymousName, fontWeight = FontWeight.Bold)
                    if (user.isSocialBanned) {
                        Spacer(modifier = Modifier.width(8.dp))
                        Surface(
                            color = Color.Red,
                            shape = androidx.compose.foundation.shape.RoundedCornerShape(4.dp)
                        ) {
                            Text(
                                "BANNED",
                                color = Color.White,
                                style = MaterialTheme.typography.labelSmall,
                                modifier = Modifier.padding(horizontal = 4.dp)
                            )
                        }
                    }
                }
                Text(user.email, style = MaterialTheme.typography.bodySmall, color = Color.Gray)
            }

            // Nút Ban/Unban
            TextButton(
                onClick = {
                    pendingBanStatus = !user.isSocialBanned
                    showConfirmDialog = true
                },
                colors = ButtonDefaults.textButtonColors(
                    contentColor = if (user.isSocialBanned) Color(0xFF4CAF50) else Color.Red
                )
            ) {
                Text(if (user.isSocialBanned) "Mở khóa" else "Khóa")
            }
        }
    }

    if (showConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showConfirmDialog = false },
            title = { Text(if (pendingBanStatus) "Khóa người dùng" else "Mở khóa") },
            text = { Text("Xác nhận thay đổi quyền cộng đồng của ${user.anonymousName}?") },
            confirmButton = {
                TextButton(onClick = {
                    onToggleBan(pendingBanStatus)
                    showConfirmDialog = false
                }) {
                    Text("Đồng ý")
                }
            },
            dismissButton = {
                TextButton(onClick = { showConfirmDialog = false }) {
                    Text("Hủy")
                }
            }
        )
    }
}
@Preview(showBackground = true, name = "Reported Post Card")
@Composable
fun ReportedPostCardPreview() {
    // Dữ liệu giả để test UI card bài viết
    val dummyPost = com.soulmate.app.ui.social.CommunityPost(
        id = "1",
        userName = "Nguyễn Văn A",
        textContent = "Đây là một nội dung bài viết vi phạm tiêu chuẩn cộng đồng để chúng ta test thử giao diện xem nó hiển thị như thế nào.",
        reportCount = 12
    )
    Box(modifier = Modifier.padding(16.dp)) {
        ReportedPostCard(
            post = dummyPost,
            onDelete = {},
            onIgnore = {}
        )
    }
}

@Preview(showBackground = true, name = "User Admin Card")
@Composable
fun UserAdminCardPreview() {
    // Dữ liệu giả để test UI card user
    val dummyUser = com.soulmate.app.domain.model.User(
        userId = "user123",
        anonymousName = "Soulmate User Test",
        email = "test@gmail.com",
        isSocialBanned = true // Test thử trạng thái bị Ban
    )
    Box(modifier = Modifier.padding(16.dp)) {
        UserAdminCard(
            user = dummyUser,
            onToggleBan = {}
        )
    }
}