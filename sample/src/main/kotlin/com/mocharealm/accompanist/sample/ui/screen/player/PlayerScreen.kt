package com.mocharealm.accompanist.sample.ui.screen.player

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import com.mocharealm.accompanist.lyrics.core.model.karaoke.KaraokeLine
import com.mocharealm.accompanist.lyrics.ui.composable.background.BackgroundVisualState
import com.mocharealm.accompanist.lyrics.ui.composable.background.FlowingLightBackground
import com.mocharealm.accompanist.lyrics.ui.composable.lyrics.KaraokeLyricsView
import com.mocharealm.accompanist.lyrics.ui.theme.SFPro
import com.mocharealm.accompanist.sample.domain.model.MusicItem
import com.mocharealm.accompanist.sample.ui.composable.ModalScaffold
import com.mocharealm.accompanist.sample.ui.screen.share.ShareContext
import com.mocharealm.accompanist.sample.ui.screen.share.ShareScreen
import com.mocharealm.accompanist.sample.ui.screen.share.ShareViewModel
import kotlinx.coroutines.android.awaitFrame
import org.koin.androidx.compose.koinViewModel


@Composable
fun PlayerScreen(
    playerViewModel: PlayerViewModel = koinViewModel(),
    shareViewModel: ShareViewModel = koinViewModel(),
) {
    val listState = rememberLazyListState()
    var animatedPosition by remember { mutableLongStateOf(0L) }

    val uiState by playerViewModel.uiState.collectAsState()
    val latestPlaybackState by rememberUpdatedState(uiState.playbackState)

    LaunchedEffect(latestPlaybackState.isPlaying) {
        if (latestPlaybackState.isPlaying) {
            while (true) {
                val elapsed = System.currentTimeMillis() - latestPlaybackState.lastUpdateTime
                animatedPosition = (latestPlaybackState.position + elapsed).coerceAtMost(
                    latestPlaybackState.duration
                )
                awaitFrame()
            }
        } else {
            animatedPosition = latestPlaybackState.position
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        ModalScaffold(
            isModalOpen = uiState.isShareSheetVisible,
            modifier = Modifier.fillMaxSize(),
            onDismissRequest = {
                playerViewModel.onShareDismissed()
                shareViewModel.reset()
            },
            modalContent = {
                ShareScreen(it, shareViewModel = shareViewModel)
            }
        ) {
            uiState.backgroundState.bitmap?.let { bitmap ->
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
                        uiState.backgroundState.bitmap?.let { bitmap ->
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
                        Column(Modifier
                            .graphicsLayer {
                                blendMode = BlendMode.Plus
                            }) {
                            Text(
                                uiState.currentMusicItem?.label ?: "Unknown Title",
                                fontWeight = FontWeight.Bold,
                                fontFamily = SFPro,
                                lineHeight = 1.em,
                                color = Color.White
                            )
                            Text(
                                uiState.currentMusicItem?.testTarget?.split(" [")[0] ?: "Unknown",
                                Modifier.alpha(0.6f),
                                fontFamily = SFPro,
                                lineHeight = 1.em,
                                color = Color.White
                            )
                        }
                    }
                    Row(Modifier
                        .graphicsLayer {
                            blendMode = BlendMode.Plus
                        }, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                        Box(
                            Modifier
                                .clip(CircleShape)
                                .background(Color.White.copy(0.2f))
                                .clickable { playerViewModel.onOpenSongSelection() }
                                .padding(4.dp)
                                .size(20.dp)
                        )
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
                            playerViewModel.onShareRequested()
                            val context = ShareContext(
                                lyrics = finalLyrics,
                                initialLine = line as KaraokeLine,
                                backgroundState = BackgroundVisualState(
                                    bitmap = uiState.backgroundState.bitmap,
                                    isBright = uiState.backgroundState.isBright
                                )
                            )
                            shareViewModel.prepareForSharing(context)
                            playerViewModel.onShareRequested()
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
                    items = uiState.availableSongs,
                    onItemSelected = { item ->
                        playerViewModel.onSongSelected(item)
                    },
                    onDismissRequest = { /* Optionally handle dismiss */ }
                )
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