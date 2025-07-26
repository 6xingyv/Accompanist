// PlayerViewModel.kt
package com.mocharealm.accompanist

import android.app.Application
import android.content.ComponentName
import android.graphics.BitmapFactory
import android.util.Log
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.media3.common.C
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import com.google.common.util.concurrent.MoreExecutors
import com.mocharealm.accompanist.domain.model.MusicItem
import com.mocharealm.accompanist.lyrics.model.SyncedLyrics
import com.mocharealm.accompanist.lyrics.model.karaoke.KaraokeLine
import com.mocharealm.accompanist.lyrics.model.synced.SyncedLine
import com.mocharealm.accompanist.lyrics.parser.AutoParser
import com.mocharealm.accompanist.service.PlaybackService
import com.mocharealm.accompanist.ui.composable.background.calculateAverageBrightness
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

// 你可以把这些 data class 放在 ViewModel 文件的顶部，或者单独的文件里

// 播放相关的核心状态
data class PlaybackState(
    val isPlaying: Boolean = false,
    val position: Long = 0L,
    val duration: Long = 0L,
    val lastUpdateTime: Long = 0L
)

// 视觉背景相关的状态
data class BackgroundUiState(
    val artwork: ImageBitmap? = null,
    val isBright: Boolean = false
)

// UI需要的所有状态的集合
data class PlayerUiState(
    val isReady: Boolean = false, // PlayerController 是否准备好
    val showSelectionDialog: Boolean = true,
    val playbackState: PlaybackState = PlaybackState(),
    val backgroundState: BackgroundUiState = BackgroundUiState(),
    val lyrics: SyncedLyrics? = null
)

// PlayerViewModel.kt

class PlayerViewModel(application: Application) : AndroidViewModel(application) {
    private val _uiState = MutableStateFlow(PlayerUiState())
    val uiState = _uiState.asStateFlow()

    private var positionUpdateJob: Job? = null
    private var controller: MediaController? = null

    private var lastArtworkData: ByteArray? = null
    private var cachedArtworkBitmap: ImageBitmap? = null

    private val assets: android.content.res.AssetManager = application.assets
    private val autoParser = AutoParser.Builder().build()

    init {
        val sessionToken =
            SessionToken(application, ComponentName(application, PlaybackService::class.java))
        val controllerFuture = MediaController.Builder(application, sessionToken).buildAsync()

        controllerFuture.addListener({
            controller = controllerFuture.get()
            controller?.addListener(playerListener)
            _uiState.value = _uiState.value.copy(isReady = true) // 通知UI Controller已就绪
            updatePlaybackState()
        }, MoreExecutors.directExecutor())
    }

    private val playerListener = object : Player.Listener {
        override fun onIsPlayingChanged(playing: Boolean) {
            updatePlaybackState()
            if (playing) startPositionUpdates() else stopPositionUpdates()
        }

        override fun onEvents(player: Player, events: Player.Events) {
            if (events.contains(Player.EVENT_MEDIA_METADATA_CHANGED)) {
                lastArtworkData = null // 清空缓存，强制下次更新
            }
            updatePlaybackState()
        }

        override fun onPlayerError(error: PlaybackException) {
            Log.e("PlayerViewModel", "Player Error: ${error.message}", error)
        }
    }

    private fun updatePlaybackState() {
        controller?.let {
            val newArtworkData = it.mediaMetadata.artworkData
            if (newArtworkData != null && !newArtworkData.contentEquals(lastArtworkData)) {
                lastArtworkData = newArtworkData
                cachedArtworkBitmap =
                    BitmapFactory.decodeByteArray(newArtworkData, 0, newArtworkData.size)
                        .asImageBitmap()
                updateBackgroundState(cachedArtworkBitmap)
            } else if (newArtworkData == null && lastArtworkData != null) {
                lastArtworkData = null
                cachedArtworkBitmap = null
                updateBackgroundState(null)
            }

            _uiState.value = _uiState.value.copy(
                playbackState = _uiState.value.playbackState.copy(
                    isPlaying = it.isPlaying,
                    position = it.currentPosition,
                    duration = it.duration.takeIf { d -> d != C.TIME_UNSET } ?: 0L,
                    lastUpdateTime = System.currentTimeMillis()
                )
            )
        }
    }

    fun updateBackgroundState(artwork: ImageBitmap?) {
        viewModelScope.launch(Dispatchers.Default) {
            val isBright = if (artwork != null) {
                calculateAverageBrightness(artwork) > 0.65f
            } else {
                false
            }
            _uiState.value = _uiState.value.copy(
                backgroundState = BackgroundUiState(artwork = artwork, isBright = isBright)
            )
        }
    }

    fun loadLyricsFor(item: MusicItem) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val lyricsData = assets.open(item.lyrics).bufferedReader().use { it.readLines() }
                val lyricsRaw = autoParser.parse(lyricsData)

                val finalLyrics = if (item.translation != null) {
                    val translationData =
                        assets.open(item.translation).bufferedReader().use { it.readLines() }
                    val translationRaw = autoParser.parse(translationData)
                    val translationMap =
                        translationRaw.lines.associateBy { (it as SyncedLine).start }
                    SyncedLyrics(
                        lyricsRaw.lines.map { line ->
                            val karaokeLine = line as KaraokeLine
                            val translationContent =
                                (translationMap[karaokeLine.start] as SyncedLine?)?.content
                            karaokeLine.copy(translation = translationContent)
                        })
                } else {
                    lyricsRaw
                }
                _uiState.value = _uiState.value.copy(lyrics = finalLyrics)
            } catch (e: Exception) {
                Log.e("PlayerViewModel", "Failed to load or parse lyrics", e)
                _uiState.value = _uiState.value.copy(lyrics = null)
            }
        }
    }

    private fun startPositionUpdates() {
        positionUpdateJob?.cancel()
        positionUpdateJob = viewModelScope.launch {
            while (isActive) {
                controller?.let {
                    _uiState.value = _uiState.value.copy(
                        playbackState = _uiState.value.playbackState.copy(
                            position = it.currentPosition,
                            lastUpdateTime = System.currentTimeMillis()
                        )
                    )
                }
                delay(500) // 位置更新频率可以根据需要调整
            }
        }
    }

    fun onSongSelected() {
        _uiState.value = _uiState.value.copy(showSelectionDialog = false)
    }

    fun onOpenSongSelection() {
        controller?.clearMediaItems()
        controller?.stop()
        _uiState.value = _uiState.value.copy(showSelectionDialog = true)
    }

    fun setMediaItemAndPlay(item: MusicItem) {
        controller?.setMediaItem(item.mediaItem)
        controller?.prepare()
        controller?.playWhenReady = true
        loadLyricsFor(item)
    }

    private fun stopPositionUpdates() {
        positionUpdateJob?.cancel()
    }

    fun seekTo(position: Long) {
        controller?.seekTo(position)
    }

    override fun onCleared() {
        controller?.release()
    }
}