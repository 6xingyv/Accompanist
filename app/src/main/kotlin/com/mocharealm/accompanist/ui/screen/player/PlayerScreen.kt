package com.mocharealm.accompanist.ui.screen.player

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
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.mocharealm.accompanist.MusicItemSelectionDialog
import com.mocharealm.accompanist.lyrics.model.karaoke.KaraokeLine
import com.mocharealm.accompanist.ui.composable.ModalScaffold
import com.mocharealm.accompanist.ui.composable.background.BackgroundVisualState
import com.mocharealm.accompanist.ui.composable.background.FlowingLightBackground
import com.mocharealm.accompanist.ui.composable.lyrics.KaraokeLyricsView
import com.mocharealm.accompanist.ui.composable.utils.rememberScreenCornerDataDp
import com.mocharealm.accompanist.ui.screen.share.ShareContext
import com.mocharealm.accompanist.ui.screen.share.ShareScreen
import com.mocharealm.accompanist.ui.screen.share.ShareViewModel
import com.mocharealm.accompanist.ui.theme.AccompanistTheme
import com.mocharealm.accompanist.ui.theme.SFPro
import kotlinx.coroutines.android.awaitFrame


@Composable
fun PlayerScreen(
    playerViewModel: PlayerViewModel = viewModel(factory = PlayerViewModel.Factory(LocalContext.current)),
    shareViewModel: ShareViewModel = viewModel()
) {
    val uiState by playerViewModel.uiState.collectAsStateWithLifecycle()
    var animatedPosition by remember { mutableLongStateOf(0L) }
    val listState = rememberLazyListState()
    val screenCornerData = rememberScreenCornerDataDp()

    val latestPlaybackState by rememberUpdatedState(uiState.playbackState)
    LaunchedEffect(uiState.playbackState.isPlaying) {
        if (latestPlaybackState.isPlaying) {
            while (true) {
                val elapsed = System.currentTimeMillis() - latestPlaybackState.lastUpdateTime
                animatedPosition = (latestPlaybackState.position + elapsed).coerceAtMost(
                    latestPlaybackState.duration
                )
                awaitFrame()
            }
        }
    }

    AccompanistTheme {
        Box(modifier = Modifier.fillMaxSize()) {
            ModalScaffold(
                isModalOpen = uiState.isShareSheetVisible,
                modifier = Modifier.fillMaxSize(),
                onDismissRequest = {
                    playerViewModel.onShareDismissed()
                    shareViewModel.reset()
                },
                screenCornerDataDp = screenCornerData,
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
                            Column(Modifier.graphicsLayer{
                                blendMode = BlendMode.Plus
                            }) {
                                Text(
                                    uiState.currentMusicItem?.label ?: "Unknown",
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = SFPro,
                                    lineHeight = 1.em,
                                    color = Color.White
                                )
                                Text(
                                    uiState.currentMusicItem?.testTarget ?: "Unknown",
                                    Modifier.alpha(0.6f),
                                    fontFamily = SFPro,
                                    lineHeight = 1.em,
                                    color = Color.White
                                )
                            }
                        }
                        Row(Modifier.graphicsLayer{
                            blendMode = BlendMode.Plus
                        },horizontalArrangement = Arrangement.spacedBy(16.dp)) {
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
}