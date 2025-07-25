package com.mocharealm.accompanist

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.graphics.createBitmap
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.media3.common.MediaItem
import com.mocharealm.accompanist.domain.model.MusicItem
import com.mocharealm.accompanist.service.PlaybackService
import com.mocharealm.accompanist.ui.composable.background.BackgroundVisualState
import com.mocharealm.accompanist.ui.composable.background.FlowingLightBackground
import com.mocharealm.accompanist.ui.composable.lyrics.KaraokeLyricsView
import com.mocharealm.accompanist.ui.theme.AccompanistTheme
import kotlinx.coroutines.android.awaitFrame

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
        // FUCKING XIAOMI
        @Suppress("DEPRECATION") if (Build.MANUFACTURER == "Xiaomi") {
            window.addFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_NAVIGATION)
        }

        setContent {
            val uiState by playerViewModel.uiState.collectAsStateWithLifecycle()
            var animatedPosition by remember { mutableLongStateOf(0L) }
            val listState = rememberLazyListState()

            var selectedMusicItem by remember { mutableStateOf<MusicItem?>(null) }
            val testItems = listOf(
                MusicItem(
                    "La Pelirroja",
                    "TTML v1",
                    MediaItem.fromUri("asset:///la-pelirroja.mp3"),
                    "la-pelirroja.ttml"
                ),
                MusicItem(
                    "ME!",
                    "Lyricify Syllable & LRC",
                    MediaItem.fromUri("asset:///me.mp3"),
                    "me.lys",
                    "me-translation.lrc"
                )
            )

            LaunchedEffect(selectedMusicItem, uiState.isReady) {
                if (selectedMusicItem != null && uiState.isReady) {
                    playerViewModel.setMediaItemAndPlay(selectedMusicItem!!)
                }
            }

            val latestPlaybackState by rememberUpdatedState(uiState.playbackState)
            LaunchedEffect(uiState.playbackState.isPlaying) {
                if (latestPlaybackState.isPlaying) {
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

            @SuppressLint("UnusedMaterial3ScaffoldPaddingParameter") AccompanistTheme {
                Scaffold(
                    modifier = Modifier.fillMaxSize(),
                    containerColor = Color.Black,
                    contentColor = Color.White,
                ) {
                    Box(modifier = Modifier.fillMaxSize()) {
                        uiState.backgroundState.artwork?.let { bitmap ->
                            FlowingLightBackground(
                                state = BackgroundVisualState(
                                    bitmap,
                                    uiState.backgroundState.isBright
                                )
                            )
                        }

                        uiState.lyrics?.let { lyrics ->
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

                        if (uiState.showSelectionDialog) {
                            MusicItemSelectionDialog(
                                items = testItems,
                                onItemSelected = { selectedItem ->
                                    playerViewModel.onSongSelected()
                                    playerViewModel.setMediaItemAndPlay(selectedItem)
                                },
                                onDismissRequest = {
                                    // 如果用户取消了对话框，也可以通知ViewModel
                                    // playerViewModel.onDialogDismissed()
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun MusicItemSelectionDialog(
    items: List<MusicItem>,
    onItemSelected: (MusicItem) -> Unit,
    onDismissRequest: () -> Unit // 当用户点击对话框外部或返回键时调用
) {
    var selectedIndex by remember { mutableIntStateOf(-1) }
    AlertDialog(
        onDismissRequest = onDismissRequest,
        title = { Text("Choose a song to play") },
        text = {
            LazyColumn {
                itemsIndexed(items) { index, item ->
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { selectedIndex = index }
                            .padding(vertical = 12.dp)
                    ) {
                        Text(item.label, fontWeight = FontWeight.Bold)
                        Text(item.testTarget, fontSize = 14.sp)
                    }
                }
            }
        },
        // 我们不需要确认按钮，因为点击列表项就是选择了
        confirmButton = {
            Text("Confirm", Modifier.clickable {
                if (selectedIndex != -1) {
                    onItemSelected(items[selectedIndex])
                }
            })
        }
    )
}

fun Color.toImageBitmap(width: Int = 1, height: Int = 1): ImageBitmap {
    // 1. 创建一个指定尺寸的 Android 原生 Bitmap
    val bitmap = createBitmap(width, height)

    // 2. 将该 Bitmap 作为画布 (Canvas)
    val canvas = android.graphics.Canvas(bitmap)

    // 3. 将 Compose Color 转换为 Android Color Int，并用该颜色填充整个画布
    canvas.drawColor(this.toArgb())

    // 4. 将 Android Bitmap 转换为 Compose ImageBitmap 并返回
    return bitmap.asImageBitmap()
}
