package com.mocharealm.accompanist.ui.composable.lyrics

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import com.mocharealm.accompanist.lyrics.model.karaoke.KaraokeSyllable


// 音节位置和尺寸信息
data class SyllableLayout(
    val syllable: KaraokeSyllable,
    val position: Offset,
    val size: androidx.compose.ui.geometry.Size,
    val progress: Float
)

fun getSyllableLayout(
    syllable: KaraokeSyllable,
    textMeasurer: TextMeasurer,
    position: Offset,
    progress: Float,
    style: TextStyle
): SyllableLayout {
    val result = textMeasurer.measure(syllable.content, style)

    // ✨ 将额外的浮动位移计算也放在这里
    val floatOffset = 8f * (1f - progress)

    return SyllableLayout(
        syllable = syllable,
        // ✨ 直接计算出最终的绘制位置
        position = position.copy(y = position.y + floatOffset),
        size = androidx.compose.ui.geometry.Size(
            result.size.width.toFloat(),
            result.size.height.toFloat()
        ),
        progress = progress
    )
}

fun DrawScope.drawSyllable(
    syllableLayout: SyllableLayout,
    textMeasurer: TextMeasurer,
    style: TextStyle,
    brush: Brush
) {
    val result = textMeasurer.measure(syllableLayout.syllable.content, style)

    drawText(
        textLayoutResult = result,
        brush = brush,
        topLeft = syllableLayout.position,
        blendMode = BlendMode.Softlight
    )
}

data class SyllableAnimationData(
    val progress: Float,
    val syllable: KaraokeSyllable
)
