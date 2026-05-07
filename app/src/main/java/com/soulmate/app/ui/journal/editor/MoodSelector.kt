package com.soulmate.app.ui.journal.editor

import android.annotation.SuppressLint
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.snapping.rememberSnapFlingBehavior
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.ImageLoader
import coil.compose.AsyncImage
import coil.decode.GifDecoder
import coil.decode.ImageDecoderDecoder

@SuppressLint("UnusedBoxWithConstraintsScope")
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun MoodSelector(
    selectedMood: Mood,
    onMoodChange: (Mood) -> Unit
) {
    val moods = Mood.entries.toTypedArray()
    val itemWidth = 80.dp

    val listState = rememberLazyListState()
    val flingBehavior = rememberSnapFlingBehavior(lazyListState = listState)
    val context = LocalContext.current

    val imageLoader = remember {
        ImageLoader.Builder(context)
            .components {
                if (android.os.Build.VERSION.SDK_INT >= 28) add(ImageDecoderDecoder.Factory()) else add(GifDecoder.Factory())
            }.build()
    }

    val centerIndex by remember {
        derivedStateOf {
            val layoutInfo = listState.layoutInfo
            val visibleItemsInfo = layoutInfo.visibleItemsInfo
            if (visibleItemsInfo.isEmpty()) 2
            else {
                val viewportCenter = (layoutInfo.viewportStartOffset + layoutInfo.viewportEndOffset) / 2
                visibleItemsInfo.minByOrNull {
                    kotlin.math.abs((it.offset + it.size / 2) - viewportCenter)
                }?.index ?: 2
            }
        }
    }

    LaunchedEffect(centerIndex) {
        onMoodChange(moods[centerIndex])
    }

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp),
        contentAlignment = Alignment.Center
    ) {
        val maxWidth = maxWidth
        val horizontalPadding = (maxWidth - itemWidth) / 2

        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            LazyRow(
                state = listState,
                flingBehavior = flingBehavior,
                modifier = Modifier.fillMaxWidth(),
                contentPadding = PaddingValues(horizontal = horizontalPadding),
                verticalAlignment = Alignment.CenterVertically
            ) {
                itemsIndexed(moods) { index, mood ->
                    val isSelected = index == centerIndex
                    val scale by animateFloatAsState(if (isSelected) 1.6f else 0.8f, label = "scale")
                    val alpha by animateFloatAsState(if (isSelected) 1f else 0.4f, label = "alpha")

                    Box(
                        modifier = Modifier
                            .width(itemWidth)
                            .height(100.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        if (isSelected) {
                            Box(
                                modifier = Modifier
                                    .size(60.dp)
                                    .scale(scale)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.2f))
                            )
                        }

                        AsyncImage(
                            model = mood.iconRes,
                            contentDescription = mood.label,
                            imageLoader = imageLoader,
                            modifier = Modifier
                                .size(50.dp)
                                .scale(scale),
                            alpha = alpha
                        )
                    }
                }
            }

            Text(
                text = moods[centerIndex].label,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.padding(top = 4.dp)
            )
        }
    }
}