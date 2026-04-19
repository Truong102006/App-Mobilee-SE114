package com.soulmate.app.ui.home.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Icon
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

    Box(modifier = Modifier.fillMaxWidth().height(358.dp)) {
        // Background Image
        Image(
            painter = painterResource(id = R.drawable.img_3),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize()
        )

        // Text Greeting
        Column(
            modifier = Modifier.padding(start = 16.dp, top = 40.dp)
        ) {
            Text("Hello There !", fontSize = 16.sp, color = Color(0xFF6c6c6c))
            Text("Dmanhz", fontSize = 26.sp, fontWeight = FontWeight.Bold, color = Color.Black)
        }

        // Avatar Box - Updated with specific constraints
        Box(
            modifier = Modifier
                .offset(x = 329.dp, y = 40.dp) // Tọa độ chính xác theo yêu cầu
                .size(48.dp)
                .clip(RoundedCornerShape(12.dp))
                .border(1.dp, Color(0xFFE0E0E0), RoundedCornerShape(12.dp))
                .background(Color.White),
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