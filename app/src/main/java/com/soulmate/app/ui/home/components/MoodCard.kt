package com.soulmate.app.ui.home.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Stop
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun MoodCard() {
    var isRecording by remember { mutableStateOf(false) }

    // Hiệu ứng nhấp nháy khi đang ghi âm
    val infiniteTransition = rememberInfiniteTransition(label = "blink")
    val blinkAlpha by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 0.3f,
        animationSpec = infiniteRepeatable(
            animation = tween(500, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ), label = ""
    )

    Row(
        modifier = Modifier
            .padding(horizontal = 16.dp)
            .fillMaxWidth()
            .height(96.dp)
            .shadow(6.dp, shape = RoundedCornerShape(16.dp))
            .background(Color.White, shape = RoundedCornerShape(16.dp))
            // Thêm viền xanh lá cây
            .border(2.dp, Color(0xFFffbc25), RoundedCornerShape(16.dp))
            .clickable { }
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(text = "How are you feeling today?", fontWeight = FontWeight.Bold, fontSize = 15.sp)
            Text(text = if (isRecording) "Recording..." else "Tap to record mood", color = Color.Gray)
        }

        // Nút Add với hình tròn xanh nhạt
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(Color(0xFFFFE0B2)),
            contentAlignment = Alignment.Center
        ) {
            IconButton(onClick = {}) {
                Icon(Icons.Default.Add, contentDescription = "Add", tint = Color(0xFF2E7D32))
            }
        }

        Spacer(modifier = Modifier.width(8.dp))

        // Nút Mic với logic thay đổi trạng thái
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(if (isRecording) RoundedCornerShape(8.dp) else CircleShape)
                .background(if (isRecording) Color.Red else Color(0xFFFFE0B2))
                .alpha(if (isRecording) blinkAlpha else 1f),
            contentAlignment = Alignment.Center
        ) {
            IconButton(onClick = { isRecording = !isRecording }) {
                Icon(
                    imageVector = if (isRecording) Icons.Default.Stop else Icons.Default.Mic,
                    contentDescription = "Mic",
                    tint = if (isRecording) Color.White else Color(0xFF2E7D32)
                )
            }
        }
    }
}