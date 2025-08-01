package com.mocharealm.accompanist

import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextMotion
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import androidx.core.graphics.createBitmap
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.media3.common.MediaItem
import com.mocharealm.accompanist.domain.model.MusicItem
import com.mocharealm.accompanist.lyrics.model.ISyncedLine
import com.mocharealm.accompanist.lyrics.model.karaoke.KaraokeLine
import com.mocharealm.accompanist.service.PlaybackService
import com.mocharealm.accompanist.ui.composable.ModalScaffold
import com.mocharealm.accompanist.ui.composable.background.BackgroundVisualState
import com.mocharealm.accompanist.ui.composable.background.FlowingLightBackground
import com.mocharealm.accompanist.ui.composable.lyrics.KaraokeLyricsView
import com.mocharealm.accompanist.ui.composable.utils.modifier.deviceRotation
import com.mocharealm.accompanist.ui.composable.utils.rememberScreenCornerDataDp
import com.mocharealm.accompanist.ui.theme.AccompanistTheme
import com.mocharealm.accompanist.ui.theme.SFPro
import kotlinx.coroutines.android.awaitFrame
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    private val playerViewModel: MainViewModel by viewModels()

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
            val coroutineScope = rememberCoroutineScope()

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
                ),
                MusicItem(
                    "Golden Hour",
                    "TTML(AMLL)",
                    MediaItem.fromUri("asset:///golden-hour.m4a"),
                    "golden-hour.ttml"
                ),
                MusicItem(
                    "好久没下雨了",
                    "TTML CJK",
                    MediaItem.fromUri("asset:///havent-rain-for-so-long.mp3"),
                    "havent-rain-for-so-long.ttml"
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

            var pressedLine by remember { mutableStateOf<ISyncedLine?>(null) }

            var isModalOpened by remember { mutableStateOf(false) }
            val screenCornerData = rememberScreenCornerDataDp()
            AccompanistTheme {
                ModalScaffold(
                    isModalOpened,
                    modifier = Modifier.fillMaxSize(),
                    onDismissRequest = {
                        isModalOpened = false
                        pressedLine = null
                        playerViewModel.onPlayPause(true)
                    },
                    screenCornerDataDp = screenCornerData,
                    modalContent = {
                        Column(
                            Modifier
                                .systemBarsPadding()
                                .clickable(
                                    interactionSource = remember { MutableInteractionSource() },
                                    indication = null,
                                    onClick = {}
                                )) {
                            Row(
                                it
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                horizontalArrangement = Arrangement.SpaceEvenly
                            ) {
                                Text("Share")
                            }
                            pressedLine?.let { pressLine ->
                                uiState.lyrics?.let { lyrics ->
                                    var selectedLines by remember { mutableStateOf(listOf(pressLine as KaraokeLine)) }
                                    var step by remember { mutableIntStateOf(0) }
                                    val index = lyrics.lines.indexOf(pressLine)

                                    fun KaraokeLine.content(): String =
                                        this.syllables.joinToString("") { syllable ->
                                            syllable.content
                                        }

                                    when (step) {
                                        0 -> {
                                            val shareLazyState = rememberLazyListState()
                                            LaunchedEffect(pressLine) {
                                                coroutineScope.launch {
                                                    shareLazyState.animateScrollToItem(index, -200)
                                                }
                                            }
                                            LazyColumn(
                                                state = shareLazyState,
                                                modifier = Modifier.weight(1f),
                                                contentPadding = PaddingValues(16.dp),
                                                verticalArrangement = Arrangement.spacedBy(8.dp)
                                            ) {
                                                items(lyrics.lines) { line ->

                                                    Column(
                                                        Modifier
                                                            .clip(RoundedCornerShape(8.dp))
                                                            .fillMaxWidth()
                                                            .clickable {
                                                                if (selectedLines.contains(line as KaraokeLine)) {
                                                                    selectedLines =
                                                                        selectedLines - line
                                                                } else {
                                                                    selectedLines =
                                                                        selectedLines + line
                                                                }
                                                            }
                                                            .then(
                                                                if (selectedLines.contains(line as KaraokeLine))
                                                                    Modifier.background(
                                                                        MaterialTheme.colorScheme.primaryContainer
                                                                    )
                                                                else Modifier
                                                            )
                                                            .padding(8.dp)
                                                    ) {
                                                        Text(
                                                            line.content(),
                                                            style = TextStyle(
                                                                fontSize = 24.sp,
                                                                fontWeight = FontWeight.Bold,
                                                                fontFamily = SFPro,
                                                                textMotion = TextMotion.Animated
                                                            ),
                                                        )
                                                        line.translation?.let { translation ->
                                                            Text(translation, Modifier.alpha(0.6f))
                                                        }
                                                    }
                                                }
                                            }
                                            Row(
                                                it
                                                    .fillMaxWidth()
                                                    .padding(16.dp),
                                                horizontalArrangement = Arrangement.SpaceEvenly
                                            ) {
                                                Box(
                                                    Modifier
                                                        .weight(1f)
                                                        .clip(RoundedCornerShape(100))
                                                        .background(
                                                            MaterialTheme.colorScheme.primary
                                                        )
                                                        .clickable {
                                                            step = 1
                                                        }
                                                        .padding(16.dp)
                                                ) {
                                                    Text("Generate Share Card")
                                                }
                                            }
                                        }

                                        1 -> {
                                            BackHandler {
                                                step=0
                                            }
                                            Box(Modifier.fillMaxSize()) {
                                                Box(
                                                    Modifier
                                                        .align(Alignment.Center)
                                                        .fillMaxWidth(0.7f)
                                                        .deviceRotation()
                                                        .clip(RoundedCornerShape(16.dp))
                                                ) {
                                                    uiState.backgroundState.artwork?.let { bitmap ->
                                                        FlowingLightBackground(
                                                            state = BackgroundVisualState(
                                                                bitmap,
                                                                uiState.backgroundState.isBright
                                                            ),
                                                        )
                                                    }

                                                    LazyColumn(
                                                        contentPadding = PaddingValues(16.dp),
                                                        verticalArrangement = Arrangement.spacedBy(16.dp)
                                                    ) {
                                                        items(selectedLines) { line ->
                                                            Column {
                                                                Text(
                                                                    line.content(),
                                                                    style = TextStyle(
                                                                        fontSize = 24.sp,
                                                                        fontWeight = FontWeight.Bold,
                                                                        fontFamily = SFPro,
                                                                        textMotion = TextMotion.Animated
                                                                    ),
                                                                    color = Color.White
                                                                )
                                                                line.translation?.let { translation ->
                                                                    Text(
                                                                        translation,
                                                                        color = Color.White.copy(0.6f)
                                                                    )
                                                                }
                                                            }
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                ) {
                    uiState.backgroundState.artwork?.let { bitmap ->
                        FlowingLightBackground(
                            state = BackgroundVisualState(
                                bitmap, uiState.backgroundState.isBright
                            ),
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                    Column {
                        Row(
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .statusBarsPadding()
                                .padding(horizontal = 28.dp)
                                .padding(top = 28.dp)
                                .fillMaxWidth()
                        ) {
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                uiState.backgroundState.artwork?.let { bitmap ->
                                    Image(
                                        bitmap,
                                        null,
                                        Modifier
                                            .clip(RoundedCornerShape(6.dp))
                                            .border(
                                                1.dp,
                                                Color.White.copy(0.2f),
                                                RoundedCornerShape(6.dp)
                                            )
                                            .size(60.dp)
                                    )
                                }


                                Column {
                                    Text(
                                        selectedMusicItem?.label ?: "",
                                        fontWeight = FontWeight.Bold,
                                        fontFamily = SFPro,
                                        lineHeight = 1.em,
                                        color = Color.White
                                    )
                                    Text(
                                        selectedMusicItem?.testTarget ?: "",
                                        Modifier.alpha(0.6f),
                                        fontFamily = SFPro,
                                        lineHeight = 1.em,
                                        color = Color.White
                                    )
                                }

                            }
                            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                                Box(
                                    Modifier
                                        .clip(CircleShape)
                                        .background(Color.White.copy(0.2f))
                                        .clickable {
                                            playerViewModel.onOpenSongSelection()
                                        }
                                        .padding(4.dp)
                                        .size(20.dp)
                                ) {
//                                        Icon(
//                                            Icons.Rounded.Menu,
//                                            null,
//                                            tint = Color.White,
//                                            modifier = Modifier
//                                                .align(Alignment.Center)
//
//                                        )
                                }
                            }
                        }
                        uiState.lyrics?.let { finalLyrics ->
                            KaraokeLyricsView(
                                listState = listState,
                                lyrics = finalLyrics,
                                currentPosition = animatedPosition,
                                onLineClicked = { line ->
                                    playerViewModel.seekTo(line.start.toLong())
                                },
                                onLinePressed = { line ->
                                    pressedLine = line
                                    isModalOpened = true
                                    playerViewModel.onPlayPause(false)
                                },
                                modifier = Modifier
                                    .padding(horizontal = 12.dp)
                                    .graphicsLayer {
                                        blendMode = BlendMode.Plus
                                        compositingStrategy = CompositingStrategy.Offscreen
                                    },
                            )
                        }
                    }

                    if (uiState.showSelectionDialog) {
                        MusicItemSelectionDialog(
                            items = testItems,
                            onItemSelected = { item ->
                                playerViewModel.onSongSelected()
                                playerViewModel.setMediaItemAndPlay(item)
                                selectedMusicItem = item
                            },
                            onDismissRequest = {}
                        )
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
                            .padding(vertical = 12.dp)) {
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
        })
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
