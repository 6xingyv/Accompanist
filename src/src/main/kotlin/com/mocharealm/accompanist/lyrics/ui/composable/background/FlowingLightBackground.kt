package com.mocharealm.accompanist.lyrics.ui.composable.background

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
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp

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
        val colorFilter = if (state.isBright) {
            ColorFilter.tint(Color.Black.copy(alpha = 0.1f), BlendMode.Darken)
        } else {
            null
        }
        Box(
            modifier = modifier
                .graphicsLayer { clip = true }) {
            val baseModifier = Modifier.scale(3f)

            Image(
                bitmap = state.bitmap!!,
                contentDescription = null,
                colorFilter = colorFilter,
                modifier = baseModifier
                    .align(Alignment.TopStart)
                    .blur(30.dp, BlurredEdgeTreatment.Unbounded)
//                    .graphicsLayer { rotationZ = rotation1 }
            )

            Image(
                bitmap = state.bitmap,
                contentDescription = null,
                colorFilter = colorFilter,
                modifier = baseModifier
                    .align(Alignment.Center)
                    .blur(30.dp, BlurredEdgeTreatment.Unbounded)
//                    .graphicsLayer { rotationZ = rotation2 }
            )

            Image(
                bitmap = state.bitmap,
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