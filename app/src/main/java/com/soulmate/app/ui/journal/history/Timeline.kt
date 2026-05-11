package com.soulmate.app.ui.journal.history

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
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
import androidx.compose.material.*
import coil.ImageLoader
import coil.compose.AsyncImage
import coil.decode.GifDecoder
import coil.decode.ImageDecoderDecoder
import com.mohamedrejeb.richeditor.model.rememberRichTextState
import com.mohamedrejeb.richeditor.ui.material3.RichText
import com.soulmate.app.ui.home.components.RecordingNote
import com.soulmate.app.ui.journal.editor.Mood
import com.soulmate.app.ui.theme.TextSecondary
import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterialApi::class)
@Composable
fun Timeline(
    item: RecordingNote,
    isLastItem: Boolean = false,
    onDelete: () -> Unit,
    onEdit: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(IntrinsicSize.Min)
            .padding(end = 16.dp)
    ) {
        TimelineIndicator(
            moodTag = item.moodTag,
            isLastItem = isLastItem,
            modifier = Modifier.width(56.dp)
        )

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
                .weight(1f)
                .padding(bottom = 16.dp)
                .swipeable(
                    state = swipeableState,
                    anchors = anchors,
                    thresholds = { _, _ -> FractionalThreshold(0.3f) },
                    orientation = Orientation.Horizontal
                )
        ) {
            Row(
                modifier = Modifier.align(Alignment.CenterEnd).padding(end = 8.dp),
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

            Box(
                modifier = Modifier
                    .offset { IntOffset(swipeableState.offset.value.toInt(), 0) }
                    .fillMaxWidth()
            ) {
                DiaryCardContent(item)
            }
        }
    }
}

@Composable
private fun TimelineIndicator(moodTag: String?, isLastItem: Boolean, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val lineColor = MaterialTheme.colors.primary.copy(alpha = 0.3f)
    val imageLoader = remember {
        ImageLoader.Builder(context).components {
            if (android.os.Build.VERSION.SDK_INT >= 28) add(ImageDecoderDecoder.Factory()) else add(GifDecoder.Factory())
        }.build()
    }

    Box(
        modifier = modifier.fillMaxHeight().drawBehind {
            if (!isLastItem) {
                drawLine(color = lineColor, start = Offset(size.width / 2, 0f), end = Offset(size.width / 2, size.height), strokeWidth = 2.dp.toPx())
            } else {
                drawLine(color = lineColor, start = Offset(size.width / 2, 0f), end = Offset(size.width / 2, 32.dp.toPx()), strokeWidth = 2.dp.toPx())
            }
        },
        contentAlignment = Alignment.TopCenter
    ) {
        Box(
            modifier = Modifier.padding(top = 16.dp).size(40.dp).clip(CircleShape).background(MaterialTheme.colors.background).border(2.dp, MaterialTheme.colors.primary.copy(alpha = 0.5f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            if (moodTag != null) {
                Mood.entries.find { it.label == moodTag }?.let { mood ->
                    AsyncImage(model = mood.iconRes, contentDescription = mood.label, imageLoader = imageLoader, modifier = Modifier.size(24.dp))
                }
            } else {
                Box(modifier = Modifier.size(12.dp).clip(CircleShape).background(MaterialTheme.colors.primary))
            }
        }
    }
}

@Composable
private fun DiaryCardContent(item: RecordingNote) {
    val isDark = isSystemInDarkTheme()
    val richTextState = rememberRichTextState()

    var isExpanded by remember { mutableStateOf(false) }
    var showSeeMore by remember { mutableStateOf(false) }

    LaunchedEffect(item.text, isDark) {
        val processedHtml = if (isDark) {
            val blackPattern = "(?i)color\\s*:\\s*(?:rgb\\(0,\\s*0,\\s*0\\)|rgba\\(0,\\s*0,\\s*0,\\s*1(?:\\.0)?\\)|#000(?:000)?|black)".toRegex()
            item.text.replace(blackPattern, "color: #FFFFFF")
        } else item.text
        richTextState.setHtml(processedHtml)

        isExpanded = false
        showSeeMore = false
    }

    Card(
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier.fillMaxWidth().border(1.dp, MaterialTheme.colors.primary.copy(alpha = 0.2f), RoundedCornerShape(16.dp)),
        backgroundColor = MaterialTheme.colors.surface,
        elevation = 2.dp
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(text = item.dateTime, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colors.primary)
            Spacer(modifier = Modifier.height(12.dp))
            RichText(
                state = richTextState,
                modifier = Modifier.fillMaxWidth(),
                maxLines = if (isExpanded) Int.MAX_VALUE else 3,
                overflow = TextOverflow.Ellipsis,
                onTextLayout = { textLayoutResult ->

                    if (!isExpanded) {
                        showSeeMore = textLayoutResult.hasVisualOverflow
                    }
                }
            )

            if (showSeeMore) {
                Text(
                    text = if (isExpanded) "Rút gọn" else "Xem thêm...",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = TextSecondary,
                    modifier = Modifier
                        .padding(top = 4.dp, bottom = 4.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .clickable { isExpanded = !isExpanded }
                )
            }

            if (item.imageUrls.isNotEmpty()) {
                Spacer(modifier = Modifier.height(12.dp))
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(item.imageUrls) { url ->
                        AsyncImage(
                            model = url, contentDescription = null,
                            modifier = Modifier.size(90.dp).clip(RoundedCornerShape(12.dp)).border(0.5.dp, Color.LightGray.copy(alpha = 0.5f), RoundedCornerShape(12.dp)),
                            contentScale = ContentScale.Crop
                        )
                    }
                }
            }
        }
    }
}