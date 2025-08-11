package com.mocharealm.accompanist.lyrics.ui.composable.lyrics

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.text.TextLayoutResult
import com.mocharealm.accompanist.lyrics.core.model.karaoke.KaraokeSyllable


/**
 * 统一的数据结构，贯穿测量和布局全过程。
 * 初始创建时只填充测量相关字段，布局计算后，再用最终位置信息填充其他字段。
 */

data class SyllableLayout(
    // --- 测量阶段填充的字段 ---
    val syllable: KaraokeSyllable,
    val textLayoutResult: TextLayoutResult,
    val wordId: Int,
    val useAwesomeAnimation: Boolean,
    val width: Float = textLayoutResult.size.width.toFloat(),

    // --- 布局阶段填充的字段 ---
    val position: Offset = Offset.Zero,
    val wordPivot: Offset = Offset.Zero,
    val wordAnimInfo: WordAnimationInfo? = null,
    val charOffsetInWord: Int = 0
)