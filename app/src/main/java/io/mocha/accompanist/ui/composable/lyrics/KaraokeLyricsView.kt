package io.mocha.accompanist.ui.composable.lyrics

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import io.mocha.accompanist.data.model.lyrics.ISyncedLine
import io.mocha.accompanist.data.model.lyrics.SyncedLyrics
import io.mocha.accompanist.data.model.lyrics.karaoke.KaraokeLine
import io.mocha.accompanist.data.model.playback.PlaybackState


@Composable
fun KaraokeLyricsView(
    playbackState: PlaybackState,
    lyrics: SyncedLyrics,
    onLineClicked: (ISyncedLine) -> Unit,
    modifier: Modifier = Modifier
) {
    val state = rememberLazyListState()
    val top = WindowInsets.statusBars.getTop(LocalDensity.current) * 4
    LaunchedEffect(playbackState.current) {
        val index = lyrics.getCurrentFirstHighlightLineIndexByTime(playbackState.current)
        val line = lyrics.lines[index]
        if (line is KaraokeLine) {
            state.animateScrollToItem(if (line.isAccompaniment) index-1 else index,-top)
        } else {
            state.animateScrollToItem(
                index,
                -top
            )
        }
    }
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        state = state,
        verticalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        items(lyrics.lines, { line ->
            "(${line.start},${line.duration})"
        }) { line ->
            if (line is KaraokeLine) {
                if (!line.isAccompaniment) {
                    KaraokeLineText(
                        playbackState = playbackState,
                        line = line,
                        onLineClicked = onLineClicked
                    )
                } else {
                    AnimatedVisibility(
                        visible = line.isFocused(playbackState.current),
                        enter = scaleIn(
                            transformOrigin = TransformOrigin(
                                if (line.alignment == Alignment.TopStart) 0f else 1f,
                                0f
                            )
                        ) + slideInVertically()+ expandVertically(),
                        exit = scaleOut(
                            transformOrigin = TransformOrigin(
                                if (line.alignment == Alignment.TopStart) 0f else 1f,
                                0f
                            )
                        ) + slideOutVertically()+ shrinkVertically()
                    ) {
                        KaraokeLineText(
                            playbackState = playbackState,
                            line = line,
                            onLineClicked = onLineClicked
                        )
                    }
                }
            }
        }
        item("BottomSpacing") {
            Spacer(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(LocalConfiguration.current.screenHeightDp.dp)
            )
        }
    }
}