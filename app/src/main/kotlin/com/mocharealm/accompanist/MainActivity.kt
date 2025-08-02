package com.mocharealm.accompanist

import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextMotion
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import androidx.core.graphics.createBitmap
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.media3.common.MediaItem
import com.mocharealm.accompanist.domain.model.MusicItem
import com.mocharealm.accompanist.lyrics.model.ISyncedLine
import com.mocharealm.accompanist.lyrics.model.karaoke.KaraokeLine
import com.mocharealm.accompanist.service.PlaybackService
import com.mocharealm.accompanist.ui.composable.ModalScaffold
import com.mocharealm.accompanist.ui.composable.background.BackgroundVisualState
import com.mocharealm.accompanist.ui.composable.background.FlowingLightBackground
import com.mocharealm.accompanist.ui.composable.lyrics.KaraokeLyricsView
import com.mocharealm.accompanist.ui.composable.utils.modifier.deviceRotation
import com.mocharealm.accompanist.ui.composable.utils.rememberScreenCornerDataDp
import com.mocharealm.accompanist.ui.screen.player.PlayerScreen
import com.mocharealm.accompanist.ui.theme.AccompanistTheme
import com.mocharealm.accompanist.ui.theme.SFPro
import kotlinx.coroutines.android.awaitFrame
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val intent = Intent(this, PlaybackService::class.java)
        startService(intent)

        enableEdgeToEdge(
            navigationBarStyle = SystemBarStyle.auto(
                Color.Transparent.toArgb(), Color.Transparent.toArgb()
            ),
            statusBarStyle = SystemBarStyle.dark(Color.White.toArgb())
        )
        // FUCKING XIAOMI
        @Suppress("DEPRECATION") if (Build.MANUFACTURER == "Xiaomi") {
            window.addFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_NAVIGATION)
        }

        setContent {
            PlayerScreen()
        }
    }
}

@Composable
fun MusicItemSelectionDialog(
    items: List<MusicItem>,
    onItemSelected: (MusicItem) -> Unit,
    onDismissRequest: () -> Unit // 当用户点击对话框外部或返回键时调用
) {
    var selectedIndex by remember { mutableIntStateOf(-1) }
    AlertDialog(
        onDismissRequest = onDismissRequest,
        title = { Text("Choose a song to play") },
        text = {
            LazyColumn {
                itemsIndexed(items) { index, item ->
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { selectedIndex = index }
                            .padding(vertical = 12.dp)) {
                        Text(item.label, fontWeight = FontWeight.Bold)
                        Text(item.testTarget, fontSize = 14.sp)
                    }
                }
            }
        },
        // 我们不需要确认按钮，因为点击列表项就是选择了
        confirmButton = {
            Text("Confirm", Modifier.clickable {
                if (selectedIndex != -1) {
                    onItemSelected(items[selectedIndex])
                }
            })
        })
}

fun Color.toImageBitmap(width: Int = 1, height: Int = 1): ImageBitmap {
    // 1. 创建一个指定尺寸的 Android 原生 Bitmap
    val bitmap = createBitmap(width, height)

    // 2. 将该 Bitmap 作为画布 (Canvas)
    val canvas = android.graphics.Canvas(bitmap)

    // 3. 将 Compose Color 转换为 Android Color Int，并用该颜色填充整个画布
    canvas.drawColor(this.toArgb())

    // 4. 将 Android Bitmap 转换为 Compose ImageBitmap 并返回
    return bitmap.asImageBitmap()
}
