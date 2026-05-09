package com.soulmate.app.ui.journal.history

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.text.font.FontWeight
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
    val swipeLimit = with(density) { 100.dp.toPx() }
    val swipeableState = rememberSwipeableState(initialValue = 0)
    val anchors = mapOf(0f to 0, -swipeLimit to 1)

    if (swipeableState.currentValue == 1) {
        LaunchedEffect(item.id) {
            delay(3000)
            swipeableState.animateTo(0)
        }
    }

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
        // Swipe Actions (Behind)
        Row(
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .padding(end = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = onEdit,
                modifier = Modifier.size(40.dp).clip(CircleShape).background(MaterialTheme.colors.primary.copy(alpha = 0.1f))
            ) {
                Icon(Icons.Default.Edit, contentDescription = "Edit", tint = MaterialTheme.colors.primary)
            }
            Spacer(modifier = Modifier.width(8.dp))
            IconButton(
                onClick = onDelete,
                modifier = Modifier.size(40.dp).clip(CircleShape).background(Color.Red.copy(alpha = 0.1f))
            ) {
                Icon(Icons.Default.Delete, contentDescription = "Delete", tint = Color.Red)
            }
        }

        // Main Content (Foreground)
        Surface(
            modifier = Modifier
                .offset { IntOffset(swipeableState.offset.value.toInt(), 0) }
                .fillMaxWidth(),
            color = Color.White
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(IntrinsicSize.Min)
                    .padding(vertical = 12.dp)
            ) {
                // 1. Time
                val timePart = item.dateTime.split(" ").lastOrNull() ?: ""
                Text(
                    text = timePart,
                    color = Color(0xFF5B9DFF),
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.width(50.dp).padding(top = 16.dp),
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )

                // 2. Timeline Line and Dot
                TimelineIndicator(
                    isLastItem = isLastItem,
                    modifier = Modifier.width(24.dp)
                )

                // 3. Mood Icon and Content Card
                DiaryContentArea(item)
            }
        }
    }
}

@Composable
private fun TimelineIndicator(isLastItem: Boolean, modifier: Modifier = Modifier) {
    val lineColor = Color(0xFFE8F1FF)
    val dotColor = Color(0xFF5B9DFF)

    Box(
        modifier = modifier
            .fillMaxHeight()
            .drawBehind {
                val centerX = size.width / 2
                // Draw vertical line
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
                        end = Offset(centerX, 24.dp.toPx()),
                        strokeWidth = 2.dp.toPx()
                    )
                }
                // Draw dot
                drawCircle(
                    color = dotColor,
                    radius = 4.dp.toPx(),
                    center = Offset(centerX, 20.dp.toPx())
                )
            }
    )
}

@Composable
private fun DiaryContentArea(item: RecordingNote) {
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
            val blackPattern = "(?i)color\\s*:\\s*(?:rgb\\(0,\\s*0,\\s*0\\)|rgba\\(0,\\s*0,\\s*0,\\s*1(?:\\.0)?\\)|#000(?:000)?|black)".toRegex()
            item.text.replace(blackPattern, "color: #000000") // Force black for the white journal surface
        } else item.text
        richTextState.setHtml(processedHtml)
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 8.dp, end = 16.dp),
        verticalAlignment = Alignment.Top
    ) {
        // Mood Icon
        Box(
            modifier = Modifier
                .padding(top = 8.dp)
                .size(36.dp)
                .clip(CircleShape)
                .background(Color(0xFFFFFAEB)), // Light yellowish background like in image
            contentAlignment = Alignment.Center
        ) {
            Mood.entries.find { it.label == item.moodTag }?.let { mood ->
                AsyncImage(
                    model = mood.iconRes,
                    contentDescription = mood.label,
                    imageLoader = imageLoader,
                    modifier = Modifier.size(24.dp)
                )
            }
        }

        Spacer(modifier = Modifier.width(12.dp))

        // Text Content
        Column(modifier = Modifier.weight(1f).padding(top = 12.dp)) {
            RichText(
                state = richTextState,
                modifier = Modifier.fillMaxWidth(),
                maxLines = 3,
                overflow = TextOverflow.Ellipsis
            )
        }

        // Image (on the right)
        if (item.imageUrls.isNotEmpty()) {
            Spacer(modifier = Modifier.width(12.dp))
            AsyncImage(
                model = item.imageUrls.first(),
                contentDescription = null,
                modifier = Modifier
                    .size(width = 100.dp, height = 70.dp)
                    .clip(RoundedCornerShape(8.dp)),
                contentScale = ContentScale.Crop
            )
        }
    }
}
