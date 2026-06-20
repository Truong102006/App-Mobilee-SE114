package com.soulmate.app.ui.social

import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.soulmate.app.ui.login.AuthViewModel
import com.soulmate.app.ui.presence.PresenceViewModel

@Composable
fun CommunityScreen(
    viewModel: CommunityViewModel = hiltViewModel(),
    authViewModel: AuthViewModel = hiltViewModel(),
    presenceViewModel: PresenceViewModel = hiltViewModel(),
    onNavigateToChat: (String, String, String?) -> Unit = { _, _, _ -> }
) {
    val feedPosts by viewModel.posts
    val currentUser by authViewModel.currentUser
    val allComments = viewModel.postComments
    val presenceMap by presenceViewModel.userPresences.collectAsState()
    val onlineUsersCount by presenceViewModel.onlineUsersCount.collectAsState()

    val trackedUserIds = remember(feedPosts, allComments.values.toList()) {
        buildSet {
            feedPosts
                .map { it.userId }
                .filter(String::isNotBlank)
                .forEach(::add)

            allComments.values
                .flatten()
                .map { it.userId }
                .filter(String::isNotBlank)
                .forEach(::add)
        }
    }

    LaunchedEffect(trackedUserIds) {
        presenceViewModel.observeUsers(trackedUserIds)
    }

    Scaffold(
        topBar = {
            Column(modifier = Modifier.background(MaterialTheme.colors.surface)) {
                Spacer(modifier = Modifier.height(30.dp))
                TopAppBar(
                    title = {
                        Column {
                            Text("Feeds", fontWeight = FontWeight.Bold, fontSize = 24.sp)
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "Hiện có $onlineUsersCount người đang thức cùng bạn",
                                fontSize = 13.sp,
                                color = MaterialTheme.colors.onSurface.copy(alpha = 0.65f),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    },
                    backgroundColor = MaterialTheme.colors.surface,
                    contentColor = MaterialTheme.colors.primary,
                    elevation = 0.dp
                )
            }
        },
        backgroundColor = MaterialTheme.colors.background
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            items(feedPosts, key = { it.id }) { post ->
                CommunityCard(
                    post = post,
                    comments = allComments[post.id] ?: emptyList(),
                    onLikeClick = { viewModel.toggleLike(post.id) },
                    onOpenComments = { viewModel.loadComments(post.id) },
                    onHideClick = { viewModel.hidePost(post.id) },
                    onReportClick = { viewModel.reportPost(post.id) },
                    onCommentClick = { content, parentId, replyToUserName -> 
                        viewModel.addComment(
                            post.id, 
                            currentUser?.userId ?: "", 
                            currentUser?.anonymousName ?: "User", 
                            currentUser?.avatarUrl, 
                            content,
                            parentId,
                            replyToUserName
                        ) 
                    },
                    onLikeComment = { commentId -> viewModel.toggleCommentLike(post.id, commentId) },
                    onDeleteClick = { viewModel.deletePost(post.id) },
                    onEditClick = { newContent -> viewModel.updatePostContent(post.id, newContent) },
                    currentUserAvatarUrl = currentUser?.avatarUrl,
                    currentUserName = currentUser?.anonymousName ?: "User",
                    presenceMap = presenceMap,
                    onUserClick = {
                        if (post.userId.isNotEmpty()) {
                            onNavigateToChat(post.userId, post.userName, post.userAvatarUrl)
                        }
                    }
                )
            }
        }

        val showBannedDialog by viewModel.showBannedDialog

        if (showBannedDialog) {
            AlertDialog(
                onDismissRequest = { viewModel.dismissBannedDialog() },
                title = { Text("Hành động bị từ chối", fontWeight = FontWeight.Bold) },
                text = { Text("Tài khoản của bạn hiện đang bị hạn chế các tính năng cộng đồng do vi phạm tiêu chuẩn cộng đồng.") },
                confirmButton = {
                    TextButton(onClick = { viewModel.dismissBannedDialog() }) {
                        Text("Tôi đã hiểu")
                    }
                },
                shape = RoundedCornerShape(16.dp)
            )
        }
    }
}
