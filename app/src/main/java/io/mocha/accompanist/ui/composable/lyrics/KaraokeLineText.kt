package io.mocha.accompanist.ui.composable.lyrics

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.LocalContentColor
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.mocha.accompanist.data.model.lyrics.ISyncedLine
import io.mocha.accompanist.data.model.lyrics.karaoke.KaraokeLine
import io.mocha.accompanist.data.model.playback.PlaybackState
import io.mocha.accompanist.ui.theme.SFPro

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun KaraokeLineText(
    playbackState: PlaybackState,
    line: KaraokeLine,
    onLineClicked:(ISyncedLine)->Unit,
    modifier: Modifier = Modifier
) {
    val scaleAnimation by animateFloatAsState(targetValue = if (line.isFocused(playbackState.current)) 1f else 0.95f,
        tween(400))
    val alphaAnimation by animateFloatAsState(targetValue =
    if (!line.isAccompaniment)
        if (line.isFocused(playbackState.current)) 1f
        else 0.6f
    else
        if (line.isFocused(playbackState.current)) 0.6f
        else 0.3f,
        tween(400))
    Box(Modifier.fillMaxWidth()) {
        Column(
            modifier
                .align(line.alignment)
                .clip(RoundedCornerShape(8.dp))
                .clickable { onLineClicked(line) }
                .padding(vertical = 8.dp, horizontal = 16.dp)
                .graphicsLayer {
                    scaleX = scaleAnimation
                    scaleY = scaleAnimation
                    alpha = alphaAnimation
                    transformOrigin =  TransformOrigin(if (line.alignment == Alignment.TopStart) 0f else 1f, 1f)
                    compositingStrategy = CompositingStrategy.ModulateAlpha
                },
            verticalArrangement = Arrangement.spacedBy(2.dp),
            horizontalAlignment = if (line.alignment == Alignment.TopStart) Alignment.Start else Alignment.End
        ) {
            FlowRow(horizontalArrangement = if (line.alignment == Alignment.TopStart) Arrangement.Start else Arrangement.End) {
                line.syllables.forEach { syllable ->
                    KaraokeSyllableText(playbackState = playbackState, syllable = syllable, style = if (line.isAccompaniment)  TextStyle(fontSize = 16.sp, fontWeight = FontWeight.Bold, fontFamily = SFPro) else  TextStyle(fontSize = 32.sp, fontWeight = FontWeight.Bold, fontFamily = SFPro))
                }
            }
            line.translation?.let {
                val measurer = rememberTextMeasurer()
                val result = measurer.measure(it)
                val color = LocalContentColor.current.copy(0.6f)
                Canvas(
                    modifier = Modifier
                        .size(result.size.toDpSize())
                ) {
                    drawText(result, color, blendMode = BlendMode.Plus)
                }
            }
        }
    }
}