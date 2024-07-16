package io.mocha.accompanist.data.parser

import io.mocha.accompanist.data.model.lyrics.SyncedLyrics
import io.mocha.accompanist.data.model.lyrics.synced.SyncedLine

object LrcParser:LyricsParser {
    override fun parse(lines: List<String>): SyncedLyrics {
        val data = lines.map { line->
            parseLine(line)
        }
        return SyncedLyrics(lines = data)
    }

    private fun parseLine(line: String):SyncedLine {
        TODO()
    }
}