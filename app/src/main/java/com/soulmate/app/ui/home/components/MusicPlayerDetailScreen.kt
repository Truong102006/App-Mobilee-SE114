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

@Composable
fun MusicPlayerDetailScreen(
    title: String,
    artist: String, // Thêm tham số tên tác giả
    imageRes: Int,
    isPlaying: Boolean,
    onPlayPauseClick: () -> Unit,
    onNextClick: () -> Unit,
    onBackClick: () -> Unit,
    onPreviousClick: () -> Unit
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

    // Giả lập vị trí thanh Slider (Thực tế nên lấy từ ExoPlayer)
    var sliderPosition by remember { mutableStateOf(0.3f) }

    Box(modifier = Modifier.fillMaxSize()) {
        // 1. Hình nền mờ (Background Blur) lấy từ ảnh bài hát
        Image(
            painter = painterResource(id = imageRes),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .fillMaxSize()
                .blur(30.dp)
        )

        // Lớp phủ tối (Overlay) để nổi bật các nút điều khiển và chữ
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
            // Nút mũi tên xuống để thu nhỏ lại (Giống Zing MP3)
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

            // 2. Đĩa nhạc xoay tròn có VIỀN TRẮNG
            Box(
                modifier = Modifier
                    .size(300.dp)
                    .rotate(if (isPlaying) rotation else 0f)
                    .border(4.dp, Color.White, CircleShape) // Viền trắng
                    .padding(8.dp)
                    .clip(CircleShape)
            ) {
                Image(
                    painter = painterResource(id = imageRes),
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )

                // Lỗ tròn giữa đĩa (Center Hole)
                Box(
                    modifier = Modifier
                        .size(45.dp)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.8f))
                        .align(Alignment.Center)
                )
            }

            Spacer(modifier = Modifier.height(48.dp))

            // Thông tin bài hát và Tên tác giả
            Text(
                text = title,
                fontSize = 26.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                maxLines = 1
            )
            Text(
                text = artist, // Đã thay chữ "Now Playing" bằng tên tác giả
                fontSize = 18.sp,
                color = Color.LightGray,
                maxLines = 1
            )

            Spacer(modifier = Modifier.weight(1f))

            // 3. Thanh thời lượng (Seek Bar)
            Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
                Slider(
                    value = sliderPosition,
                    onValueChange = { sliderPosition = it },
                    colors = SliderDefaults.colors(
                        thumbColor = Color.White,
                        activeTrackColor = Color(0xFFED8413), // Màu cam thương hiệu của bạn
                        inactiveTrackColor = Color.White.copy(alpha = 0.3f)
                    )
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("1:20", color = Color.White, fontSize = 12.sp)
                    Text("3:45", color = Color.White, fontSize = 12.sp)
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // 4. Các nút điều hướng (Trước đó - Phát/Dừng - Kế tiếp)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Nút Bài trước đó (Previous)
                IconButton(onClick = onPreviousClick) {
                    Icon(
                        imageVector = Icons.Default.SkipPrevious,
                        contentDescription = "Previous",
                        tint = Color.White,
                        modifier = Modifier.size(50.dp)
                    )
                }

                // Nút Play/Pause chính
                FloatingActionButton(
                    onClick = onPlayPauseClick,
                    backgroundColor = Color(0xFFED8413),
                    elevation = FloatingActionButtonDefaults.elevation(8.dp)
                ) {
                    Icon(
                        imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                        contentDescription = "Play/Pause",
                        tint = Color.White,
                        modifier = Modifier.size(35.dp)
                    )
                }

                // Nút Bài tiếp theo (Next)
                IconButton(onClick = onNextClick) {
                    Icon(
                        imageVector = Icons.Default.SkipNext,
                        contentDescription = "Next",
                        tint = Color.White,
                        modifier = Modifier.size(50.dp)
                    )
                }
            }
            Spacer(modifier = Modifier.height(40.dp))
        }
    }
}