package com.soulmate.app.ui.social

import androidx.compose.foundation.Canvas
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
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.outlined.CameraAlt
import androidx.compose.material.icons.outlined.Gif
import androidx.compose.material.icons.outlined.InsertEmoticon
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage

@Composable
fun CommentSection(
    comments: List<Comment>,
    postAuthorId: String?,
    onAddComment: (content: String, parentId: String?, replyToUserName: String?) -> Unit,
    onLikeComment: (String) -> Unit,
    currentUserAvatarUrl: String?,
    currentUserName: String
) {
    var commentText by remember { mutableStateOf("") }
    var replyingTo by remember { mutableStateOf<Comment?>(null) }

    // Phân nhóm bình luận: Gốc và Phản hồi
    val rootComments = comments.filter { it.parentId == null }
    val repliesMap = comments.filter { it.parentId != null }.groupBy { it.parentId }

    Column(modifier = Modifier.fillMaxSize().background(Color(0xFF18191A))) {
        if (rootComments.isEmpty()) {
            Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                Text(text = "Chưa có bình luận nào.", color = Color.Gray, fontSize = 14.sp)
            }
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f).fillMaxWidth(),
                contentPadding = PaddingValues(16.dp)
            ) {
                rootComments.forEach { rootComment ->
                    item(key = rootComment.id) {
                        CommentItem(
                            comment = rootComment,
                            isPostAuthor = rootComment.userId == postAuthorId,
                            onLikeClick = { onLikeComment(rootComment.id) },
                            onReplyClick = { replyingTo = rootComment },
                            isReply = false,
                            hasReplies = repliesMap.containsKey(rootComment.id)
                        )
                    }
                    
                    val replies = repliesMap[rootComment.id] ?: emptyList()
                    items(replies, key = { it.id }) { reply ->
                        CommentItem(
                            comment = reply,
                            isPostAuthor = reply.userId == postAuthorId,
                            onLikeClick = { onLikeComment(reply.id) },
                            onReplyClick = { replyingTo = reply },
                            isReply = true,
                            isLastReply = reply == replies.last()
                        )
                    }
                }
            }
        }

        // Thanh trạng thái phản hồi
        replyingTo?.let { comment ->
            Row(
                modifier = Modifier.fillMaxWidth().background(Color(0xFF242526)).padding(16.dp, 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = buildAnnotatedString {
                        append("Đang phản hồi ")
                        withStyle(SpanStyle(fontWeight = FontWeight.Bold)) { append(comment.userName) }
                        append(" • ")
                    },
                    color = Color.White, fontSize = 13.sp
                )
                Text(
                    text = "Hủy",
                    color = Color.White,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.clickable { replyingTo = null }
                )
            }
            Divider(color = Color(0xFF3E4042), thickness = 0.5.dp)
        }

        // Ô nhập liệu
        Surface(color = Color(0xFF242526), elevation = 8.dp) {
            Column(modifier = Modifier.padding(12.dp)) {
                if (replyingTo != null) {
                    Surface(
                        color = Color(0xFF1D375A),
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier.padding(bottom = 8.dp, start = 44.dp)
                    ) {
                        Text(
                            text = replyingTo?.userName ?: "",
                            color = Color(0xFF2D88FF),
                            fontSize = 13.sp,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                            fontWeight = FontWeight.Medium
                        )
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(modifier = Modifier.size(36.dp).clip(CircleShape).background(Color(0xFF3A3B3C))) {
                        if (currentUserAvatarUrl != null) {
                            AsyncImage(model = currentUserAvatarUrl, contentDescription = null, contentScale = ContentScale.Crop)
                        } else {
                            Text(currentUserName.take(1).uppercase(), color = Color.White, modifier = Modifier.align(Alignment.Center))
                        }
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Box(modifier = Modifier.weight(1f).clip(RoundedCornerShape(22.dp)).background(Color(0xFF3A3B3C)).padding(12.dp, 8.dp)) {
                        BasicTextField(
                            value = commentText,
                            onValueChange = { commentText = it },
                            textStyle = TextStyle(color = Color.White, fontSize = 15.sp),
                            modifier = Modifier.fillMaxWidth(),
                            decorationBox = { if (commentText.isEmpty()) Text("Viết bình luận...", color = Color(0xFFB0B3B8), fontSize = 15.sp); it() }
                        )
                    }
                }

                Row(modifier = Modifier.fillMaxWidth().padding(top = 10.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Row {
                        Icon(Icons.Outlined.CameraAlt, null, tint = Color(0xFFB0B3B8), modifier = Modifier.size(24.dp))
                        Spacer(Modifier.width(16.dp))
                        Icon(Icons.Outlined.Gif, null, tint = Color(0xFFB0B3B8), modifier = Modifier.size(24.dp))
                        Spacer(Modifier.width(16.dp))
                        Icon(Icons.Outlined.InsertEmoticon, null, tint = Color(0xFFB0B3B8), modifier = Modifier.size(24.dp))
                    }
                    IconButton(
                        onClick = {
                            if (commentText.isNotBlank()) {
                                // Bình luận con sẽ có parentId là ID của bình luận gốc
                                val parentId = replyingTo?.parentId ?: replyingTo?.id
                                val replyToUserName = replyingTo?.userName
                                onAddComment(commentText, parentId, replyToUserName)
                                commentText = ""; replyingTo = null
                            }
                        },
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(Icons.AutoMirrored.Filled.Send, null, tint = if (commentText.isNotBlank()) Color(0xFF2D88FF) else Color(0xFF4E4F50))
                    }
                }
            }
        }
    }
}

@Composable
fun CommentItem(
    comment: Comment,
    isPostAuthor: Boolean,
    onLikeClick: () -> Unit,
    onReplyClick: () -> Unit,
    isReply: Boolean,
    hasReplies: Boolean = false,
    isLastReply: Boolean = false
) {
    Box(modifier = Modifier.fillMaxWidth()) {
        // Vẽ đường kẻ (Threads) bám sát theo ảnh mẫu
        if (isReply || hasReplies) {
            Canvas(modifier = Modifier.matchParentSize()) {
                val startX = 18.dp.toPx()
                if (hasReplies) {
                    drawLine(Color(0xFF3E4042), Offset(startX, 40.dp.toPx()), Offset(startX, size.height), 1.5f)
                }
                if (isReply) {
                    val midY = 16.dp.toPx()
                    drawLine(Color(0xFF3E4042), Offset(startX, 0f), Offset(startX, midY), 1.5f)
                    drawLine(Color(0xFF3E4042), Offset(startX, midY), Offset(startX + 26.dp.toPx(), midY), 1.5f)
                    if (!isLastReply) drawLine(Color(0xFF3E4042), Offset(startX, midY), Offset(startX, size.height), 1.5f)
                }
            }
        }

        Row(modifier = Modifier.fillMaxWidth().padding(start = if (isReply) 44.dp else 0.dp, bottom = 12.dp)) {
            Box(modifier = Modifier.size(if (isReply) 32.dp else 36.dp).clip(CircleShape).background(Color(0xFF3A3B3C))) {
                if (comment.userAvatarUrl != null) {
                    AsyncImage(model = comment.userAvatarUrl, contentDescription = null, contentScale = ContentScale.Crop)
                } else {
                    Text(comment.userName.take(1).uppercase(), color = Color.White, modifier = Modifier.align(Alignment.Center), fontSize = 12.sp)
                }
            }
            Spacer(modifier = Modifier.width(8.dp))
            Column {
                Surface(color = Color(0xFF3A3B3C), shape = RoundedCornerShape(18.dp)) {
                    Column(modifier = Modifier.padding(12.dp, 8.dp)) {
                        if (isPostAuthor) {
                            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(bottom = 2.dp)) {
                                Icon(Icons.Default.Edit, null, tint = Color(0xFFB0B3B8), modifier = Modifier.size(12.dp))
                                Spacer(Modifier.width(4.dp))
                                Text("Tác giả", color = Color(0xFFB0B3B8), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                        Text(text = comment.userName, fontWeight = FontWeight.Bold, fontSize = 13.sp, color = Color.White)
                        Text(
                            text = buildAnnotatedString {
                                if (comment.replyToUserName != null) {
                                    withStyle(SpanStyle(color = Color(0xFF2D88FF), fontWeight = FontWeight.Bold)) {
                                        append(comment.replyToUserName)
                                    }
                                    append(" ")
                                }
                                append(comment.content)
                            },
                            fontSize = 15.sp, color = Color.White
                        )
                    }
                }
                Row(modifier = Modifier.padding(start = 8.dp, top = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text(comment.timeAgo, fontSize = 12.sp, color = Color(0xFFB0B3B8))
                    Spacer(Modifier.width(16.dp))
                    Text("Thích", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = if (comment.isLiked) Color(0xFF2D88FF) else Color(0xFFB0B3B8), modifier = Modifier.clickable { onLikeClick() })
                    Spacer(Modifier.width(16.dp))
                    Text("Trả lời", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color(0xFFB0B3B8), modifier = Modifier.clickable { onReplyClick() })
                }
            }
        }
    }
}
