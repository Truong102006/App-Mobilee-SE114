package com.soulmate.app.ui.home

import androidx.compose.foundation.*
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.*
import androidx.hilt.navigation.compose.hiltViewModel
import com.google.firebase.auth.FirebaseAuth
import com.soulmate.app.R
import com.soulmate.app.ui.chat.ChatViewModel
import com.soulmate.app.ui.home.components.*
import com.soulmate.app.ui.journal.history.HistoryViewModel
import com.soulmate.app.ui.login.AuthViewModel
import com.soulmate.app.ui.social.Comment
import com.soulmate.app.ui.social.CommunityCard
import com.soulmate.app.ui.social.CommunityViewModel
import kotlin.math.roundToInt

@Composable
fun HomeScreen(
    musicViewModel: MusicViewModel, 
    historyViewModel: HistoryViewModel,
    communityViewModel: CommunityViewModel = hiltViewModel(),
    chatViewModel: ChatViewModel = hiltViewModel(),
    onChatBubbleClick: () -> Unit = {},
    onNavigateToChat: (String, String, String?) -> Unit = { _, _, _ -> }
) {
    val authViewModel: AuthViewModel = hiltViewModel()
    val currentUserId = remember { FirebaseAuth.getInstance().currentUser?.uid ?: "" }

    val songs = musicViewModel.songs
    val currentPlayingSong by musicViewModel.currentPlayingSong
    val isPlaying by musicViewModel.isPlaying
    val currentPosition by musicViewModel.currentPosition
    val duration by musicViewModel.duration
    val isFullScreen by musicViewModel.isFullScreen
    val currentUser by authViewModel.currentUser
    val communityPosts by communityViewModel.posts
    val postComments = communityViewModel.postComments
    
    val hasUnread by if (currentUserId.isNotEmpty()) {
        chatViewModel.hasUnreadMessages(currentUserId).collectAsState()
    } else {
        remember { mutableStateOf(false) }
    }

    val virtualCount = 50000
    val listState = rememberLazyListState()
    var selectedIndex by remember { mutableIntStateOf(0) }

    LaunchedEffect(currentUserId) {
        if (currentUserId.isNotEmpty()) {
            chatViewModel.loadLastMessages(currentUserId)
        }
    }

    LaunchedEffect(Unit) {
        listState.scrollToItem(virtualCount / 2)
    }

    LaunchedEffect(Unit) {
        while (true) {
            kotlinx.coroutines.delay(2800)
            try { listState.animateScrollToItem(listState.firstVisibleItemIndex + 1) } catch (_: Exception) {}
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
        currentUser = currentUser,
        communityPosts = communityPosts,
        postComments = postComments,
        onLikeClick = { postId -> communityViewModel.toggleLike(postId) },
        onCommentClick = { postId, content, parentId, replyToUserName -> 
            val user = authViewModel.currentUser.value
            communityViewModel.addComment(
                postId, 
                user?.userId ?: "", 
                user?.anonymousName ?: "User", 
                user?.avatarUrl, 
                content,
                parentId,
                replyToUserName
            ) 
        },
        onLikeComment = { postId, commentId -> communityViewModel.toggleCommentLike(postId, commentId) },
        onOpenComments = { postId -> communityViewModel.loadComments(postId) },
        onDeleteClick = { postId -> communityViewModel.deletePost(postId) },
        onEditClick = { postId, content -> communityViewModel.updatePostContent(postId, content) },
        onChatBubbleClick = onChatBubbleClick,
        onUserClick = onNavigateToChat,
        hasUnread = hasUnread
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
    currentUser: com.soulmate.app.domain.model.User? = null,
    communityPosts: List<com.soulmate.app.ui.social.CommunityPost>,
    postComments: Map<String, List<Comment>>,
    onLikeClick: (String) -> Unit,
    onCommentClick: (String, String, String?, String?) -> Unit,
    onLikeComment: (String, String) -> Unit,
    onOpenComments: (String) -> Unit,
    onDeleteClick: (String) -> Unit,
    onEditClick: (String, String) -> Unit,
    onChatBubbleClick: () -> Unit,
    onUserClick: (String, String, String?) -> Unit = { _, _, _ -> },
    hasUnread: Boolean = false
) {
    val virtualCount = 50000

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
                        CommunityCard(
                            post = post,
                            comments = postComments[post.id] ?: emptyList(),
                            onLikeClick = { onLikeClick(post.id) },
                            onCommentClick = { content, parentId, replyToUserName -> 
                                onCommentClick(post.id, content, parentId, replyToUserName) 
                            },
                            onLikeComment = { commentId -> onLikeComment(post.id, commentId) },
                            onOpenComments = { onOpenComments(post.id) },
                            onDeleteClick = { onDeleteClick(post.id) },
                            onEditClick = { newContent -> onEditClick(post.id, newContent) },
                            currentUserAvatarUrl = currentUser?.avatarUrl,
                            currentUserName = currentUser?.anonymousName ?: "User",
                            onUserClick = { 
                                if (post.userId.isNotEmpty()) {
                                    onUserClick(post.userId, post.userName, post.userAvatarUrl)
                                }
                            },
                            onChatClick = {
                                if (post.userId.isNotEmpty()) {
                                    onUserClick(post.userId, post.userName, post.userAvatarUrl)
                                }
                            }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(100.dp))
            }
        }

        var offsetX by remember { mutableFloatStateOf(0f) }
        var offsetY by remember { mutableFloatStateOf(0f) }

        Box(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(end = 20.dp, bottom = 120.dp)
                .offset { IntOffset(offsetX.roundToInt(), offsetY.roundToInt()) }
                .pointerInput(Unit) {
                    detectDragGesturesAfterLongPress { change, dragAmount ->
                        change.consume()
                        offsetX += dragAmount.x
                        offsetY += dragAmount.y
                    }
                }
                .size(54.dp)
                .shadow(elevation = 8.dp, shape = CircleShape)
                .clip(CircleShape)
                .background(Color.White)
                .clickable { onChatBubbleClick() },
            contentAlignment = Alignment.Center
        ) {
            Image(
                painter = painterResource(id = R.drawable.messenger),
                contentDescription = "Chat",
                modifier = Modifier
                    .fillMaxSize()
                    .padding(8.dp),
                contentScale = ContentScale.Fit
            )
            
            if (hasUnread) {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(top = 4.dp, end = 4.dp)
                        .size(12.dp)
                        .clip(CircleShape)
                        .background(Color.Red)
                        .border(1.5.dp, Color.White, CircleShape)
                )
            }
        }

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
