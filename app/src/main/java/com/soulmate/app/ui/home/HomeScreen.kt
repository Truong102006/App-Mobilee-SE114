package com.soulmate.app.ui.home

import android.net.Uri
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.material.Scaffold
import androidx.compose.material.Text
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.*
import androidx.media3.common.MediaItem
import androidx.media3.exoplayer.ExoPlayer
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.distinctUntilChanged
import com.soulmate.app.R
import com.soulmate.app.ui.home.components.*
import kotlin.math.abs

@Composable
fun HomeScreen() {
    val context = LocalContext.current

    // 1. Khởi tạo ExoPlayer (Nâng cấp)
    val exoPlayer = remember {
        ExoPlayer.Builder(context).build()
    }

    // Giải phóng Player khi thoát ứng dụng để tránh rò rỉ bộ nhớ
    DisposableEffect(Unit) {
        onDispose {
            exoPlayer.release()
        }
    }

    // 2. Danh sách bài hát (Nâng cấp lên Triple: Tên - Ảnh - File Nhạc)
    // Lưu ý: R.raw.song1, R.raw.song2... là các file bạn bỏ vào thư mục res/raw
    // 2. Danh sách bài hát (Sửa lỗi ép kiểu)
    val songs = remember {
        listOf<Triple<String, Int, Int>>(
            Triple("Alaba trap", R.drawable.song111, R.raw.song1),
            Triple("Thích quá rùi nà", R.drawable.song222, R.raw.song2),
            Triple("Nghe như tình yêu", R.drawable.song33, R.raw.song3),
            Triple("Stay", R.drawable.song4, R.raw.song4),
            Triple("Bước qua mùa cô đơn", R.drawable.song5, R.raw.song5),
            Triple("Lạ lùng", R.drawable.song6, R.raw.song6),
            Triple("Cần gì nói yêu", R.drawable.song77, R.raw.song7),
            Triple("Cua", R.drawable.song88, R.raw.song8),
            Triple("Mamma Mia", R.drawable.song99, R.raw.song9),
            Triple("Big City Boy", R.drawable.song10, R.raw.song10),
            Triple("Pho Real", R.drawable.song_11, R.raw.pho_real)
        )
    }

    // Trạng thái bài hát đang phát
    var currentPlayingSong by remember { mutableStateOf<Triple<String, Int, Int>?>(null) }
    var isPlaying by remember { mutableStateOf(false) }

    // 3. Logic xử lý phát nhạc khi nhấn chọn bài
    LaunchedEffect(currentPlayingSong) {
        currentPlayingSong?.let { song ->
            val uri = Uri.parse("android.resource://${context.packageName}/${song.third}")
            val mediaItem = MediaItem.fromUri(uri)
            exoPlayer.setMediaItem(mediaItem)
            exoPlayer.prepare()
            exoPlayer.play()
            isPlaying = true
        }
    }

    // Logic xử lý Play/Pause từ Mini Player
    LaunchedEffect(isPlaying) {
        if (isPlaying) exoPlayer.play() else exoPlayer.pause()
    }

    // --- Giữ nguyên logic cuộn vô tận ---
    val virtualCount = 10000
    val listState = rememberLazyListState()
    var selectedIndex by remember { mutableStateOf(0) }

    LaunchedEffect(Unit) {
        val centerStart = (virtualCount / 2)
        val offsetStart = centerStart - (centerStart % songs.size)
        listState.scrollToItem(offsetStart)
    }

    LaunchedEffect(Unit) {
        while (true) {
            delay(3200)
            if (!listState.isScrollInProgress) {
                listState.animateScrollToItem(listState.firstVisibleItemIndex + 1)
            }
        }
    }

    LaunchedEffect(listState) {
        snapshotFlow { listState.layoutInfo }
            .distinctUntilChanged()
            .collect { layoutInfo ->
                val viewportCenter = (layoutInfo.viewportStartOffset + layoutInfo.viewportEndOffset) / 2
                val closestItem = layoutInfo.visibleItemsInfo.minByOrNull { item ->
                    abs((item.offset + item.size / 2) - viewportCenter)
                }
                closestItem?.let {
                    selectedIndex = it.index % songs.size
                }
            }
    }

    Scaffold(
        bottomBar = {
            currentPlayingSong?.let { song ->
                BottomMusicPlayer(
                    title = song.first,
                    imageRes = song.second,
                    isPlaying = isPlaying,
                    onPlayPauseClick = { isPlaying = !isPlaying }
                )
            }
        },
        backgroundColor = Color(0xFFFDFDFD)
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
        ) {
            HeaderSection()
            Spacer(modifier = Modifier.height(16.dp))
            MoodCard()
            Spacer(modifier = Modifier.height(18.dp))

            Text(
                text = "Your Favourite Songs",
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(start = 16.dp),
                color = Color.Black
            )

            Spacer(modifier = Modifier.height(0.dp))

            Box(modifier = Modifier.fillMaxWidth().height(280.dp)) {
                LazyRow(
                    state = listState,
                    contentPadding = PaddingValues(horizontal = 90.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(virtualCount) { index ->
                        val songIndex = index % songs.size
                        val song = songs[songIndex]

                        Box(modifier = Modifier.clickable {
                            currentPlayingSong = song
                        }) {
                            SongItem(
                                title = song.first,
                                image = song.second,
                                selected = songIndex == selectedIndex
                            )
                        }
                    }
                }
            }
            Spacer(modifier = Modifier.height(60.dp))
        }
    }
}