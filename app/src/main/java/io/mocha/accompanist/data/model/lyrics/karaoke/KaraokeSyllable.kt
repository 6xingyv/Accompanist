package io.mocha.accompanist.data.model.lyrics.karaoke

import androidx.compose.runtime.Stable

@Stable
data class KaraokeSyllable(
    val content: String,
    val start: Int,
    val end: Int,
    val isMagnificent: Boolean = false
) {
    val duration = end - start

    init {
        require(end >= start)
    }

    fun progress(current: Int): Float {
        return when {
            current < start -> 0f
            current in start..end -> (current - start).toFloat() / duration
            current > end -> 1f
            else -> 0f
        }.coerceIn(0f, 1f)
    }
}
