package com.mocharealm.accompanist.ui.composable.lyrics

import androidx.compose.ui.geometry.Offset
import com.mocharealm.accompanist.lyrics.model.karaoke.KaraokeSyllable


// 音节位置和尺寸信息
data class SyllableLayout(
    val syllable: KaraokeSyllable,
    val position: Offset,
    val size: androidx.compose.ui.geometry.Size,
    val progress: Float
)