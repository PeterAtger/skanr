package app.skanr.ui

import android.text.format.DateFormat
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsBottomHeight
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import app.skanr.data.Doc
import app.skanr.data.DocStore
import app.skanr.scan.Scanner
import app.skanr.scan.ScannerStatus
import app.skanr.ui.theme.LocalTokens
import app.skanr.ui.theme.Type
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import kotlin.math.roundToInt

private data class Group(val title: String, val docs: List<Doc>)

@Composable
fun HomeScreen(docs: List<Doc>, onOpen: (String) -> Unit, onScan: () -> Unit, waiting: Boolean) {
    val t = LocalTokens.current
    val context = LocalContext.current
    var query by rememberSaveable { mutableStateOf("") }
    var settings by remember { mutableStateOf(false) }

    val shown = remember(docs, query) {
        val q = query.trim()
        if (q.isEmpty()) docs else docs.filter { it.name.contains(q, true) || it.text.contains(q, true) }
    }
    val groups = remember(shown) { group(shown) }
    val timeFormat = remember { DateFormat.getTimeFormat(context) }

    Box(Modifier.fillMaxSize()) {
        Column(Modifier.fillMaxSize().statusBarsPadding()) {
            Row(
                Modifier.fillMaxWidth().padding(start = 14.dp, end = 10.dp, top = 10.dp, bottom = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Logo()
                Spacer(Modifier.weight(1f))
                IconTap(Icons.Settings, "Settings") { settings = true }
            }
            if (docs.isNotEmpty()) SearchField(query) { query = it }

            when {
                docs.isEmpty() -> EmptyState(Modifier.weight(1f))
                shown.isEmpty() -> Text(
                    "Nothing matches “${query.trim()}”.",
                    style = Type.body,
                    color = t.pencil,
                    modifier = Modifier.padding(24.dp),
                )
                else -> LazyColumn(
                    Modifier.weight(1f),
                    contentPadding = PaddingValues(top = 6.dp, bottom = 120.dp),
                ) {
                    groups.forEach { g ->
                        item(key = "h_${g.title}") {
                            Text(
                                g.title,
                                style = Type.label,
                                color = t.pencil,
                                modifier = Modifier.padding(start = 22.dp, end = 22.dp, top = 16.dp, bottom = 4.dp),
                            )
                        }
                        items(g.docs, key = { it.id }) { doc ->
                            DocRow(doc, whenLabel(doc.created, timeFormat)) { onOpen(doc.id) }
                        }
                    }
                    item { Spacer(Modifier.windowInsetsBottomHeight(WindowInsets.navigationBars)) }
                }
            }
        }

        Column(
            Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .padding(bottom = 26.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            if (waiting && Scanner.status != ScannerStatus.Ready) {
                Text(
                    "Getting the scanner ready, ${(Scanner.progress * 100).roundToInt()}%",
                    style = Type.small,
                    color = t.pencil,
                    modifier = Modifier
                        .background(t.paper.copy(alpha = .9f), CircleShape)
                        .padding(horizontal = 12.dp, vertical = 6.dp),
                )
                Spacer(Modifier.height(10.dp))
            }
            ScanButton(onScan)
        }
    }

    if (settings) SettingsSheet(docs) { settings = false }
}

@Composable
private fun SearchField(query: String, onChange: (String) -> Unit) {
    val t = LocalTokens.current
    BasicTextField(
        value = query,
        onValueChange = onChange,
        singleLine = true,
        textStyle = Type.body.copy(color = t.graphite),
        cursorBrush = SolidColor(t.ink),
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 6.dp),
        decorationBox = { inner ->
            Row(
                Modifier
                    .fillMaxWidth()
                    .background(t.sheet, RoundedCornerShape(14.dp))
                    .border(1.dp, t.rule, RoundedCornerShape(14.dp))
                    .padding(horizontal = 14.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(Icons.Search, null, tint = t.pencil, modifier = Modifier.size(19.dp))
                Spacer(Modifier.width(10.dp))
                Box(Modifier.weight(1f)) {
                    if (query.isEmpty()) Text("Search names and text", style = Type.body, color = t.pencil)
                    inner()
                }
            }
        },
    )
}

@Composable
private fun DocRow(doc: Doc, whenText: String, onClick: () -> Unit) {
    val t = LocalTokens.current
    val first = doc.pages.firstOrNull()?.let { DocStore.pageFile(doc, it) }
    val ratio = remember(first?.path) { first?.let(::pageRatio) ?: A4 }
    Row(
        Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 22.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.size(width = 50.dp, height = 62.dp), contentAlignment = Alignment.Center) {
            if (first != null) {
                val m = if (ratio >= 50f / 62f) Modifier.fillMaxWidth() else Modifier.height(62.dp)
                PageImage(first, m.aspectRatio(ratio), maxSide = 200)
            }
        }
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    doc.name,
                    style = Type.strong,
                    color = t.graphite,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f, fill = false),
                )
                if (!doc.shared) {
                    Spacer(Modifier.width(8.dp))
                    Box(Modifier.size(7.dp).background(t.ink, CircleShape))
                }
            }
            Spacer(Modifier.height(2.dp))
            Text("${pagesLabel(doc.pages.size)}, ${formatBytes(doc.bytes)}", style = Type.meta, color = t.pencil)
        }
        Spacer(Modifier.width(12.dp))
        Text(whenText, style = Type.meta, color = t.pencil, modifier = Modifier.align(Alignment.Top).padding(top = 3.dp))
    }
}

@Composable
private fun EmptyState(modifier: Modifier) {
    val t = LocalTokens.current
    Column(
        modifier.fillMaxWidth().padding(horizontal = 40.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text("No scans yet", style = Type.emptyTitle, color = t.graphite, textAlign = TextAlign.Center)
        Spacer(Modifier.height(8.dp))
        Text("Tap Scan to add your first document.", style = Type.body, color = t.pencil, textAlign = TextAlign.Center)
        Spacer(Modifier.height(90.dp))
    }
}

@Composable
private fun ScanButton(onClick: () -> Unit) {
    val t = LocalTokens.current
    Row(
        Modifier
            .shadow(16.dp, CircleShape, ambientColor = Color(0xAA2A1A10), spotColor = Color(0xAA2A1A10))
            .clip(CircleShape)
            .background(t.ink)
            .clickable(onClick = onClick)
            .padding(start = 24.dp, end = 30.dp, top = 17.dp, bottom = 17.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(Icons.Scan, null, tint = t.onInk, modifier = Modifier.size(25.dp))
        Spacer(Modifier.width(11.dp))
        Text("Scan", style = Type.scan, color = t.onInk)
    }
}

private fun group(docs: List<Doc>): List<Group> {
    val now = Calendar.getInstance()
    val startOfToday = (now.clone() as Calendar).apply {
        set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0); set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
    }.timeInMillis
    val weekStart = startOfToday - 6L * 24 * 60 * 60 * 1000
    val month = SimpleDateFormat("MMMM", Locale.getDefault())
    val monthYear = SimpleDateFormat("MMMM yyyy", Locale.getDefault())
    val year = now.get(Calendar.YEAR)
    return docs.groupBy { doc ->
        when {
            doc.created >= startOfToday -> "Today"
            doc.created >= weekStart -> "This week"
            else -> {
                val c = Calendar.getInstance().apply { timeInMillis = doc.created }
                if (c.get(Calendar.YEAR) == year) month.format(Date(doc.created)) else monthYear.format(Date(doc.created))
            }
        }
    }.map { (title, list) -> Group(title, list) }
}

private fun whenLabel(time: Long, timeFormat: java.text.DateFormat): String {
    val startOfToday = Calendar.getInstance().apply {
        set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0); set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
    }.timeInMillis
    val date = Date(time)
    return when {
        time >= startOfToday -> timeFormat.format(date)
        time >= startOfToday - 6L * 24 * 60 * 60 * 1000 -> SimpleDateFormat("EEE", Locale.getDefault()).format(date)
        else -> SimpleDateFormat("d MMM", Locale.getDefault()).format(date)
    }
}
