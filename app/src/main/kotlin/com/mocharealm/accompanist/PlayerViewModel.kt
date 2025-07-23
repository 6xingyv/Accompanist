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
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import com.google.common.util.concurrent.MoreExecutors
import com.mocharealm.accompanist.service.PlaybackService
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

data class PlaybackState(
    val isReady: Boolean = false,
    val isPlaying: Boolean = false,
    val position: Long = 0L,
    val duration: Long = 0L,
    val artwork: ImageBitmap? = null,
    val lastUpdateTime: Long = 0L
)

class PlayerViewModel(application: Application) : AndroidViewModel(application) {

    private val _playbackState = MutableStateFlow(PlaybackState())
    val playbackState = _playbackState.asStateFlow()

    private var positionUpdateJob: Job? = null
    private var controller: MediaController? = null

    // ✨ 1. 添加私有变量来缓存上一次的封面数据和解码后的 Bitmap
    private var lastArtworkData: ByteArray? = null
    private var cachedArtworkBitmap: ImageBitmap? = null

    init {
        val sessionToken = SessionToken(application, ComponentName(application, PlaybackService::class.java))
        val controllerFuture = MediaController.Builder(application, sessionToken).buildAsync()

        controllerFuture.addListener({
            controller = controllerFuture.get()
            controller?.addListener(playerListener)
            _playbackState.value = _playbackState.value.copy(isReady = true)
            updateState()
        }, MoreExecutors.directExecutor())
    }

    private val playerListener = object : Player.Listener {
        override fun onIsPlayingChanged(playing: Boolean) {
            updateState()
            if (playing) startPositionUpdates() else stopPositionUpdates()
        }

        override fun onEvents(player: Player, events: Player.Events) {
            // 当 onMediaMetadataChanged 事件发生时，强制更新封面
            if (events.contains(Player.EVENT_MEDIA_METADATA_CHANGED)) {
                // 清空缓存，强制下次 updateState 重新解码
                lastArtworkData = null
            }
            updateState()
        }

        override fun onPlayerError(error: PlaybackException) {
            Log.e("PlayerViewModel", "Player Error: ${error.message}", error)
        }
    }

    private fun updateState() {
        controller?.let {
            // ✨ 2. 在更新状态前，处理封面缓存
            val newArtworkData = it.mediaMetadata.artworkData

            // 只有当新的封面数据不为空，且与上次缓存的数据不同时，才解码新图片
            if (newArtworkData != null && !newArtworkData.contentEquals(lastArtworkData)) {
                lastArtworkData = newArtworkData
                cachedArtworkBitmap = BitmapFactory.decodeByteArray(newArtworkData, 0, newArtworkData.size).asImageBitmap()
            } else if (newArtworkData == null) {
                // 如果封面被移除，则清空缓存
                lastArtworkData = null
                cachedArtworkBitmap = null
            }

            // ✨ 3. 更新 PlaybackState，使用缓存的 artwork
            _playbackState.value = _playbackState.value.copy(
                isPlaying = it.isPlaying,
                position = it.currentPosition,
                duration = it.duration.takeIf { d -> d != C.TIME_UNSET } ?: 0L,
                artwork = cachedArtworkBitmap, // <-- 使用缓存的 bitmap
                lastUpdateTime = System.currentTimeMillis()
            )
        }
    }

    private fun startPositionUpdates() {
        positionUpdateJob?.cancel()
        positionUpdateJob = viewModelScope.launch {
            while (isActive) {
                updateState()
                delay(500)
            }
        }
    }

    // ... 其他方法保持不变 ...
    private fun stopPositionUpdates() {
        positionUpdateJob?.cancel()
    }
    fun setMediaItem(item: MediaItem) { controller?.setMediaItem(item) }
    fun prepare() { controller?.prepare() }
    fun playWhenReady(play: Boolean) { controller?.playWhenReady = play }
    fun seekTo(position: Long) { controller?.seekTo(position) }
    override fun onCleared() { controller?.release() }
}