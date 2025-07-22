package io.mocha.accompanist.ui.composable.lyrics

import android.annotation.SuppressLint
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.LocalContentColor
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.BlurredEdgeTreatment
import androidx.compose.ui.draw.blur
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
import io.mocha.accompanist.ui.theme.SFPro

@Composable
private fun IntSize.toDpSize(): DpSize {
    val density = LocalDensity.current
    return with(density) {
        DpSize(width.toDp(), height.toDp())
    }
}


// 计算整行的布局信息
private fun calculateLineLayout(
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

        // 计算行对齐
        val startX = when (lineAlignment) {
            Alignment.TopStart -> 0f
            Alignment.TopEnd -> canvasWidth - wrappedLine.totalWidth
            else -> (canvasWidth - wrappedLine.totalWidth) / 2f
        }

        var currentX = startX

        wrappedLine.syllables.map { data ->
            val syllableLayout = getSyllableLayout(
                syllable = data.syllable,
                textMeasurer = textMeasurer,
                position = Offset(currentX, lineY),
                floatOffset = data.floatOffset,
                style = style
            )

            currentX += syllableLayout.size.width
            syllableLayout
        }
    }
}

// 创建整行渐变笔刷
private fun createLineGradientBrush(
    lineLayout: List<SyllableLayout>,
    currentPosition: Int,
): Brush {
    val activeColor = Color.Transparent
    val inactiveColor = Color.White.copy(0.6f)
    if (lineLayout.isEmpty()) {
        return Brush.horizontalGradient(0f to inactiveColor, 1f to inactiveColor)
    }
    val totalWidth = lineLayout.last().position.x + lineLayout.last().size.width

    val currentSyllable = lineLayout.find {
        it.syllable.progress(currentPosition) < 1f && it.syllable.progress(currentPosition) > 0f
    }

    val lineProgress = currentSyllable?.let {
        (currentSyllable.position.x + currentSyllable.size.width * currentSyllable.syllable.progress(
            currentPosition
        )) / totalWidth
    } ?: if (lineLayout.first().syllable.progress(currentPosition) == 1f) 1f else 0f

    val fadeRange = 0.1f
    val fadeHalf = fadeRange / 2f

    return when (lineProgress) {
        0f -> Brush.horizontalGradient(
            0f to inactiveColor,
            1f to inactiveColor
        )

        1f -> Brush.horizontalGradient(
            0f to activeColor,
            1f to activeColor
        )

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

@SuppressLint("UnusedBoxWithConstraintsScope")
@OptIn(ExperimentalLayoutApi::class)
@Stable
@Composable
fun KaraokeLineText(
    line: KaraokeLine,
    onLineClicked: (ISyncedLine) -> Unit,
    currentTimeMs: Int,
    modifier: Modifier = Modifier,
) {// 稳定状态计算，减少重组
    val isFocused by remember(currentTimeMs, line.start, line.end) {
        derivedStateOf { line.isFocused(currentTimeMs) }
    }

    val textMeasurer = rememberTextMeasurer()

    // 优化动画数据计算，使用稳定的状态
    val syllableAnimations by remember(line.syllables, currentTimeMs) {
        derivedStateOf {
            line.syllables.map { syllable ->
                SyllableAnimationData(
                    progress = syllable.progress(currentTimeMs),
                    floatOffset = 0f,
                    syllable = syllable
                )
            }
        }
    }

    val animatedScale by animateFloatAsState(
        targetValue = if (isFocused) 1.05f else 0.95f, // 增强缩放效果
        animationSpec = tween(300), // 恢复动画时间
        label = "scale"
    )

    val alphaAnimation by animateFloatAsState(
        targetValue = when {
            !line.isAccompaniment -> if (isFocused) 1f else 0.6f
            else -> if (isFocused) 0.6f else 0.3f
        },
        animationSpec = tween(300), // 恢复动画时间
        label = "alpha"
    )
    val blurAnimation by animateDpAsState(
        targetValue = if (isFocused) 0.dp else 2.dp, // 增加模糊强度使效果更明显
        animationSpec = tween(300), // 恢复动画时间
        label = "blur"
    )

    Box(Modifier.fillMaxWidth()) {
        Column(
            modifier
                .align(if (line.alignment == KaraokeAlignment.End) Alignment.TopEnd else Alignment.TopStart)
                .clip(RoundedCornerShape(8.dp))
                .clickable { onLineClicked(line) }
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
                }
                .blur(blurAnimation, BlurredEdgeTreatment.Unbounded),
            verticalArrangement = Arrangement.spacedBy(2.dp),
            horizontalAlignment = if (line.alignment == KaraokeAlignment.Start) Alignment.Start else Alignment.End
        ) {
            BoxWithConstraints {
                val density = LocalDensity.current
                val availableWidthPx =
                    with(density) { maxWidth.toPx() }                // 优化绘制性能，使用缓存的计算结果
                val wrappedLines = remember(syllableAnimations, availableWidthPx, textMeasurer) {
                    calculateWrappedLines(
                        syllables = syllableAnimations,
                        availableWidthPx = availableWidthPx,
                        textMeasurer = textMeasurer,
                        isAccompaniment = line.isAccompaniment
                    )
                }

                val lineLayouts = remember(wrappedLines, availableWidthPx) {
                    calculateLineLayout(
                        wrappedLines = wrappedLines,
                        textMeasurer = textMeasurer,
                        isAccompaniment = line.isAccompaniment,
                        lineAlignment = if (line.alignment == KaraokeAlignment.End) Alignment.TopEnd else Alignment.TopStart,
                        canvasWidth = availableWidthPx
                    )
                }

                val totalHeight = remember(wrappedLines, line.isAccompaniment) {
                    val style = if (line.isAccompaniment) {
                        TextStyle(
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = SFPro
                        )
                    } else {
                        TextStyle(
                            fontSize = 32.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = SFPro
                        )
                    }
                    val lineHeight = textMeasurer.measure("M", style).size.height
                    lineHeight * wrappedLines.size
                }

                val activeColor = LocalContentColor.current
                val inactiveColor = remember(activeColor) { activeColor.copy(0.2f) }

                Canvas(
                    modifier = Modifier.size(maxWidth, totalHeight.toDp())
                ) {
                    drawLine(
                        lineLayouts = lineLayouts,
                        textMeasurer = textMeasurer,
                        isAccompaniment = line.isAccompaniment,
                        currentPosition = currentTimeMs,
                        activeColor = activeColor,
                        inactiveColor = inactiveColor
                    )
                }
            }

            // Translation 保持不变
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

data class WrappedLine(
    val syllables: List<SyllableAnimationData>,
    val totalWidth: Float
)

private fun calculateWrappedLines(
    syllables: List<SyllableAnimationData>,
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
    var currentLine = mutableListOf<SyllableAnimationData>()
    var currentLineWidth = 0f

    syllables.forEach { syllable ->
        val syllableWidth = textMeasurer.measure(
            syllable.syllable.content,
            style
        ).size.width.toFloat()

        // 检查是否需要换行（这里是显示行的换行）
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

    // 处理最后一个显示行
    if (currentLine.isNotEmpty()) {
        val trimmedDisplayLine = trimDisplayLineTrailingSpaces(currentLine, textMeasurer, style)
        if (trimmedDisplayLine.syllables.isNotEmpty()) {
            lines.add(trimmedDisplayLine)
        }
    }

    return lines
}

private fun trimDisplayLineTrailingSpaces(
    displayLineSyllables: List<SyllableAnimationData>,
    textMeasurer: TextMeasurer,
    style: TextStyle
): WrappedLine {
    if (displayLineSyllables.isEmpty()) {
        return WrappedLine(emptyList(), 0f)
    }

    val processedSyllables = displayLineSyllables.toMutableList()

    // 处理最后一个音节，移除其尾部空格
    val lastIndex = processedSyllables.size - 1
    if (lastIndex >= 0) {
        val lastSyllable = processedSyllables[lastIndex]
        val originalContent = lastSyllable.syllable.content

        // 移除尾部空格
        val trimmedContent = originalContent.trimEnd()

        if (trimmedContent != originalContent && trimmedContent.isNotEmpty()) {
            // 创建新的音节，移除尾部空格
            val trimmedSyllable = createTrimmedSyllable(lastSyllable.syllable, trimmedContent)
            processedSyllables[lastIndex] = SyllableAnimationData(
                progress = lastSyllable.progress,
                floatOffset = lastSyllable.floatOffset,
                syllable = trimmedSyllable
            )
        } else if (trimmedContent.isEmpty()) {
            // 如果音节只有空格，直接移除
            processedSyllables.removeAt(lastIndex)
        }
    }

    // 重新计算显示行的总宽度
    val totalWidth = processedSyllables.sumOf { syllable ->
        textMeasurer.measure(
            syllable.syllable.content,
            style
        ).size.width.toDouble()
    }.toFloat()

    return WrappedLine(processedSyllables, totalWidth)
}

// 创建去除尾部空格的新音节
private fun createTrimmedSyllable(
    originalSyllable: KaraokeSyllable,
    trimmedContent: String
): KaraokeSyllable = originalSyllable.copy(content = trimmedContent)


@Composable
private fun Int.toDp(): androidx.compose.ui.unit.Dp {
    val density = LocalDensity.current
    return with(density) { this@toDp.toDp() }
}

private fun DrawScope.drawLine(
    lineLayouts: List<List<SyllableLayout>>,
    textMeasurer: TextMeasurer,
    isAccompaniment: Boolean,
    currentPosition: Int,
    activeColor: Color,
    inactiveColor: Color
) {
    val style = if (isAccompaniment) {
        TextStyle(fontSize = 16.sp, fontWeight = FontWeight.Bold, fontFamily = SFPro)
    } else {
        TextStyle(fontSize = 32.sp, fontWeight = FontWeight.Bold, fontFamily = SFPro)
    }    // 优化渐变绘制，减少不必要的计算
    lineLayouts.forEach { rowLayouts ->
        rowLayouts.forEach { syllableLayout ->
//            val progress = syllableLayout.syllable.progress(currentPosition)
//            val gradientBrush = when {
//                progress <= 0f -> Brush.horizontalGradient(0f to inactiveColor, 1f to inactiveColor)
//                progress >= 1f -> Brush.horizontalGradient(0f to activeColor, 1f to activeColor)
//                else -> Brush.horizontalGradient(
//                    0f to activeColor,
//                    progress.coerceAtLeast(0.1f) - 0.05f to activeColor,
//                    progress.coerceAtMost(0.9f) + 0.05f to inactiveColor,
//                    1f to inactiveColor
//                )
//            }
            drawSyllable(
                syllableLayout = syllableLayout,
                textMeasurer = textMeasurer,
                style = style,
                brush = Brush.horizontalGradient(0f to activeColor, 1f to activeColor)
            )
        }
        val width = rowLayouts.last().position.x + rowLayouts.last().size.width
        val height = rowLayouts.maxOf { it.size.height }
        drawRect(
            createLineGradientBrush(rowLayouts, currentPosition),
            topLeft = rowLayouts.first().position,
            size = Size(width,height),
            blendMode = BlendMode.DstOut
        )
    }
}
