package io.mocha.accompanist.data.model.playback

import androidx.compose.foundation.lazy.LazyListState
import com.mocharealm.accompanist.lyrics.model.SyncedLyrics

data class LyricsState(
    val current:()->Int,
    val duration: Int,
    val lyrics: SyncedLyrics,
    val lazyListState: LazyListState,
    var isScrollingProgrammatically:Boolean = false,
    val firstFocusedLine:()->Int={
        lyrics.getCurrentFirstHighlightLineIndexByTime(current())
    },
)
