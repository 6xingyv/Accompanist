package io.mocha.accompanist.ui.composable.lyrics

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.mocharealm.accompanist.lyrics.model.karaoke.KaraokeSyllable
import io.mocha.accompanist.ui.theme.SFPro

fun textGradientBrush(
    progress: Float,
    activeColor: Color,
    inactiveColor: Color,
    fadeRange: Float
): Brush {
    val fade = fadeRange / 2
    return when (progress) {
        0f -> Brush.horizontalGradient(0f to inactiveColor, 1f to inactiveColor)
        1f -> Brush.horizontalGradient(0f to activeColor, 1f to activeColor)
        else -> Brush.horizontalGradient(
            0f to activeColor,
            (progress - fade).coerceAtLeast(0f) to activeColor,
            (progress + fade).coerceAtMost(1f) to inactiveColor,
            1f to inactiveColor
        )
    }
}

// 辅助函数：检查音节是否为空格（用于显示行尾空格检测）
fun KaraokeSyllable.isTrailingSpace(): Boolean {
    return content.isBlank() && content.isNotEmpty()
}

// 新增：检查音节是否有尾部空格
fun KaraokeSyllable.hasTrailingSpace(): Boolean {
    return content.isNotEmpty() && content != content.trimEnd()
}

// 新增：获取音节的尾部空格
fun KaraokeSyllable.getTrailingSpaces(): String {
    return if (content.isNotEmpty()) {
        content.substring(content.trimEnd().length)
    } else {
        ""
    }
}

// 新增：获取音节去除尾部空格后的内容
fun KaraokeSyllable.getTrimmedContent(): String {
    return content.trimEnd()
}

// 辅助函数：检查音节是否只包含空白字符
fun KaraokeSyllable.isWhitespace(): Boolean {
    return content.all { it.isWhitespace() }
}

// 辅助函数：获取显示内容
fun KaraokeSyllable.getDisplayContent(): String {
    return when {
        content.isEmpty() -> " "
        content.isBlank() -> content.ifEmpty { " " }
        else -> content
    }
}

fun DrawScope.drawSyllableText(
    syllable: KaraokeSyllable,
    textMeasurer: TextMeasurer,
    progress: Float,
    floatOffset: Float,
    position: Offset = Offset.Zero,
    style: TextStyle = TextStyle(fontSize = 32.sp, fontWeight = FontWeight.Bold, fontFamily = SFPro),
    activeColor: Color = Color.White,
    inactiveColor: Color = activeColor.copy(0.2f),
    fadeRange: Float = 0.4f
) {
    val result = textMeasurer.measure(syllable.content, style)
    val brush = textGradientBrush(progress, activeColor, inactiveColor, fadeRange)
    
    drawText(
        textLayoutResult = result,
        brush = brush,
        topLeft = position.copy(y = position.y + floatOffset),
        blendMode = BlendMode.Plus
    )
}

fun DrawScope.drawSyllableTextAwesome(
    syllable: KaraokeSyllable,
    textMeasurer: TextMeasurer,
    progress: Float,
    floatOffset: Float,
    position: Offset = Offset.Zero,
    style: TextStyle = TextStyle(fontSize = 32.sp, fontWeight = FontWeight.Bold, fontFamily = SFPro),
    activeColor: Color = Color.White,
    inactiveColor: Color = activeColor.copy(0.2f),
    fadeRange: Float = 0.4f
) {
    val brush = textGradientBrush(progress, activeColor, inactiveColor, fadeRange)
    var pivot = position.x
    
    syllable.content.forEach { char ->
        val charResult = textMeasurer.measure(char.toString(), style)
        drawText(
            textLayoutResult = charResult,
            brush = brush,
            topLeft = Offset(pivot, position.y + floatOffset),
            blendMode = BlendMode.Plus
        )
        pivot += charResult.size.width.toFloat()
    }
}


// 音节位置和尺寸信息
data class SyllableLayout(
    val syllable: KaraokeSyllable,
    val position: Offset,
    val size: androidx.compose.ui.geometry.Size,
    val floatOffset: Float
)

// 获取音节的布局信息（不绘制，只计算尺寸和位置）
fun getSyllableLayout(
    syllable: KaraokeSyllable,
    textMeasurer: TextMeasurer,
    position: Offset,
    floatOffset: Float,
    style: TextStyle
): SyllableLayout {
    val result = textMeasurer.measure(syllable.content, style)
    
    return SyllableLayout(
        syllable = syllable,
        position = position.copy(y = position.y + floatOffset),
        size = androidx.compose.ui.geometry.Size(
            result.size.width.toFloat(),
            result.size.height.toFloat()
        ),
        floatOffset = floatOffset
    )
}

// 使用蒙版方式绘制音节（作为整体渐变的一部分）
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
        blendMode = BlendMode.Plus
    )
}

data class SyllableAnimationData(
    val progress: Float,
    val floatOffset: Float,
    val syllable: KaraokeSyllable
)
