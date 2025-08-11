package com.mocharealm.accompanist.lyrics.ui.composable.lyrics

import android.annotation.SuppressLint
import android.os.Build
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import com.mocharealm.accompanist.lyrics.core.model.ISyncedLine
import com.mocharealm.accompanist.lyrics.core.model.karaoke.KaraokeAlignment
import com.mocharealm.accompanist.lyrics.core.model.karaoke.KaraokeLine
import com.mocharealm.accompanist.lyrics.core.model.karaoke.KaraokeSyllable
import com.mocharealm.accompanist.lyrics.ui.utils.easing.Bounce
import com.mocharealm.accompanist.lyrics.ui.utils.easing.DipAndRise
import com.mocharealm.accompanist.lyrics.ui.utils.easing.EasingOutCubic
import com.mocharealm.accompanist.lyrics.ui.utils.easing.Swell
import kotlin.math.pow
import kotlin.math.roundToInt

/**
 * 单词级别的 "awesome" 动画所需的所有预计算参数。
 */
data class WordAnimationInfo(
    val wordStartTime: Long,
    val wordEndTime: Long,
    val wordContent: String,
    val wordDuration: Long = wordEndTime - wordStartTime
)


data class WrappedLine(
    val syllables: List<SyllableLayout>,
    val totalWidth: Float
)


// --- LOGIC FOR MEASUREMENT AND LAYOUT CALCULATION ---
private fun groupIntoWords(syllables: List<KaraokeSyllable>): List<List<KaraokeSyllable>> {
    if (syllables.isEmpty()) return emptyList()
    val words = mutableListOf<List<KaraokeSyllable>>()
    var currentWord = mutableListOf<KaraokeSyllable>()
    syllables.forEach { syllable ->
        currentWord.add(syllable)
        if (syllable.content.trimEnd().length < syllable.content.length) {
            words.add(currentWord.toList())
            currentWord = mutableListOf()
        }
    }
    if (currentWord.isNotEmpty()) {
        words.add(currentWord.toList())
    }
    return words
}

/**
 * 测量音节并决定动画类型，直接产出包含测量信息的 "半成品" SyllableLayout 列表。
 */
private fun measureSyllablesAndDetermineAnimation(
    syllables: List<KaraokeSyllable>,
    textMeasurer: TextMeasurer,
    style: TextStyle,
    isAccompanimentLine: Boolean
): List<SyllableLayout> {
    val words = groupIntoWords(syllables)
    val fastCharAnimationThresholdMs = 200f

    return words.flatMapIndexed { wordIndex, word ->
        val wordContent = word.joinToString("") { it.content }
        val wordDuration = if (word.isNotEmpty()) word.last().end - word.first().start else 0
        val perCharDuration = if (wordContent.isNotEmpty() && wordDuration > 0) {
            wordDuration.toFloat() / wordContent.length
        } else {
            0f
        }

        val useAwesomeAnimation = perCharDuration > fastCharAnimationThresholdMs &&
                wordDuration >= 1000 &&
                !wordContent.isPureCjk() &&
                !isAccompanimentLine

        word.map { syllable ->
            SyllableLayout(
                syllable = syllable,
                textLayoutResult = textMeasurer.measure(syllable.content, style),
                wordId = wordIndex,
                useAwesomeAnimation = useAwesomeAnimation
                // 其他布局字段保留默认值
            )
        }
    }
}


private fun calculateGreedyWrappedLines(
    syllableLayouts: List<SyllableLayout>,
    availableWidthPx: Float,
    textMeasurer: TextMeasurer,
    style: TextStyle
): List<WrappedLine> {

    val lines = mutableListOf<WrappedLine>()
    val currentLine = mutableListOf<SyllableLayout>()
    var currentLineWidth = 0f

    syllableLayouts.forEach { syllableLayout ->
        if (currentLineWidth + syllableLayout.width > availableWidthPx && currentLine.isNotEmpty()) {
            val trimmedDisplayLine = trimDisplayLineTrailingSpaces(currentLine, textMeasurer, style)
            if (trimmedDisplayLine.syllables.isNotEmpty()) {
                lines.add(trimmedDisplayLine)
            }
            currentLine.clear()
            currentLineWidth = 0f
        }
        currentLine.add(syllableLayout)
        currentLineWidth += syllableLayout.width
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
    syllableLayouts: List<SyllableLayout>,
    availableWidthPx: Float,
    textMeasurer: TextMeasurer,
    style: TextStyle
): List<WrappedLine> {
    if (syllableLayouts.isEmpty()) return emptyList()

    val n = syllableLayouts.size
    val costs = DoubleArray(n + 1) { Double.POSITIVE_INFINITY }
    val breaks = IntArray(n + 1)
    costs[0] = 0.0

    for (i in 1..n) {
        var currentLineWidth = 0f
        for (j in i downTo 1) {
            currentLineWidth += syllableLayouts[j - 1].width

            if (currentLineWidth > availableWidthPx) break

            val badness = (availableWidthPx - currentLineWidth).pow(2).toDouble()

            if (costs[j - 1] != Double.POSITIVE_INFINITY && costs[j - 1] + badness < costs[i]) {
                costs[i] = costs[j - 1] + badness
                breaks[i] = j - 1
            }
        }
    }

    if (costs[n] == Double.POSITIVE_INFINITY) {
        return calculateGreedyWrappedLines(syllableLayouts, availableWidthPx, textMeasurer, style)
    }

    val lines = mutableListOf<WrappedLine>()
    var currentIndex = n
    while (currentIndex > 0) {
        val startIndex = breaks[currentIndex]
        val lineSyllables = syllableLayouts.subList(startIndex, currentIndex)
        val trimmedLine = trimDisplayLineTrailingSpaces(lineSyllables, textMeasurer, style)
        lines.add(0, trimmedLine)
        currentIndex = startIndex
    }

    return lines
}

/**
 * 接收包含测量信息的 "半成品" SyllableLayout，计算最终位置和动画参数，
 * 输出 "完整版" 的 SyllableLayout 列表。
 */
private fun calculateStaticLineLayout(
    wrappedLines: List<WrappedLine>,
    lineAlignment: Alignment,
    canvasWidth: Float,
    lineHeight: Float
): List<List<SyllableLayout>> {
    val layoutsByWord = mutableMapOf<Int, MutableList<SyllableLayout>>()

    // Pass 1: 计算初始位置，并用 .copy() 更新 SyllableLayout 对象。同时按 wordId 分组。
    val positionedLines = wrappedLines.mapIndexed { lineIndex, wrappedLine ->
        val lineY = lineIndex * lineHeight
        val startX = when (lineAlignment) {
            Alignment.TopStart -> 0f
            Alignment.TopEnd -> canvasWidth - wrappedLine.totalWidth
            else -> (canvasWidth - wrappedLine.totalWidth) / 2f
        }
        var currentX = startX

        wrappedLine.syllables.map { initialLayout ->
            val positionedLayout = initialLayout.copy(position = Offset(currentX, lineY))
            layoutsByWord.getOrPut(positionedLayout.wordId) { mutableListOf() }.add(positionedLayout)
            currentX += positionedLayout.width
            positionedLayout
        }
    }

    // Pass 2: 预计算所有单词级别的动画信息。
    val animInfoByWord = mutableMapOf<Int, WordAnimationInfo>()
    val charOffsetsBySyllable = mutableMapOf<SyllableLayout, Int>()

    layoutsByWord.forEach { (wordId, layouts) ->
        if (layouts.first().useAwesomeAnimation) {
            animInfoByWord[wordId] = WordAnimationInfo(
                wordStartTime = layouts.minOf { it.syllable.start }.toLong(),
                wordEndTime = layouts.maxOf { it.syllable.end }.toLong(),
                wordContent = layouts.joinToString("") { it.syllable.content }
            )
            var runningCharOffset = 0
            layouts.forEach { layout ->
                charOffsetsBySyllable[layout] = runningCharOffset
                runningCharOffset += layout.syllable.content.length
            }
        }
    }

    // Pass 3: 最后一次 .copy()，将单词级别的动画信息和焦点注入每个 SyllableLayout。
    return positionedLines.map { line ->
        line.map { positionedLayout ->
            val wordLayouts = layoutsByWord.getValue(positionedLayout.wordId)
            val minX = wordLayouts.minOf { it.position.x }
            val maxX = wordLayouts.maxOf { it.position.x + it.width }
            val bottomY = wordLayouts.maxOf { it.position.y + it.textLayoutResult.size.height }

            positionedLayout.copy(
                wordPivot = Offset(x = (minX + maxX) / 2f, y = bottomY),
                wordAnimInfo = animInfoByWord[positionedLayout.wordId],
                charOffsetInWord = charOffsetsBySyllable[positionedLayout] ?: 0
            )
        }
    }
}

private fun createLineGradientBrush(
    lineLayout: List<SyllableLayout>,
    currentTimeMs: Int,
): Brush {
    val activeColor = Color.White
    val inactiveColor = Color.White.copy(alpha = 0.2f)
    val minFadeWidth = 60f

    if (lineLayout.isEmpty()) {
        return Brush.horizontalGradient(colors = listOf(inactiveColor, inactiveColor))
    }

    val totalWidth = lineLayout.last().let { it.position.x + it.width }
    if (totalWidth <= 0f) {
        val isFinished = currentTimeMs >= lineLayout.last().syllable.end
        val color = if (isFinished) activeColor else inactiveColor
        return Brush.horizontalGradient(colors = listOf(color, color))
    }

    val firstSyllableStart = lineLayout.first().syllable.start
    val lastSyllableEnd = lineLayout.last().syllable.end
    val lineDuration = (lastSyllableEnd - firstSyllableStart).toFloat()

    val fadeInDuration = if (lineDuration < 2000) lineDuration * 0.1f else 0f
    val fadeOutDuration = fadeInDuration
    val fadeInEndTime = firstSyllableStart + fadeInDuration
    val fadeOutStartTime = lastSyllableEnd - fadeOutDuration

    val lineProgress = run {
        val activeSyllableLayout = lineLayout.find {
            currentTimeMs in it.syllable.start until it.syllable.end
        }

        val currentPixelPosition = when {
            activeSyllableLayout != null -> {
                val syllableProgress = activeSyllableLayout.syllable.progress(currentTimeMs)
                activeSyllableLayout.position.x + activeSyllableLayout.width * syllableProgress
            }
            currentTimeMs >= lastSyllableEnd -> totalWidth
            else -> {
                val lastFinished = lineLayout.lastOrNull { currentTimeMs >= it.syllable.end }
                lastFinished?.let { it.position.x + it.width } ?: 0f
            }
        }
        (currentPixelPosition / totalWidth).coerceIn(0f, 1f)
    }

    val fadeRange = run {
        val fadeWidthPx = maxOf(totalWidth * 0.2f, minFadeWidth)
        (fadeWidthPx / totalWidth).coerceAtMost(1f)
    }

    fun lerpColor(start: Color, end: Color, fraction: Float): Color {
        return Color(
            red = (start.red + (end.red - start.red) * fraction),
            green = (start.green + (end.green - start.green) * fraction),
            blue = (start.blue + (end.blue - start.blue) * fraction),
            alpha = (start.alpha + (end.alpha - start.alpha) * fraction)
        )
    }

    return when {
        currentTimeMs >= lastSyllableEnd -> Brush.horizontalGradient(colors = listOf(activeColor, activeColor))
        currentTimeMs < firstSyllableStart -> Brush.horizontalGradient(colors = listOf(inactiveColor, inactiveColor))
        currentTimeMs < fadeInEndTime -> {
            val phaseProgress = ((currentTimeMs - firstSyllableStart) / fadeInDuration).coerceIn(0f, 1f)
            val dynamicFade = fadeRange * phaseProgress
            val fadeStart = (lineProgress - dynamicFade / 2).coerceAtLeast(0f)
            val fadeEnd = (lineProgress + dynamicFade / 2).coerceAtMost(1f)
            Brush.horizontalGradient(
                colorStops = arrayOf(
                    0.0f to activeColor,
                    fadeStart to activeColor,
                    ((fadeStart + fadeEnd) / 2f) to lerpColor(activeColor, inactiveColor, phaseProgress),
                    fadeEnd to inactiveColor,
                    1.0f to inactiveColor
                ),
                endX = totalWidth
            )
        }
        currentTimeMs > fadeOutStartTime -> {
            val phaseProgress = ((currentTimeMs - fadeOutStartTime) / fadeOutDuration).coerceIn(0f, 1f)
            val dynamicFade = fadeRange * (1f - phaseProgress)
            val fadeStart = (lineProgress - dynamicFade / 2).coerceAtLeast(0f)
            val fadeEnd = (lineProgress + dynamicFade / 2).coerceAtMost(1f)
            Brush.horizontalGradient(
                colorStops = arrayOf(
                    0.0f to activeColor,
                    fadeStart to activeColor,
                    ((fadeStart + fadeEnd) / 2f) to lerpColor(activeColor, inactiveColor, phaseProgress),
                    fadeEnd to inactiveColor,
                    1.0f to inactiveColor
                ),
                endX = totalWidth
            )
        }
        else -> {
            val fadeStart = (lineProgress - fadeRange / 2).coerceAtLeast(0f)
            val fadeEnd = (lineProgress + fadeRange / 2).coerceAtMost(1f)
            Brush.horizontalGradient(
                colorStops = arrayOf(
                    0.0f to activeColor,
                    fadeStart to activeColor,
                    ((fadeStart + fadeEnd) / 2f) to lerpColor(activeColor, inactiveColor, 0.5f),
                    fadeEnd to inactiveColor,
                    1.0f to inactiveColor
                ),
                endX = totalWidth
            )
        }
    }
}


fun Char.isCjk(): Boolean {
    val cjkBlock = mutableListOf(
        Character.UnicodeBlock.CJK_UNIFIED_IDEOGRAPHS,
        Character.UnicodeBlock.CJK_UNIFIED_IDEOGRAPHS_EXTENSION_A,
        Character.UnicodeBlock.CJK_UNIFIED_IDEOGRAPHS_EXTENSION_B,
        Character.UnicodeBlock.CJK_UNIFIED_IDEOGRAPHS_EXTENSION_C,
        Character.UnicodeBlock.CJK_UNIFIED_IDEOGRAPHS_EXTENSION_D,
        Character.UnicodeBlock.CJK_COMPATIBILITY_IDEOGRAPHS,
        Character.UnicodeBlock.CJK_SYMBOLS_AND_PUNCTUATION,
        Character.UnicodeBlock.HIRAGANA,
        Character.UnicodeBlock.KATAKANA,
        Character.UnicodeBlock.HANGUL_SYLLABLES,
        Character.UnicodeBlock.HANGUL_JAMO,
        Character.UnicodeBlock.HANGUL_COMPATIBILITY_JAMO
    )
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
        cjkBlock.add(Character.UnicodeBlock.CJK_UNIFIED_IDEOGRAPHS_EXTENSION_E)
        cjkBlock.add(Character.UnicodeBlock.CJK_UNIFIED_IDEOGRAPHS_EXTENSION_F)
        cjkBlock.add(Character.UnicodeBlock.CJK_UNIFIED_IDEOGRAPHS_EXTENSION_G)
    }
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.BAKLAVA) {
        cjkBlock.add(Character.UnicodeBlock.CJK_UNIFIED_IDEOGRAPHS_EXTENSION_H)
    }
    return Character.UnicodeBlock.of(this) in cjkBlock
}

fun String.isPureCjk(): Boolean {
    val cleanedStr = this.filter { it != ' ' && it != ',' && it != '\n' && it != '\r' }
    if (cleanedStr.isEmpty()) {
        return false
    }
    return cleanedStr.all { it.isCjk() }
}

fun DrawScope.drawLine(
    lineLayouts: List<List<SyllableLayout>>,
    currentTimeMs: Int,
    color: Color,
    textMeasurer: TextMeasurer
) {
    lineLayouts.forEach { rowLayouts ->
        if (rowLayouts.isEmpty()) return@forEach

        val firstSyllableLayout = rowLayouts.first()
        val lastSyllableLayout = rowLayouts.last()
        val totalHeight = rowLayouts.maxOf { it.textLayoutResult.size.height.toFloat() }

        val verticalPadding = 12.dp.toPx()
        val horizontalPadding = 8.dp.toPx()

        drawIntoCanvas { canvas ->
            val layerBounds = Rect(
                left = firstSyllableLayout.position.x - horizontalPadding,
                top = firstSyllableLayout.position.y - verticalPadding,
                right = lastSyllableLayout.position.x + lastSyllableLayout.width + horizontalPadding,
                bottom = firstSyllableLayout.position.y + totalHeight + verticalPadding
            )
            canvas.saveLayer(layerBounds, Paint())

            rowLayouts.forEach { syllableLayout ->
                val wordAnimInfo = syllableLayout.wordAnimInfo

                if (wordAnimInfo != null) {
                    val textStyle = syllableLayout.textLayoutResult.layoutInput.style
                    val fastCharAnimationThresholdMs = 200f
                    val awesomeDuration = wordAnimInfo.wordDuration * 0.8f

                    syllableLayout.syllable.content.forEachIndexed { charIndex, char ->
                        val absoluteCharIndex = syllableLayout.charOffsetInWord + charIndex
                        val numCharsInWord = wordAnimInfo.wordContent.length
                        val earliestStartTime = wordAnimInfo.wordStartTime
                        val latestStartTime = wordAnimInfo.wordEndTime - awesomeDuration

                        val charRatio = if (numCharsInWord > 1) absoluteCharIndex.toFloat() / (numCharsInWord - 1) else 0.5f
                        val awesomeStartTime = (earliestStartTime + (latestStartTime - earliestStartTime) * charRatio).toLong()
                        val awesomeProgress = ((currentTimeMs - awesomeStartTime).toFloat() / awesomeDuration).coerceIn(0f, 1f)

                        val floatOffset = 6f * DipAndRise(dip = ((0.5 * (wordAnimInfo.wordDuration - fastCharAnimationThresholdMs * numCharsInWord) / 1000)).coerceIn(0.0, 0.5)).transform(1.0f - awesomeProgress)
                        val scale = 1f + Swell((0.1 * (wordAnimInfo.wordDuration - fastCharAnimationThresholdMs * numCharsInWord) / 1000).coerceIn(0.0, 0.1)).transform(awesomeProgress)
                        val yPos = syllableLayout.position.y + floatOffset
                        val xPos = syllableLayout.position.x + syllableLayout.textLayoutResult.getHorizontalPosition(offset = charIndex, usePrimaryDirection = true)
                        val blurRadius = 10f * Bounce.transform(awesomeProgress)
                        val shadow = Shadow(color = color.copy(0.4f), offset = Offset(0f, 0f), blurRadius = blurRadius)
                        val charLayoutResult = textMeasurer.measure(char.toString(), style = textStyle)

                        withTransform({ scale(scale = scale, pivot = syllableLayout.wordPivot) }) {
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
                    val progress = syllableLayout.syllable.progress(currentTimeMs)
                    val floatOffset = 6f * EasingOutCubic.transform(1f - progress)
                    val finalPosition = syllableLayout.position.copy(y = syllableLayout.position.y + floatOffset)
                    drawText(
                        textLayoutResult = syllableLayout.textLayoutResult,
                        brush = Brush.horizontalGradient(0f to color, 1f to color),
                        topLeft = finalPosition,
                        blendMode = BlendMode.Plus
                    )
                }
            }

            val progressBrush = createLineGradientBrush(rowLayouts, currentTimeMs)
            drawRect(brush = progressBrush, topLeft = layerBounds.topLeft, size = layerBounds.size, blendMode = BlendMode.DstIn)
            canvas.restore()
        }
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
    activeColor: Color = Color.White,
    normalLineTextStyle: TextStyle,
    accompanimentLineTextStyle: TextStyle
) {
    val isFocused = line.isFocused(currentTimeMs)
    val textMeasurer = rememberTextMeasurer()

    val animatedScale by animateFloatAsState(targetValue = if (isFocused) 1.05f else 1f, label = "scale")
    val alphaAnimation by animateFloatAsState(
        targetValue = if (!line.isAccompaniment) if (isFocused) 1f else 0.4f else if (isFocused) 0.6f else 0.2f,
        label = "alpha"
    )

    Box(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .combinedClickable(onClick = { onLineClicked(line) }, onLongClick = { onLinePressed(line) })
    ) {
        Column(
            modifier
                .align(if (line.alignment == KaraokeAlignment.End) Alignment.TopEnd else Alignment.TopStart)
                .padding(vertical = 8.dp, horizontal = 16.dp)
                .graphicsLayer {
                    scaleX = animatedScale
                    scaleY = animatedScale
                    alpha = alphaAnimation
                    transformOrigin = TransformOrigin(if (line.alignment == KaraokeAlignment.Start) 0f else 1f, 1f)
                },
            verticalArrangement = Arrangement.spacedBy(2.dp),
            horizontalAlignment = if (line.alignment == KaraokeAlignment.Start) Alignment.Start else Alignment.End
        ) {
            BoxWithConstraints {
                val density = LocalDensity.current
                val availableWidthPx = with(density) { maxWidth.toPx() }

                val textStyle = remember(line.isAccompaniment) {
                    if (line.isAccompaniment) accompanimentLineTextStyle else normalLineTextStyle
                }

                // 1. 测量并产出 "半成品" Layout 对象
                val initialLayouts = remember(line.syllables, textStyle, textMeasurer, line.isAccompaniment) {
                    measureSyllablesAndDetermineAnimation(
                        syllables = line.syllables,
                        textMeasurer = textMeasurer,
                        style = textStyle,
                        isAccompanimentLine = line.isAccompaniment
                    )
                }

                // 2. 将 Layout 对象进行换行
                val wrappedLines = remember(initialLayouts, availableWidthPx, textMeasurer, textStyle) {
                    calculateBalancedLines(
                        syllableLayouts = initialLayouts,
                        availableWidthPx = availableWidthPx,
                        textMeasurer = textMeasurer,
                        style = textStyle
                    )
                }

                val lineHeight = remember(textStyle) {
                    textMeasurer.measure("M", textStyle).size.height.toFloat()
                }

                // 3. 计算最终位置和动画参数，得到 "完整版" Layout 对象
                val finalLineLayouts = remember(wrappedLines, availableWidthPx, lineHeight) {
                    calculateStaticLineLayout(
                        wrappedLines = wrappedLines,
                        lineAlignment = if (line.alignment == KaraokeAlignment.End) Alignment.TopEnd else Alignment.TopStart,
                        canvasWidth = availableWidthPx,
                        lineHeight = lineHeight
                    )
                }

                val totalHeight = remember(wrappedLines, lineHeight) {
                    lineHeight * wrappedLines.size
                }

                Canvas(modifier = Modifier.size(maxWidth, (totalHeight.roundToInt() + 8).toDp())) {
                    drawLine(
                        lineLayouts = finalLineLayouts,
                        currentTimeMs = currentTimeMs,
                        color = activeColor,
                        textMeasurer = textMeasurer
                    )
                }
            }

            line.translation?.let { translation ->
                val result = remember(translation) { textMeasurer.measure(translation) }
                val color = activeColor.copy(0.6f)
                Canvas(modifier = Modifier.size(result.size.toDpSize())) {
                    drawText(result, color, blendMode = BlendMode.Plus)
                }
            }
        }
    }
}

private fun trimDisplayLineTrailingSpaces(
    displayLineSyllables: List<SyllableLayout>,
    textMeasurer: TextMeasurer,
    style: TextStyle
): WrappedLine {
    if (displayLineSyllables.isEmpty()) {
        return WrappedLine(emptyList(), 0f)
    }

    val processedSyllables = displayLineSyllables.toMutableList()
    val lastIndex = processedSyllables.lastIndex
    val lastLayout = processedSyllables[lastIndex]

    val originalContent = lastLayout.syllable.content
    val trimmedContent = originalContent.trimEnd()

    if (trimmedContent.length < originalContent.length) {
        if (trimmedContent.isNotEmpty()) {
            val trimmedLayoutResult = textMeasurer.measure(trimmedContent, style)
            val trimmedLayout = lastLayout.copy(
                syllable = lastLayout.syllable.copy(content = trimmedContent),
                textLayoutResult = trimmedLayoutResult
            )
            processedSyllables[lastIndex] = trimmedLayout
        } else {
            processedSyllables.removeAt(lastIndex)
        }
    }

    val totalWidth = processedSyllables.sumOf { it.width.toDouble() }.toFloat()

    return WrappedLine(processedSyllables, totalWidth)
}

@Composable
private fun Int.toDp(): Dp = with(LocalDensity.current) { this@toDp.toDp() }

@Composable
private fun IntSize.toDpSize(): DpSize = with(LocalDensity.current) { DpSize(width.toDp(), height.toDp()) }