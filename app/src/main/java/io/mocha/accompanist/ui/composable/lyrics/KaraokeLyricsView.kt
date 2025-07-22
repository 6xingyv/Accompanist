package io.mocha.accompanist.ui.composable.lyrics

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.gestures.animateScrollBy
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.unit.dp
import com.mocharealm.accompanist.lyrics.model.ISyncedLine
import com.mocharealm.accompanist.lyrics.model.SyncedLyrics
import com.mocharealm.accompanist.lyrics.model.karaoke.KaraokeAlignment
import com.mocharealm.accompanist.lyrics.model.karaoke.KaraokeLine
import io.mocha.accompanist.data.model.playback.LyricsState

@Composable
fun KaraokeLyricsView(
    lyricsState: LyricsState,
    lyrics: SyncedLyrics,
    currentPosition: Long,
    onLineClicked: (ISyncedLine) -> Unit,
    modifier: Modifier = Modifier
) {
    val listState = lyricsState.lazyListState

    val currentTimeMs = currentPosition.toInt()

    val focusedLineIndex
           = lyrics.getCurrentFirstHighlightLineIndexByTime(currentTimeMs)

    LaunchedEffect(Unit) {
        listState.animateScrollToItem(focusedLineIndex)
    }

    LaunchedEffect(focusedLineIndex) {
        if (focusedLineIndex >= 0 &&
            focusedLineIndex < lyrics.lines.size &&
            !listState.isScrollInProgress) {

            try {
                println("自动滚动到行: $focusedLineIndex")
                listState.animateScrollBy(40f)
            } catch (e: Exception) {
                println("滚动异常: ${e.message}")
            }
        }
    }

    LazyColumn(
        state = listState,
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(vertical = 100.dp)
    ) {
        itemsIndexed(
            items = lyrics.lines,
            key = { index, line -> "${line.start}-${line.end}-$index" }
        ) { index, line ->
            if (line is KaraokeLine) {
                // 判断是否为当前焦点行（用于伴唱显示）
                val isCurrentFocusLine = remember(index, focusedLineIndex) {
                    index == focusedLineIndex ||
                    (line.isAccompaniment && kotlin.math.abs(index - focusedLineIndex) <= 1)
                }

                if (!line.isAccompaniment) {
                    KaraokeLineText(
                        line = line,
                        onLineClicked = onLineClicked,
                        currentTimeMs = currentTimeMs
                    )
                } else {
                    AnimatedVisibility(
                        visible = isCurrentFocusLine,
                        enter = scaleIn(
                            transformOrigin = TransformOrigin(
                                if (line.alignment == KaraokeAlignment.Start) 0f else 1f,
                                0f
                            ),
                            animationSpec = tween(300)
                        ) + slideInVertically(
                            animationSpec = tween(300)
                        ) + expandVertically(
                            animationSpec = tween(300)
                        ),
                        exit = scaleOut(
                            transformOrigin = TransformOrigin(
                                if (line.alignment == KaraokeAlignment.Start) 0f else 1f,
                                0f
                            ),
                            animationSpec = tween(300)
                        ) + slideOutVertically(
                            animationSpec = tween(300)
                        ) + shrinkVertically(
                            animationSpec = tween(300)
                        )
                    ) {
                        KaraokeLineText(
                            line = line,
                            onLineClicked = onLineClicked,
                            currentTimeMs = currentTimeMs
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