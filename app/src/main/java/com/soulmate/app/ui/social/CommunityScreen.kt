package com.soulmate.app.ui.social

import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.soulmate.app.ui.login.AuthViewModel

@Composable
fun CommunityScreen(
    viewModel: CommunityViewModel = hiltViewModel(),
    authViewModel: AuthViewModel = hiltViewModel(),
    onNavigateToChat: (String, String, String?) -> Unit = { _, _, _ -> }
) {
    val feedPosts by viewModel.posts
    val currentUser by authViewModel.currentUser
    val allComments = viewModel.postComments

    Scaffold(
        topBar = {
            Column(modifier = Modifier.background(MaterialTheme.colors.surface)) {
                Spacer(modifier = Modifier.height(30.dp))
                TopAppBar(
                    title = { Text("Feeds", fontWeight = FontWeight.Bold, fontSize = 24.sp) },
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
                    comments = allComments[post.id] ?: emptyList(), // TRUYỀN DANH SÁCH BÌNH LUẬN Ở ĐÂY
                    onLikeClick = { viewModel.toggleLike(post.id) },
                    onOpenComments = { viewModel.loadComments(post.id) }, // LOAD BÌNH LUẬN KHI MỞ
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
                    onUserClick = {
                        if (post.userId.isNotEmpty()) {
                            onNavigateToChat(post.userId, post.userName, post.userAvatarUrl)
                        }
                    }
                )
            }
        }
    }
}
