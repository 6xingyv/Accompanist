package com.mocharealm.accompanist.ui.composable.lyrics

import android.annotation.SuppressLint
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.LocalContentColor
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mocharealm.accompanist.lyrics.model.ISyncedLine
import com.mocharealm.accompanist.lyrics.model.karaoke.KaraokeAlignment
import com.mocharealm.accompanist.lyrics.model.karaoke.KaraokeLine
import com.mocharealm.accompanist.lyrics.model.karaoke.KaraokeSyllable
import com.mocharealm.accompanist.ui.theme.SFPro

// WrappedLine 定义保持不变
data class WrappedLine(
    val syllables: List<KaraokeSyllable>,
    val totalWidth: Float
)

// calculateWrappedLines 只依赖静态信息
private fun calculateWrappedLines(
    syllables: List<KaraokeSyllable>,
    availableWidthPx: Float,
    textMeasurer: TextMeasurer,
    isAccompaniment: Boolean
): List<WrappedLine> {
    val style = if (isAccompaniment) {
        TextStyle(fontSize = 16.sp, fontWeight = FontWeight.Bold, fontFamily = SFPro)
    } else {
        TextStyle(fontSize = 32.sp, fontWeight = FontWeight.Bold, fontFamily = SFPro)
    }

    val lines = mutableListOf<WrappedLine>()
    var currentLine = mutableListOf<KaraokeSyllable>()
    var currentLineWidth = 0f

    syllables.forEach { syllable ->
        val syllableWidth = textMeasurer.measure(syllable.content, style).size.width.toFloat()
        if (currentLineWidth + syllableWidth > availableWidthPx && currentLine.isNotEmpty()) {
            val trimmedDisplayLine = trimDisplayLineTrailingSpaces(currentLine, textMeasurer, style)
            if (trimmedDisplayLine.syllables.isNotEmpty()) {
                lines.add(trimmedDisplayLine)
            }
            currentLine.clear()
            currentLineWidth = 0f
        }
        currentLine.add(syllable)
        currentLineWidth += syllableWidth
    }

    if (currentLine.isNotEmpty()) {
        val trimmedDisplayLine = trimDisplayLineTrailingSpaces(currentLine, textMeasurer, style)
        if (trimmedDisplayLine.syllables.isNotEmpty()) {
            lines.add(trimmedDisplayLine)
        }
    }
    return lines
}

// 计算静态布局
private fun calculateStaticLineLayout(
    wrappedLines: List<WrappedLine>,
    textMeasurer: TextMeasurer,
    isAccompaniment: Boolean,
    lineAlignment: Alignment,
    canvasWidth: Float
): List<List<SyllableLayout>> {
    val style = if (isAccompaniment) {
        TextStyle(fontSize = 16.sp, fontWeight = FontWeight.Bold, fontFamily = SFPro)
    } else {
        TextStyle(fontSize = 32.sp, fontWeight = FontWeight.Bold, fontFamily = SFPro)
    }

    val lineHeight = textMeasurer.measure("M", style).size.height.toFloat()

    return wrappedLines.mapIndexed { lineIndex, wrappedLine ->
        val lineY = lineIndex * lineHeight
        val startX = when (lineAlignment) {
            Alignment.TopStart -> 0f
            Alignment.TopEnd -> canvasWidth - wrappedLine.totalWidth
            else -> (canvasWidth - wrappedLine.totalWidth) / 2f
        }
        var currentX = startX

        wrappedLine.syllables.map { syllable ->
            val result = textMeasurer.measure(syllable.content, style)
            val layout = SyllableLayout(
                syllable = syllable,
                position = Offset(currentX, lineY),
                size = Size(result.size.width.toFloat(), result.size.height.toFloat()),
                progress = 0f // progress 在这里是临时的，将在绘制时动态计算
            )
            currentX += layout.size.width
            layout
        }
    }
}


// 创建整行渐变笔刷
private fun createLineGradientBrush(
    lineLayout: List<SyllableLayout>,
    currentTimeMs: Int,
): Brush {
    val activeColor = Color.Transparent
    val inactiveColor = Color.White.copy(0.6f)
    if (lineLayout.isEmpty()) {
        return Brush.horizontalGradient(0f to inactiveColor, 1f to inactiveColor)
    }
    val totalWidth = lineLayout.last().position.x + lineLayout.last().size.width

    val currentSyllable = lineLayout.find {
        it.syllable.progress(currentTimeMs) > 0f && it.syllable.progress(currentTimeMs) < 1f
    }

    val lineProgress = currentSyllable?.let {
        (it.position.x + it.size.width * it.syllable.progress(currentTimeMs)) / totalWidth
    } ?: if (lineLayout.first().syllable.progress(currentTimeMs) == 1f) 1f else 0f

    val fadeRange = 0.1f
    val fadeHalf = fadeRange / 2f

    return when (lineProgress) {
        0f -> Brush.horizontalGradient(0f to inactiveColor, 1f to inactiveColor)
        1f -> Brush.horizontalGradient(0f to activeColor, 1f to activeColor)
        else -> {
            Brush.horizontalGradient(
                0f to activeColor,
                (lineProgress - fadeHalf).coerceAtLeast(0f) to activeColor,
                (lineProgress + fadeHalf).coerceAtMost(1f) to inactiveColor,
                1f to inactiveColor
            )
        }
    }
}


private fun DrawScope.drawLine(
    lineLayouts: List<List<SyllableLayout>>,
    textMeasurer: TextMeasurer,
    isAccompaniment: Boolean,
    currentTimeMs: Int,
    color: Color
) {
    val style = if (isAccompaniment) {
        TextStyle(fontSize = 16.sp, fontWeight = FontWeight.Bold, fontFamily = SFPro)
    } else {
        TextStyle(fontSize = 32.sp, fontWeight = FontWeight.Bold, fontFamily = SFPro)
    }

    lineLayouts.forEach { rowLayouts ->
        // 动态计算高亮渐变
        val progressBrush = createLineGradientBrush(rowLayouts, currentTimeMs)

        // 绘制底层基础文本
        rowLayouts.forEach { syllableLayout ->
            // 在绘制时动态计算每个音节的进度和浮动
            val progress = syllableLayout.syllable.progress(currentTimeMs)
            val floatOffset = 8f * (1f - progress)
            val finalPosition = syllableLayout.position.copy(y = syllableLayout.position.y + floatOffset)

            val result = textMeasurer.measure(syllableLayout.syllable.content, style)
            drawText(
                textLayoutResult = result,
                brush = Brush.horizontalGradient(0f to color, 1f to color),
                topLeft = finalPosition,
                blendMode = BlendMode.Softlight
            )
        }

        // 在上层叠加高亮效果
        val width = rowLayouts.last().position.x + rowLayouts.last().size.width
        val height = rowLayouts.maxOf { it.size.height }
        drawRect(
            brush = progressBrush,
            topLeft = rowLayouts.first().position,
            size = Size(width, height + 8f), // 增加高度以覆盖浮动范围
            blendMode = BlendMode.DstOut
        )
    }
}


@SuppressLint("UnusedBoxWithConstraintsScope")
@Stable
@Composable
fun KaraokeLineText(
    line: KaraokeLine,
    onLineClicked: (ISyncedLine) -> Unit,
    currentTimeMs: Int,
    modifier: Modifier = Modifier,
) {
    val isFocused = line.isFocused(currentTimeMs)
    val textMeasurer = rememberTextMeasurer()

    val animatedScale by animateFloatAsState(
        targetValue = if (isFocused) 1.05f else 1f,
        label = "scale"
    )

    val alphaAnimation by animateFloatAsState(
        targetValue = when {
            !line.isAccompaniment -> if (isFocused) 1f else 0.6f
            else -> if (isFocused) 0.6f else 0.3f
        },
        label = "alpha"
    )

    Box(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .clickable { onLineClicked(line) }
    ) {
        Column(
            modifier
                .align(if (line.alignment == KaraokeAlignment.End) Alignment.TopEnd else Alignment.TopStart)
                .padding(vertical = 8.dp, horizontal = 16.dp)
                .graphicsLayer {
                    scaleX = animatedScale
                    scaleY = animatedScale
                    alpha = alphaAnimation
                    transformOrigin = TransformOrigin(
                        if (line.alignment == KaraokeAlignment.Start) 0f else 1f,
                        1f
                    )
                    compositingStrategy = CompositingStrategy.Offscreen
                },
            verticalArrangement = Arrangement.spacedBy(2.dp),
            horizontalAlignment = if (line.alignment == KaraokeAlignment.Start) Alignment.Start else Alignment.End
        ) {
            BoxWithConstraints {
                val density = LocalDensity.current
                val availableWidthPx = with(density) { maxWidth.toPx() }

                // ✨ 核心修改点：所有布局计算的 remember key 不再包含 currentTimeMs
                val wrappedLines = remember(line.syllables, availableWidthPx, textMeasurer) {
                    calculateWrappedLines(
                        syllables = line.syllables,
                        availableWidthPx = availableWidthPx,
                        textMeasurer = textMeasurer,
                        isAccompaniment = line.isAccompaniment
                    )
                }

                val staticLineLayouts = remember(wrappedLines, availableWidthPx) {
                    calculateStaticLineLayout(
                        wrappedLines = wrappedLines,
                        textMeasurer = textMeasurer,
                        isAccompaniment = line.isAccompaniment,
                        lineAlignment = if (line.alignment == KaraokeAlignment.End) Alignment.TopEnd else Alignment.TopStart,
                        canvasWidth = availableWidthPx
                    )
                }

                val totalHeight = remember(wrappedLines, line.isAccompaniment) {
                    val style = if (line.isAccompaniment) {
                        TextStyle(fontSize = 16.sp, fontWeight = FontWeight.Bold, fontFamily = SFPro)
                    } else {
                        TextStyle(fontSize = 32.sp, fontWeight = FontWeight.Bold, fontFamily = SFPro)
                    }
                    val lineHeight = textMeasurer.measure("M", style).size.height
                    lineHeight * wrappedLines.size
                }

                val activeColor = LocalContentColor.current

                Canvas(
                    modifier = Modifier.size(maxWidth, (totalHeight + 8).toDp())
                ) {
                    // ✨ 动态的 currentTimeMs 只在绘制时传入
                    drawLine(
                        lineLayouts = staticLineLayouts,
                        textMeasurer = textMeasurer,
                        isAccompaniment = line.isAccompaniment,
                        currentTimeMs = currentTimeMs,
                        color = activeColor
                    )
                }
            }

            line.translation?.let { translation ->
                val result = remember(translation) {
                    textMeasurer.measure(translation)
                }
                val color = LocalContentColor.current.copy(0.6f)
                Canvas(
                    modifier = Modifier.size(result.size.toDpSize())
                ) {
                    drawText(result, color, blendMode = BlendMode.Plus)
                }
            }
        }
    }
}

private fun trimDisplayLineTrailingSpaces(
    displayLineSyllables: List<KaraokeSyllable>,
    textMeasurer: TextMeasurer,
    style: TextStyle
): WrappedLine {
    if (displayLineSyllables.isEmpty()) {
        return WrappedLine(emptyList(), 0f)
    }
    val processedSyllables = displayLineSyllables.toMutableList()
    val lastIndex = processedSyllables.size - 1
    if (lastIndex >= 0) {
        val lastSyllable = processedSyllables[lastIndex]
        val originalContent = lastSyllable.content
        val trimmedContent = originalContent.trimEnd()
        if (trimmedContent != originalContent && trimmedContent.isNotEmpty()) {
            val trimmedSyllable = lastSyllable.copy(content = trimmedContent)
            processedSyllables[lastIndex] = trimmedSyllable
        } else if (trimmedContent.isEmpty()) {
            processedSyllables.removeAt(lastIndex)
        }
    }
    val totalWidth = processedSyllables.sumOf { syllable ->
        textMeasurer.measure(syllable.content, style).size.width.toDouble()
    }.toFloat()
    return WrappedLine(processedSyllables, totalWidth)
}

@Composable
private fun Int.toDp(): androidx.compose.ui.unit.Dp {
    val density = LocalDensity.current
    return with(density) { this@toDp.toDp() }
}

@Composable
private fun IntSize.toDpSize(): DpSize {
    val density = LocalDensity.current
    return with(density) {
        DpSize(width.toDp(), height.toDp())
    }
}