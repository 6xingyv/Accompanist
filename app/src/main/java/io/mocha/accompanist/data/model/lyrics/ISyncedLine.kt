package io.mocha.accompanist.data.model.lyrics

import androidx.compose.runtime.Stable

@Stable
interface ISyncedLine {
    val start:Int
    val end:Int
    val duration:Int
}