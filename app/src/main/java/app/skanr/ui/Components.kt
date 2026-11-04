package app.skanr.ui

import android.util.LruCache
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import app.skanr.data.Images
import app.skanr.ui.theme.LocalTokens
import app.skanr.ui.theme.Type
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.util.Locale

const val A4 = 210f / 297f

fun Modifier.brackets(color: Color, length: Dp = 14.dp, width: Dp = 2.5.dp) = drawWithContent {
    drawContent()
    val l = length.toPx()
    val w = width.toPx()
    val r = size.width
    val b = size.height
    listOf(
        Offset(0f, 0f) to Size(l, w), Offset(0f, 0f) to Size(w, l),
        Offset(r - l, 0f) to Size(l, w), Offset(r - w, 0f) to Size(w, l),
        Offset(0f, b - w) to Size(l, w), Offset(0f, b - l) to Size(w, l),
        Offset(r - l, b - w) to Size(l, w), Offset(r - w, b - l) to Size(w, l),
    ).forEach { (o, s) -> drawRect(color, o, s) }
}

private object Thumbs {
    private val cache = object : LruCache<String, ImageBitmap>((Runtime.getRuntime().maxMemory() / 6).toInt()) {
        override fun sizeOf(key: String, value: ImageBitmap) = value.width * value.height * 4
    }

    fun peek(key: String): ImageBitmap? = cache.get(key)

    suspend fun load(file: File, maxSide: Int): ImageBitmap? = withContext(Dispatchers.IO) {
        val key = "${file.path}@$maxSide"
        cache.get(key) ?: Images.decode(file, maxSide)?.asImageBitmap()?.also { cache.put(key, it) }
    }
}

@Composable
fun PageImage(file: File, modifier: Modifier = Modifier, maxSide: Int = 400, raised: Boolean = false) {
    val t = LocalTokens.current
    val bucket = when {
        maxSide <= 200 -> 200
        maxSide <= 500 -> 500
        else -> 1600
    }
    val bitmap by produceState(Thumbs.peek("${file.path}@$bucket"), file.path, bucket) {
        value = Thumbs.load(file, bucket)
    }
    val shape = RoundedCornerShape(2.dp)
    var m = modifier
    if (raised) m = m.shadow(10.dp, shape, ambientColor = Color(0x552A1A10), spotColor = Color(0x552A1A10))
    Box(
        m.clip(shape).background(t.page).border(1.dp, t.rule, shape),
    ) {
        bitmap?.let {
            Image(it, contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.matchParentSize())
        }
    }
}

fun pageRatio(file: File): Float {
    val (w, h) = Images.size(file)
    return if (w > 0 && h > 0) w.toFloat() / h else A4
}

@Composable
fun PaperSketch(modifier: Modifier = Modifier, variant: Int = 0) {
    val t = LocalTokens.current
    val shape = RoundedCornerShape(2.dp)
    Box(
        modifier
            .aspectRatio(A4)
            .shadow(12.dp, shape, ambientColor = Color(0x662A1A10), spotColor = Color(0x662A1A10))
            .background(t.page, shape)
            .border(1.dp, t.rule, shape)
            .drawWithContent {
                val w = size.width
                val h = size.height
                val line = h * 0.011f
                val gap = h * 0.034f
                when (variant) {
                    1 -> {
                        drawRect(t.pageInk, Offset(w * .14f, h * .08f), Size(w * .3f, line * 2.4f))
                        drawRect(t.pageInkSoft, Offset(w * .6f, h * .07f), Size(w * .26f, h * .08f))
                        for (i in 0 until 8) drawRect(t.pageInkSoft, Offset(w * .14f, h * .32f + i * gap), Size(w * .72f, line))
                        drawRect(t.pageInk, Offset(w * .58f, h * .82f), Size(w * .28f, line * 3f))
                    }
                    2 -> {
                        for (i in 0 until 11) drawRect(t.pageInkSoft, Offset(w * .14f, h * .14f + i * gap), Size(w * .72f, line))
                        drawOval(t.ink, Offset(w * .16f, h * .77f), Size(w * .3f, h * .045f), alpha = .8f)
                        drawRect(t.pageInk, Offset(w * .14f, h * .84f), Size(w * .34f, 1.5f))
                    }
                    else -> {
                        drawRect(t.pageInk, Offset(w * .14f, h * .1f), Size(w * .46f, line * 2.4f))
                        for (i in 0 until 14) drawRect(t.pageInkSoft, Offset(w * .14f, h * .24f + i * gap), Size(if (i % 5 == 4) w * .48f else w * .72f, line))
                    }
                }
            },
    )
}

@Composable
fun PillButton(
    text: String,
    modifier: Modifier = Modifier,
    solid: Boolean = true,
    icon: ImageVector? = null,
    enabled: Boolean = true,
    onClick: () -> Unit,
) {
    val t = LocalTokens.current
    val shape = CircleShape
    var m = modifier.height(52.dp).clip(shape)
    m = if (solid) m.background(t.ink) else m.background(t.sheet).border(1.5.dp, t.rule, shape)
    Row(
        m
            .alpha(if (enabled) 1f else .55f)
            .clickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = 18.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        val color = if (solid) t.onInk else t.graphite
        icon?.let {
            Icon(it, null, tint = color, modifier = Modifier.size(20.dp))
            Spacer(Modifier.width(8.dp))
        }
        Text(text, style = Type.button, color = color, maxLines = 1)
    }
}

@Composable
fun IconTap(icon: ImageVector, label: String, onClick: () -> Unit) {
    val t = LocalTokens.current
    Box(
        Modifier
            .size(44.dp)
            .clip(CircleShape)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(icon, contentDescription = label, tint = t.graphite, modifier = Modifier.size(23.dp))
    }
}

@Composable
fun TopBar(
    title: String,
    subtitle: String?,
    navIcon: ImageVector,
    onNav: () -> Unit,
    actions: @Composable RowScope.() -> Unit = {},
) {
    val t = LocalTokens.current
    Row(
        Modifier
            .fillMaxWidth()
            .padding(start = 6.dp, end = 6.dp, top = 6.dp, bottom = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconTap(navIcon, "Back", onNav)
        Column(Modifier.weight(1f).padding(start = 4.dp)) {
            Text(title, style = Type.title, color = t.graphite, maxLines = 1, overflow = TextOverflow.Ellipsis)
            subtitle?.let { Text(it, style = Type.meta, color = t.pencil) }
        }
        actions()
    }
}

@Composable
fun Toast(text: String, action: String?, onAction: () -> Unit, modifier: Modifier = Modifier) {
    val t = LocalTokens.current
    Row(
        modifier
            .fillMaxWidth()
            .shadow(8.dp, RoundedCornerShape(12.dp))
            .background(t.toast, RoundedCornerShape(12.dp))
            .padding(start = 16.dp, end = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(text, style = Type.body, color = t.toastFg, modifier = Modifier.weight(1f).padding(vertical = 13.dp))
        action?.let {
            Text(
                it,
                style = Type.strong,
                color = t.toastFg.copy(alpha = .75f),
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .clickable(onClick = onAction)
                    .padding(horizontal = 12.dp, vertical = 10.dp),
            )
        }
    }
}

@Composable
fun ConfirmDialog(
    title: String,
    text: String,
    confirm: String,
    dismiss: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    val t = LocalTokens.current
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = t.sheet,
        shape = RoundedCornerShape(22.dp),
        title = { Text(title, style = Type.sheetTitle, color = t.graphite) },
        text = { Text(text, style = Type.body, color = t.pencil) },
        confirmButton = { DialogAction(confirm, onConfirm) },
        dismissButton = { DialogAction(dismiss, onDismiss) },
    )
}

@Composable
fun DialogAction(text: String, onClick: () -> Unit) {
    val t = LocalTokens.current
    Text(
        text,
        style = Type.button,
        color = t.ink,
        modifier = Modifier
            .clip(CircleShape)
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 10.dp),
    )
}

@Composable
fun NameField(value: TextFieldValue, onChange: (TextFieldValue) -> Unit, modifier: Modifier = Modifier) {
    val t = LocalTokens.current
    BasicTextField(
        value = value,
        onValueChange = onChange,
        singleLine = true,
        textStyle = Type.body.copy(fontSize = Type.body.fontSize * 1.06f, color = t.graphite),
        cursorBrush = SolidColor(t.ink),
        modifier = modifier,
        decorationBox = { inner ->
            Box(
                Modifier
                    .fillMaxWidth()
                    .background(t.sheet, RoundedCornerShape(12.dp))
                    .border(2.dp, t.ink, RoundedCornerShape(12.dp))
                    .padding(horizontal = 14.dp, vertical = 14.dp),
            ) { inner() }
        },
    )
}

fun selectAll(text: String) = TextFieldValue(text, TextRange(0, text.length))

fun cursorAtEnd(text: String) = TextFieldValue(text, TextRange(text.length))

fun formatBytes(bytes: Long): String = when {
    bytes < 1024 * 1024 -> "${(bytes / 1024).coerceAtLeast(1)} KB"
    else -> String.format(Locale.US, "%.1f MB", bytes / (1024.0 * 1024.0))
}

fun pagesLabel(n: Int) = if (n == 1) "1 page" else "$n pages"

@Composable
fun dpToPx(dp: Dp): Int = with(LocalDensity.current) { dp.roundToPx() }

@Composable
fun Logo(modifier: Modifier = Modifier) {
    val t = LocalTokens.current
    Box(
        modifier
            .brackets(t.ink, 6.dp, 1.5.dp)
            .padding(start = 8.dp, end = 8.dp, top = 5.dp, bottom = 7.dp),
    ) {
        Text("skanr", style = Type.wordmark, color = t.graphite)
    }
}
