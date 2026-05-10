package com.soulmate.app.ui.journal.history

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
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
import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterialApi::class)
@Composable
fun Timeline(
    item: RecordingNote,
    isFirstItem: Boolean = false,
    isLastItem: Boolean = false,
    onDelete: () -> Unit,
    onEdit: () -> Unit
) {
    val timelineColor = Color(0xFF42A5F5)

    // Swipe state logic
    val density = LocalDensity.current
    val swipeLimit = with(density) { 120.dp.toPx() }
    val swipeableState = rememberSwipeableState(initialValue = 0)
    val anchors = mapOf(0f to 0, -swipeLimit to 1)

    // Auto reset after 2.5 seconds
    if (swipeableState.currentValue == 1) {
        LaunchedEffect(item.diaryId) {
            delay(2500)
            swipeableState.animateTo(0)
        }
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(IntrinsicSize.Min), // Cố định chiều cao theo nội dung bên phải
        verticalAlignment = Alignment.Top
    ) {
        // --- CỘT TRÁI: GIỜ & TIMELINE DỌC ---
        Box(
            modifier = Modifier
                .width(65.dp)
                .fillMaxHeight()
                .drawBehind {
                    val centerX = size.width / 2
                    val dotY = 36.dp.toPx() // Vị trí dấu chấm xanh

                    // 1. Đường kẻ nối từ phía trên (màu mờ) để tạo sự liên kết
                    if (!isFirstItem) {
                        drawLine(
                            color = timelineColor.copy(alpha = 0.1f),
                            start = Offset(centerX, 0f),
                            end = Offset(centerX, dotY),
                            strokeWidth = 2.dp.toPx()
                        )
                    }

                    // 2. Đường kẻ xanh đậm kéo dài xuống (Chỉ dài tới cuối nội dung của item này)
                    // Trừ đi 24.dp padding bottom của nội dung để không bị lố sang item khác
                    drawLine(
                        color = timelineColor,
                        start = Offset(centerX, dotY),
                        end = Offset(centerX, size.height - 24.dp.toPx()),
                        strokeWidth = 2.dp.toPx()
                    )

                    // 3. Vẽ dấu chấm tròn xanh
                    drawCircle(
                        color = timelineColor,
                        radius = 4.5.dp.toPx(),
                        center = Offset(centerX, dotY)
                    )
                }
        ) {
            val timePart = item.dateTime.split(" ").lastOrNull() ?: ""
            Text(
                text = timePart,
                color = timelineColor,
                fontSize = 12.sp,
                fontWeight = FontWeight.ExtraBold,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 10.dp),
                textAlign = TextAlign.Center
            )
        }

        // --- CỘT PHẢI: ICON + NỘI DUNG (Hỗ trợ Swipe) ---
        Box(
            modifier = Modifier
                .weight(1f)
                .swipeable(
                    state = swipeableState,
                    anchors = anchors,
                    thresholds = { _, _ -> FractionalThreshold(0.3f) },
                    orientation = Orientation.Horizontal
                )
        ) {
            // Nút Edit và Delete ở dưới (hiện ra khi swipe)
            Row(
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .padding(end = 12.dp, bottom = 24.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = {
                        onEdit()
                    },
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF4CAF50).copy(alpha = 0.1f))
                ) {
                    Icon(Icons.Default.Edit, contentDescription = "Edit", tint = Color(0xFF4CAF50))
                }

                Spacer(modifier = Modifier.width(12.dp))

                IconButton(
                    onClick = {
                        onDelete()
                    },
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(Color.Red.copy(alpha = 0.1f))
                ) {
                    Icon(Icons.Default.Delete, contentDescription = "Delete", tint = Color.Red)
                }
            }

            // Nội dung chính (Swipeable layer)
            Row(
                modifier = Modifier
                    .offset { IntOffset(swipeableState.offset.value.toInt(), 0) }
                    .background(Color.White) // Cần background để che đi các nút ở dưới
                    .fillMaxWidth()
                    .padding(start = 4.dp, end = 16.dp, bottom = 24.dp),
                verticalAlignment = Alignment.Top
            ) {
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

                // Ảnh bên phải ngoài cùng
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
