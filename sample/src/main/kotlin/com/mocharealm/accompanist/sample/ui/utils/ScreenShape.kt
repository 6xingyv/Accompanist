package com.mocharealm.accompanist.sample.ui.utils

import android.os.Build
import android.view.RoundedCorner
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp

data class ScreenCornerData(
    val topLeft: RoundedCorner?,
    val topRight: RoundedCorner?,
    val bottomLeft: RoundedCorner?,
    val bottomRight: RoundedCorner?
)

data class ScreenCornerDataDp(
    val topLeft: Dp,
    val topRight:Dp,
    val bottomLeft: Dp,
    val bottomRight: Dp
)

@Composable
fun rememberScreenCornerShape(): Shape {
    val view = LocalView.current

    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) {
        return RectangleShape
    }

    val cornerShape by remember(view, view.rootWindowInsets) {
        val insets = view.rootWindowInsets
        val cornerData = if (insets != null) {
            ScreenCornerData(
                topLeft = insets.getRoundedCorner(RoundedCorner.POSITION_TOP_LEFT),
                topRight = insets.getRoundedCorner(RoundedCorner.POSITION_TOP_RIGHT),
                bottomLeft = insets.getRoundedCorner(RoundedCorner.POSITION_BOTTOM_LEFT),
                bottomRight = insets.getRoundedCorner(RoundedCorner.POSITION_BOTTOM_RIGHT)
            )
        } else {
            // 如果无法获取 insets，则使用空数据
            ScreenCornerData(null, null, null, null)
        }

        // 如果所有角都是null（例如在没有圆角的屏幕上），则直接返回矩形以优化性能。
        if (cornerData.topLeft == null && cornerData.topRight == null && cornerData.bottomLeft == null && cornerData.bottomRight == null) {
            lazy { RectangleShape }
        } else {
            lazy { ScreenCornerShape(cornerData) }
        }
    }

    return cornerShape
}

class ScreenCornerShape(private val cornerData: ScreenCornerData) : Shape {

    override fun createOutline(
        size: Size,
        layoutDirection: LayoutDirection,
        density: Density
    ): Outline {
        val path = Path().apply {
            val width = size.width
            val height = size.height

            // 右上角
            cornerData.topRight?.let { corner ->
                val radius = corner.radius.toFloat()
                moveTo(width - radius, 0f)
                arcTo(
                    rect = Rect(left = width - 2 * radius, top = 0f, right = width, bottom = 2 * radius),
                    startAngleDegrees = -90f,
                    sweepAngleDegrees = 90f,
                    forceMoveTo = false
                )
            } ?: run {
                moveTo(width, 0f)
                lineTo(width, 0f)
            }
            lineTo(width, 0f)

            // 右下角
            cornerData.bottomRight?.let { corner ->
                val radius = corner.radius.toFloat()
                lineTo(width, height - radius)
                arcTo(
                    rect = Rect(left = width - 2 * radius, top = height - 2 * radius, right = width, bottom = height),
                    startAngleDegrees = 0f,
                    sweepAngleDegrees = 90f,
                    forceMoveTo = false
                )
            } ?: run {
                lineTo(width, height)
            }
            lineTo(width, height)


            // 左下角
            cornerData.bottomLeft?.let { corner ->
                val radius = corner.radius.toFloat()
                lineTo(radius, height)
                arcTo(
                    rect = Rect(left = 0f, top = height - 2 * radius, right = 2 * radius, bottom = height),
                    startAngleDegrees = 90f,
                    sweepAngleDegrees = 90f,
                    forceMoveTo = false
                )
            } ?: run {
                lineTo(0f, height)
            }
            lineTo(0f, height)


            // 左上角
            cornerData.topLeft?.let { corner ->
                val radius = corner.radius.toFloat()
                lineTo(0f, radius)
                arcTo(
                    rect = Rect(left = 0f, top = 0f, right = 2 * radius, bottom = 2 * radius),
                    startAngleDegrees = 180f,
                    sweepAngleDegrees = 90f,
                    forceMoveTo = false
                )
            } ?: run {
                lineTo(0f, 0f)
            }
            lineTo(0f, 0f)

            close()
        }
        return Outline.Generic(path)
    }
}

@Composable
fun rememberScreenCornerDataDp(): ScreenCornerDataDp {
    val view = LocalView.current
    val density = LocalDensity.current

    return remember(view, view.rootWindowInsets, density) {
        val insets = view.rootWindowInsets ?: return@remember ScreenCornerDataDp(0.dp, 0.dp, 0.dp, 0.dp)

        // 分别获取四个角的半径（像素）
        val topLeftRadiusPx = insets.getRoundedCorner(RoundedCorner.POSITION_TOP_LEFT)?.radius ?: 0
        val topRightRadiusPx = insets.getRoundedCorner(RoundedCorner.POSITION_TOP_RIGHT)?.radius ?: 0
        val bottomLeftRadiusPx = insets.getRoundedCorner(RoundedCorner.POSITION_BOTTOM_LEFT)?.radius ?: 0
        val bottomRightRadiusPx = insets.getRoundedCorner(RoundedCorner.POSITION_BOTTOM_RIGHT)?.radius ?: 0

        // 将像素转换为 Dp
        val topLeftDp = with(density) { topLeftRadiusPx.toDp() }
        val topRightDp = with(density) { topRightRadiusPx.toDp() }
        val bottomLeftDp = with(density) { bottomLeftRadiusPx.toDp() }
        val bottomRightDp = with(density) { bottomRightRadiusPx.toDp() }

        // 创建 RoundedCornerShape
        ScreenCornerDataDp(
            topLeft = topLeftDp,
            topRight = topRightDp,
            bottomLeft = bottomLeftDp,
            bottomRight = bottomRightDp
        )
    }
}