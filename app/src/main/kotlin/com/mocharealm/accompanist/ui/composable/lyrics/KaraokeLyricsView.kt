package com.mocharealm.accompanist.ui.composable.lyrics

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.unit.dp
import com.mocharealm.accompanist.lyrics.model.ISyncedLine
import com.mocharealm.accompanist.lyrics.model.SyncedLyrics
import com.mocharealm.accompanist.lyrics.model.karaoke.KaraokeAlignment
import com.mocharealm.accompanist.lyrics.model.karaoke.KaraokeLine
import com.mocharealm.accompanist.lyrics.model.synced.SyncedLine

@Composable
fun KaraokeLyricsView(
    listState: LazyListState,
    lyrics: SyncedLyrics,
    currentPosition: Long,
    onLineClicked: (ISyncedLine) -> Unit,
    modifier: Modifier = Modifier
) {
    val currentTimeMs by rememberUpdatedState(currentPosition.toInt())

    val focusedLineIndex
           = lyrics.getCurrentFirstHighlightLineIndexByTime(currentTimeMs)

    val isDuoView by remember {
        derivedStateOf {
            var hasStart = false
            var hasEnd = false

            if (lyrics.lines.isEmpty()) {
                return@derivedStateOf false
            }

            for (line in lyrics.lines) {
                if (line is KaraokeLine) {
                    when (line.alignment) {
                        KaraokeAlignment.Start -> hasStart = true
                        KaraokeAlignment.End -> hasEnd = true
                        else -> {}
                    }
                }
                if (hasStart && hasEnd) {
                    break
                }
            }
            hasStart && hasEnd
        }
    }

    LaunchedEffect(focusedLineIndex) {
        if (focusedLineIndex >= 0 &&
            focusedLineIndex < lyrics.lines.size &&
            !listState.isScrollInProgress) {

            try {
                listState.animateScrollToItem(focusedLineIndex)
            } catch (e: Exception) { }
        }
    }

    LazyColumn(
        state = listState,
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(vertical = 100.dp)
    ) {
        itemsIndexed(
            items = lyrics.lines,
            key = { index, line -> "${line.start}-${line.end}-$index" }
        ) { index, line ->
            when(line) {
                is KaraokeLine ->  {
                    // Check if the line is the current focus line
                    val isCurrentFocusLine = remember(index, focusedLineIndex) {
                        index == focusedLineIndex ||
                                (line.isAccompaniment && kotlin.math.abs(index - focusedLineIndex) <= 1)
                    }

                    val lineTimeMs = if (isCurrentFocusLine) currentTimeMs else 0

                    if (!line.isAccompaniment) {
                        KaraokeLineText(
                            line = line,
                            onLineClicked = onLineClicked,
                            currentTimeMs = lineTimeMs,
                            modifier = Modifier.fillMaxWidth(if (isDuoView) 0.85f else 1f)
                        )
                    } else {
                        AnimatedVisibility(
                            visible = isCurrentFocusLine,
                            enter = scaleIn(
                                transformOrigin = TransformOrigin(
                                    if (line.alignment == KaraokeAlignment.Start) 0f else 1f,
                                    0f
                                ),
                                animationSpec = tween(300)
                            ) + slideInVertically(
                                animationSpec = tween(300)
                            ) + expandVertically(
                                animationSpec = tween(300)
                            ),
                            exit = scaleOut(
                                transformOrigin = TransformOrigin(
                                    if (line.alignment == KaraokeAlignment.Start) 0f else 1f,
                                    0f
                                ),
                                animationSpec = tween(300)
                            ) + slideOutVertically(
                                animationSpec = tween(300)
                            ) + shrinkVertically(
                                animationSpec = tween(300)
                            )
                        ) {
                            KaraokeLineText(
                                line = line,
                                onLineClicked = onLineClicked,
                                currentTimeMs = lineTimeMs,
                                Modifier.fillMaxWidth(if (isDuoView) 0.85f else 1f)
                            )
                        }
                    }
                }
                is SyncedLine -> {

                }
            }
            val nextLine = lyrics.lines.getOrNull(index + 1)
            val showDotInPause = remember(line, nextLine, currentPosition) {
                nextLine != null &&
                        (nextLine.start - line.end > 5000) &&
                        (currentPosition in line.end..nextLine.start)
            }

            val showDotInIntro = remember(line, currentPosition) {
                index == 0 &&
                        (line.start > 5000) &&
                        (currentPosition in 0 until line.start)
            }

            // 只要满足上述任一条件，就显示呼吸点
            AnimatedVisibility(visible = showDotInPause || showDotInIntro) {
                Text("Breathing dot")
            }
        }

        item("BottomSpacing") {
            Spacer(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(LocalConfiguration.current.screenHeightDp.dp)
            )
        }
    }
}