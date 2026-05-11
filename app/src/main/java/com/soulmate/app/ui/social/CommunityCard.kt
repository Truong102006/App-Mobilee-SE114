package com.soulmate.app.ui.social

import android.text.TextUtils
import android.widget.TextView
import androidx.compose.animation.animateContentSize
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
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.rounded.RemoveRedEye
import androidx.compose.material.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.getValue
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
import androidx.core.text.HtmlCompat
import coil.compose.AsyncImage
import com.soulmate.app.ui.journal.editor.Mood

data class CommunityPost(
    val id: String,
    val userName: String,
    val userAvatarUrl: String?,
    val mood: String?,
    val timeAgo: String,
    val textContent: String,
    val imageUrls: List<String>,
    val likeCount: Int,
    val commentCount: Int,
    val viewCount: Int
)

@OptIn(ExperimentalMaterialApi::class)
@Composable
fun ActionPillButton(
    icon: ImageVector,
    text: String,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
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
                tint = MaterialTheme.colors.onSurface.copy(alpha = 0.6f)
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
        modifier: Modifier = Modifier)
    {
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

@Composable
fun CommunityCard(
    post: CommunityPost,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .border(1.5.dp, MaterialTheme.colors.primary.copy(alpha = 0.5f), RoundedCornerShape(20.dp)),
        shape = RoundedCornerShape(24.dp),
        backgroundColor = androidx.compose.material.MaterialTheme.colors.surface,
        elevation = 2.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            // --- HEADER: Avatar, Tên, Tích xanh, Cảm xúc & Thời gian ---
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Avatar (Dùng Box nền xám làm Placeholder tạm)
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

                Spacer(modifier = Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    // Tên + Tích xanh
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = post.userName,
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp,
                            color = MaterialTheme.colors.onSurface
                        )
                    }

                    // Cảm xúc + Thời gian (Theo đúng yêu cầu của bạn)
                    val moodEnum = Mood.entries.find { it.label == post.mood } ?: Mood.Neutral
                    val moodColor = moodEnum.displayColor

                    val moodTimeText = buildAnnotatedString {
                        append("Feeling ")
                        withStyle(
                            style = SpanStyle(
                                color = moodColor,
                                fontWeight = FontWeight.Bold
                            )
                        ) {
                            append(post.mood)
                        }
                        append(", ${post.timeAgo}")
                    }
                    Text(text = moodTimeText, fontSize = 13.sp, color = MaterialTheme.colors.onSurface.copy(alpha = 0.6f))
                }

                // Nút 3 chấm (Options)
                IconButton(onClick = { /* TODO: Mở menu */ }) {
                    Icon(
                        imageVector = Icons.Default.MoreVert,
                        contentDescription = "Options",
                        tint = MaterialTheme.colors.onSurface.copy(alpha = 0.6f)
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // --- BODY: Text Content ---
            var isExpanded by remember { mutableStateOf(false) }
            var hasOverflow by remember { mutableStateOf(false) }
            val htmlLength = post.textContent.length

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

            // --- BODY: Ảnh đính kèm (Hiển thị tất cả dạng cuộn ngang) ---
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
                                .border(
                                    1.dp,
                                    MaterialTheme.colors.onSurface.copy(alpha = 0.05f),
                                    RoundedCornerShape(12.dp)
                                ),
                            contentScale = ContentScale.Crop
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // --- FOOTER: Các nút tương tác ---
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Like Pill
                ActionPillButton(
                    icon = Icons.Outlined.FavoriteBorder,
                    text = post.likeCount.toString(),
                    onClick = { /* TODO */ }
                )

                Spacer(modifier = Modifier.width(8.dp))

                // Comment Pill
                ActionPillButton(
                    icon = Icons.Outlined.ChatBubbleOutline,
                    text = post.commentCount.toString(),
                    onClick = { /* TODO */ }
                )

                Spacer(modifier = Modifier.weight(1f))

                // View Count (Chỉ hiển thị, không bấm được)
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
}