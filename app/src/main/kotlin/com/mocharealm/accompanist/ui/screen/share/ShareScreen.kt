package com.mocharealm.accompanist.ui.screen.share

import android.annotation.SuppressLint
import android.content.Intent
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextMotion
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.mocharealm.accompanist.lyrics.model.karaoke.KaraokeLine
import com.mocharealm.accompanist.ui.composable.background.FlowingLightBackground
import com.mocharealm.accompanist.ui.composable.utils.Capturable
import com.mocharealm.accompanist.ui.composable.utils.modifier.deviceRotation
import com.mocharealm.accompanist.ui.composable.utils.rememberCapturableController
import com.mocharealm.accompanist.ui.theme.SFPro
import kotlinx.coroutines.launch

fun KaraokeLine.content(): String = this.syllables.joinToString("") { it.content }

@SuppressLint("ModifierParameter")
@Composable
fun ShareScreen(
    dragModifier: Modifier,
    shareViewModel: ShareViewModel = viewModel()
) {
    val uiState by shareViewModel.uiState.collectAsStateWithLifecycle()
    val context = uiState.context ?: return // If context isn't ready, don't render anything

    Column(
        Modifier
            .navigationBarsPadding()
            .clickable( // Absorb clicks to prevent dismissing the modal by clicking content
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = {}
            )
    ) {
        Box(
            dragModifier
                .fillMaxWidth()
                .padding(16.dp),
        ) {
//            Text(
//                text = "Dismiss",
//                Modifier.align(Alignment.CenterStart)
//            )
            Text(
                text = "Share",
                Modifier.align(Alignment.Center),
                color = if (isSystemInDarkTheme()) Color.White else Color.Black
            )
        }
        Spacer(
            Modifier
                .fillMaxWidth()
                .height(0.5.dp)
                .background(
                    if (isSystemInDarkTheme()) Color.White.copy(0.05f) else Color.Black.copy(0.2f)
                )
        )
        when (uiState.step) {
            ShareStep.SELECTING -> {
                ShareSelectionStep(
                    context = context,
                    selectedLines = uiState.selectedLines,
                    onLineToggled = { shareViewModel.toggleLineSelection(it) },
                    onGenerateClicked = { shareViewModel.proceedToGenerate() }
                )
            }

            ShareStep.GENERATING -> {
                ShareGenerateStep(
                    context = context,
                    selectedLines = uiState.selectedLines,
                    onBackPressed = { shareViewModel.returnToSelection() },
                    shareViewModel = shareViewModel
                )
            }
        }
    }
}

@Composable
private fun ColumnScope.ShareSelectionStep(
    context: ShareContext,
    selectedLines: List<KaraokeLine>,
    onLineToggled: (KaraokeLine) -> Unit,
    onGenerateClicked: () -> Unit
) {
    val shareLazyState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()

    LaunchedEffect(selectedLines.first()) {
        coroutineScope.launch {
            val index = context.lyrics.lines.indexOf(selectedLines.first())
            if (index != -1) {
                shareLazyState.animateScrollToItem(index, -200)
            }
        }
    }
    LazyColumn(
        state = shareLazyState,
        modifier = Modifier
            .weight(1f)
            .background(
                if (isSystemInDarkTheme()) Color.White.copy(0.1f) else Color.Black.copy(0.02f)
            ),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(context.lyrics.lines, key = { it.start }) { line ->
            val karaokeLine = line as KaraokeLine
            val isSelected = selectedLines.contains(karaokeLine)
            val backgroundColor by animateColorAsState(
                if (isSelected) Color(0xFF3482FF).copy(0.2f)
                else if (selectedLines.size == 5) Color.Black.copy(0.2f)
                else if (isSystemInDarkTheme()) Color.White.copy(0.1f) else Color.White
            )
            Column(
                Modifier
                    .clip(
                        if (selectedLines.size > 1)
                            when {
                                selectedLines.first() == line -> RoundedCornerShape(
                                    16.dp,
                                    16.dp,
                                    8.dp,
                                    8.dp
                                )

                                selectedLines.last() == line -> RoundedCornerShape(
                                    8.dp,
                                    8.dp,
                                    16.dp,
                                    16.dp
                                )

                                else -> RoundedCornerShape(8.dp)
                            }
                        else RoundedCornerShape(16.dp)
                    )
                    .fillMaxWidth()
                    .clickable { onLineToggled(karaokeLine) }
                    .background(
                        backgroundColor
                    )
                    .padding(16.dp)
            ) {
                Text(
                    karaokeLine.content(),
                    style = TextStyle(
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = SFPro,
                        textMotion = TextMotion.Animated
                    ),
                    color = if (isSystemInDarkTheme()) Color.White else Color.Black
                )
                karaokeLine.translation?.let { translation ->
                    Text(
                        translation, Modifier.alpha(0.6f),
                        color = if (isSystemInDarkTheme()) Color.White else Color.Black
                    )
                }
            }
        }
    }
    Column {
        Spacer(
            Modifier
                .fillMaxWidth()
                .height(0.5.dp)
                .background(
                    if (isSystemInDarkTheme()) Color.White.copy(0.2f) else Color.Black.copy(0.2f)
                )
        )
        Row(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            Box(
                Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(100))
                    .background(Color(0xFF3482FF))
                    .clickable(enabled = selectedLines.isNotEmpty()) { onGenerateClicked() }
                    .padding(16.dp),
                contentAlignment = Alignment.Center
            ) {
                Text("Generate Share Card", color = Color.White)
            }
        }
    }

}

@Composable
private fun ColumnScope.ShareGenerateStep(
    context: ShareContext,
    shareViewModel: ShareViewModel,
    selectedLines: List<KaraokeLine>,
    onBackPressed: () -> Unit,
) {
    BackHandler { onBackPressed() }
    var showTranslation by remember { mutableStateOf(true) }
    val capturableController = rememberCapturableController()
    val localContext = LocalContext.current
    LaunchedEffect(Unit) {
        shareViewModel.toastEvent.collect { message ->
            Toast.makeText(localContext, message, Toast.LENGTH_SHORT).show()
        }
    }

    // 监听分享事件
    LaunchedEffect(Unit) {
        shareViewModel.shareEvent.collect { uri ->
            val sendIntent: Intent = Intent().apply {
                action = Intent.ACTION_SEND
                putExtra(Intent.EXTRA_STREAM, uri)
                type = "image/png"
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            val shareIntent = Intent.createChooser(sendIntent, null)
            localContext.startActivity(shareIntent)
        }
    }

    Box(
        Modifier
            .weight(1f)
            .fillMaxWidth()
            .background(if (isSystemInDarkTheme()) Color.White.copy(0.1f) else Color.Black.copy(0.1f))
    ) {
        Box(
            Modifier
                .align(Alignment.Center)
                .fillMaxWidth(0.7f)
                .deviceRotation()
                .shadow(16.dp, RoundedCornerShape(16.dp))
                .clip(RoundedCornerShape(16.dp))
        ) {
            Capturable(
                controller = capturableController
            ) {
                FlowingLightBackground(state = context.backgroundState)

                LazyColumn(
                    modifier = Modifier.graphicsLayer {
                        blendMode = BlendMode.Plus
                        compositingStrategy = CompositingStrategy.Offscreen
                    },
//                verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    items(selectedLines, key = { it.start }) { line ->
                        Column(
                            Modifier
                                .padding(top = 16.dp)
                                .padding(horizontal = 16.dp)
                        ) {
                            Text(
                                line.content(),
                                style = TextStyle(
                                    fontSize = 24.sp,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = SFPro,
                                    textMotion = TextMotion.Animated
                                ),
                                color = Color.White.copy(0.4f)
                            )
                            AnimatedVisibility(showTranslation) {
                                line.translation?.let { translation ->
                                    Text(
                                        translation,
                                        color = Color.White.copy(0.8f)
                                    )
                                }
                            }
                        }
                    }
                    item("spacing") {
                        Spacer(
                            Modifier
                                .fillMaxWidth()
                                .height(16.dp)
                        )
                    }
                    item("logo") {
                        Row(
                            Modifier
                                .fillMaxWidth()
                                .background(Color.White.copy(0.1f))
                                .padding(16.dp)
                        ) {
                            Text(
                                "by Accompanist",
                                style = TextStyle(
                                    fontSize = 16.sp,
                                    fontFamily = SFPro,
                                    textMotion = TextMotion.Animated
                                ),
                                color = Color.White
                            )
                        }
                    }
                }
            }

        }
    }
    Column {
        Spacer(
            Modifier
                .fillMaxWidth()
                .height(0.5.dp)
                .background(
                    if (isSystemInDarkTheme()) Color.White.copy(0.2f) else Color.Black.copy(0.2f)
                )
        )
        if (selectedLines.any { it.translation != null }) {

            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                Arrangement.SpaceBetween,
                Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        "Show translation",
                        color = if (isSystemInDarkTheme()) Color.White else Color.Black
                    )
                    Text(
                        "Tap to toggle",
                        color = if (isSystemInDarkTheme()) Color.White else Color.Black
                    )
                }
                Switch(showTranslation, onCheckedChange = { showTranslation = it })
            }
            Spacer(
                Modifier
                    .fillMaxWidth()
                    .height(0.5.dp)
                    .background(
                        if (isSystemInDarkTheme()) Color.White.copy(0.2f) else Color.Black.copy(0.2f)
                    )
            )
        }
        Row(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Box(
                Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(100))
                    .background(Color(0xFF3482FF))
                    .clickable(enabled = selectedLines.isNotEmpty()) {
                        capturableController.capture { bitmap ->
                            shareViewModel.saveBitmapToGallery(localContext, bitmap.asAndroidBitmap())
                        }
                    }
                    .padding(16.dp),
                contentAlignment = Alignment.Center
            ) {
                Text("Save", color = Color.White)
            }
            Box(
                Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(100))
                    .background(Color(0xFF3482FF))
                    .clickable(enabled = selectedLines.isNotEmpty()) {
                        capturableController.capture { bitmap ->
                            shareViewModel.prepareForSharing(localContext, bitmap.asAndroidBitmap())
                        }
                    }
                    .padding(16.dp),
                contentAlignment = Alignment.Center
            ) {
                Text("Share", color = Color.White)
            }
        }
    }
}