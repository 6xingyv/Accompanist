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
            // If insets cannot be obtained, use empty data
            ScreenCornerData(null, null, null, null)
        }

        // If all corners are null (e.g., on screens without rounded corners), return Rectangle directly for performance optimization
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

            // Top right corner
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

            // Bottom right corner
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


            // Bottom left corner
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


            // Top left corner
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

        // Get radius of each corner separately (in pixels)
        val topLeftRadiusPx = insets.getRoundedCorner(RoundedCorner.POSITION_TOP_LEFT)?.radius ?: 0
        val topRightRadiusPx = insets.getRoundedCorner(RoundedCorner.POSITION_TOP_RIGHT)?.radius ?: 0
        val bottomLeftRadiusPx = insets.getRoundedCorner(RoundedCorner.POSITION_BOTTOM_LEFT)?.radius ?: 0
        val bottomRightRadiusPx = insets.getRoundedCorner(RoundedCorner.POSITION_BOTTOM_RIGHT)?.radius ?: 0

        // Convert pixels to Dp
        val topLeftDp = with(density) { topLeftRadiusPx.toDp() }
        val topRightDp = with(density) { topRightRadiusPx.toDp() }
        val bottomLeftDp = with(density) { bottomLeftRadiusPx.toDp() }
        val bottomRightDp = with(density) { bottomRightRadiusPx.toDp() }

        // Create ScreenCornerDataDp
        ScreenCornerDataDp(
            topLeft = topLeftDp,
            topRight = topRightDp,
            bottomLeft = bottomLeftDp,
            bottomRight = bottomRightDp
        )
    }
}