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
    isLastItem: Boolean = false,
    onDelete: () -> Unit,
    onEdit: () -> Unit
) {
    val density = LocalDensity.current
    val swipeLimit = with(density) { 80.dp.toPx() }
    val swipeableState = rememberSwipeableState(initialValue = 0)
    val anchors = mapOf(0f to 0, -swipeLimit to 1)

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .swipeable(
                state = swipeableState,
                anchors = anchors,
                thresholds = { _, _ -> FractionalThreshold(0.3f) },
                orientation = Orientation.Horizontal
            )
    ) {
        // Nút Edit/Delete ẩn bên dưới khi vuốt sang trái
        Row(
            modifier = Modifier.align(Alignment.CenterEnd).padding(end = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = onEdit,
                modifier = Modifier.size(38.dp).clip(CircleShape).background(Color(0xFFE3F2FD))
            ) {
                Icon(Icons.Default.Edit, contentDescription = null, tint = Color(0xFF1E88E5), modifier = Modifier.size(20.dp))
            }
            Spacer(modifier = Modifier.width(10.dp))
            IconButton(
                onClick = onDelete,
                modifier = Modifier.size(38.dp).clip(CircleShape).background(Color(0xFFFFEBEE))
            ) {
                Icon(Icons.Default.Delete, contentDescription = null, tint = Color.Red, modifier = Modifier.size(20.dp))
            }
        }

        // Nội dung hiển thị chính
        Surface(
            modifier = Modifier.offset { IntOffset(swipeableState.offset.value.toInt(), 0) }.fillMaxWidth(),
            color = Color.Transparent // Để hiện màu của container cha
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(IntrinsicSize.Min)
                    .padding(vertical = 12.dp),
                verticalAlignment = Alignment.Top
            ) {
                // 1. Giờ (màu xanh blue)
                Text(
                    text = item.dateTime.split(" ").lastOrNull() ?: "",
                    color = Color(0xFF5B9DFF),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.width(55.dp).padding(top = 18.dp),
                    textAlign = TextAlign.Center
                )

                // 2. Đường kẻ và dấu chấm
                TimelineIndicator(isLastItem = isLastItem, modifier = Modifier.width(20.dp))

                // 3. Nội dung chính: Icon tâm trạng + Chữ + Ảnh bên phải
                DiaryContentArea(item, modifier = Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun TimelineIndicator(isLastItem: Boolean, modifier: Modifier = Modifier) {
    val isDark = isSystemInDarkTheme()
    val lineColor = if (isDark) Color.White.copy(alpha = 0.15f) else Color(0xFFE8F1FF)
    val dotColor = Color(0xFF5B9DFF)

    Box(
        modifier = modifier.fillMaxHeight().drawBehind {
            val centerX = size.width / 2
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
                    end = Offset(centerX, 20.dp.toPx()),
                    strokeWidth = 2.dp.toPx()
                )
            }
            drawCircle(
                color = dotColor,
                radius = 4.5.dp.toPx(),
                center = Offset(centerX, 20.dp.toPx())
            )
        }
    )
}

@Composable
private fun DiaryContentArea(item: RecordingNote, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val isDark = isSystemInDarkTheme()
    val richTextState = rememberRichTextState()
    val imageLoader = remember {
        ImageLoader.Builder(context).components {
            if (android.os.Build.VERSION.SDK_INT >= 28) add(ImageDecoderDecoder.Factory()) else add(GifDecoder.Factory())
        }.build()
    }

    LaunchedEffect(item.text, isDark) {
        val processedHtml = if (isDark) {
            val blackPattern = "(?i)color\\s*:\\s*(?:rgb\\(0,\\s*0,\\s*0\\)|#000000|black)".toRegex()
            item.text.replace(blackPattern, "color: #FFFFFF")
        } else {
            val whitePattern = "(?i)color\\s*:\\s*(?:rgb\\(255,\\s*255,\\s*255\\)|#FFFFFF|white)".toRegex()
            item.text.replace(whitePattern, "color: #000000")
        }
        richTextState.setHtml(processedHtml)
    }

    Row(
        modifier = modifier.fillMaxWidth().padding(start = 6.dp, end = 8.dp),
        verticalAlignment = Alignment.Top
    ) {
        // Icon tâm trạng (Vòng tròn vàng nhạt)
        Box(
            modifier = Modifier
                .padding(top = 8.dp)
                .size(36.dp)
                .clip(CircleShape)
                .background(if (isDark) Color.White.copy(alpha = 0.1f) else Color(0xFFFFFAEB)),
            contentAlignment = Alignment.Center
        ) {
            Mood.entries.find { it.label == item.moodTag }?.let { mood ->
                AsyncImage(
                    model = mood.iconRes,
                    contentDescription = null,
                    imageLoader = imageLoader,
                    modifier = Modifier.size(24.dp)
                )
            }
        }

        Spacer(modifier = Modifier.width(12.dp))

        // Nội dung văn bản
        Column(modifier = Modifier.weight(1f).padding(top = 14.dp)) {
            RichText(
                state = richTextState,
                modifier = Modifier.fillMaxWidth(),
                maxLines = 4,
                overflow = TextOverflow.Ellipsis
            )
        }

        // Ảnh nhật ký bên phải
        if (item.imageUrls.isNotEmpty()) {
            Spacer(modifier = Modifier.width(12.dp))
            AsyncImage(
                model = item.imageUrls.first(),
                contentDescription = "Diary Image",
                modifier = Modifier
                    .padding(top = 8.dp)
                    .size(width = 100.dp, height = 75.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .border(0.5.dp, Color.LightGray.copy(alpha = 0.3f), RoundedCornerShape(12.dp)),
                contentScale = ContentScale.Crop,
                placeholder = painterResource(id = R.drawable.img_3),
                error = painterResource(id = R.drawable.img_3)
            )
        }
    }
}
