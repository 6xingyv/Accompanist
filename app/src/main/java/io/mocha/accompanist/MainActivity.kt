package io.mocha.accompanist

import android.content.ContentResolver
import android.content.Context
import android.net.Uri
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.imageResource
import androidx.compose.ui.unit.dp
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.session.MediaSession
import com.mocharealm.accompanist.lyrics.model.SyncedLyrics
import com.mocharealm.accompanist.lyrics.parser.LyricifySyllableParser
import io.mocha.accompanist.data.model.playback.LyricsState
import io.mocha.accompanist.ui.composable.background.FlowingLightBackground
import io.mocha.accompanist.ui.composable.lyrics.KaraokeLyricsView
import io.mocha.accompanist.ui.theme.AccompanistTheme
import kotlinx.coroutines.android.awaitFrame

class MainActivity : ComponentActivity() {
    private fun Context.resourceUri(resourceId: Int): Uri = with(resources) {
        Uri.Builder()
            .scheme(ContentResolver.SCHEME_ANDROID_RESOURCE)
            .authority(getResourcePackageName(resourceId))
            .appendPath(getResourceTypeName(resourceId))
            .appendPath(getResourceEntryName(resourceId))
            .build()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Fucking edge to edge
        enableEdgeToEdge(
            navigationBarStyle = SystemBarStyle.auto(
                Color.Transparent.toArgb(),
                Color.Transparent.toArgb()
            ),
            statusBarStyle = SystemBarStyle.dark(Color.White.toArgb())
        )
        window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS)
        window.statusBarColor = Color.Transparent.toArgb()
        window.navigationBarColor = Color.Transparent.toArgb()
        window.isStatusBarContrastEnforced = false
        window.isNavigationBarContrastEnforced = false

        setContent {
            val context = LocalContext.current
            val player = remember { 
                ExoPlayer.Builder(context)
                    .build()
                    .apply {
                        setAudioAttributes(
                            AudioAttributes.Builder()
                                .setContentType(C.AUDIO_CONTENT_TYPE_MUSIC)
                                .setUsage(C.USAGE_MEDIA)
                                .build(),
                            true
                        )
                    }
            }
            val item = MediaItem.fromUri(this.resourceUri(R.raw.test))
            val mediaSession = remember { 
                MediaSession.Builder(context, player).build() 
            }
            var currentPosition by remember { mutableLongStateOf(0L) }
            var isPlaying by remember { mutableStateOf(false) }
            val listState = rememberLazyListState()
            val currentPlayer by rememberUpdatedState(player)

            // 监听播放状态
            LaunchedEffect(player) {
                val listener = object : Player.Listener {
                    override fun onIsPlayingChanged(playing: Boolean) {
                        isPlaying = playing
                    }
                }
                player.addListener(listener)
            }

            DisposableEffect(Unit) {
                player.setMediaItem(item)
                player.prepare()
                player.playWhenReady = true
                onDispose {
                    mediaSession.release()
                    player.release()
                }
            }

            LaunchedEffect(currentPlayer) {
                while (true) {
                    awaitFrame()
                    val newPosition = currentPlayer.currentPosition
                    currentPosition = newPosition
                }
            }

            AccompanistTheme {
                Scaffold(
                    modifier = Modifier.fillMaxSize(),
                    containerColor = Color.Black,
                    contentColor = Color.White,
                ) { innerPadding ->
                    Box {
                        FlowingLightBackground(
                            bitmap = remember {
                                ImageBitmap.imageResource(
                                    resources,
                                    R.drawable.test
                                )
                            }
                        )

                        Column(Modifier.padding()) {
                            var lyrics: SyncedLyrics? by remember {
                                mutableStateOf(null)
                            }
                            LaunchedEffect(Unit) {
                                try {
                                    val asset = application.assets.open("me.lys")
                                    val data = asset.bufferedReader().use { it.readLines() }
                                    asset.close()
                                    val parsedLyrics = LyricifySyllableParser.parse(data)
                                    lyrics = parsedLyrics
                                    // 调试信息
                                    println("歌词加载成功: ${parsedLyrics.lines.size} 行")
                                } catch (e: Exception) {
                                    println("歌词加载失败: ${e.message}")
                                    lyrics = SyncedLyrics(emptyList())
                                }
                            }
                              lyrics?.let { lyricsData ->
                                // 稳定的 LyricsState 创建，避免不必要的重组
                                val lyricsState = remember(lyricsData) {
                                    LyricsState(
                                        { currentPlayer.currentPosition.toInt() },
                                        if (currentPlayer.duration == C.TIME_UNSET) 0 else currentPlayer.duration.toInt(),
                                        lyricsData,
                                        listState
                                    )
                                }
                                  KaraokeLyricsView(
                                    lyricsState = lyricsState,
                                    lyrics = lyricsData,
                                    currentPosition = currentPosition,
                                    onLineClicked = { line ->
                                        currentPlayer.seekTo(line.start.toLong())
                                    },
                                    modifier = Modifier
                                        .padding(horizontal = 12.dp)
                                        .graphicsLayer {
                                            compositingStrategy = CompositingStrategy.ModulateAlpha
                                        }
                                )
                            }
                        }
                        Spacer(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(innerPadding.calculateTopPadding().value.dp * 4)
                                .background(
                                    Brush.verticalGradient(
                                        0f to Color.Black.copy(0.6f),
                                        1f to Color.Transparent
                                    )
                                )
                        )


                        Text(currentPosition.toString(), modifier = Modifier.statusBarsPadding())
                    }
                }
            }
        }
    }
}
