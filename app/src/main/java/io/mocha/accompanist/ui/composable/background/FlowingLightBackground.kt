package io.mocha.accompanist.ui.composable.background

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.BiasAlignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.BlurredEdgeTreatment
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp

@Composable
fun FlowingLightBackground(
    bitmap: ImageBitmap,
    modifier: Modifier = Modifier
) {
    Box(Modifier.fillMaxSize()) {
        val rotation = remember {
            Animatable(0f)
        }
        LaunchedEffect(Unit) {
            rotation.animateTo(
                targetValue = 360f,
                animationSpec = infiniteRepeatable(
                    animation = tween(
                        durationMillis = 100000, // Duration of one full rotation
                        easing = LinearEasing // Linear easing for constant speed
                    ),
                    repeatMode = RepeatMode.Restart
                )
            )
        }
        Image(
            bitmap = bitmap,
            contentDescription = "",
            modifier = Modifier
                .align(Alignment.TopStart)
                .blur(50.dp, BlurredEdgeTreatment.Unbounded)
                .rotate(rotation.value)
                .scale(3f)
                //
        )
        Image(
            bitmap = bitmap,
            contentDescription = "",
            modifier = Modifier
                .align(Alignment.Center)
                .blur(120.dp, BlurredEdgeTreatment.Unbounded)
                .rotate(rotation.value+120)
                .scale(3f)
            //
        )
        Image(
            bitmap = bitmap,
            contentDescription = "",
            modifier = Modifier
                .align(Alignment.BottomStart)
                .blur(100.dp, BlurredEdgeTreatment.Unbounded)
                .rotate(-rotation.value+200f)
                .scale(2f)
                //
        )
    }
}

@Preview
@Composable
private fun FlowingLightBackgroundPrev() {

}
