package com.soulmate.app.ui.home.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.graphics.Color
import androidx.compose.material.Text
import androidx.compose.runtime.Composable // 🔥 bắt buộc
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun SongItem(
    title: String,
    image: Int,
    selected: Boolean = false
) {

    val size = if (selected) 180.dp else 140.dp
    val textSize = if (selected) 16.sp else 14.sp
    val textColor = if (selected) Color(0xFFFF9800) else Color.Gray

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.padding(end = 12.dp)
    ) {

        Image(
            painter = painterResource(id = image),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .size(size)
                .clip(RoundedCornerShape(16.dp))
        )

        Text(
            text = title,
            fontWeight = FontWeight.Bold,
            fontSize = textSize,
            color = textColor,
            modifier = Modifier.padding(top = 4.dp)
        )
    }
}