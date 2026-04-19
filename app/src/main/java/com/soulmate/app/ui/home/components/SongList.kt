package com.soulmate.app.ui.home.components

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp // 🔥 thiếu cái này gây lỗi
import com.soulmate.app.R

@Composable
fun SongList() {

    Row(
        modifier = Modifier
            .horizontalScroll(rememberScrollState())
            .padding(start = 16.dp)
    ) {

        SongItem("Blinding Lights", R.drawable.song1)
        SongItem("Tháp drill tự do", R.drawable.song22, true)
        SongItem("Nghe như tình yêu", R.drawable.song36)
        SongItem("Stay", R.drawable.song4)
        SongItem("Bước qua mùa cô đơn", R.drawable.song5)

        SongItem("Lạ lùng", R.drawable.song6)
        SongItem("Nàng thơ", R.drawable.song7)
        SongItem("Có chắc yêu là đây", R.drawable.song8)
        SongItem("Đưa nhau đi trốn", R.drawable.song9)
        SongItem("Big City Boy", R.drawable.song10)

        SongItem("Túy Âm", R.drawable.song11)

    }
}