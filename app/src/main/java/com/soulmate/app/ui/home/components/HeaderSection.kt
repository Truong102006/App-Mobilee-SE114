package com.soulmate.app.ui.home.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.Text
import androidx.compose.runtime.Composable // 🔥 QUAN TRỌNG (thiếu cái này gây lỗi)
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
    Box {
        Image(
            painter = painterResource(id = R.drawable.img_3),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .fillMaxWidth()
                .height(358.dp)
        )

        Column(
            modifier = Modifier.padding(start = 16.dp, top = 50.dp)
        ) {
            Text(
                text = "Hello There !",
                fontSize = 16.sp,
                color = Color(0xFF768B90)
            )

            Text(
                text = "Dmanhz",
                fontSize = 26.sp,
                fontWeight = FontWeight.Bold
            )
        }

        Image(
            painter = painterResource(id = R.drawable.ava1),
            contentDescription = null,
            modifier = Modifier
                .size(52.dp)
                .align(Alignment.TopEnd)
                .padding(top = 50.dp, end = 16.dp)
                .clip(CircleShape)
        )
    }
}