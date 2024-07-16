package io.mocha.accompanist.data.parser

import io.mocha.accompanist.data.model.lyrics.SyncedLyrics

interface LyricsParser {
    fun parse(lines:List<String>): SyncedLyrics
}