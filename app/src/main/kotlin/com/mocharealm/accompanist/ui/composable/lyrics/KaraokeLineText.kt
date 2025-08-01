package com.mocharealm.accompanist.ui.composable.lyrics

import android.annotation.SuppressLint
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.detectTapGestures
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
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextLayoutResult
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
import com.mocharealm.accompanist.ui.composable.utils.easedHorizontalGradient
import com.mocharealm.accompanist.ui.composable.utils.easing.Bounce
import com.mocharealm.accompanist.ui.composable.utils.easing.DipAndRise
import com.mocharealm.accompanist.ui.composable.utils.easing.EasingOutCubic
import com.mocharealm.accompanist.ui.composable.utils.easing.Swell
import com.mocharealm.accompanist.ui.theme.SFPro
import kotlin.math.pow

data class MeasuredSyllable(
    val syllable: KaraokeSyllable,
    val textLayoutResult: TextLayoutResult,
    val width: Float = textLayoutResult.size.width.toFloat()
)

data class WrappedLine(
    val syllables: List<MeasuredSyllable>,
    val totalWidth: Float
)

private fun calculateGreedyWrappedLines(
    syllables: List<KaraokeSyllable>,
    availableWidthPx: Float,
    textMeasurer: TextMeasurer,
    style: TextStyle
): List<WrappedLine> {

    val measuredSyllables = syllables.map {
        MeasuredSyllable(
            syllable = it,
            textLayoutResult = textMeasurer.measure(it.content, style)
        )
    }

    val lines = mutableListOf<WrappedLine>()
    val currentLine = mutableListOf<MeasuredSyllable>()
    var currentLineWidth = 0f

    measuredSyllables.forEach { measuredSyllable ->
        if (currentLineWidth + measuredSyllable.width > availableWidthPx && currentLine.isNotEmpty()) {
            val trimmedDisplayLine = trimDisplayLineTrailingSpaces(currentLine, textMeasurer, style)
            if (trimmedDisplayLine.syllables.isNotEmpty()) {
                lines.add(trimmedDisplayLine)
            }
            currentLine.clear()
            currentLineWidth = 0f
        }
        currentLine.add(measuredSyllable)
        currentLineWidth += measuredSyllable.width
    }

    if (currentLine.isNotEmpty()) {
        val trimmedDisplayLine = trimDisplayLineTrailingSpaces(currentLine, textMeasurer, style)
        if (trimmedDisplayLine.syllables.isNotEmpty()) {
            lines.add(trimmedDisplayLine)
        }
    }
    return lines
}

private fun calculateBalancedLines(
    syllables: List<KaraokeSyllable>,
    availableWidthPx: Float,
    textMeasurer: TextMeasurer,
    style: TextStyle
): List<WrappedLine> {
    if (syllables.isEmpty()) return emptyList()

    val measuredSyllables = syllables.map {
        MeasuredSyllable(
            syllable = it,
            textLayoutResult = textMeasurer.measure(it.content, style)
        )
    }
    val n = measuredSyllables.size

    val costs = DoubleArray(n + 1) { Double.POSITIVE_INFINITY }
    val breaks = IntArray(n + 1)
    costs[0] = 0.0

    for (i in 1..n) {
        var currentLineWidth = 0f
        for (j in i downTo 1) {
            currentLineWidth += measuredSyllables[j - 1].width

            if (currentLineWidth > availableWidthPx) break

            val badness = (availableWidthPx - currentLineWidth).pow(2).toDouble()

            if (costs[j - 1] != Double.POSITIVE_INFINITY && costs[j - 1] + badness < costs[i]) {
                costs[i] = costs[j - 1] + badness
                breaks[i] = j - 1
            }
        }
    }

    if (costs[n] == Double.POSITIVE_INFINITY) {
        // 如果无法找到一个有效的换行方案（比如某个音节本身就超宽），则退回到原始的贪心算法
        // (此处的降级策略代码未实现，为保持简洁，假设总能找到解)
        // Log.w("KaraokeLayout", "Could not find a balanced layout.")
        calculateGreedyWrappedLines(syllables, availableWidthPx, textMeasurer, style)
    }

    val lines = mutableListOf<WrappedLine>()
    var currentIndex = n
    while (currentIndex > 0) {
        val startIndex = breaks[currentIndex]
        val lineSyllables = measuredSyllables.subList(startIndex, currentIndex)
        val lineWidth = lineSyllables.sumOf { it.width.toDouble() }.toFloat()
        // 注意：trimDisplayLineTrailingSpaces 也需要应用在这里
        val trimmedLine = trimDisplayLineTrailingSpaces(lineSyllables, textMeasurer, style)
        lines.add(0, trimmedLine) // 将行添加到列表的开头
        currentIndex = startIndex
    }

    return lines
}

private fun calculateStaticLineLayout(
    wrappedLines: List<WrappedLine>,
    lineAlignment: Alignment,
    canvasWidth: Float,
    lineHeight: Float
): List<List<SyllableLayout>> {
    return wrappedLines.mapIndexed { lineIndex, wrappedLine ->
        val lineY = lineIndex * lineHeight
        val startX = when (lineAlignment) {
            Alignment.TopStart -> 0f
            Alignment.TopEnd -> canvasWidth - wrappedLine.totalWidth
            else -> (canvasWidth - wrappedLine.totalWidth) / 2f
        }
        var currentX = startX

        wrappedLine.syllables.map { measuredSyllable ->
            val layout = SyllableLayout(
                syllable = measuredSyllable.syllable,
                position = Offset(currentX, lineY),
                size = Size(
                    measuredSyllable.width,
                    measuredSyllable.textLayoutResult.size.height.toFloat()
                ),
                textLayoutResult = measuredSyllable.textLayoutResult
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
    val activeColor = Color.White
    val inactiveColor = Color.White.copy(alpha = 0.2f)
    val minFadeWidth = 30f

    if (lineLayout.isEmpty()) {
        return Brush.horizontalGradient(colors = listOf(inactiveColor, inactiveColor))
    }

    val totalWidth = lineLayout.last().let { it.position.x + it.size.width }
    if (totalWidth <= 0f) {
        val isFinished = currentTimeMs >= lineLayout.last().syllable.end
        val color = if (isFinished) activeColor else inactiveColor
        return Brush.horizontalGradient(colors = listOf(color, color))
    }

    val firstSyllableStart = lineLayout.first().syllable.start
    val lastSyllableEnd = lineLayout.last().syllable.end
    val lineDuration = (lastSyllableEnd - firstSyllableStart).toFloat()

    // 时间阶段：fadeIn、主阶段、fadeOut
    val fadeInDuration = if (lineDuration < 2000) lineDuration * 0.1f else 0f
    val fadeOutDuration = fadeInDuration
    val fadeInEndTime = firstSyllableStart + fadeInDuration
    val fadeOutStartTime = lastSyllableEnd - fadeOutDuration

    val lineProgress = run {
        val activeSyllable = lineLayout.find {
            currentTimeMs in it.syllable.start until it.syllable.end
        }

        val currentPixelPosition = when {
            activeSyllable != null -> {
                val syllableProgress = activeSyllable.syllable.progress(currentTimeMs)
                activeSyllable.position.x + activeSyllable.size.width * syllableProgress
            }

            currentTimeMs >= lastSyllableEnd -> totalWidth
            else -> {
                val lastFinished = lineLayout.lastOrNull { currentTimeMs >= it.syllable.end }
                lastFinished?.let { it.position.x + it.size.width } ?: 0f
            }
        }
        (currentPixelPosition / totalWidth).coerceIn(0f, 1f)
    }

    val fadeRange = run {
        val fadeWidthPx = maxOf(totalWidth * 0.2f, minFadeWidth)
        (fadeWidthPx / totalWidth).coerceAtMost(1f)
    }

    return when {
        currentTimeMs >= lastSyllableEnd -> Brush.easedHorizontalGradient(
            0f to activeColor,
            1f to activeColor
        )

        currentTimeMs < firstSyllableStart -> Brush.easedHorizontalGradient(
            0f to inactiveColor,
            1f to inactiveColor
        )

        currentTimeMs < fadeInEndTime -> {
            val phaseProgress =
                ((currentTimeMs - firstSyllableStart) / fadeInDuration).coerceIn(0f, 1f)
            val dynamicFade = fadeRange * phaseProgress
            Brush.easedHorizontalGradient(
                0.0f to activeColor,
                (lineProgress - dynamicFade / 2).coerceAtLeast(0f) to activeColor,
                (lineProgress + dynamicFade / 2).coerceAtMost(1f) to inactiveColor,
                1.0f to inactiveColor,
                steps = 100
            )
        }

        currentTimeMs > fadeOutStartTime -> {
            val phaseProgress =
                ((currentTimeMs - fadeOutStartTime) / fadeOutDuration).coerceIn(0f, 1f)
            val dynamicFade = fadeRange * (1f - phaseProgress)
            Brush.horizontalGradient(
                colorStops = arrayOf(
                    0.0f to activeColor,
                    (lineProgress - dynamicFade / 2).coerceAtLeast(0f) to activeColor,
                    (lineProgress + dynamicFade / 2).coerceAtMost(1f) to inactiveColor,
                    1.0f to inactiveColor
                )
            )
        }

        else -> {
            Brush.horizontalGradient(
                colorStops = arrayOf(
                    0.0f to activeColor,
                    (lineProgress - fadeRange / 2).coerceAtLeast(0f) to activeColor,
                    (lineProgress + fadeRange / 2).coerceAtMost(1f) to inactiveColor,
                    1.0f to inactiveColor
                )
            )
        }
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
                    val xPos =
                        syllableLayout.position.x + syllableLayout.textLayoutResult.getHorizontalPosition(
                            offset = index,
                            usePrimaryDirection = true
                        )

                    val blurRadius = 10f * Bounce.transform(awesomeProgress)
                    val shadow = Shadow(
                        color = color.copy(0.4f),
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
        val width = rowLayouts.last().position.x + rowLayouts.last().size.width
        val height = rowLayouts.maxOf { it.size.height }

        val progressBrush = createLineGradientBrush(rowLayouts, currentTimeMs)
        drawRect(
            brush = progressBrush,
            topLeft = rowLayouts.first().position.copy(y = rowLayouts.first().position.y),
            // TODO: Extract 8f to a constant
            size = Size(width, height + 12f),
            blendMode = BlendMode.DstIn
        )
    }
}


@SuppressLint("UnusedBoxWithConstraintsScope")
@Stable
@Composable
fun KaraokeLineText(
    line: KaraokeLine,
    onLineClicked: (ISyncedLine) -> Unit,
    onLinePressed: (ISyncedLine) -> Unit,
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
            .combinedClickable(onClick = {onLineClicked(line)},onLongClick = {onLinePressed(line)})

    ) {
        val activeColor = Color.White
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
                    calculateBalancedLines(
                        syllables = line.syllables,
                        availableWidthPx = availableWidthPx,
                        textMeasurer = textMeasurer,
                        style = textStyle
                    )
                }

                val staticLineLayouts = remember(wrappedLines, availableWidthPx) {
                    calculateStaticLineLayout(
                        wrappedLines = wrappedLines,
                        lineAlignment = if (line.alignment == KaraokeAlignment.End) Alignment.TopEnd else Alignment.TopStart,
                        canvasWidth = availableWidthPx,
                        lineHeight = textMeasurer.measure("M", textStyle).size.height.toFloat()
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
                val color = activeColor.copy(0.6f)
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
    displayLineSyllables: List<MeasuredSyllable>,
    textMeasurer: TextMeasurer,
    style: TextStyle
): WrappedLine {
    if (displayLineSyllables.isEmpty()) {
        return WrappedLine(emptyList(), 0f)
    }

    val processedSyllables = displayLineSyllables.toMutableList()
    val lastIndex = processedSyllables.lastIndex
    val lastMeasuredSyllable = processedSyllables[lastIndex]

    val originalContent = lastMeasuredSyllable.syllable.content
    val trimmedContent = originalContent.trimEnd()

    if (trimmedContent.length < originalContent.length) {
        if (trimmedContent.isNotEmpty()) {
            val trimmedLayoutResult = textMeasurer.measure(trimmedContent, style)
            val trimmedSyllable = lastMeasuredSyllable.copy(
                syllable = lastMeasuredSyllable.syllable.copy(content = trimmedContent),
                textLayoutResult = trimmedLayoutResult
            )
            processedSyllables[lastIndex] = trimmedSyllable
        } else {
            processedSyllables.removeAt(lastIndex)
        }
    }

    // 3. 高效计算总宽度：直接累加缓存的宽度值
    val totalWidth = processedSyllables.sumOf { it.width.toDouble() }.toFloat()

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


