package com.mocharealm.accompanist.lyrics.ui.lyrics

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
import com.mocharealm.accompanist.lyrics.ui.theme.SFPro
import com.mocharealm.accompanist.ui.composable.lyrics.SyllableLayout
import com.mocharealm.accompanist.lyrics.ui.utils.easing.Bounce
import com.mocharealm.accompanist.lyrics.ui.utils.easing.DipAndRise
import com.mocharealm.accompanist.lyrics.ui.utils.easing.EasingOutCubic
import com.mocharealm.accompanist.lyrics.ui.utils.easing.Swell
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
        calculateGreedyWrappedLines(syllables, availableWidthPx, textMeasurer, style)
    }

    val lines = mutableListOf<WrappedLine>()
    var currentIndex = n
    while (currentIndex > 0) {
        val startIndex = breaks[currentIndex]
        val lineSyllables = measuredSyllables.subList(startIndex, currentIndex)
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
    val minFadeWidth = 60f

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
        currentTimeMs >= lastSyllableEnd -> Brush.horizontalGradient(colors = listOf(activeColor, activeColor))
        currentTimeMs < firstSyllableStart -> Brush.horizontalGradient(colors = listOf(inactiveColor, inactiveColor))
        currentTimeMs < fadeInEndTime -> {
            val phaseProgress = ((currentTimeMs - firstSyllableStart) / fadeInDuration).coerceIn(0f, 1f)
            val dynamicFade = fadeRange * phaseProgress
            Brush.horizontalGradient(
                colorStops = arrayOf(
                    0.0f to activeColor,
                    (lineProgress - dynamicFade / 2).coerceAtLeast(0f) to activeColor,
                    (lineProgress + dynamicFade / 2).coerceAtMost(1f) to inactiveColor,
                    1.0f to inactiveColor
                ),

                endX =totalWidth
            )
        }
        currentTimeMs > fadeOutStartTime -> {
            val phaseProgress = ((currentTimeMs - fadeOutStartTime) / fadeOutDuration).coerceIn(0f, 1f)
            val dynamicFade = fadeRange * (1f - phaseProgress)
            Brush.horizontalGradient(
                colorStops = arrayOf(
                    0.0f to activeColor,
                    (lineProgress - dynamicFade / 2).coerceAtLeast(0f) to activeColor,
                    (lineProgress + dynamicFade / 2).coerceAtMost(1f) to inactiveColor,
                    1.0f to inactiveColor
                ),
                        endX =totalWidth
            )
        }
        else -> {
            Brush.horizontalGradient(
                colorStops = arrayOf(
                    0.0f to activeColor,
                    (lineProgress - fadeRange / 2).coerceAtLeast(0f) to activeColor,
                    (lineProgress + fadeRange / 2).coerceAtMost(1f) to inactiveColor,
                    1.0f to inactiveColor
                ),
                endX =totalWidth
            )
        }
    }
}


fun Char.isCjk(): Boolean {
    val cjkBlock = mutableListOf(
        Character.UnicodeBlock.CJK_UNIFIED_IDEOGRAPHS,
        Character.UnicodeBlock.CJK_UNIFIED_IDEOGRAPHS_EXTENSION_A,
        Character.UnicodeBlock.CJK_UNIFIED_IDEOGRAPHS_EXTENSION_B,
        // 如果需要，可以加入更多扩展区, 如 C, D, E, F, G...
        Character.UnicodeBlock.CJK_UNIFIED_IDEOGRAPHS_EXTENSION_C,
        Character.UnicodeBlock.CJK_UNIFIED_IDEOGRAPHS_EXTENSION_D,
        Character.UnicodeBlock.CJK_COMPATIBILITY_IDEOGRAPHS,
        Character.UnicodeBlock.CJK_SYMBOLS_AND_PUNCTUATION, // 包含中日韩标点符号，如 `。` `「` `」`
        Character.UnicodeBlock.HIRAGANA,  // 日文平假名
        Character.UnicodeBlock.KATAKANA,  // 日文片假名
        Character.UnicodeBlock.HANGUL_SYLLABLES, // 韩文音节
        Character.UnicodeBlock.HANGUL_JAMO,      // 韩文字母
        Character.UnicodeBlock.HANGUL_COMPATIBILITY_JAMO // 韩文兼容字母
    )
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
        cjkBlock.add(
            Character.UnicodeBlock.CJK_UNIFIED_IDEOGRAPHS_EXTENSION_E
        )
        cjkBlock.add(
            Character.UnicodeBlock.CJK_UNIFIED_IDEOGRAPHS_EXTENSION_F
        )
        cjkBlock.add(
            Character.UnicodeBlock.CJK_UNIFIED_IDEOGRAPHS_EXTENSION_G
        )

    }
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.BAKLAVA) {
        cjkBlock.add(
            Character.UnicodeBlock.CJK_UNIFIED_IDEOGRAPHS_EXTENSION_H
        )
    }
    return Character.UnicodeBlock.of(this) in cjkBlock
}

fun String.isPureCjk(): Boolean {
    // 移除所有空格和逗号
    val cleanedStr =
        this.filter { it != ' ' && it != ',' && it != '\n' && it != '\r' }

    // 如果清理后字符串为空，也视为符合条件
    if (cleanedStr.isEmpty()) {
        return false
    }

    // 使用 all 函数检查是否所有剩余字符都满足 isCjk 条件
    return cleanedStr.all { it.isCjk() }
}

fun DrawScope.drawLine(
    lineLayouts: List<List<SyllableLayout>>,
    currentTimeMs: Int,
    color: Color,
    textMeasurer: TextMeasurer
) {
    lineLayouts.forEachIndexed { index, rowLayouts ->
        // 安全检查，防止行数据为空
        if (rowLayouts.isEmpty()) return@forEachIndexed

        // --- 1. 计算基础几何信息和动画所需的外边距 ---
        val firstSyllable = rowLayouts.first()
        val lastSyllable = rowLayouts.last()
        val totalHeight = rowLayouts.maxOf { it.size.height }

        // 定义一些安全边距，确保文本动画不会被图层边界裁剪掉
        // 12.dp 的垂直边距对于常见的上下浮动和缩放动画来说是比较安全的值
        val verticalPadding = 12.dp.toPx()
        // 如果水平方向的动画不剧烈，可以设置一个较小的水平边距
        val horizontalPadding = 8.dp.toPx()

        // --- 2. 使用隔离的图层进行绘制，以确保 BlendMode 和动画正确结合 ---
        drawIntoCanvas { canvas ->
            // a. 定义图层的边界，要比文本的静态边界更大，以容纳所有动画
            val layerBounds = Rect(
                left = firstSyllable.position.x - horizontalPadding,
                top = firstSyllable.position.y - verticalPadding,
                right = lastSyllable.position.x + lastSyllable.size.width + horizontalPadding,
                bottom = firstSyllable.position.y + totalHeight + verticalPadding
            )

            // b. 保存当前画布状态，并创建一个新的离屏图层
            // 在 saveLayer 和 restore 之间的所有绘制指令都会先作用于这个临时图层上
            canvas.saveLayer(layerBounds, Paint())


            // --- 3. 在新图层上绘制所有带动画的文本 ---
            // 这部分是您原有的、完整的文本动画和绘制逻辑，保持不变
            val fastCharAnimationThresholdMs = 200f
            if (rowLayouts.any { it.syllable.content.isPureCjk() }) {
                // CJK 歌词的动画路径
                rowLayouts.forEach { syllableLayout ->
                    val syllable = syllableLayout.syllable
                    val progress = syllable.progress(currentTimeMs)
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
            } else {
                // 非 CJK 歌词的动画路径
                rowLayouts.forEach { syllableLayout ->
                    val syllable = syllableLayout.syllable
                    val progress = syllable.progress(currentTimeMs)
                    val perCharDuration = if (syllable.content.isNotEmpty()) {
                        syllable.duration.toFloat() / syllable.content.length
                    } else {
                        0f
                    }

                    if (perCharDuration > fastCharAnimationThresholdMs && syllable.duration >= 1000 && !syllable.content.isPureCjk()) {
                        // "Awesome" 逐字动画
                        val textStyle = syllableLayout.textLayoutResult.layoutInput.style
                        val syllableBottomCenter = Offset(
                            x = syllableLayout.position.x + syllableLayout.size.width / 2f,
                            y = syllableLayout.position.y + syllableLayout.size.height
                        )
                        val awesomeDuration = syllable.duration * 0.8f
                        syllable.content.forEachIndexed { charIndex, char ->
                            val numChars = syllable.content.length
                            val earliestStartTime = syllable.start
                            val latestStartTime = syllable.end - awesomeDuration
                            val charRatio =
                                if (numChars > 1) charIndex.toFloat() / (numChars - 1) else 0.5f
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
                                    offset = charIndex,
                                    usePrimaryDirection = true
                                )
                            val blurRadius = 10f * Bounce.transform(awesomeProgress)
                            val shadow = Shadow(
                                color = color.copy(0.4f),
                                offset = Offset(0f, 0f),
                                blurRadius = blurRadius
                            )
                            val charLayoutResult =
                                textMeasurer.measure(char.toString(), style = textStyle)

                            withTransform({
                                scale(scale = scale, pivot = syllableBottomCenter)
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
                        // 标准逐音节动画
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
            }


            // --- 4. 在同一个图层上，绘制渐变遮罩 ---
            val progressBrush = createLineGradientBrush(rowLayouts, currentTimeMs)

            // 使用 DstIn 模式，用笔刷来“裁切”已经画好的文本
            // 这个遮罩矩形必须覆盖整个图层，才能影响到图层内的所有动画像素
            drawRect(
                brush = progressBrush,
                topLeft = layerBounds.topLeft,
                size = layerBounds.size,
                blendMode = BlendMode.DstIn
            )


            // --- 5. 恢复画布状态，将处理好的图层合并到主屏幕上 ---
            canvas.restore()
        }
    }
}

private fun calculateProgressPx(
    lineLayout: List<SyllableLayout>,
    currentTimeMs: Int,
    totalWidth: Float,
    lineStartX: Float
): Float {
    // 假设 SyllableLayout 包含 syllable 字段，且 syllable 有 start, end, progress() 方法
    val firstSyllable = lineLayout.first()
    val lastSyllable = lineLayout.last()

    if (currentTimeMs < firstSyllable.syllable.start) return 0f
    if (currentTimeMs >= lastSyllable.syllable.end) return totalWidth

    val activeOrNextIndex = lineLayout.indexOfFirst { currentTimeMs < it.syllable.end }
    if (activeOrNextIndex == -1) return totalWidth

    val activeOrNextSyllable = lineLayout[activeOrNextIndex]

    if (currentTimeMs >= activeOrNextSyllable.syllable.start) {
        // 情况A: 时间在音节内部
        val syllableProgress = activeOrNextSyllable.syllable.progress(currentTimeMs)
        val widthBeforeThisSyllable = activeOrNextSyllable.position.x - lineStartX
        val widthInsideThisSyllable = activeOrNextSyllable.size.width * syllableProgress
        return widthBeforeThisSyllable + widthInsideThisSyllable
    } else {
        // 情况B: 时间在音节之间的间隙
        if (activeOrNextIndex == 0) return 0f // 在第一个音节之前
        val prevSyllable = lineLayout[activeOrNextIndex - 1]

        val gapStartTime = prevSyllable.syllable.end
        val gapEndTime = activeOrNextSyllable.syllable.start
        val gapDuration = gapEndTime - gapStartTime

        val pixelStart = (prevSyllable.position.x + prevSyllable.size.width) - lineStartX
        val pixelEnd = activeOrNextSyllable.position.x - lineStartX

        if (gapDuration <= 0) return pixelEnd

        val gapProgress = (currentTimeMs - gapStartTime).toFloat() / gapDuration
        return pixelStart + (pixelEnd - pixelStart) * gapProgress
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
    normalTextStyle: TextStyle = TextStyle(
        fontSize = 34.sp,
        fontWeight = FontWeight.Bold,
        fontFamily = SFPro,
        textMotion = TextMotion.Animated,
    ),
    accompanimentTextStyle: TextStyle = TextStyle(
        fontSize = 16.sp,
        fontWeight = FontWeight.Bold,
        fontFamily = SFPro,
        textMotion = TextMotion.Animated,
    )
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
            .combinedClickable(
                onClick = { onLineClicked(line) },
                onLongClick = { onLinePressed(line) })

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
                        accompanimentTextStyle
                    } else {
                        normalTextStyle
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
                    val lineHeight = textMeasurer.measure("M", textStyle).size.height
                    lineHeight * wrappedLines.size
                }


                Canvas(
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


