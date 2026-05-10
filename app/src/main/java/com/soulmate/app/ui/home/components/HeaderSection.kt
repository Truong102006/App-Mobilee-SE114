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
import coil.compose.AsyncImage
import com.soulmate.app.R
import com.soulmate.app.domain.model.User

@Composable
fun HeaderSection(user: User?) {
    // Giảm chiều cao Box tổng xuống để ảnh nền ngắn lại
    Box(modifier = Modifier.fillMaxWidth().height(310.dp)) {

        // 1. Background Image
        Image(
            painter = painterResource(id = R.drawable.img_3),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize()
        )

        // 2. Text Greeting
        Column(
            modifier = Modifier.padding(start = 16.dp, top = 40.dp)
        ) {
            Text("Hello There !", fontSize = 16.sp, color = MaterialTheme.colors.onSurface.copy(alpha = 0.6f))
            Text(
                text = user?.anonymousName ?: "SoulMate User",
                fontSize = 26.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colors.onSurface
            )
        }

        // 3. Avatar Box
        Box(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(top = 40.dp, end = 10.dp)
                .size(52.dp)
                .clip(RoundedCornerShape(12.dp))
                .border(1.dp, MaterialTheme.colors.onSurface.copy(alpha = 0.1f), RoundedCornerShape(12.dp))
                .background(MaterialTheme.colors.surface),
            contentAlignment = Alignment.Center
        ) {
            if (user?.avatarUrl != null) {
                AsyncImage(
                    model = user.avatarUrl,
                    contentDescription = "User Avatar",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize(),
                    placeholder = painterResource(id = R.drawable.ava1),
                    error = painterResource(id = R.drawable.ava1)
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