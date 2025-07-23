// FlowingLightBackground.kt
package com.mocharealm.accompanist.ui.composable.background

import android.graphics.Color as AndroidColor
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.android.awaitFrame
import kotlinx.coroutines.withContext

// ✨ 1. 定义一个新的数据类，用来捆绑所有视觉状态
@Stable
data class BackgroundVisualState(
    val bitmap: ImageBitmap,
    val isBright: Boolean
)

@Composable
fun FlowingLightBackground(
    state: BackgroundVisualState,
    modifier: Modifier = Modifier
) {
    Crossfade(
        targetState = state,
        animationSpec = tween(durationMillis = 1500),
        label = "background_visual_state_crossfade",
        modifier = modifier.fillMaxSize()
    ) { currentState ->
        val colorFilter = if (currentState.isBright) {
            ColorFilter.tint(Color.Black.copy(alpha = 0.3f), BlendMode.Darken)
        } else {
            null
        }

        Box(modifier = Modifier.fillMaxSize()) {
            val baseModifier = Modifier.scale(3f)

//            var rotation1 by remember { mutableFloatStateOf(0f) }
//            LaunchedEffect(Unit) {
//                while (true) { rotation1 += 0.3f; awaitFrame() }
//            }
            Image(
                bitmap = currentState.bitmap, // 使用 currentState 中的 bitmap
                contentDescription = "",
                colorFilter = colorFilter,
                modifier = baseModifier
                    .align(Alignment.TopStart)
                    .blur(30.dp, BlurredEdgeTreatment.Unbounded)
//                    .graphicsLayer { rotationZ = rotation1 }
            )

//            var rotation2 by remember { mutableFloatStateOf(0f) }
//            LaunchedEffect(Unit) {
//                while (true) { rotation2 -= 0.2f; awaitFrame() }
//            }
            Image(
                bitmap = currentState.bitmap,
                contentDescription = "",
                colorFilter = colorFilter,
                modifier = baseModifier
                    .align(Alignment.Center)
                    .blur(30.dp, BlurredEdgeTreatment.Unbounded)
//                    .graphicsLayer { rotationZ = rotation2 }
            )

//            var rotation3 by remember { mutableFloatStateOf(0f) }
//            LaunchedEffect(Unit) {
//                while (true) { rotation3 += 0.1f; awaitFrame() }
//            }
            Image(
                bitmap = currentState.bitmap,
                contentDescription = "",
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

suspend fun calculateAverageBrightness(bitmap: ImageBitmap): Float {
    return withContext(Dispatchers.Default) {
        // ... 函数内容保持不变 ...
        if (bitmap.width <= 0 || bitmap.height <= 0) return@withContext 0f
        val androidBitmap = bitmap.asAndroidBitmap()
        val width = androidBitmap.width
        val height = androidBitmap.height
        var totalLuminance = 0.0
        var sampledPixelCount = 0
        val step = 10
        for (x in 0 until width step step) {
            for (y in 0 until height step step) {
                val pixel = androidBitmap.getPixel(x, y)
                val luminance = (0.299 * AndroidColor.red(pixel) + 0.587 * AndroidColor.green(pixel) + 0.114 * AndroidColor.blue(pixel))
                totalLuminance += luminance
                sampledPixelCount++
            }
        }
        if (sampledPixelCount == 0) return@withContext 0f
        val averageLuminance = totalLuminance / sampledPixelCount
        (averageLuminance / 255.0).toFloat()
    }
}