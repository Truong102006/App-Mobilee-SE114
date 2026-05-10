package com.soulmate.app.ui.home

import androidx.compose.foundation.*
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Scaffold
import androidx.compose.material.Text
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.*
import androidx.hilt.navigation.compose.hiltViewModel
import com.soulmate.app.ui.home.components.*
import com.soulmate.app.ui.journal.history.HistoryViewModel
import com.soulmate.app.ui.login.AuthViewModel
import com.soulmate.app.ui.social.CommunityCard
import com.soulmate.app.ui.social.getMockCommunityPosts

@Composable
fun HomeScreen(
    musicViewModel: MusicViewModel, 
    historyViewModel: HistoryViewModel,
    authViewModel: AuthViewModel = hiltViewModel()
) {
    val songs = musicViewModel.songs
    val currentPlayingSong by musicViewModel.currentPlayingSong
    val isPlaying by musicViewModel.isPlaying
    val currentPosition by musicViewModel.currentPosition
    val duration by musicViewModel.duration
    val isFullScreen by musicViewModel.isFullScreen
    val currentUser by authViewModel.currentUser

    // --- LOGIC HIỂN THỊ DANH SÁCH ---
    val virtualCount = 50000
    val listState = rememberLazyListState()
    var selectedIndex by remember { mutableIntStateOf(0) }

    LaunchedEffect(Unit) {
        listState.scrollToItem(virtualCount / 2)
    }

    LaunchedEffect(Unit) {
        while (true) {
            kotlinx.coroutines.delay(2800)
            try { listState.animateScrollToItem(listState.firstVisibleItemIndex + 1) } catch (e: Exception) {}
        }
    }

    LaunchedEffect(listState) {
        snapshotFlow { listState.layoutInfo }
            .collect { layoutInfo ->
                val viewportCenter = (layoutInfo.viewportStartOffset + layoutInfo.viewportEndOffset) / 2
                val closestItem = layoutInfo.visibleItemsInfo.minByOrNull { item ->
                    kotlin.math.abs((item.offset + item.size / 2) - viewportCenter)
                }
                closestItem?.let { selectedIndex = it.index % songs.size }
            }
    }

    HomeScreenContent(
        songs = songs,
        isFullScreen = isFullScreen,
        currentPlayingSong = currentPlayingSong,
        isPlaying = isPlaying,
        currentPosition = currentPosition,
        duration = duration,
        listState = listState,
        selectedIndex = selectedIndex,
        onSongClick = { musicViewModel.playSong(it) },
        onPlayPauseClick = { musicViewModel.togglePlayPause() },
        onNextClick = { musicViewModel.playNextSong() },
        onPreviousClick = { musicViewModel.playPreviousSong() },
        onPlayerClick = { musicViewModel.toggleFullScreen(true) },
        onBackClick = { musicViewModel.toggleFullScreen(false) },
        onSeek = { musicViewModel.seekTo(it) },
        historyViewModel = historyViewModel,
        currentUser = currentUser
    )
}

@Composable
fun HomeScreenContent(
    songs: List<Song>,
    isFullScreen: Boolean,
    currentPlayingSong: Song?,
    isPlaying: Boolean,
    currentPosition: Long,
    duration: Long,
    listState: LazyListState,
    selectedIndex: Int,
    onSongClick: (Song) -> Unit,
    onPlayPauseClick: () -> Unit,
    onNextClick: () -> Unit,
    onPreviousClick: () -> Unit,
    onPlayerClick: () -> Unit,
    onBackClick: () -> Unit,
    onSeek: (Long) -> Unit,
    historyViewModel: HistoryViewModel? = null,
    currentUser: com.soulmate.app.domain.model.User? = null
) {
    val virtualCount = 50000
    val communityPosts = remember { getMockCommunityPosts() }

    // --- GIAO DIỆN ---
    Box(modifier = Modifier.fillMaxSize()) {
        Scaffold(
            bottomBar = {
                currentPlayingSong?.let { song ->
                    if (!isFullScreen) {
                        BottomMusicPlayer(
                            title = song.title,
                            artist = song.artist,
                            imageRes = song.imageRes,
                            isPlaying = isPlaying,
                            onPlayPauseClick = onPlayPauseClick,
                            onNextClick = onNextClick,
                            onPlayerClick = onPlayerClick
                        )
                    }
                }
            },
            backgroundColor = MaterialTheme.colors.background
        ) { paddingValues ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .verticalScroll(rememberScrollState())
            ) {
                HeaderSection(user = currentUser)
                Spacer(modifier = Modifier.height(16.dp))
                MoodCard(historyViewModel)
                Spacer(modifier = Modifier.height(18.dp))
                
                Text(
                    text = "Your Favourite Songs",
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colors.onBackground,
                    modifier = Modifier.padding(start = 16.dp)
                )
                Spacer(modifier = Modifier.height(15.dp))
                Box(modifier = Modifier.fillMaxWidth().height(280.dp)) {
                    LazyRow(
                        state = listState,
                        contentPadding = PaddingValues(horizontal = 70.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        items(virtualCount) { index ->
                            val songIndex = index % songs.size
                            val song = songs[songIndex]
                            Box(modifier = Modifier.clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null
                            ) { onSongClick(song) }) {
                                SongItem(song.title, song.imageRes, songIndex == selectedIndex)
                            }
                        }
                    }
                }

                // --- PHẦN COMMUNITY FEEDS ĐƯỢC CHUYỂN TỪ COMMUNITYSCREEN ---
                Spacer(modifier = Modifier.height(24.dp))
                Text(
                    text = "Community Feeds",
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colors.onBackground,
                    modifier = Modifier.padding(start = 16.dp)
                )
                Spacer(modifier = Modifier.height(16.dp))
                
                Column(
                    modifier = Modifier.padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(20.dp)
                ) {
                    communityPosts.forEach { post ->
                        CommunityCard(post = post)
                    }
                }

                Spacer(modifier = Modifier.height(100.dp))
            }
        }

        // Màn hình chi tiết với thanh thời lượng thực tế
        if (isFullScreen && currentPlayingSong != null) {
            MusicPlayerDetailScreen(
                title = currentPlayingSong.title,
                artist = currentPlayingSong.artist,
                imageRes = currentPlayingSong.imageRes,
                isPlaying = isPlaying,
                currentPosition = currentPosition,
                duration = duration,
                onPlayPauseClick = onPlayPauseClick,
                onNextClick = onNextClick,
                onPreviousClick = onPreviousClick,
                onBackClick = onBackClick,
                onSeek = onSeek
            )
        }
    }
}
