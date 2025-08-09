package com.mocharealm.accompanist.sample.domain.repository

import com.mocharealm.accompanist.sample.domain.model.MusicItem
import com.mocharealm.accompanist.lyrics.model.SyncedLyrics

interface MusicRepository {
    suspend fun getMusicItems(): List<MusicItem>
    suspend fun getLyricsFor(item: MusicItem): SyncedLyrics?
}