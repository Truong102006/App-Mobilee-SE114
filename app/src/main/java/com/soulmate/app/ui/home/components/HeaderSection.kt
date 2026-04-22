package com.soulmate.app.ui.home.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Icon
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Person
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.soulmate.app.R

@Composable
fun HeaderSection() {
    val hasAvatar = true

    // Giảm chiều cao Box tổng xuống (ví dụ 280.dp thay vì 358.dp) để ảnh nền ngắn lại
    Box(modifier = Modifier.fillMaxWidth().height(310.dp)) {

        // 1. Background Image - Nằm trọn trong Box đã thu ngắn
        Image(
            painter = painterResource(id = R.drawable.img_3),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize()
        )

        // 2. Text Greeting - Giữ nguyên vị trí bên trái
        Column(
            modifier = Modifier.padding(start = 16.dp, top = 40.dp)
        ) {
            Text("Hello There !", fontSize = 16.sp, color = MaterialTheme.colors.onSurface.copy(alpha = 0.6f))
            Text("Dmanhz", fontSize = 26.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colors.onSurface)
        }

        // 3. Avatar Box - Thay đổi từ offset sang Alignment
        Box(
            modifier = Modifier
                .align(Alignment.TopEnd) // Đưa box về phía góc trên bên phải
                .padding(top = 40.dp, end = 10.dp) // Margin Top 40dp và Margin Right 10dp
                .size(52.dp)
                .clip(RoundedCornerShape(12.dp))
                .border(1.dp, MaterialTheme.colors.onSurface.copy(alpha = 0.1f), RoundedCornerShape(12.dp))
                .background(MaterialTheme.colors.surface),
            contentAlignment = Alignment.Center
        ) {
            if (hasAvatar) {
                Image(
                    painter = painterResource(id = R.drawable.ava1),
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                Icon(
                    imageVector = Icons.Default.Person,
                    contentDescription = "User",
                    tint = Color.Gray,
                    modifier = Modifier.size(24.dp)
                )
            }
        }
    }
}