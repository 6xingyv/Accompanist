// FlowingLightBackground.kt
package com.mocharealm.accompanist.ui.composable.background

import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.BlurredEdgeTreatment
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import androidx.core.graphics.get
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import android.graphics.Color as AndroidColor

@Stable
data class BackgroundVisualState(
    val bitmap: ImageBitmap?,
    val isBright: Boolean
)

@Composable
fun FlowingLightBackground(
    state: BackgroundVisualState,
    modifier: Modifier = Modifier
) {
    if (state.bitmap != null) {
        Crossfade(
            targetState = state,
            animationSpec = tween(durationMillis = 1500),
            label = "background_visual_state_crossfade",
        ) { currentState ->
            val colorFilter = if (currentState.isBright) {
                ColorFilter.tint(Color.Black.copy(alpha = 0.1f), BlendMode.Darken)
            } else {
                null
            }
            Box(modifier = modifier) {
                val baseModifier = Modifier.scale(3f)

                Image(
                    bitmap = currentState.bitmap!!,
                    contentDescription = null,
                    colorFilter = colorFilter,
                    modifier = baseModifier
                        .align(Alignment.TopStart)
                        .blur(30.dp, BlurredEdgeTreatment.Unbounded)
//                    .graphicsLayer { rotationZ = rotation1 }
                )

                Image(
                    bitmap = currentState.bitmap,
                    contentDescription = null,
                    colorFilter = colorFilter,
                    modifier = baseModifier
                        .align(Alignment.Center)
                        .blur(30.dp, BlurredEdgeTreatment.Unbounded)
//                    .graphicsLayer { rotationZ = rotation2 }
                )

                Image(
                    bitmap = currentState.bitmap,
                    contentDescription = null,
                    colorFilter = colorFilter,
                    modifier = baseModifier
                        .align(Alignment.BottomEnd)
                        .blur(50.dp, BlurredEdgeTreatment.Unbounded)
                        .scale(2f)
                        .graphicsLayer {
//                        rotationZ = rotation3
                            translationX = 50f
                        }
                )
            }
        }
    }
}

suspend fun calculateAverageBrightness(bitmap: ImageBitmap): Float {
    return withContext(Dispatchers.Default) {
        if (bitmap.width <= 0 || bitmap.height <= 0) return@withContext 0f
        val androidBitmap = bitmap.asAndroidBitmap()
        val width = androidBitmap.width
        val height = androidBitmap.height
        var totalLuminance = 0.0
        var sampledPixelCount = 0
        val step = 10
        for (x in 0 until width step step) {
            for (y in 0 until height step step) {
                val pixel = androidBitmap[x, y]
                val luminance =
                    (0.299 * AndroidColor.red(pixel) + 0.587 * AndroidColor.green(pixel) + 0.114 * AndroidColor.blue(
                        pixel
                    ))
                totalLuminance += luminance
                sampledPixelCount++
            }
        }
        if (sampledPixelCount == 0) return@withContext 0f
        val averageLuminance = totalLuminance / sampledPixelCount
        (averageLuminance / 255.0).toFloat()
    }
}