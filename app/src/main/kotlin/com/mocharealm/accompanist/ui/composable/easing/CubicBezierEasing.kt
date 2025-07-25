package com.mocharealm.accompanist.ui.composable.easing

import androidx.compose.animation.core.Easing
import kotlin.math.abs

/**
 * 一个实现了 cubic-bezier(x1, y1, x2, y2) 功能的 Easing 类。
 * 这使得它可以直接在 Jetpack Compose 的动画中使用。
 *
 * @param x1 P1 控制点的 x 坐标。
 * @param y1 P1 控制点的 y 坐标。
 * @param x2 P2 控制点的 x 坐标。
 * @param y2 P2 控制点的 y 坐标。
 */
class CubicBezierEasing(
    private val x1: Float,
    private val y1: Float,
    private val x2: Float,
    private val y2: Float
) : Easing {

    // 预计算 X 和 Y 的贝塞尔曲线系数
    private val cx = 3.0f * x1
    private val bx = 3.0f * (x2 - x1) - cx
    private val ax = 1.0f - cx - bx

    private val cy = 3.0f * y1
    private val by = 3.0f * (y2 - y1) - cy
    private val ay = 1.0f - cy - by

    /**
     * Easing 接口的核心方法，将输入的线性进度 (fraction) 转换为缓动后的进度。
     */
    override fun transform(fraction: Float): Float {
        // 对于线性曲线，直接返回原始进度
        if (fraction == 0f || fraction == 1f || (x1 == y1 && x2 == y2)) {
            return fraction
        }
        // 对于其他情况，计算 Y 值
        return sampleCurveY(solveTForX(fraction))
    }

    private fun sampleCurveY(t: Float): Float {
        return ((ay * t + by) * t + cy) * t
    }

    private fun sampleCurveX(t: Float): Float {
        return ((ax * t + bx) * t + cx) * t
    }

    private fun sampleCurveDerivativeX(t: Float): Float {
        return (3.0f * ax * t + 2.0f * bx) * t + cx
    }

    private fun solveTForX(x: Float): Float {
        var t2 = x
        // 使用牛顿-拉弗森法迭代8次，足以满足动画的精度要求
        for (i in 0..7) {
            val x2 = sampleCurveX(t2) - x
            // 如果结果已经足够接近，则提前返回
            if (abs(x2) < 1e-6f) {
                return t2
            }
            val d2 = sampleCurveDerivativeX(t2)
            // 避免除以零
            if (abs(d2) < 1e-6f) {
                break
            }
            t2 -= x2 / d2
        }
        return t2
    }
}