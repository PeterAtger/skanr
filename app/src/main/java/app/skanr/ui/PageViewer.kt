package app.skanr.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.calculatePan
import androidx.compose.foundation.gestures.calculateZoom
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChanged
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import app.skanr.ui.theme.DarkTokens
import app.skanr.ui.theme.Type
import java.io.File

private const val MAX_ZOOM = 5f
private const val DOUBLE_TAP_ZOOM = 2.5f

@Composable
fun PageViewer(pages: List<File>, start: Int, onClose: () -> Unit) {
    val t = DarkTokens
    val pager = rememberPagerState(initialPage = start.coerceIn(0, (pages.size - 1).coerceAtLeast(0))) { pages.size }
    Dialog(
        onDismissRequest = onClose,
        properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false),
    ) {
        Box(Modifier.fillMaxSize().background(t.desk)) {
            HorizontalPager(state = pager, modifier = Modifier.fillMaxSize(), key = { pages[it].path }) { index ->
                ZoomablePage(pages[index])
            }
            Row(
                Modifier
                    .fillMaxWidth()
                    .systemBarsPadding()
                    .padding(horizontal = 8.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(
                    Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(Color.Black.copy(alpha = .35f))
                        .clickable(onClick = onClose),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(Icons.Close, contentDescription = "Close", tint = t.graphite, modifier = Modifier.size(22.dp))
                }
                Box(Modifier.weight(1f), contentAlignment = Alignment.Center) {
                    if (pages.size > 1) {
                        Text(
                            "${pager.currentPage + 1} of ${pages.size}",
                            style = Type.meta,
                            color = t.graphite,
                            modifier = Modifier
                                .clip(CircleShape)
                                .background(Color.Black.copy(alpha = .35f))
                                .padding(horizontal = 12.dp, vertical = 6.dp),
                        )
                    }
                }
                Box(Modifier.size(44.dp))
            }
        }
    }
}

@Composable
private fun ZoomablePage(file: File) {
    var scale by remember { mutableFloatStateOf(1f) }
    var offset by remember { mutableStateOf(Offset.Zero) }
    val ratio = remember(file.path) { pageRatio(file) }

    BoxWithConstraints(
        Modifier
            .fillMaxSize()
            .pointerInput(Unit) {
                detectTapGestures(onDoubleTap = { tap ->
                    if (scale > 1f) {
                        scale = 1f
                        offset = Offset.Zero
                    } else {
                        scale = DOUBLE_TAP_ZOOM
                        val center = Offset(size.width / 2f, size.height / 2f)
                        offset = clamp((center - tap) * (DOUBLE_TAP_ZOOM - 1f), scale, size.width.toFloat(), size.height.toFloat())
                    }
                })
            }
            .pointerInput(Unit) {
                awaitEachGesture {
                    awaitFirstDown(requireUnconsumed = false)
                    do {
                        val event = awaitPointerEvent()
                        val pinching = event.changes.size > 1
                        if (pinching || scale > 1f) {
                            scale = (scale * event.calculateZoom()).coerceIn(1f, MAX_ZOOM)
                            offset = if (scale == 1f) Offset.Zero else clamp(offset + event.calculatePan(), scale, size.width.toFloat(), size.height.toFloat())
                            event.changes.forEach { if (it.positionChanged()) it.consume() }
                        }
                    } while (event.changes.any { it.pressed })
                }
            },
        contentAlignment = Alignment.Center,
    ) {
        val wide = ratio >= maxWidth / maxHeight
        PageImage(
            file,
            Modifier
                .padding(horizontal = 12.dp, vertical = 64.dp)
                .fillMaxWidth()
                .aspectRatio(ratio, matchHeightConstraintsFirst = !wide)
                .graphicsLayer {
                    scaleX = scale
                    scaleY = scale
                    translationX = offset.x
                    translationY = offset.y
                },
            maxSide = 2400,
        )
    }
}

private fun clamp(offset: Offset, scale: Float, width: Float, height: Float): Offset {
    val maxX = width * (scale - 1f) / 2f
    val maxY = height * (scale - 1f) / 2f
    return Offset(offset.x.coerceIn(-maxX, maxX), offset.y.coerceIn(-maxY, maxY))
}
