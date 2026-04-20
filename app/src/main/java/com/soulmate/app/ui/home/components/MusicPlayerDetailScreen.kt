package com.soulmate.app.ui.home.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.util.Locale

@Composable
fun MusicPlayerDetailScreen(
    title: String,
    artist: String,
    imageRes: Int,
    isPlaying: Boolean,
    currentPosition: Long, // Thêm: Vị trí hiện tại (ms) từ ExoPlayer
    duration: Long,        // Thêm: Tổng thời lượng (ms) từ ExoPlayer
    onPlayPauseClick: () -> Unit,
    onNextClick: () -> Unit,
    onBackClick: () -> Unit,
    onPreviousClick: () -> Unit,
    onSeek: (Long) -> Unit // Thêm: Hàm để xử lý khi người dùng kéo thanh nhạc
) {
    // Logic xoay đĩa nhạc
    val infiniteTransition = rememberInfiniteTransition()
    val rotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(15000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        )
    )

    Box(modifier = Modifier.fillMaxSize()) {
        // 1. Background Blur
        Image(
            painter = painterResource(id = imageRes),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .fillMaxSize()
                .blur(30.dp)
        )

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.5f))
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            IconButton(
                onClick = onBackClick,
                modifier = Modifier.align(Alignment.Start)
            ) {
                Icon(
                    imageVector = Icons.Default.KeyboardArrowDown,
                    contentDescription = "Minimize",
                    tint = Color.White,
                    modifier = Modifier.size(36.dp)
                )
            }

            Spacer(modifier = Modifier.height(40.dp))

            // 2. Đĩa nhạc
            Box(
                modifier = Modifier
                    .size(300.dp)
                    .rotate(if (isPlaying) rotation else 0f)
                    .border(4.dp, Color.White, CircleShape)
                    .padding(8.dp)
                    .clip(CircleShape)
            ) {
                Image(
                    painter = painterResource(id = imageRes),
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
                Box(
                    modifier = Modifier
                        .size(45.dp)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.8f))
                        .align(Alignment.Center)
                )
            }

            Spacer(modifier = Modifier.height(48.dp))

            Text(text = title, fontSize = 26.sp, fontWeight = FontWeight.Bold, color = Color.White, maxLines = 1)
            Text(text = artist, fontSize = 18.sp, color = Color.LightGray, maxLines = 1)

            Spacer(modifier = Modifier.weight(1f))

            // 3. THANH THỜI LƯỢNG (SEEK BAR) THỰC TẾ
            Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
                Slider(
                    // Tính toán % tiến trình: (vị trí hiện tại / tổng thời gian)
                    value = if (duration > 0f) currentPosition.toFloat() / duration else 0f,
                    onValueChange = { percent ->
                        // Khi kéo thanh, tính toán lại mili giây tương ứng
                        onSeek((percent * duration).toLong())
                    },
                    colors = SliderDefaults.colors(
                        thumbColor = Color.White,
                        activeTrackColor = Color(0xFFED8413),
                        inactiveTrackColor = Color.White.copy(alpha = 0.3f)
                    )
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    // Hiển thị thời gian hiện tại (VD: 01:20)
                    Text(text = formatTime(currentPosition), color = Color.White, fontSize = 12.sp)
                    // Hiển thị tổng thời gian (VD: 03:45)
                    Text(text = formatTime(duration), color = Color.White, fontSize = 12.sp)
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // 4. Các nút điều hướng
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onPreviousClick) {
                    Icon(Icons.Default.SkipPrevious, "Prev", tint = Color.White, modifier = Modifier.size(50.dp))
                }

                FloatingActionButton(
                    onClick = onPlayPauseClick,
                    backgroundColor = Color(0xFFED8413)
                ) {
                    Icon(
                        imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                        contentDescription = "Play/Pause",
                        tint = Color.White,
                        modifier = Modifier.size(35.dp)
                    )
                }

                IconButton(onClick = onNextClick) {
                    Icon(Icons.Default.SkipNext, "Next", tint = Color.White, modifier = Modifier.size(50.dp))
                }
            }
            Spacer(modifier = Modifier.height(40.dp))
        }
    }
}

/**
 * Hàm hỗ trợ định dạng mili giây sang chuỗi thời gian mm:ss
 */
private fun formatTime(ms: Long): String {
    val totalSeconds = ms / 1000
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return String.format(Locale.getDefault(), "%02d:%02d", minutes, seconds)
}