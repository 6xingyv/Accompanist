package io.mocha.accompanist.ui.composable.lyrics

import androidx.compose.animation.core.animateDpAsState
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
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.BlurredEdgeTreatment
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
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
import io.mocha.accompanist.data.model.playback.LyricsState
import io.mocha.accompanist.ui.theme.SFPro
import kotlin.math.absoluteValue

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun KaraokeLineText(
    lyricsState: LyricsState,
    line: KaraokeLine,
    onLineClicked: (ISyncedLine) -> Unit,
    currentLineIndex: Int,
    modifier: Modifier = Modifier,
) {
    val isFocused by remember(lyricsState.firstFocusedLine) {
        derivedStateOf {
            line.isFocused(lyricsState.current())
        }
    }
    val scaleAnimation by animateFloatAsState(
        targetValue = if (isFocused) 1f else 0.95f,
        tween(400)
    )
    val alphaAnimation by animateFloatAsState(
        targetValue =
        if (!line.isAccompaniment)
            if (isFocused) 1f
            else 0.6f
        else
            if (isFocused) 0.6f
            else 0.3f,
        tween(400)
    )

    val blurAnimation by animateDpAsState(
        targetValue =
        if (isFocused or lyricsState.lazyListState.isScrollInProgress) 0.dp
        else (lyricsState.firstFocusedLine() - currentLineIndex).absoluteValue.dp
    )

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
                    transformOrigin =
                        TransformOrigin(if (line.alignment == Alignment.TopStart) 0f else 1f, 1f)
                    compositingStrategy = CompositingStrategy.ModulateAlpha
                }
                .blur(blurAnimation, BlurredEdgeTreatment.Unbounded),
            verticalArrangement = Arrangement.spacedBy(2.dp),
            horizontalAlignment = if (line.alignment == Alignment.TopStart) Alignment.Start else Alignment.End
        ) {
            val textMeasurer = rememberTextMeasurer()
            FlowRow(horizontalArrangement = if (line.alignment == Alignment.TopStart) Arrangement.Start else Arrangement.End) {
                line.syllables.forEach { syllable ->
                    KaraokeSyllableText(
                        lyricsState = lyricsState,
                        syllable = syllable,
                        style = if (line.isAccompaniment) TextStyle(
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = SFPro
                        ) else TextStyle(
                            fontSize = 32.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = SFPro
                        ),
                        textMeasurer = textMeasurer
                    )
                }
            }
            line.translation?.let {
                val measurer = rememberTextMeasurer()
                val result = measurer.measure(it)
                val color = LocalContentColor.current.copy(0.6f)
                Canvas(
                    modifier = Modifier.size(result.size.toDpSize())
                ) {
                    drawText(result, color, blendMode = BlendMode.Plus)
                }
            }
        }
    }
}
