package com.mocharealm.accompanist.sample.ui.composable

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Easing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.SubcomposeLayout
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import kotlin.math.roundToInt

@Composable
fun Marquee(
    modifier: Modifier = Modifier,
    params: MarqueeParams = defaultMarqueeParams,
    content: @Composable () -> Unit
) {
    var textWidth by remember { mutableIntStateOf(0) }
    var containerWidth by remember { mutableIntStateOf(0) }
    val animatable = remember { Animatable(0f) }

    val density = LocalDensity.current
    val ltr = LocalLayoutDirection.current == LayoutDirection.Ltr

    val needsMarquee = remember(textWidth, containerWidth) {
        textWidth > containerWidth
    }

    LaunchedEffect(needsMarquee, params, textWidth, containerWidth) {
        if (!needsMarquee) {
            animatable.snapTo(0f) // 不需要滚动则重置动画
            return@LaunchedEffect
        }

        // 动画持续时间与内容宽度成正比，保证滚动速度恒定
        val durationMillis = params.period * textWidth / containerWidth

        animatable.animateTo(
            targetValue = 1f,
            animationSpec = infiniteRepeatable(
                animation = tween(
                    durationMillis = durationMillis,
                    easing = params.easing,
                    delayMillis = params.waitTimeMillis.toInt()
                ),
                repeatMode = RepeatMode.Restart
            )
        )
    }

    SubcomposeLayout(
        modifier = modifier
            .clipToBounds()
            .drawWithCache {
                val gradientWidthPx = params.gradientWidth.toPx()
                // 仅在需要滚动时才应用渐变效果
                val gradientBrush = if (params.gradientEnabled && needsMarquee) {
                    val fadeStop = (gradientWidthPx / size.width).coerceAtMost(0.5f)
                    Brush.horizontalGradient(
                        0f to Color.Transparent,
                        fadeStop to Color.Black,
                        1f - fadeStop to Color.Black,
                        1f to Color.Transparent
                    )
                } else {
                    null
                }

                onDrawWithContent {
                    drawContent()
                    gradientBrush?.let {
                        drawRect(brush = it, blendMode = BlendMode.DstIn)
                    }
                }
            }
    ) { constraints ->
        val mainPlaceable = subcompose(MarqueeLayers.Main, content)
            .first()
            .measure(constraints.copy(maxWidth = Int.MAX_VALUE))

        textWidth = mainPlaceable.width
        containerWidth = constraints.maxWidth

        val gradientWidthPx = with(density) { params.gradientWidth.toPx() }
        if (!needsMarquee) {
            // 如果内容宽度小于等于容器宽度，则不需要滚动，直接放置
            val nonMarqueePlaceable = subcompose(MarqueeLayers.Secondary, content)
                .first()
                .measure(constraints)
            layout(nonMarqueePlaceable.width + gradientWidthPx.roundToInt(), nonMarqueePlaceable.height) {
                nonMarqueePlaceable.placeRelative(gradientWidthPx.roundToInt(), 0)
            }
        } else {
            // 需要滚动时的布局逻辑
            val spacingPx = with(density) { params.spacing.toPx() }
            val totalScrollDistance = textWidth + spacingPx

            layout(containerWidth, mainPlaceable.height) {
                // 动画驱动的基础偏移量，从 0 滚动到 -totalScrollDistance
                val baseOffset = -totalScrollDistance * animatable.value

                // 核心改动：在基础偏移量上，增加一个初始内边距，其值为渐变区域的宽度
                // 这使得文本在等待时（animatable.value = 0）就处于安全位置
                val startPadding = if (ltr) gradientWidthPx else -gradientWidthPx
                val xOffset = startPadding + baseOffset

                // 放置第一个可滚动内容
                mainPlaceable.placeRelative(xOffset.roundToInt(), 0)

                // 放置第二个内容，以实现无缝连接
                val secondPlaceable = subcompose(MarqueeLayers.Secondary, content)
                    .first()
                    .measure(constraints.copy(maxWidth = Int.MAX_VALUE))
                secondPlaceable.placeRelative((xOffset + totalScrollDistance).roundToInt(), 0)
            }
        }
    }
}


/**
 * Marquee 效果的配置参数.
 *
 * @param period 滚动一个容器宽度的基准时间，滚动速度与之成反比.
 * @param gradientEnabled 是否启用边缘渐变效果.
 * @param gradientWidth 渐变区域的宽度.
 * @param easing 动画缓动函数.
 * @param waitTimeMillis 每次滚动循环开始前的等待时间.
 * @param spacing 两个滚动内容之间的间距.
 */
data class MarqueeParams(
    val period: Int,
    val gradientEnabled: Boolean,
    val gradientWidth: Dp,
    val easing: Easing,
    val waitTimeMillis: Long,
    val spacing: Dp
)

val defaultMarqueeParams = MarqueeParams(
    period = 10000,
    gradientEnabled = true,
    gradientWidth = 16.dp,
    easing = LinearEasing,
    waitTimeMillis = 1500,
    spacing = 40.dp
)

private enum class MarqueeLayers {
    Main,
    Secondary
}