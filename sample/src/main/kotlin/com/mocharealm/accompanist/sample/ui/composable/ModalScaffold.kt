package com.mocharealm.accompanist.sample.ui.composable

import androidx.activity.compose.PredictiveBackHandler
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.AnimationSpec
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.mocharealm.accompanist.lyrics.ui.utils.copyHsl
import com.mocharealm.accompanist.sample.ui.composable.utils.ScreenCornerDataDp
import com.mocharealm.accompanist.sample.ui.composable.utils.rememberScreenCornerDataDp
import kotlinx.coroutines.launch
import kotlin.coroutines.cancellation.CancellationException
import kotlin.math.roundToInt

@Composable
fun ModalScaffold(
    isModalOpen: Boolean,
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier,
    confirmDismiss: () -> Boolean = { true },
    screenCornerDataDp: ScreenCornerDataDp = rememberScreenCornerDataDp(),
    targetRadius: Dp = 16.dp,
    animationSpec: AnimationSpec<Float> = tween(durationMillis = 400),
    backgroundScale: Float =
        ((LocalConfiguration.current.screenHeightDp.dp / 2 - WindowInsets.statusBars.asPaddingValues().calculateTopPadding()) / (LocalConfiguration.current.screenHeightDp / 2)).value.coerceAtMost(0.95f),
    dismissThresholdFraction: Float = 0.5f,
    modalContent: @Composable (dragHandleModifier: Modifier) -> Unit,
    content: @Composable () -> Unit
) {
    val scope = rememberCoroutineScope()

    // 1. 使用 Animatable 来管理垂直偏移量
    val offsetY = remember { Animatable(0f) }
    var modalHeight by remember { mutableFloatStateOf(0f) }

    // 2. 监听 isModalOpen 的变化，驱动模态窗口的开合动画
    LaunchedEffect(isModalOpen, modalHeight) {
        if (modalHeight == 0f) return@LaunchedEffect
        val targetValue = if (isModalOpen) 0f else modalHeight
        // 如果当前位置和目标不一致，则播放动画
        if (offsetY.value != targetValue) {
            scope.launch {
                offsetY.animateTo(targetValue, animationSpec)
            }
        }
    }
    PredictiveBackHandler(enabled = isModalOpen) { progressFlow ->
        // 在开始处理手势前，先检查是否允许关闭
        if (!confirmDismiss()) {
            // 如果不允许，则直接退出，手势将不会被处理
            return@PredictiveBackHandler
        }

        try {
            // 当用户拖动返回手势时，收集进度事件
            progressFlow.collect { backEvent ->
                // 根据手势进度，立即更新 offsetY 的值
                // backEvent.progress 是一个从 0.0 到 1.0 的值
                offsetY.snapTo(backEvent.progress * modalHeight)
            }
            // 当 collect 正常完成时，意味着用户确认了返回手势
            onDismissRequest()
        } catch (_: CancellationException) {
            offsetY.animateTo(0f, animationSpec)
        }
    }

    // 3. 实时计算动画进度，用于驱动背景动画
    val progress = if (modalHeight > 0) {
        (offsetY.value / modalHeight).coerceIn(0f, 1f)
    } else {
        if (isModalOpen) 0f else 1f
    }

    val scale = lerp(backgroundScale, 1f, progress)
    val modalTopPadding = (
            (LocalConfiguration.current.screenHeightDp.dp * (1 - backgroundScale) / 2f) + 16.dp).coerceAtLeast(0.dp)

    val dimAlpha = lerp(0.4f, 0f, progress)

    val topLeftRadius = lerp(targetRadius, screenCornerDataDp.topLeft, progress)
    val topRightRadius = lerp(targetRadius, screenCornerDataDp.topRight, progress)
    val bottomLeftRadius = lerp(targetRadius, screenCornerDataDp.bottomLeft, progress)
    val bottomRightRadius = lerp(targetRadius, screenCornerDataDp.bottomRight, progress)

    val screenShape = if (progress!=1f) RoundedCornerShape(
        topStart = topLeftRadius,
        topEnd = topRightRadius,
        bottomStart = bottomLeftRadius,
        bottomEnd = bottomRightRadius
    ) else RectangleShape

    val modalShape = RoundedCornerShape(
        topStart = topLeftRadius,
        topEnd = topRightRadius,
    )

    // 4. 定义拖拽手势 Modifier
    val dragHandleModifier = Modifier.pointerInput(Unit) {
        detectVerticalDragGestures(
            onDragEnd = {
                scope.launch {
                    if (offsetY.value > modalHeight * dismissThresholdFraction) {
                        // 检查是否允许关闭
                        if (confirmDismiss()) {
                            offsetY.animateTo(modalHeight, animationSpec)
                            onDismissRequest() // 动画结束后回调
                        } else {
                            // 不允许关闭，弹回
                            offsetY.animateTo(0f, animationSpec)
                        }
                    } else {
                        // 未超过阈值，弹回
                        offsetY.animateTo(0f, animationSpec)
                    }
                }
            },
            onVerticalDrag = { change, dragAmount ->
                change.consume()
                scope.launch {
                    // 将拖动量应用到偏移量上，并限制不能向上拖出边界
                    val newOffset = (offsetY.value + dragAmount).coerceAtLeast(0f)
                    offsetY.snapTo(newOffset)
                }
            }
        )
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer {
                    scaleX = scale
                    scaleY = scale
                }
                .clip(screenShape)
        ) {
            content()
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = dimAlpha))
        )

        Box(
            modifier = Modifier
                .offset { IntOffset(0, offsetY.value.roundToInt()) }
                .alpha(if (progress != 1f) 1f else 0f)
                .padding(top = modalTopPadding)
                .fillMaxSize()
                .clip(modalShape)
                .background(if (isSystemInDarkTheme()) Color.Black.copyHsl(lightness = 0.15f) else Color.White)
                .onSizeChanged { size ->
                    // 仅在第一次获取到高度且模态窗口初始为关闭时，立即设置偏移量
                    if (modalHeight == 0f && !isModalOpen) {
                        scope.launch {
                            offsetY.snapTo(size.height.toFloat())
                        }
                    }
                    modalHeight = size.height.toFloat()
                },
        ) {
            modalContent(dragHandleModifier)
        }
    }

}



// 线性插值函数 (Linear Interpolation)
private fun lerp(start: Float, stop: Float, fraction: Float): Float {
    return (1 - fraction) * start + fraction * stop
}

private fun lerp(start: Dp, stop: Dp, fraction: Float): Dp {
    return Dp(lerp(start.value, stop.value, fraction))
}