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
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.unit.dp
import io.mocha.accompanist.data.model.lyrics.ISyncedLine
import io.mocha.accompanist.data.model.lyrics.SyncedLyrics
import io.mocha.accompanist.data.model.lyrics.karaoke.KaraokeLine
import io.mocha.accompanist.data.model.playback.LyricsState

@Composable
fun KaraokeLyricsView(
    lyricsState: LyricsState,
    lyrics: SyncedLyrics,
    onLineClicked: (ISyncedLine) -> Unit,
    modifier: Modifier = Modifier
) {
//    var lastIndex by remember { mutableIntStateOf(lyricsState.firstFocusedLine()) }
//
//    LaunchedEffect(lyricsState.firstFocusedLine) {
//        val scrollToItem = lyricsState.lazyListState.layoutInfo.visibleItemsInfo.find {
//            it.index == lyricsState.firstFocusedLine()
//        }
//
//        val lastItem = lyricsState.lazyListState.layoutInfo.visibleItemsInfo.find {
//            it.index == lastIndex
//        }
//
//        lyricsState.isScrollingProgrammatically = true
//
//        try {
//            if (scrollToItem != null && lastItem != null && scrollToItem.offset >= 0) {
//                val diff = (scrollToItem.offset - lastItem.offset).toFloat()
//                lyricsState.lazyListState.animateScrollBy(diff, tween(300))
//            } else {
//                lyricsState.lazyListState.animateScrollToItem(lyricsState.firstFocusedLine())
//            }
//        } finally {
//            lyricsState.isScrollingProgrammatically = false
//        }
//
//        lastIndex = lyricsState.firstFocusedLine()
//    }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        state = lyricsState.lazyListState,
        verticalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        itemsIndexed(lyrics.lines, key = { index, line -> line.hashCode() }) { index, line ->
            if (line is KaraokeLine) {
                if (!line.isAccompaniment) {
                    KaraokeLineText(
                        lyricsState = lyricsState,
                        line = line,
                        onLineClicked = onLineClicked,
                        currentLineIndex = index
                    )
                } else {
                    AnimatedVisibility(
                        visible = line.isFocused(lyricsState.current()),
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
                            lyricsState = lyricsState,
                            line = line,
                            onLineClicked = onLineClicked,
                            currentLineIndex = index
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