package com.soulmate.app.ui.social

import android.text.TextUtils
import android.widget.TextView
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Card
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.outlined.ChatBubbleOutline
import androidx.compose.material.icons.outlined.Favorite
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.RemoveRedEye
import androidx.compose.material.*
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.text.HtmlCompat
import coil.compose.AsyncImage
import com.soulmate.app.ui.journal.editor.Mood
import com.soulmate.app.ui.theme.CommunityTick

@Composable
fun ActionPillButton(
    icon: ImageVector,
    text: String,
    iconTint: Color = MaterialTheme.colors.onSurface.copy(alpha = 0.6f),
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .clickable { onClick() },
        shape = RoundedCornerShape(50),
        border = BorderStroke(1.dp, MaterialTheme.colors.onSurface.copy(alpha = 0.1f)),
        color = Color.Transparent
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(16.dp),
                tint = iconTint
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = text,
                fontSize = 13.sp,
                color = MaterialTheme.colors.onSurface.copy(alpha = 0.6f),
                fontWeight = FontWeight.Medium
            )
        }
    }
}

@Composable
fun HtmlText(
    html: String,
    textColor: androidx.compose.ui.graphics.Color,
    maxLines: Int = Int.MAX_VALUE,
    onTextOverflow: (Boolean) -> Unit = {},
    modifier: Modifier = Modifier
) {
    AndroidView(
        modifier = modifier,
        factory = { context ->
            TextView(context).apply {
                textSize = 15f
                setTextColor(textColor.toArgb())
                ellipsize = TextUtils.TruncateAt.END
            }
        },
        update = { textView ->
            textView.text = HtmlCompat.fromHtml(html, HtmlCompat.FROM_HTML_MODE_COMPACT)
            textView.maxLines = maxLines

            textView.post {
                val layout = textView.layout
                if (layout != null) {
                    val lines = layout.lineCount
                    if (lines > 0 && maxLines != Int.MAX_VALUE) {
                        val ellipsisCount = layout.getEllipsisCount(lines - 1)
                        if (ellipsisCount > 0) {
                            onTextOverflow(true)
                        }
                    }
                }
            }
        }
    )
}

@OptIn(ExperimentalMaterialApi::class)
@Composable
fun CommunityCard(
    post: CommunityPost,
    comments: List<Comment> = emptyList(),
    onLikeClick: () -> Unit,
    onCommentClick: (String, String?, String?) -> Unit,
    onLikeComment: (String) -> Unit,
    onOpenComments: () -> Unit = {},
    onDeleteClick: () -> Unit,
    onEditClick: (String) -> Unit,
    currentUserAvatarUrl: String?,
    currentUserName: String,
    onUserClick: () -> Unit = {},
    onChatClick: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    var showMenu by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf(false) }
    var showCommentsModal by remember { mutableStateOf(false) }
    var showEditDialog by remember { mutableStateOf(false) }
    var editedText by remember { mutableStateOf(post.textContent) }
    
    // State to handle full screen image viewing
    var fullScreenImageUrl by remember { mutableStateOf<String?>(null) }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .border(1.5.dp, MaterialTheme.colors.primary.copy(alpha = 0.5f), RoundedCornerShape(20.dp)),
        shape = RoundedCornerShape(24.dp),
        backgroundColor = MaterialTheme.colors.surface,
        elevation = 2.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            // --- HEADER ---
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    modifier = Modifier.weight(1f).clickable { onUserClick() },
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (post.userAvatarUrl != null) {
                        AsyncImage(
                            model = post.userAvatarUrl,
                            contentDescription = null,
                            modifier = Modifier
                                .size(48.dp)
                                .clip(CircleShape),
                            contentScale = ContentScale.Crop
                        )
                    } else {
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colors.primary.copy(alpha = 0.1f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = post.userName.take(1).uppercase(),
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colors.primary,
                                fontSize = 20.sp
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = post.userName,
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp,
                                color = MaterialTheme.colors.onSurface
                            )
                            if (post.isVerified) {
                                Spacer(modifier = Modifier.width(4.dp))
                                Icon(
                                    imageVector = Icons.Rounded.CheckCircle,
                                    contentDescription = "Verified",
                                    tint = CommunityTick,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }

                        val moodEnum = Mood.entries.find { it.label == post.mood } ?: Mood.Neutral
                        val moodColor = moodEnum.displayColor

                        val moodTimeText = buildAnnotatedString {
                            append("Feeling ")
                            withStyle(style = SpanStyle(color = moodColor, fontWeight = FontWeight.Bold)) {
                                append(post.mood)
                            }
                            append(", ${post.timeAgo}")
                        }
                        Text(text = moodTimeText, fontSize = 13.sp, color = MaterialTheme.colors.onSurface.copy(alpha = 0.6f))
                    }
                }

                Box {
                    IconButton(onClick = { showMenu = true }) {
                        Icon(
                            imageVector = Icons.Default.MoreVert,
                            contentDescription = "Options",
                            tint = MaterialTheme.colors.onSurface.copy(alpha = 0.6f)
                        )
                    }
                    DropdownMenu(
                        expanded = showMenu,
                        onDismissRequest = { showMenu = false }
                    ) {
                        DropdownMenuItem(onClick = {
                            showMenu = false
                            onChatClick()
                        }) {
                            Icon(Icons.AutoMirrored.Filled.Chat, contentDescription = null, modifier = Modifier.size(18.dp), tint = Color(0xFF0084FF))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Chat")
                        }
                        DropdownMenuItem(onClick = {
                            showMenu = false
                            showDeleteDialog = true
                        }) {
                            Icon(Icons.Default.Delete, contentDescription = null, tint = Color.Red, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Delete", color = Color.Red)
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // --- BODY ---
            var isExpanded by remember { mutableStateOf(false) }
            var hasOverflow by remember { mutableStateOf(false) }

            Column(modifier = Modifier.animateContentSize()) {
                HtmlText(
                    html = post.textContent,
                    textColor = MaterialTheme.colors.onSurface,
                    maxLines = if (isExpanded) Int.MAX_VALUE else 3,
                    onTextOverflow = { isOverflowing -> hasOverflow = isOverflowing },
                    modifier = Modifier.fillMaxWidth()
                )

                if (hasOverflow) {
                    Text(
                        text = if (isExpanded) "Show less" else "Read more...",
                        color = MaterialTheme.colors.onSurface.copy(alpha = 0.5f),
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier
                            .padding(top = 6.dp)
                            .clickable { isExpanded = !isExpanded }
                    )
                }
            }

            if (post.imageUrls.isNotEmpty()) {
                Spacer(modifier = Modifier.height(12.dp))
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    items(post.imageUrls) { imageUrl ->
                        AsyncImage(
                            model = imageUrl,
                            contentDescription = "Post Image",
                            modifier = Modifier
                                .size(120.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .border(1.dp, MaterialTheme.colors.onSurface.copy(alpha = 0.05f), RoundedCornerShape(12.dp))
                                .clickable { fullScreenImageUrl = imageUrl }, // Bấm để xem full
                            contentScale = ContentScale.Crop
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // --- FOOTER ---
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                ActionPillButton(
                    icon = if (post.isLiked) Icons.Outlined.Favorite else Icons.Outlined.FavoriteBorder,
                    text = post.likeCount.toString(),
                    iconTint = if (post.isLiked) Color.Red else MaterialTheme.colors.onSurface.copy(alpha = 0.6f),
                    onClick = onLikeClick
                )

                Spacer(modifier = Modifier.width(8.dp))

                ActionPillButton(
                    icon = Icons.Outlined.ChatBubbleOutline,
                    text = post.commentCount.toString(),
                    onClick = { 
                        onOpenComments()
                        showCommentsModal = true 
                    }
                )

                Spacer(modifier = Modifier.width(8.dp))

                IconButton(onClick = { /* TODO */ }, modifier = Modifier.size(32.dp).clip(CircleShape)) {
                    Icon(
                        imageVector = Icons.Outlined.Share,
                        contentDescription = "Share",
                        modifier = Modifier.size(14.dp),
                        tint = MaterialTheme.colors.onSurface.copy(alpha = 0.6f)
                    )
                }

                Spacer(modifier = Modifier.weight(1f))

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Rounded.RemoveRedEye,
                        contentDescription = "Views",
                        modifier = Modifier.size(16.dp),
                        tint = MaterialTheme.colors.onSurface.copy(alpha = 0.4f)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = post.viewCount.toString(),
                        fontSize = 13.sp,
                        color = MaterialTheme.colors.onSurface.copy(alpha = 0.4f),
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }
    }

    // --- FULL SCREEN IMAGE VIEW ---
    if (fullScreenImageUrl != null) {
        Dialog(
            onDismissRequest = { fullScreenImageUrl = null },
            properties = DialogProperties(usePlatformDefaultWidth = false)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black)
                    .clickable { fullScreenImageUrl = null }
            ) {
                AsyncImage(
                    model = fullScreenImageUrl,
                    contentDescription = "Full Image",
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Fit
                )
                IconButton(
                    onClick = { fullScreenImageUrl = null },
                    modifier = Modifier
                        .padding(16.dp)
                        .align(Alignment.TopStart)
                        .statusBarsPadding()
                        .background(Color.Black.copy(alpha = 0.3f), CircleShape)
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Close",
                        tint = Color.White,
                        modifier = Modifier.size(32.dp)
                    )
                }
            }
        }
    }

    // --- COMMENTS MODAL ---
    if (showCommentsModal) {
        Dialog(
            onDismissRequest = { showCommentsModal = false },
            properties = DialogProperties(usePlatformDefaultWidth = false)
        ) {
            Scaffold(
                topBar = {
                    TopAppBar(
                        title = {
                            Text("Bài viết của ${post.userName}", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color.White)
                        },
                        actions = {
                            IconButton(onClick = { showCommentsModal = false }) {
                                Icon(Icons.Default.Close, contentDescription = "Close", tint = Color.White)
                            }
                        },
                        backgroundColor = Color(0xFF242526),
                        elevation = 0.dp
                    )
                },
                backgroundColor = Color(0xFF18191A)
            ) { padding ->
                Box(modifier = Modifier.fillMaxSize().padding(padding).background(Color(0xFF18191A))) {
                    CommentSection(
                        comments = comments,
                        postAuthorId = post.userId, // BỔ SUNG THIẾU SÓT QUAN TRỌNG
                        onAddComment = onCommentClick,
                        onLikeComment = onLikeComment,
                        currentUserAvatarUrl = currentUserAvatarUrl,
                        currentUserName = currentUserName
                    )
                }
            }
        }
    }

    // Dialogs...
    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("Delete Post", fontWeight = FontWeight.Bold) },
            text = { Text("Are you sure you want to delete this post?") },
            confirmButton = {
                TextButton(onClick = {
                    showDeleteDialog = false
                    onDeleteClick()
                }) {
                    Text("Delete", color = Color.Red, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) {
                    Text("Cancel")
                }
            },
            shape = RoundedCornerShape(16.dp)
        )
    }

    if (showEditDialog) {
        AlertDialog(
            onDismissRequest = { showEditDialog = false },
            title = { Text("Edit Post", fontWeight = FontWeight.Bold) },
            text = {
                Column {
                    OutlinedTextField(
                        value = editedText,
                        onValueChange = { editedText = it },
                        modifier = Modifier.fillMaxWidth().height(150.dp),
                        placeholder = { Text("Edit your content...") }
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    showEditDialog = false
                    onEditClick(editedText)
                }) {
                    Text("Save", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showEditDialog = false }) {
                    Text("Cancel")
                }
            },
            shape = RoundedCornerShape(16.dp)
        )
    }
}
