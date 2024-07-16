package io.mocha.accompanist.data.model.lyrics.synced

import io.mocha.accompanist.data.model.lyrics.ISyncedLine

data class SyncedLine(
    val content: String,
    val translation: String?,
    override val start: Int,
    override val end: Int,
) : ISyncedLine {
    override val duration = end - start
}
