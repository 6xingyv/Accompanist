package com.mocharealm.accompanist.ui.composable

import androidx.compose.animation.core.AnimationSpec
import androidx.compose.animation.core.animate
import androidx.compose.animation.core.tween
import androidx.compose.foundation.OverscrollEffect
import androidx.compose.foundation.gestures.FlingBehavior
import androidx.compose.foundation.gestures.ScrollableDefaults
import androidx.compose.foundation.gestures.animateScrollBy
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyItemScope
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberOverscrollEffect
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.pow

// 类型别名和内部状态类
private typealias PhysicsParams = Triple<Float, Float, Float> // stiffness, damping, drag

class SpringItemState {
    var position by mutableFloatStateOf(0f)
    var velocity by mutableFloatStateOf(0f)
}

/**
 * SpringLazyColumn 的状态持有者。
 * 它包含了底层的 LazyListState，并暴露了与物理动画交互的API。
 *
 * @param lazyListState 内部持有的标准 LazyListState。
 * @param coroutineScope 用于启动动画协程的范围。
 */
@Stable
class SpringLazyListState(
    val lazyListState: LazyListState,
    private val coroutineScope: CoroutineScope
) {
    internal val itemStates = mutableStateListOf<SpringItemState>()

    val isScrollInProgress: Boolean
        get() = lazyListState.isScrollInProgress

    /**
     * "拨动"列表中的某一项。此方法会【并发】启动滚动动画和物理冲击动画。
     *
     * @param itemIndex 要拨动的项在核心列表中的索引。
     * @param targetVelocity 最终要达到的目标速度增量。
     * @param scrollAnimationSpec (可选) 定义滚动动画的曲线。如果为null或目标项不可见，则使用默认滚动。
     * @param accelerationSpec (可选) 定义速度如何从0加速到目标值的动画曲线。
     * @param offset (可选) 滚动完成后，目标项距离视口顶部的最终像素偏移量。
     * @param propagationDepth (可选) 速度传播的深度。
     * @param propagationFalloff (可选) 速度传播的衰减率。
     */
    fun pluck(
        itemIndex: Int,
        targetVelocity: Float,
        scrollAnimationSpec: AnimationSpec<Float>? = null,
        accelerationSpec: AnimationSpec<Float> = tween(300),
        offset: Int = 0,
        propagationDepth: Int = 3,
        propagationFalloff: Float = 0.4f
    ) {
        coroutineScope.launch {
            val targetItemInfo =
                lazyListState.layoutInfo.visibleItemsInfo.find { it.index == itemIndex }

            if (scrollAnimationSpec != null && targetItemInfo != null) {
                lazyListState.animateScrollBy(
                    value = (targetItemInfo.offset + offset+900).toFloat(),
                    animationSpec = scrollAnimationSpec
                )
            } else {
                lazyListState.animateScrollToItem(itemIndex, offset+300)
            }
        }

        coroutineScope.launch {
            if (itemIndex in itemStates.indices) {
                for (i in 0 until propagationDepth) {
                    val currentIndex = itemIndex + i
                    if (currentIndex in itemStates.indices) {
                        val velocityToReach = targetVelocity * (propagationFalloff.pow(i))

                        launch {
                            val itemState = itemStates[currentIndex]
                            val startVelocity = itemState.velocity
                            val finalVelocity = startVelocity + velocityToReach

                            animate(
                                initialValue = startVelocity,
                                targetValue = finalVelocity,
                                animationSpec = accelerationSpec
                            ) { value, _ ->
                                itemState.velocity = value
                            }
                        }
                    } else {
                        break
                    }
                }
            }
        }
    }

    internal fun syncItemCount(count: Int) {
        val currentSize = itemStates.size
        if (count > currentSize) {
            val diff = count - currentSize
            itemStates.addAll(List(diff) { SpringItemState() })
        } else if (count < currentSize) {
            itemStates.removeRange(count, currentSize)
        }
    }
}

/**
 * 创建并记住一个 SpringLazyListState。
 * @return 一个新的 SpringLazyListState 实例。
 */
@Composable
fun rememberSpringLazyListState(): SpringLazyListState {
    val lazyListState = rememberLazyListState()
    val scope = rememberCoroutineScope()
    return remember {
        SpringLazyListState(lazyListState, scope)
    }
}

@Composable
fun <T> SpringLazyColumn(
    items: List<T>,
    itemContent: @Composable LazyItemScope.(index: Int, item: T) -> Unit,
    modifier: Modifier = Modifier,
    state: SpringLazyListState = rememberSpringLazyListState(),
    key: ((item: T) -> Any)? = null,
    prefixContent: (LazyListScope.() -> Unit)? = null,
    suffixContent: (LazyListScope.() -> Unit)? = null,
    livelyParams: PhysicsParams = Triple(500f, 4f, 2f),
    crispParams: PhysicsParams = Triple(500f, 45f, 25f),
    velocityThreshold: Float = 80f,
    contentPadding: PaddingValues = PaddingValues(0.dp),
    reverseLayout: Boolean = false,
    verticalArrangement: Arrangement.Vertical = if (!reverseLayout) Arrangement.Top else Arrangement.Bottom,
    horizontalAlignment: Alignment.Horizontal = Alignment.Start,
    flingBehavior: FlingBehavior = ScrollableDefaults.flingBehavior(),
    userScrollEnabled: Boolean = true,
    overscrollEffect: OverscrollEffect? = rememberOverscrollEffect()
) {
    LaunchedEffect(items.size) {
        state.syncItemCount(items.size)
    }

    fun getParamsForVelocity(velocity: Float): PhysicsParams {
        return if (abs(velocity) > velocityThreshold) livelyParams else crispParams
    }

    LaunchedEffect(state.itemStates, livelyParams, crispParams, velocityThreshold) {
        var lastFrameTimeNanos = -1L
        val forces = FloatArray(state.itemStates.size)

        while (true) {
            withFrameNanos { frameTimeNanos ->
                if (lastFrameTimeNanos > 0) {
                    val deltaTime = (frameTimeNanos - lastFrameTimeNanos) / 1_000_000_000f

                    if (forces.size != state.itemStates.size) {
                        lastFrameTimeNanos = frameTimeNanos
                    }

                    for (i in state.itemStates.indices) {
                        val itemState = state.itemStates[i]
                        var totalForce = 0f
                        val (stiffness, damping, drag) = getParamsForVelocity(itemState.velocity)
                        if (i > 0) {
                            val neighborState = state.itemStates[i - 1]
                            val (neighborStiffness, _, _) = getParamsForVelocity(neighborState.velocity)
                            val effectiveStiffness = (stiffness + neighborStiffness) / 2f
                            totalForce += effectiveStiffness * (neighborState.position - itemState.position)
                        }
                        if (i < state.itemStates.size - 1) {
                            val neighborState = state.itemStates[i + 1]
                            val (neighborStiffness, _, _) = getParamsForVelocity(neighborState.velocity)
                            val effectiveStiffness = (stiffness + neighborStiffness) / 2f
                            totalForce += effectiveStiffness * (neighborState.position - itemState.position)
                        }
                        totalForce += -drag * itemState.position
                        totalForce += -damping * itemState.velocity
                        forces[i] = totalForce
                    }

                    // --- 阶段二: 应用所有力来更新状态 ---
                    for (i in state.itemStates.indices) {
                        val itemState = state.itemStates[i]
                        val acceleration = forces[i]
                        itemState.velocity += acceleration * deltaTime
                        itemState.position += itemState.velocity * deltaTime
                    }
                }
                lastFrameTimeNanos = frameTimeNanos
            }
        }
    }

    LazyColumn(
        modifier = modifier,
        state = state.lazyListState,
        contentPadding = contentPadding,
        reverseLayout = reverseLayout,
        verticalArrangement = verticalArrangement,
        horizontalAlignment = horizontalAlignment,
        flingBehavior = flingBehavior,
        userScrollEnabled = userScrollEnabled,
        overscrollEffect = overscrollEffect
    ) {
        prefixContent?.invoke(this)
        itemsIndexed(
            items = items,
            key = if (key != null) { _, item -> key(item) } else null) { index, item ->
            val springState = state.itemStates.getOrNull(index)

            if (springState != null) {
                Box(
                    modifier = Modifier.graphicsLayer {
                        translationY = springState.position
                    }) {
                    this@itemsIndexed.itemContent(index, item)
                }
            } else {
                this@itemsIndexed.itemContent(index, item)
            }
        }
        suffixContent?.invoke(this)
    }
}