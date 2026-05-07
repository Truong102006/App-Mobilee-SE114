package com.soulmate.app.ui.social

import android.widget.TextView
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.RemoveRedEye
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.text.HtmlCompat
import coil.compose.AsyncImage
import com.soulmate.app.ui.journal.editor.Mood
import com.soulmate.app.ui.theme.CommunityTick

data class CommunityPost(
    val id: String,
    val userName: String,
    val userAvatarUrl: String?,
    val isVerified: Boolean,
    val mood: String,
    val timeAgo: String,
    val textContent: String,
    val imageUrls: List<String>,
    val likeCount: Int,
    val commentCount: Int,
    val viewCount: Int
)

@Composable
fun ActionPillButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    text: String,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(50),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f)),
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
                tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = text,
                fontSize = 13.sp,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                fontWeight = FontWeight.Medium
            )
        }
    }
}

@Composable
fun HtmlText(html: String, textColor: androidx.compose.ui.graphics.Color, modifier: Modifier = Modifier) {
    AndroidView(
        modifier = modifier,
        factory = { context ->
            TextView(context).apply {
                textSize = 15f
                setTextColor(textColor.toArgb())
            }
        },
        update = { textView ->
            textView.text = HtmlCompat.fromHtml(html, HtmlCompat.FROM_HTML_MODE_COMPACT)
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
            .border(1.5.dp, androidx.compose.material3.MaterialTheme.colorScheme.primary.copy(alpha = 0.5f), RoundedCornerShape(20.dp)),
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
                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = post.userName.take(1).uppercase(),
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
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
                            color = MaterialTheme.colorScheme.onSurface
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
                    Text(text = moodTimeText, fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
                }

                // Nút 3 chấm (Options)
                IconButton(onClick = { /* TODO: Mở menu */ }) {
                    Icon(
                        imageVector = Icons.Default.MoreVert,
                        contentDescription = "Options",
                        tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // --- BODY: Text Content ---
            HtmlText(
                html = post.textContent,
                textColor = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.fillMaxWidth()
            )

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
                                    MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f),
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

                Spacer(modifier = Modifier.width(8.dp))

                // Share Icon Button (Không viền, dạng tròn nhỏ)
                IconButton(
                    onClick = { /* TODO */ },
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Share,
                        contentDescription = "Share",
                        modifier = Modifier.size(14.dp),
                        tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
                }

                Spacer(modifier = Modifier.weight(1f))

                // View Count (Chỉ hiển thị, không bấm được)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Rounded.RemoveRedEye,
                        contentDescription = "Views",
                        modifier = Modifier.size(16.dp),
                        tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = post.viewCount.toString(),
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f),
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFFF8F9FA) // Nền xám nhạt để dễ nhìn Card trắng
@Composable
fun PreviewCommunityCardWithImages() {
    MaterialTheme {
        Box(modifier = Modifier.padding(16.dp)) {
            CommunityCard(
                post = CommunityPost(
                    id = "1",
                    userName = "Marvin McKinney",
                    userAvatarUrl = null,
                    isVerified = true,
                    mood = "Happy",
                    timeAgo = "Today at 6:41",
                    textContent = "<h3>10 Tips for Beginners</h3> Đây là một đoạn text <b>in đậm</b> và có thể xuống dòng <br> Dùng HTMLCompat thật là tuyệt vời! 🚀",
                    imageUrls = listOf(
                        "https://dummyimage.com/400x400/e0e0e0/000000.png&text=Image+1",
                        "https://dummyimage.com/400x400/e0e0e0/000000.png&text=Image+2"
                    ),
                    likeCount = 2321,
                    commentCount = 5321,
                    viewCount = 5321
                )
            )
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFFF8F9FA)
@Composable
fun PreviewCommunityCardTextOnly() {
    MaterialTheme {
        Box(modifier = Modifier.padding(16.dp)) {
            CommunityCard(
                post = CommunityPost(
                    id = "2",
                    userName = "Nguyễn Khánh",
                    userAvatarUrl = null,
                    isVerified = false,
                    mood = "Satisfied",
                    timeAgo = "Yesterday at 14:30",
                    textContent = "Hôm nay thời tiết thật đẹp, mình đã hoàn thành xong đồ án môn học. Một ngày thật năng suất và ý nghĩa! ✨",
                    imageUrls = emptyList(),
                    likeCount = 128,
                    commentCount = 12,
                    viewCount = 450
                )
            )
        }
    }
}