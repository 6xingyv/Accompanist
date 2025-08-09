package com.mocharealm.accompanist.sample.data.repository

import android.content.Context
import androidx.media3.common.MediaItem
import com.mocharealm.accompanist.lyrics.model.SyncedLyrics
import com.mocharealm.accompanist.lyrics.model.karaoke.KaraokeLine
import com.mocharealm.accompanist.lyrics.model.synced.SyncedLine
import com.mocharealm.accompanist.lyrics.parser.AutoParser
import com.mocharealm.accompanist.sample.domain.model.MusicItem
import com.mocharealm.accompanist.sample.domain.repository.MusicRepository

class MusicRepositoryImpl(private val context: Context): MusicRepository {
    private val autoParser = AutoParser.Builder().build()
    override suspend fun getMusicItems(): List<MusicItem> {
        return listOf(
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
    }

    override suspend fun getLyricsFor(item: MusicItem): SyncedLyrics? {
        return try {
            val lyricsData = context.assets.open(item.lyrics).bufferedReader().use { it.readLines() }
            val lyricsRaw = autoParser.parse(lyricsData)

            if (item.translation != null) {
                val translationData = context.assets.open(item.translation).bufferedReader().use { it.readLines() }
                val translationRaw = autoParser.parse(translationData)
                val translationMap = translationRaw.lines.associateBy { (it as SyncedLine).start }
                SyncedLyrics(
                    lyricsRaw.lines.map { line ->
                        val karaokeLine = line as KaraokeLine
                        val translationContent = (translationMap[karaokeLine.start] as SyncedLine?)?.content
                        karaokeLine.copy(translation = translationContent)
                    }
                )
            } else {
                lyricsRaw
            }
        } catch (_: Exception) {
            null
        }
    }
}