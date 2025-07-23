package com.mocharealm.accompanist.ui.composable.lyrics

import android.annotation.SuppressLint
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

data class WrappedLine(
    val syllables: List<KaraokeSyllable>,
    val totalWidth: Float
)

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

private fun createLineGradientBrush(
    lineLayout: List<SyllableLayout>,
    currentTimeMs: Int,
): Brush {
    val activeColor = Color.Transparent
    val inactiveColor = Color.White.copy(alpha = 0.5f)
    val minFadeWidth = 8f

    if (lineLayout.isEmpty()) {
        return Brush.horizontalGradient(colors = listOf(inactiveColor, inactiveColor))
    }

    val totalWidth = lineLayout.last().let { it.position.x + it.size.width }

    if (totalWidth <= 0f) {
        val isFinished = currentTimeMs >= (lineLayout.lastOrNull()?.syllable?.end ?: 0)
        val color = if (isFinished) activeColor else inactiveColor
        return Brush.horizontalGradient(colors = listOf(color, color))
    }

    // Calculate the overall line progress from 0.0f to 1.0f.
    val lineProgress = run {
        val activeSyllable = lineLayout.find {
            currentTimeMs in it.syllable.start until it.syllable.end
        }

        val currentPixelPosition = when {
            // Case A: Inside an active syllable.
            activeSyllable != null -> {
                val syllableProgress = activeSyllable.syllable.progress(currentTimeMs)
                activeSyllable.position.x + activeSyllable.size.width * syllableProgress
            }
            // Case B: After the entire line has finished.
            currentTimeMs >= lineLayout.last().syllable.end -> {
                totalWidth
            }
            // Case C: In a pause between syllables or before the line starts.
            else -> {
                val lastFinished = lineLayout.lastOrNull { currentTimeMs >= it.syllable.end }
                lastFinished?.let { it.position.x + it.size.width } ?: 0f
            }
        }
        (currentPixelPosition / totalWidth).coerceIn(0f, 1f)
    }

    val fadeRange = run {
        val fadeWidthPx = maxOf(totalWidth * 0.05f, minFadeWidth)
        (fadeWidthPx / totalWidth).coerceAtMost(1f)
    }

    return when(lineProgress) {
        0f -> Brush.horizontalGradient(colors = listOf(inactiveColor, inactiveColor))
        1f -> Brush.horizontalGradient(colors = listOf(activeColor, activeColor))
        else -> Brush.horizontalGradient(
            colorStops = arrayOf(
                0.0f to activeColor,
                (lineProgress - fadeRange / 2).coerceAtLeast(0f) to activeColor,
                (lineProgress + fadeRange / 2).coerceAtMost(1f) to inactiveColor,
                1.0f to inactiveColor
            )
        )
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
        // Calculate the progress brush
        val progressBrush = createLineGradientBrush(rowLayouts, currentTimeMs)

        // Draw syllables
        rowLayouts.forEach { syllableLayout ->
            val progress = syllableLayout.syllable.progress(currentTimeMs)
            // TODO: Extract 4f to a constant
            val floatOffset = 4f * (1f - progress)
            val finalPosition =
                syllableLayout.position.copy(y = syllableLayout.position.y + floatOffset)

            val result = textMeasurer.measure(syllableLayout.syllable.content, style)
            drawText(
                textLayoutResult = result,
                brush = Brush.horizontalGradient(0f to color, 1f to color),
                topLeft = finalPosition,
                blendMode = BlendMode.Plus
            )
        }

        // Draw the mask gradient
        val width = rowLayouts.last().position.x + rowLayouts.last().size.width
        val height = rowLayouts.maxOf { it.size.height }
        drawRect(
            brush = progressBrush,
            topLeft = rowLayouts.first().position,
            // TODO: Extract 8f to a constant
            size = Size(width, height + 8f), // Expand height for float & glow animation
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

                Canvas(
                    // TODO: Extract 8 to a constant
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