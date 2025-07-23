package com.mocharealm.accompanist

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.media3.common.MediaItem
import com.mocharealm.accompanist.lyrics.model.SyncedLyrics
import com.mocharealm.accompanist.lyrics.model.karaoke.KaraokeLine
import com.mocharealm.accompanist.lyrics.model.synced.SyncedLine
import com.mocharealm.accompanist.lyrics.parser.LrcParser
import com.mocharealm.accompanist.lyrics.parser.LyricifySyllableParser
import com.mocharealm.accompanist.service.PlaybackService
import com.mocharealm.accompanist.ui.composable.background.BackgroundVisualState
import com.mocharealm.accompanist.ui.composable.background.FlowingLightBackground
import com.mocharealm.accompanist.ui.composable.background.calculateAverageBrightness
import com.mocharealm.accompanist.ui.composable.lyrics.KaraokeLyricsView
import com.mocharealm.accompanist.ui.theme.AccompanistTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.android.awaitFrame
import kotlinx.coroutines.withContext

class MainActivity : ComponentActivity() {

    private val playerViewModel: PlayerViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val intent = Intent(this, PlaybackService::class.java)
        startService(intent)

        enableEdgeToEdge(
            navigationBarStyle = SystemBarStyle.auto(
                Color.Transparent.toArgb(), Color.Transparent.toArgb()
            ), statusBarStyle = SystemBarStyle.dark(Color.White.toArgb())
        )
        @Suppress("DEPRECATION") if (Build.MANUFACTURER == "Xiaomi") {
            window.addFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_NAVIGATION)
        }

        setContent {
            // 1. STATE DECLARATIONS
            val playbackState by playerViewModel.playbackState.collectAsStateWithLifecycle()
            var animatedPosition by remember { mutableLongStateOf(0L) }
            val listState = rememberLazyListState()
            val placeholderBitmap = remember { ImageBitmap(1, 1) }
            var backgroundState by remember {
                mutableStateOf(
                    BackgroundVisualState(
                        placeholderBitmap, false
                    )
                )
            }
            var syncedLyrics by remember { mutableStateOf<SyncedLyrics?>(null) }


            // 2. SIDE EFFECTS

            // Effect for background analysis
            LaunchedEffect(playbackState.artwork) {
                val artwork = playbackState.artwork ?: placeholderBitmap
                val isBright = withContext(Dispatchers.Default) {
                    calculateAverageBrightness(artwork) > 0.65f
                }
                backgroundState = BackgroundVisualState(artwork, isBright)
            }

            // Effect for player initialization
            LaunchedEffect(playbackState.isReady) {
                if (playbackState.isReady && playbackState.duration == 0L) {
                    val mediaItem = MediaItem.fromUri("asset:///me.flac")
                    playerViewModel.setMediaItem(mediaItem)
                    playerViewModel.prepare()
                    playerViewModel.playWhenReady(true)
                }
            }

            // Effect for smooth position animation
            val latestPlaybackState by rememberUpdatedState(playbackState)
            LaunchedEffect(playbackState.isPlaying) {
                if (playbackState.isPlaying) {
                    while (true) {
                        val elapsed =
                            System.currentTimeMillis() - latestPlaybackState.lastUpdateTime
                        animatedPosition = (latestPlaybackState.position + elapsed).coerceAtMost(
                            latestPlaybackState.duration
                        )
                        awaitFrame()
                    }
                }
            }

            // Effect for loading lyrics on a background thread
            LaunchedEffect(Unit) {
                withContext(Dispatchers.IO) {
                    try {
                        val lyricsData = application.assets.open("me.lys").bufferedReader()
                            .use { it.readLines() }
                        val lyricsRaw = LyricifySyllableParser.parse(lyricsData)

                        val translationData =
                            application.assets.open("me-translation.lrc").bufferedReader()
                                .use { it.readLines() }
                        val translationRaw = LrcParser.parse(translationData)

                        val translationMap =
                            translationRaw.lines.associateBy { (it as SyncedLine).start }

                        val finalLyrics = SyncedLyrics(
                            lyricsRaw.lines.map { line ->
                                val karaokeLine = line as KaraokeLine
                                val translationContent =
                                    (translationMap[karaokeLine.start] as SyncedLine?)?.content
                                karaokeLine.copy(translation = translationContent)
                            })
                        // Create the new, simple, and stable LyricsState object
                        syncedLyrics = finalLyrics
                    } catch (e: Exception) {
                        Log.e("LyricsLoader", "Failed to load or parse lyrics", e)
                    }
                }
            }


            @SuppressLint("UnusedMaterial3ScaffoldPaddingParameter") AccompanistTheme {
                Scaffold(
                    modifier = Modifier.fillMaxSize(),
                    containerColor = Color.Black,
                    contentColor = Color.White,
                ) {
                    Box(modifier = Modifier.fillMaxSize()) {
                        FlowingLightBackground(state = backgroundState)


                        // Only display lyrics UI when lyrics are loaded
                        syncedLyrics?.let { lyrics ->
                            KaraokeLyricsView(
                                listState = listState,
                                lyrics = lyrics,
                                currentPosition = animatedPosition,
                                onLineClicked = { line ->
                                    playerViewModel.seekTo(line.start.toLong())
                                },
                                modifier = Modifier
                                    .padding(horizontal = 12.dp)
                                    .graphicsLayer {
                                        compositingStrategy = CompositingStrategy.Offscreen
                                    },
                            )
                        }
                    }
                }
            }
        }
    }
}