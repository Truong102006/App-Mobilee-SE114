package com.soulmate.app.ui.social

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.automirrored.outlined.StickyNote2
import androidx.compose.material.icons.outlined.CameraAlt
import androidx.compose.material.icons.outlined.Gif
import androidx.compose.material.icons.outlined.InsertEmoticon
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage

@Composable
fun CommentSection(
    comments: List<Comment>,
    onAddComment: (String) -> Unit,
    onLikeComment: (String) -> Unit,
    currentUserAvatarUrl: String?,
    currentUserName: String
) {
    var commentText by remember { mutableStateOf("") }

    Column(modifier = Modifier.fillMaxSize()) {
        // Danh sách bình luận dùng LazyColumn để hỗ trợ cuộn
        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            items(comments, key = { it.id }) { comment ->
                CommentItem(
                    comment = comment,
                    onLikeClick = { onLikeComment(comment.id) }
                )
            }
        }

        // Thanh nhập liệu cố định ở dưới cùng
        Surface(
            color = Color(0xFF242526),
            elevation = 8.dp
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    // Avatar người dùng hiện tại
                    if (currentUserAvatarUrl != null) {
                        AsyncImage(
                            model = currentUserAvatarUrl,
                            contentDescription = null,
                            modifier = Modifier.size(36.dp).clip(CircleShape),
                            contentScale = ContentScale.Crop
                        )
                    } else {
                        Box(
                            modifier = Modifier.size(36.dp).clip(CircleShape).background(Color(0xFF3A3B3C)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(currentUserName.take(1).uppercase(), color = Color.White)
                        }
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    // Ô nhập liệu
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(22.dp))
                            .background(Color(0xFF3A3B3C))
                            .padding(horizontal = 12.dp, vertical = 8.dp)
                    ) {
                        BasicTextField(
                            value = commentText,
                            onValueChange = { commentText = it },
                            textStyle = TextStyle(color = Color.White, fontSize = 15.sp),
                            modifier = Modifier.fillMaxWidth(),
                            decorationBox = { innerTextField ->
                                if (commentText.isEmpty()) {
                                    Text("Viết bình luận...", color = Color(0xFFB0B3B8), fontSize = 15.sp)
                                }
                                innerTextField()
                            }
                        )
                    }
                }

                // Hàng icon tiện ích
                Row(
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp, start = 44.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row {
                        Icon(Icons.Outlined.InsertEmoticon, null, tint = Color(0xFFB0B3B8), modifier = Modifier.size(22.dp))
                        Spacer(modifier = Modifier.width(16.dp))
                        Icon(Icons.Outlined.CameraAlt, null, tint = Color(0xFFB0B3B8), modifier = Modifier.size(22.dp))
                        Spacer(modifier = Modifier.width(16.dp))
                        Icon(Icons.Outlined.Gif, null, tint = Color(0xFFB0B3B8), modifier = Modifier.size(22.dp))
                        Spacer(modifier = Modifier.width(16.dp))
                        Icon(Icons.AutoMirrored.Outlined.StickyNote2, null, tint = Color(0xFFB0B3B8), modifier = Modifier.size(22.dp))
                    }
                    
                    IconButton(
                        onClick = {
                            if (commentText.isNotBlank()) {
                                onAddComment(commentText)
                                commentText = ""
                            }
                        },
                        modifier = Modifier.size(24.dp)
                    ) {
                        Icon(
                            Icons.AutoMirrored.Filled.Send,
                            contentDescription = "Send",
                            tint = if (commentText.isNotBlank()) Color(0xFF2D88FF) else Color(0xFF4E4F50)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun CommentItem(comment: Comment, onLikeClick: () -> Unit) {
    Row(modifier = Modifier.fillMaxWidth()) {
        if (comment.userAvatarUrl != null) {
            AsyncImage(
                model = comment.userAvatarUrl,
                contentDescription = null,
                modifier = Modifier.size(36.dp).clip(CircleShape),
                contentScale = ContentScale.Crop
            )
        } else {
            Box(
                modifier = Modifier.size(36.dp).clip(CircleShape).background(Color(0xFF3A3B3C)),
                contentAlignment = Alignment.Center
            ) {
                Text(comment.userName.take(1).uppercase(), color = Color.White, fontSize = 14.sp)
            }
        }

        Spacer(modifier = Modifier.width(8.dp))

        Column {
            Surface(
                color = Color(0xFF3A3B3C),
                shape = RoundedCornerShape(18.dp)
            ) {
                Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)) {
                    Text(text = comment.userName, fontWeight = FontWeight.Bold, fontSize = 13.sp, color = Color.White)
                    Text(text = comment.content, fontSize = 15.sp, color = Color.White)
                }
            }
            Row(
                modifier = Modifier.padding(start = 8.dp, top = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(text = comment.timeAgo, fontSize = 12.sp, color = Color(0xFFB0B3B8))
                Spacer(modifier = Modifier.width(16.dp))
                Text(
                    text = "Thích",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (comment.isLiked) Color(0xFF2D88FF) else Color(0xFFB0B3B8),
                    modifier = Modifier.clickable { onLikeClick() }
                )
                if (comment.likeCount > 0) {
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(text = comment.likeCount.toString(), fontSize = 12.sp, color = Color(0xFFB0B3B8))
                }
                Spacer(modifier = Modifier.width(16.dp))
                Text(text = "Trả lời", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color(0xFFB0B3B8), modifier = Modifier.clickable { })
            }
        }
    }
}
