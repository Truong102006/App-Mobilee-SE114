package com.soulmate.app.ui.home

import android.app.Application
import android.net.Uri
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import com.soulmate.app.R
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class MusicViewModel @Inject constructor(
    application: Application
) : AndroidViewModel(application) {

    private val _exoPlayer = ExoPlayer.Builder(application).build()
    val exoPlayer: ExoPlayer = _exoPlayer

    val songs = listOf(
        Song("Alaba trap", "MCK", R.drawable.song111, R.raw.song1),

        Song("Hooligan", "BTS", R.drawable.song12, R.raw.song12),
        Song("IDOL", "BTS", R.drawable.song13, R.raw.song13),
        Song("DNA", "BTS", R.drawable.song14, R.raw.song14),
        Song("Not Today", "BTS", R.drawable.song15, R.raw.song15),
        Song("Go Go", "BTS", R.drawable.song16, R.raw.song16),

        Song("Thích quá rùi nà", "Tlinh", R.drawable.song22, R.raw.song2),
        Song("Nghe như tình yêu", "HIEUTHUHAI", R.drawable.song33, R.raw.song3),
        Song("Stay", "Justin Bieber", R.drawable.song4, R.raw.song4),
        Song("Bước qua mùa cô đơn", "Vũ", R.drawable.song5, R.raw.song5),
        Song("Lạ lùng", "Vũ", R.drawable.song6, R.raw.song6),

        Song("Chìm Sâu", "RPT MCK", R.drawable.song17, R.raw.song17),
        Song("Vẫn Đợi", "Wrxdie", R.drawable.song18, R.raw.song18),
        Song("Phóng Zìn Zìn", "Tlinh", R.drawable.song19, R.raw.song19),
        Song("Đen đá không đường", "AMEE", R.drawable.song20, R.raw.song20),
        Song("50 Năm Về Sau", "Đặng Thanh Tuyền", R.drawable.song21, R.raw.song21),

        Song("Cần gì nói yêu", "Wxrdie", R.drawable.song77, R.raw.song7),
        Song("Cua", "HIEUTHUHAI", R.drawable.song88, R.raw.song8),
        Song("Mamma Mia", "HIEUTHUHAI", R.drawable.song99, R.raw.song9),
        Song("Big City Boy", "Binz", R.drawable.song10, R.raw.song10),
        Song("Pho Real", "Low G", R.drawable.song11, R.raw.pho_real),





    )

    private val _currentPlayingSong = mutableStateOf<Song?>(null)
    val currentPlayingSong: State<Song?> = _currentPlayingSong

    private val _isPlaying = mutableStateOf(false)
    val isPlaying: State<Boolean> = _isPlaying

    private val _currentPosition = mutableLongStateOf(0L)
    val currentPosition: State<Long> = _currentPosition

    private val _duration = mutableLongStateOf(0L)
    val duration: State<Long> = _duration

    private val _isFullScreen = mutableStateOf(false)
    val isFullScreen: State<Boolean> = _isFullScreen

    init {
        _exoPlayer.addListener(object : Player.Listener {
            override fun onPlaybackStateChanged(state: Int) {
                if (state == Player.STATE_ENDED) playNextSong()
            }
            override fun onIsPlayingChanged(isPlaying: Boolean) {
                _isPlaying.value = isPlaying
            }
        })

        viewModelScope.launch {
            while (true) {
                if (_isPlaying.value) {
                    _currentPosition.longValue = _exoPlayer.currentPosition
                    _duration.longValue = _exoPlayer.duration.coerceAtLeast(0L)
                }
                delay(500)
            }
        }
    }

    fun toggleFullScreen(show: Boolean) {
        _isFullScreen.value = show
    }

    fun playSong(song: Song) {
        _currentPlayingSong.value = song
        val uri = Uri.parse("android.resource://${getApplication<Application>().packageName}/${song.musicRes}")
        val mediaItem = MediaItem.fromUri(uri)
        _exoPlayer.setMediaItem(mediaItem)
        _exoPlayer.prepare()
        _exoPlayer.play()
    }

    fun togglePlayPause() {
        if (_exoPlayer.isPlaying) _exoPlayer.pause() else _exoPlayer.play()
    }

    fun playNextSong() {
        _currentPlayingSong.value?.let { current ->
            val currentIndex = songs.indexOf(current)
            val nextIndex = (currentIndex + 1) % songs.size
            playSong(songs[nextIndex])
        }
    }

    fun playPreviousSong() {
        _currentPlayingSong.value?.let { current ->
            val currentIndex = songs.indexOf(current)
            val prevIndex = if (currentIndex <= 0) songs.size - 1 else currentIndex - 1
            playSong(songs[prevIndex])
        }
    }

    fun seekTo(position: Long) {
        _exoPlayer.seekTo(position)
    }

    override fun onCleared() {
        super.onCleared()
        _exoPlayer.release()
    }
}
