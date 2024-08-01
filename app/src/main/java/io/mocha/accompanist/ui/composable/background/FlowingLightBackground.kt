package io.mocha.accompanist.ui.composable.background

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.BlurredEdgeTreatment
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp

@Stable
@Composable
fun FlowingLightBackground(
    bitmap: ImageBitmap,
    modifier: Modifier = Modifier
) {
    Box(modifier.fillMaxSize()) {
        val baseModifier = Modifier.scale(3f)

        Image(
            bitmap = bitmap,
            contentDescription = "",
            modifier = baseModifier
                .align(Alignment.TopStart)
                .blur(30.dp, BlurredEdgeTreatment.Unbounded) // Reduced blur
        )
        Image(
            bitmap = bitmap,
            contentDescription = "",
            modifier = baseModifier
                .align(Alignment.Center)
                .blur(60.dp, BlurredEdgeTreatment.Unbounded) // Reduced blur
        )
        Image(
            bitmap = bitmap,
            contentDescription = "",
            modifier = baseModifier
                .align(Alignment.BottomStart)
                .blur(50.dp, BlurredEdgeTreatment.Unbounded) // Reduced blur
                .scale(2f) // Specific scale for this Image
        )
    }
}

@Preview
@Composable
private fun FlowingLightBackgroundPrev() {
    // Add preview content here if needed
}
