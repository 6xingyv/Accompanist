package io.mocha.accompanist.data.model.playback

import io.mocha.accompanist.data.model.lyrics.SyncedLyrics

data class PlaybackState(
    val current:Int,
    val duration: Int,
    val lyrics: SyncedLyrics,
)
