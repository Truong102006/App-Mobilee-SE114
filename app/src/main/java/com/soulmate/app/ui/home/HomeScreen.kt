package com.soulmate.app.ui.home

import android.net.Uri
import androidx.compose.foundation.*
import androidx.compose.foundation.interaction.MutableInteractionSource
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

// Khai báo cấu trúc bài hát mới
data class Song(
    val title: String,
    val artist: String,
    val imageRes: Int,
    val musicRes: Int
)

@Composable
fun HomeScreen() {
    val context = LocalContext.current
    val exoPlayer = remember { ExoPlayer.Builder(context).build() }
    var isFullScreen by remember { mutableStateOf(false) }

    DisposableEffect(Unit) {
        onDispose { exoPlayer.release() }
    }

    // Danh sách bài hát với đầy đủ Tên - Tác giả - Ảnh - Nhạc
    val songs = remember {
        listOf(
            Song("Alaba trap", "MCK", R.drawable.song111, R.raw.song1),
            Song("Thích quá rùi nà", "Tlinh", R.drawable.song222, R.raw.song2),
            Song("Nghe như tình yêu", "HIEUTHUHAI", R.drawable.song33, R.raw.song3),
            Song("Stay", "Justin Bieber", R.drawable.song4, R.raw.song4),
            Song("Bước qua mùa cô đơn", "Vũ", R.drawable.song5, R.raw.song5),
            Song("Lạ lùng", "Vũ", R.drawable.song6, R.raw.song6),
            Song("Cần gì nói yêu", "Wxrdie", R.drawable.song77, R.raw.song7),
            Song("Cua", "HIEUTHUHAI", R.drawable.song88, R.raw.song8),
            Song("Mamma Mia", "HIEUTHUHAI", R.drawable.song99, R.raw.song9),
            Song("Big City Boy", "Binz", R.drawable.song10, R.raw.song10),
            Song("Pho Real", "Low G", R.drawable.song_11, R.raw.pho_real)
        )
    }

    var currentPlayingSong by remember { mutableStateOf<Song?>(null) }
    var isPlaying by remember { mutableStateOf(false) }

    val playNextSong = {
        currentPlayingSong?.let { current ->
            val currentIndex = songs.indexOf(current)
            val nextIndex = (currentIndex + 1) % songs.size
            currentPlayingSong = songs[nextIndex]
        } ?: Unit
    }

    val playPreviousSong = {
        currentPlayingSong?.let { current ->
            val currentIndex = songs.indexOf(current)
            val prevIndex = if (currentIndex <= 0) songs.size - 1 else currentIndex - 1
            currentPlayingSong = songs[prevIndex]
        } ?: Unit
    }

    LaunchedEffect(currentPlayingSong) {
        currentPlayingSong?.let { song ->
            val uri = Uri.parse("android.resource://${context.packageName}/${song.musicRes}")
            val mediaItem = MediaItem.fromUri(uri)
            exoPlayer.setMediaItem(mediaItem)
            exoPlayer.prepare()
            exoPlayer.play()
            isPlaying = true
        }
    }

    LaunchedEffect(isPlaying) {
        if (isPlaying) exoPlayer.play() else exoPlayer.pause()
    }

    val virtualCount = 50000
    val listState = rememberLazyListState()
    var selectedIndex by remember { mutableStateOf(0) }

    LaunchedEffect(Unit) {
        listState.scrollToItem(virtualCount / 2)
    }

    LaunchedEffect(Unit) {
        while (true) {
            delay(2800)
            try { listState.animateScrollToItem(listState.firstVisibleItemIndex + 1) } catch (e: Exception) {}
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
                closestItem?.let { selectedIndex = it.index % songs.size }
            }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Scaffold(
            bottomBar = {
                currentPlayingSong?.let { song ->
                    if (!isFullScreen) {
                        BottomMusicPlayer(
                            title = song.title,
                            artist = song.artist, // Truyền thêm artist
                            imageRes = song.imageRes,
                            isPlaying = isPlaying,
                            onPlayPauseClick = { isPlaying = !isPlaying },
                            onNextClick = playNextSong,
                            onPlayerClick = { isFullScreen = true }
                        )
                    }
                }
            },
            backgroundColor = Color(0xFFFDFDFD)
        ) { paddingValues ->
            Column(
                modifier = Modifier.fillMaxSize().padding(paddingValues).verticalScroll(rememberScrollState())
            ) {
                HeaderSection()
                Spacer(modifier = Modifier.height(16.dp))
                MoodCard()
                Spacer(modifier = Modifier.height(18.dp))
                Text(
                    text = "Your Favourite Songs",
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(start = 16.dp)
                )
                Spacer(modifier = Modifier.height(15.dp))
                Box(modifier = Modifier.fillMaxWidth().height(280.dp)) {
                    LazyRow(
                        state = listState,
                        contentPadding = PaddingValues(horizontal = 90.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        items(virtualCount) { index ->
                            val songIndex = index % songs.size
                            val song = songs[songIndex]
                            Box(modifier = Modifier.clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null
                            ) { currentPlayingSong = song }) {
                                SongItem(song.title, song.imageRes, songIndex == selectedIndex)
                            }
                        }
                    }
                }
                Spacer(modifier = Modifier.height(60.dp))
            }
        }

        if (isFullScreen && currentPlayingSong != null) {
            MusicPlayerDetailScreen(
                title = currentPlayingSong!!.title,
                artist = currentPlayingSong!!.artist, // Truyền thêm artist
                imageRes = currentPlayingSong!!.imageRes,
                isPlaying = isPlaying,
                onPlayPauseClick = { isPlaying = !isPlaying },
                onNextClick = playNextSong,
                onPreviousClick = playPreviousSong,
                onBackClick = { isFullScreen = false }
            )
        }
    }
}