package io.mocha.accompanist.ui.composable.lyrics

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.material3.LocalContentColor
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.sp
import io.mocha.accompanist.data.model.lyrics.karaoke.KaraokeSyllable
import io.mocha.accompanist.data.model.playback.PlaybackState
import io.mocha.accompanist.ui.theme.SFPro
import kotlin.math.pow

@Composable
fun textGradientBrush(
    progress: Float,
    activeColor: Color,
    inactiveColor: Color,
    fadeRange: Float
): Brush {
//    require(progress in 0f..1f)
    val fade = fadeRange / 2
    val brush by remember(progress) {
        derivedStateOf {
            when (progress) {
                0f -> Brush.horizontalGradient(0f to inactiveColor, 1f to inactiveColor)
                1f -> Brush.horizontalGradient(0f to activeColor, 1f to activeColor)
                else -> Brush.horizontalGradient(
                    0f to activeColor,
                    (progress - fade) to activeColor,
                    (progress + fade) to inactiveColor,
                    1f to inactiveColor
                )
            }
        }
    }
    return brush
}

@Composable
fun KaraokeSyllableText(
    playbackState: PlaybackState,
    syllable: KaraokeSyllable,
    modifier: Modifier = Modifier,
    activeColor: Color = LocalContentColor.current,
    inactiveColor: Color = activeColor.copy(0.2f),
    fadeRange: Float = 0.4f,
    style: TextStyle = TextStyle(fontSize = 32.sp, fontWeight = FontWeight.Bold, fontFamily = SFPro)
) {
    val progress by remember(playbackState.current) {
        derivedStateOf { syllable.progress(playbackState.current) }
    }
    val brush = textGradientBrush(progress = progress, activeColor, inactiveColor, fadeRange)

    val float by animateFloatAsState(targetValue = (1f - progress) * 8f, spring(Spring.DampingRatioMediumBouncy), label = "")

    val measurer = rememberTextMeasurer()
    val result = measurer.measure(syllable.content, style)
    Canvas(
        modifier = modifier
            .size(result.size.toDpSize())
            .graphicsLayer {
                translationY = float
                compositingStrategy = CompositingStrategy.ModulateAlpha
            }
    ) {
        drawText(result, brush, blendMode = BlendMode.Plus)
    }

}

@Composable
fun KaraokeAwesomeSyllableText(
    playbackState: PlaybackState,
    syllable: KaraokeSyllable, modifier: Modifier = Modifier,
    activeColor: Color = LocalContentColor.current,
    inactiveColor: Color = activeColor.copy(0.2f),
    fadeRange: Float = 0.4f,
    style: TextStyle = TextStyle(fontSize = 32.sp, fontWeight = FontWeight.Bold, fontFamily = SFPro)
) {
    val progress by remember(playbackState.current) {
        derivedStateOf { syllable.progress(playbackState.current) }
    }
    val brush = textGradientBrush(progress = progress, activeColor, inactiveColor, fadeRange)

    val float by animateFloatAsState(targetValue = (1f - progress) * 8f, label = "")

    val measurer = rememberTextMeasurer()
    val monoResult = measurer.measure(syllable.content, style)

    Canvas(
        modifier = Modifier
            .size(monoResult.size.toDpSize())
            .graphicsLayer {
                translationY = float
                compositingStrategy = CompositingStrategy.ModulateAlpha
            }
    ) {
        var pivot = 0f
        syllable.content.forEach {
            val result = measurer.measure(it.toString(), style)
            drawText(result, activeColor, Offset(pivot,0f),blendMode = BlendMode.Plus)
            pivot += result.size.width.toFloat()
        }
    }
}

@Composable
fun IntSize.toDpSize(): DpSize {
    val density = LocalDensity.current
    return with(density) {
        DpSize(width.toDp(), height.toDp())
    }
}