package com.soulmate.app.ui.home

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.text.font.*
import androidx.compose.ui.unit.*
import kotlinx.coroutines.delay
import com.soulmate.app.R
import com.soulmate.app.ui.home.components.*
import com.soulmate.app.ui.home.components.SongItem

@Composable
fun HomeScreen() {

    val listState = rememberLazyListState()


    val songs = listOf(
        "Blinding Lights" to R.drawable.song1,
        "Tháp drill tự do" to R.drawable.song22,
        "Nghe như tình yêu" to R.drawable.song36,
        "Stay" to R.drawable.song4,

        "Bước qua mùa cô đơn" to R.drawable.song5,
        "Lạ lùng" to R.drawable.song6,
        "Nàng thơ" to R.drawable.song7,
        "Có chắc yêu là đây" to R.drawable.song8,
        "Đưa nhau đi trốn" to R.drawable.song9,
        "Big City Boy" to R.drawable.song10,
        "Túy Âm" to R.drawable.song11
    )

    var selectedIndex by remember { mutableStateOf(0) }

    // 🔥 AUTO SCROLL + CENTER
    LaunchedEffect(Unit) {
        var index = 0
        while (true) {
            delay(2000)

            index++
            if (index >= songs.size) index = 0

            selectedIndex = index

            listState.animateScrollToItem(index)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
    ) {

        HeaderSection()

        Spacer(modifier = Modifier.height(16.dp))

        MoodCard()

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = "Your Favourite Songs",
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(start = 16.dp)
        )

        Spacer(modifier = Modifier.height(12.dp))

        LazyRow(
            state = listState,
            contentPadding = PaddingValues(horizontal = 100.dp) // 🔥 tạo khoảng để center
        ) {
            itemsIndexed(songs) { index, song ->

                SongItem(
                    title = song.first,
                    image = song.second,
                    selected = index == selectedIndex
                )
            }
        }
    }
}