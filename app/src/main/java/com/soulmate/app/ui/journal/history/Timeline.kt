package com.soulmate.app.ui.journal.history

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.ImageLoader
import coil.compose.AsyncImage
import coil.decode.GifDecoder
import coil.decode.ImageDecoderDecoder
import com.mohamedrejeb.richeditor.model.rememberRichTextState
import com.mohamedrejeb.richeditor.ui.material3.RichText
import com.soulmate.app.R
import com.soulmate.app.ui.home.components.RecordingNote
import com.soulmate.app.ui.journal.editor.Mood

@Composable
fun Timeline(
    item: RecordingNote,
    isLastItem: Boolean = false,
    onDelete: () -> Unit,
    onEdit: () -> Unit
) {
    // Đã ẩn nút Edit/Delete và bỏ swipe theo yêu cầu thiết kế mới
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(IntrinsicSize.Min) // Cần thiết để đường kẻ dọc chiếm hết chiều cao nội dung
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.Top
    ) {
        // --- CỘT TRÁI: GIỜ & TIMELINE DỌC ---
        Column(
            modifier = Modifier.width(65.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            val timePart = item.dateTime.split(" ").lastOrNull() ?: ""
            Text(
                text = timePart,
                color = Color(0xFF1E88E5),
                fontSize = 12.sp,
                fontWeight = FontWeight.ExtraBold,
                modifier = Modifier.padding(top = 10.dp),
                textAlign = TextAlign.Center
            )
            
            Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
                TimelineIndicatorVertical(isLastItem = isLastItem)
            }
        }

        // --- CỘT PHẢI: ICON + CHỮ + ẢNH ---
        Row(
            modifier = Modifier
                .weight(1f)
                .padding(start = 4.dp, end = 16.dp, bottom = 20.dp),
            verticalAlignment = Alignment.Top
        ) {
            // Spacer và căn chỉnh nội dung để Icon ngang hàng với dấu chấm tròn
            Column(modifier = Modifier.weight(1f)) {
                Spacer(modifier = Modifier.height(26.dp)) 
                Row(verticalAlignment = Alignment.Top) {
                    MoodIconDesign(item.moodTag)
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f).padding(top = 2.dp)) {
                        RichTextEntry(item.text)
                    }
                }
            }

            // Ảnh bên phải ngoài cùng (Hiển thị từ Firebase URL)
            if (item.imageUrls.isNotEmpty()) {
                Spacer(modifier = Modifier.width(12.dp))
                AsyncImage(
                    model = item.imageUrls.first(),
                    contentDescription = "Diary Photo",
                    modifier = Modifier
                        .padding(top = 30.dp)
                        .size(width = 110.dp, height = 75.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .border(0.5.dp, Color.LightGray.copy(alpha = 0.3f), RoundedCornerShape(12.dp)),
                    contentScale = ContentScale.Crop,
                    placeholder = painterResource(id = R.drawable.img_3),
                    error = painterResource(id = R.drawable.img_3)
                )
            }
        }
    }
}

@Composable
private fun TimelineIndicatorVertical(isLastItem: Boolean) {
    val isDark = isSystemInDarkTheme()
    val lineColor = if (isDark) Color.White.copy(alpha = 0.15f) else Color(0xFFE3F2FD)
    val dotColor = Color(0xFF42A5F5)

    Box(
        modifier = Modifier.fillMaxSize().drawBehind {
            val centerX = size.width / 2
            val dotY = 16.dp.toPx() // Vị trí dấu chấm ngay bên dưới giờ

            // Vẽ đường kẻ dọc
            if (!isLastItem) {
                drawLine(
                    color = lineColor,
                    start = Offset(centerX, 0f),
                    end = Offset(centerX, size.height),
                    strokeWidth = 2.dp.toPx()
                )
            } else {
                drawLine(
                    color = lineColor,
                    start = Offset(centerX, 0f),
                    end = Offset(centerX, dotY),
                    strokeWidth = 2.dp.toPx()
                )
            }

            // Vẽ dấu chấm tròn
            drawCircle(
                color = dotColor,
                radius = 4.5.dp.toPx(),
                center = Offset(centerX, dotY)
            )
        }
    )
}

@Composable
private fun MoodIconDesign(moodTag: String?) {
    val context = LocalContext.current
    val isDark = isSystemInDarkTheme()
    val imageLoader = remember {
        ImageLoader.Builder(context).components {
            if (android.os.Build.VERSION.SDK_INT >= 28) add(ImageDecoderDecoder.Factory()) else add(GifDecoder.Factory())
        }.build()
    }
    Box(
        modifier = Modifier
            .size(36.dp)
            .clip(CircleShape)
            .background(if (isDark) Color.White.copy(alpha = 0.1f) else Color(0xFFFFFAEB)),
        contentAlignment = Alignment.Center
    ) {
        Mood.entries.find { it.label == moodTag }?.let { mood ->
            AsyncImage(
                model = mood.iconRes,
                contentDescription = null,
                imageLoader = imageLoader,
                modifier = Modifier.size(24.dp)
            )
        }
    }
}

@Composable
private fun RichTextEntry(text: String) {
    val richTextState = rememberRichTextState()
    val isDark = isSystemInDarkTheme()
    LaunchedEffect(text, isDark) {
        val processedHtml = if (isDark) {
            val blackPattern = "(?i)color\\s*:\\s*(?:rgb\\(0,\\s*0,\\s*0\\)|#000000|black)".toRegex()
            text.replace(blackPattern, "color: #FFFFFF")
        } else {
            val whitePattern = "(?i)color\\s*:\\s*(?:rgb\\(255,\\s*255,\\s*255\\)|#FFFFFF|white)".toRegex()
            text.replace(whitePattern, "color: #000000")
        }
        richTextState.setHtml(processedHtml)
    }
    RichText(
        state = richTextState,
        modifier = Modifier.fillMaxWidth(),
        maxLines = 4,
        overflow = TextOverflow.Ellipsis
    )
}
