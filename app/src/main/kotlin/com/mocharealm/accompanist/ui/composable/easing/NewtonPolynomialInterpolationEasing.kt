package com.mocharealm.accompanist.ui.composable.easing

import androidx.compose.animation.core.Easing

class NewtonPolynomialInterpolationEasing(points: List<Pair<Double, Double>>): Easing {

    // 辅助构造函数，允许使用 vararg 形式传入数据点
    constructor(vararg points: Pair<Double, Double>) : this(points.toList())

    private val dividedDifferences: List<Double>
    private val xValues: List<Double>

    init {
        // 确保没有重复的 x 值，否则无法插值
        require(points.map { it.first }.toSet().size == points.size) {
            "All x-coordinates of the points must be unique."
        }

        val n = points.size
        xValues = points.map { it.first }

        // --- 核心修正 ---
        // 使用一个二维数组来存储差商表，避免就地修改导致的错误
        val table = Array(n) { DoubleArray(n) }

        // 第一列是 y 值
        for (i in 0 until n) {
            table[i][0] = points[i].second
        }

        // 计算差商表的其余部分
        for (j in 1 until n) {
            for (i in j until n) {
                table[i][j] = (table[i][j - 1] - table[i - 1][j - 1]) / (xValues[i] - xValues[i - j])
            }
        }

        // 最终的系数是差商表的对角线
        dividedDifferences = List(n) { i -> table[i][i] }
    }

    /**
     * Easing 接口的核心方法，将输入的线性进度 (fraction) 转换为缓动后的进度。
     */
    override fun transform(fraction: Float): Float {
        val x = fraction.toDouble()
        val n = xValues.size - 1
        var result = dividedDifferences[n]

        // 使用霍纳法则进行高效计算
        for (i in (n - 1) downTo 0) {
            result = result * (x - xValues[i]) + dividedDifferences[i]
        }
        return result.toFloat()
    }
}

val DipAndRise = NewtonPolynomialInterpolationEasing(
    0.0 to 0.0,
    0.5 to -0.5,
    1.0 to 1.0
)

val Swell = NewtonPolynomialInterpolationEasing(
    0.0 to 0.0,
    0.7 to 0.02,
    1.0 to 0.0
)

val Bounce = NewtonPolynomialInterpolationEasing(
    0.0 to 0.0,
    0.6 to 1.0,
    1.0 to 0.0
)