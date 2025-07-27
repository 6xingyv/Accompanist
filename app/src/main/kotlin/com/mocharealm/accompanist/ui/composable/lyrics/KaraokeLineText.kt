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
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextMotion
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mocharealm.accompanist.lyrics.model.ISyncedLine
import com.mocharealm.accompanist.lyrics.model.karaoke.KaraokeAlignment
import com.mocharealm.accompanist.lyrics.model.karaoke.KaraokeLine
import com.mocharealm.accompanist.lyrics.model.karaoke.KaraokeSyllable
import com.mocharealm.accompanist.ui.composable.easing.Bounce
import com.mocharealm.accompanist.ui.composable.easing.DipAndRise
import com.mocharealm.accompanist.ui.composable.easing.EasingOutCubic
import com.mocharealm.accompanist.ui.composable.easing.Swell
import com.mocharealm.accompanist.ui.theme.SFPro

data class WrappedLine(
    val syllables: List<KaraokeSyllable>,
    val totalWidth: Float
)

private fun calculateWrappedLines(
    syllables: List<KaraokeSyllable>,
    availableWidthPx: Float,
    textMeasurer: TextMeasurer,
    style: TextStyle
): List<WrappedLine> {

    val lines = mutableListOf<WrappedLine>()
    val currentLine = mutableListOf<KaraokeSyllable>()
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
    lineAlignment: Alignment,
    canvasWidth: Float,
    style: TextStyle
): List<List<SyllableLayout>> {
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
                textLayoutResult = result
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
    val activeColor = Color.White.copy(alpha = 0f)
    val inactiveColor = Color.White.copy(alpha = 0.6f)
    val minFadeWidth = 30f

    if (lineLayout.isEmpty()) {
        return Brush.horizontalGradient(colors = listOf(inactiveColor, inactiveColor))
    }

    val totalWidth = lineLayout.last().let { it.position.x + it.size.width }

    val firstSyllableStart = lineLayout.first().syllable.start
    val lastSyllableEnd = lineLayout.last().syllable.end
    val lineDuration = (lastSyllableEnd - firstSyllableStart).toFloat()

    if (totalWidth <= 0f || lineDuration <= 0f) {
        val isFinished = currentTimeMs >= lastSyllableEnd
        val color = if (isFinished) activeColor else inactiveColor
        return Brush.horizontalGradient(colors = listOf(color, color))
    }

    // --- Time Remapping ---
    // Define the three phases based on the total line duration
    val fadeInEndTime = firstSyllableStart + lineDuration * 0.15f
    val fadeOutStartTime = firstSyllableStart + lineDuration * 0.85f

    val baseFadeWidthPx = maxOf(totalWidth * 0.2f, minFadeWidth)
    val baseFadeRange = (baseFadeWidthPx / totalWidth).coerceAtMost(1f)

    var lineProgress = 0f
    var fadeRange = 0f

    when {
        // Phase 1: Fade-in
        currentTimeMs < fadeInEndTime -> {
            val phaseProgress = (currentTimeMs - firstSyllableStart) / (lineDuration * 0.15f)
            fadeRange = baseFadeRange * phaseProgress.coerceIn(0f, 1f)
            lineProgress = 0f
        }
        // Phase 3: Fade-out
        currentTimeMs > fadeOutStartTime -> {
            val phaseProgress = (currentTimeMs - fadeOutStartTime) / (lineDuration * 0.15f)
            fadeRange = baseFadeRange * (1f - phaseProgress.coerceIn(0f, 1f))
            lineProgress = 1f
        }
        // Phase 2: Main translation
        else -> {
            fadeRange = baseFadeRange
            // Remap the time in this phase to a 0-1 progress
            val phaseDuration = fadeOutStartTime - fadeInEndTime
            val timeInPhase = currentTimeMs - fadeInEndTime
            lineProgress = (timeInPhase / phaseDuration).coerceIn(0f, 1f)
        }
    }

    return when {
        // Line is completely finished
        currentTimeMs >= lastSyllableEnd -> Brush.horizontalGradient(colors = listOf(activeColor, activeColor))
        // Line has not started yet
        currentTimeMs < firstSyllableStart -> Brush.horizontalGradient(colors = listOf(inactiveColor, inactiveColor))
        // During the animation
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
    currentTimeMs: Int,
    color: Color,
    textMeasurer: TextMeasurer
) {
    lineLayouts.forEach { rowLayouts ->
        // Calculate the progress brush
        val progressBrush = createLineGradientBrush(rowLayouts, currentTimeMs)

        // Draw syllables
        rowLayouts.forEach { syllableLayout ->
            val syllable = syllableLayout.syllable
            val progress = syllable.progress(currentTimeMs)



            val perCharDuration = if (syllable.content.isNotEmpty()) {
                syllable.duration.toFloat() / syllable.content.length
            } else {
                0f
            }

            // Threshold for switching between animation styles.
            // If characters are very fast (e.g., in a rap), animate the whole syllable at once.
            val fastCharAnimationThresholdMs = 200f

            if (perCharDuration > fastCharAnimationThresholdMs && syllable.duration >= 1000) {

                val textStyle = syllableLayout.textLayoutResult.layoutInput.style
                val syllableBottomCenter = Offset(
                    x = syllableLayout.position.x + syllableLayout.size.width / 2f,
                    y = syllableLayout.position.y + syllableLayout.size.height
                )
                val awesomeDuration = syllable.duration * 0.8f
                syllable.content.forEachIndexed { index, char ->
                    val numChars = syllable.content.length
                    val earliestStartTime = syllable.start
                    val latestStartTime = syllable.end - awesomeDuration

                    val charRatio = if (numChars > 1) index.toFloat() / (numChars - 1) else 0.5f
                    val awesomeStartTime =
                        (earliestStartTime + (latestStartTime - earliestStartTime) * charRatio).toLong()

                    val awesomeProgress =
                        ((currentTimeMs - awesomeStartTime).toFloat() / awesomeDuration).coerceIn(
                            0f,
                            1f
                        )
                    val floatOffset =
                        6f * DipAndRise.transform(1.0f - awesomeProgress)
                    val scale = 1f + Swell.transform(awesomeProgress)

                    val yPos = syllableLayout.position.y + floatOffset
                    val xPos = syllableLayout.position.x + syllableLayout.textLayoutResult.getHorizontalPosition(
                        offset = index,
                        usePrimaryDirection = true
                    )

                    val blurRadius = 10f * Bounce.transform(awesomeProgress)
                    val shadow = Shadow(
                        color = color,
                        offset = Offset(0f, 0f),
                        blurRadius = blurRadius
                    )

                    val charLayoutResult = textMeasurer.measure(char.toString(), style = textStyle)

                    withTransform({
//                        // 围绕音节的底部中心进行变换
//                        // 1. 移动到变换原点
//                        translate(left = syllableBottomCenter.x, top = syllableBottomCenter.y)
//                        // 2. 以原点 (0,0) 进行缩放
                        scale(scale = scale, pivot = syllableBottomCenter)
//                        // 3. 移回原位
//                        translate(left = -syllableBottomCenter.x, top = -syllableBottomCenter.y)
//
//                        // 4. 最后再平移到字符的最终绘制位置
////                        translate(left = xPos, top = yPos)
                    }) {
                        drawText(
                            textLayoutResult = charLayoutResult,
                            brush = Brush.horizontalGradient(0f to color, 1f to color),
                            topLeft = Offset(xPos, yPos),
                            shadow = shadow,
                            blendMode = BlendMode.Plus
                        )
                    }
                }
            } else {
                val floatOffset = 6f * EasingOutCubic.transform(1f - progress)
                val finalPosition =
                    syllableLayout.position.copy(y = syllableLayout.position.y + floatOffset)
                drawText(
                    textLayoutResult = syllableLayout.textLayoutResult,
                    brush = Brush.horizontalGradient(0f to color, 1f to color),
                    topLeft = finalPosition,
                    blendMode = BlendMode.Plus
                )
            }
        }


        // Draw the mask gradient
        val width = rowLayouts.last().position.x + rowLayouts.last().size.width
        val height = rowLayouts.maxOf { it.size.height }
        drawRect(
            brush = progressBrush,
            topLeft = rowLayouts.first().position.copy(y = rowLayouts.first().position.y),
            // TODO: Extract 8f to a constant
            size = Size(width, height + 12f), // Expand height for float & glow animation
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
        targetValue =
            if (!line.isAccompaniment)
                if (isFocused) 1f else 0.4f
            else
                if (isFocused) 0.6f else 0.2f,
        label = "alpha"
    )

    Box(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
//            .drawWithCache {
//                // Example that shows how to redirect rendering to an Android Picture and then
//                // draw the picture into the original destination
//                // Note:
//                // Canvas#drawPicture is supported with hardware acceleration on Android API 23+
//                // Check
//                // https://developer.android.com/topic/performance/hardware-accel#drawing-support
//                // for details of which drawing operations are supported with hardware acceleration
//                val picture = android.graphics.Picture()
//                val width = this.size.width.toInt()
//                val height = this.size.height.toInt()
//                val graphicsLayer = obtainGraphicsLayer()
//                graphicsLayer.apply {
//                    this.blendMode = blendMode
//                }
//                onDrawWithContent {
//                    val pictureCanvas =
//                        androidx.compose.ui.graphics.Canvas(picture.beginRecording(width, height))
//                    draw(this, this.layoutDirection, pictureCanvas, this.size, graphicsLayer) {
//                        this@onDrawWithContent.drawContent()
//                    }
//                    picture.endRecording()
//
//                    drawIntoCanvas {
//                        it.nativeCanvas.drawPicture(picture)
//                    }
//                }
//            }
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
                },
            verticalArrangement = Arrangement.spacedBy(2.dp),
            horizontalAlignment = if (line.alignment == KaraokeAlignment.Start) Alignment.Start else Alignment.End
        ) {
            BoxWithConstraints {
                val density = LocalDensity.current
                val availableWidthPx = with(density) { maxWidth.toPx() }

                val textStyle = remember(line.isAccompaniment) {
                    if (line.isAccompaniment) {
                        TextStyle(
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = SFPro,
                            textMotion = TextMotion.Animated
                        )
                    } else {
                        TextStyle(
                            fontSize = 32.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = SFPro,
                            textMotion = TextMotion.Animated
                        )
                    }
                }

                val wrappedLines = remember(line.syllables, availableWidthPx, textMeasurer) {
                    calculateWrappedLines(
                        syllables = line.syllables,
                        availableWidthPx = availableWidthPx,
                        textMeasurer = textMeasurer,
                        style = textStyle
                    )
                }

                val staticLineLayouts = remember(wrappedLines, availableWidthPx) {
                    calculateStaticLineLayout(
                        wrappedLines = wrappedLines,
                        textMeasurer = textMeasurer,
                        lineAlignment = if (line.alignment == KaraokeAlignment.End) Alignment.TopEnd else Alignment.TopStart,
                        canvasWidth = availableWidthPx,
                        style = textStyle
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
                    drawLine(
                        lineLayouts = staticLineLayouts,
                        currentTimeMs = currentTimeMs,
                        color = activeColor,
                        textMeasurer = textMeasurer
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
private fun Int.toDp(): Dp {
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


