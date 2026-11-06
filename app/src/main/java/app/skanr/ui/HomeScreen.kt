package app.skanr.ui

import android.text.format.DateFormat
import android.widget.Toast as AndroidToast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
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
import androidx.compose.runtime.rememberCoroutineScope
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
import kotlinx.coroutines.launch
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
    var selected by remember { mutableStateOf(setOf<String>()) }
    var sharing by remember { mutableStateOf(false) }
    var confirmDelete by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val selectedDocs = docs.filter { it.id in selected }
    val selecting = selectedDocs.isNotEmpty()

    fun toggle(id: String) {
        selected = if (id in selected) selected - id else selected + id
    }

    BackHandler(enabled = selecting) { selected = emptySet() }

    val shown = remember(docs, query) {
        val q = query.trim()
        if (q.isEmpty()) docs else docs.filter { it.name.contains(q, true) || it.text.contains(q, true) }
    }
    val groups = remember(shown) { group(shown) }
    val timeFormat = remember { DateFormat.getTimeFormat(context) }

    Box(Modifier.fillMaxSize()) {
        Column(Modifier.fillMaxSize().statusBarsPadding()) {
            if (selecting) {
                Row(
                    Modifier.fillMaxWidth().height(62.dp).padding(start = 6.dp, end = 10.dp, top = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    IconTap(Icons.Close, "Cancel") { selected = emptySet() }
                    Text("${selectedDocs.size} selected", style = Type.title, color = t.graphite, modifier = Modifier.weight(1f).padding(start = 4.dp))
                    IconTap(Icons.Delete, "Delete") { confirmDelete = true }
                }
            } else {
                Row(
                    Modifier.fillMaxWidth().height(62.dp).padding(start = 14.dp, end = 10.dp, top = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Logo()
                    Spacer(Modifier.weight(1f))
                    IconTap(Icons.Settings, "Settings") { settings = true }
                }
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
                            DocRow(
                                doc = doc,
                                whenText = whenLabel(doc.created, timeFormat),
                                selected = doc.id in selected,
                                onClick = { if (selecting) toggle(doc.id) else onOpen(doc.id) },
                                onLongClick = { toggle(doc.id) },
                            )
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
            if (selecting) {
                val label = if (selectedDocs.size == 1) "Share 1 document" else "Share ${selectedDocs.size} documents"
                PillButton(label, Modifier.fillMaxWidth().padding(horizontal = 18.dp), icon = Icons.Share) { sharing = true }
            } else {
                ScanButton(onScan)
            }
        }
    }

    if (settings) SettingsSheet(docs) { settings = false }

    if (sharing) ShareSheet(selectedDocs, onDismiss = { sharing = false }) { message ->
        sharing = false
        selected = emptySet()
        message?.let { AndroidToast.makeText(context, it, AndroidToast.LENGTH_SHORT).show() }
    }

    if (confirmDelete) {
        val n = selectedDocs.size
        ConfirmDialog(
            title = if (n == 1) "Delete this document?" else "Delete $n documents?",
            text = if (n == 1) "${selectedDocs.first().name} will be removed from this phone." else "They'll be removed from this phone.",
            confirm = "Delete",
            dismiss = "Cancel",
            onConfirm = {
                confirmDelete = false
                val ids = selectedDocs.map { it.id }
                selected = emptySet()
                scope.launch { ids.forEach { DocStore.delete(it) } }
            },
            onDismiss = { confirmDelete = false },
        )
    }
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
private fun DocRow(doc: Doc, whenText: String, selected: Boolean, onClick: () -> Unit, onLongClick: () -> Unit) {
    val t = LocalTokens.current
    val first = doc.pages.firstOrNull()?.let { DocStore.pageFile(doc, it) }
    Row(
        Modifier
            .fillMaxWidth()
            .background(if (selected) t.inkSoft else Color.Transparent)
            .combinedClickable(onClick = onClick, onLongClick = onLongClick)
            .padding(horizontal = 22.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.size(width = 50.dp, height = 62.dp)) {
            Box(
                Modifier
                    .matchParentSize()
                    .then(if (selected) Modifier.brackets(t.ink, 8.dp, 2.dp).padding(5.dp) else Modifier),
                contentAlignment = Alignment.Center,
            ) {
                if (first != null) {
                    PageImage(first, Modifier.fillMaxHeight().aspectRatio(A4), maxSide = 200)
                }
            }
            if (selected) {
                Box(
                    Modifier
                        .align(Alignment.BottomEnd)
                        .offset(x = 6.dp, y = 6.dp)
                        .size(20.dp)
                        .background(t.ink, CircleShape)
                        .border(1.5.dp, t.inkSoft, CircleShape),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(Icons.Check, null, tint = t.onInk, modifier = Modifier.size(13.dp))
                }
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
