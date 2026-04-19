package com.soulmate.app.ui.home.components

import androidx.compose.animation.core.tween
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.graphics.Color
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
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

    // 🔥 SIZE ANIMATION
    val animatedSize = animateDpAsState(
        targetValue = if (selected) 180.dp else 140.dp,
        animationSpec = tween(durationMillis = 300),
        label = ""
    )

    // 🔥 TEXT SIZE ANIMATION
    val animatedTextSize = animateFloatAsState(
        targetValue = if (selected) 16f else 14f,
        animationSpec = tween(300),
        label = ""
    )

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
                .size(animatedSize.value)
                .clip(RoundedCornerShape(16.dp))
                .graphicsLayer {
                    // 🔥 thêm scale nhẹ cho mượt hơn
                    val scale = if (selected) 1.1f else 1f
                    scaleX = scale
                    scaleY = scale
                }
        )

        Text(
            text = title,
            fontWeight = FontWeight.Bold,
            fontSize = animatedTextSize.value.sp,
            color = textColor,
            modifier = Modifier.padding(top = 4.dp)
        )
    }
}