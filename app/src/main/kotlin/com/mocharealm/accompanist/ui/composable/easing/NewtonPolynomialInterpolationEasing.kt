package com.mocharealm.accompanist.ui.composable.easing

import androidx.compose.animation.core.Easing

class NewtonPolynomialInterpolationEasing(points: List<Pair<Double, Double>>): Easing {

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
        val yValues = points.map { it.second }.toMutableList()

        // 计算差商表
        val coeffs = mutableListOf<Double>()
        for (i in 0 until n) {
            coeffs.add(yValues[i])
            for (j in (i + 1) until n) {
                yValues[j] = (yValues[j] - yValues[j - 1]) / (xValues[j] - xValues[j - i - 1])
            }
        }
        dividedDifferences = coeffs
    }

    override fun transform(fraction: Float): Float {
        val n = xValues.size - 1
        var result = dividedDifferences[n]
        for (i in (n - 1) downTo 0) {
            result = result * (fraction - xValues[i]) + dividedDifferences[i]
        }
        return result.toFloat()
    }
}

val DipAndRise = NewtonPolynomialInterpolationEasing(
    0.0 to 0.0,      // (输入=0，输出=0)
    0.5 to -0.5,    // (输入=0.5，输出=-0.25)
    1.0 to 1.0       // (输入=1.0，输出=1.0)

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