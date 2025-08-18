package com.mocharealm.accompanist.lyrics.ui.composable.lyrics

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.text.TextLayoutResult
import com.mocharealm.accompanist.lyrics.core.model.karaoke.KaraokeSyllable


/**
 * Unified data structure that spans the entire measurement and layout process.
 * Initially created with only measurement-related fields populated. After layout calculation,
 * other fields are filled with final position information.
 */
data class SyllableLayout(
    // --- Fields populated during measurement phase ---
    val syllable: KaraokeSyllable,
    val textLayoutResult: TextLayoutResult,
    val wordId: Int,
    val useAwesomeAnimation: Boolean,
    val width: Float = textLayoutResult.size.width.toFloat(),

    // --- Fields populated during layout phase ---
    val position: Offset = Offset.Zero,
    val wordPivot: Offset = Offset.Zero,
    val wordAnimInfo: WordAnimationInfo? = null,
    val charOffsetInWord: Int = 0
)