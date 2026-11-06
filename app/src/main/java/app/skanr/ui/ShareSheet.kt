package app.skanr.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import app.skanr.data.Doc
import app.skanr.data.DocStore
import app.skanr.data.Exporter
import app.skanr.data.Quality
import app.skanr.ui.theme.LocalTokens
import app.skanr.ui.theme.Type
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ShareSheet(docs: List<Doc>, onDismiss: () -> Unit, onDone: (String?) -> Unit) {
    val t = LocalTokens.current
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val state = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var asPdf by remember { mutableStateOf(true) }
    var quality by remember { mutableStateOf(Quality.Standard) }
    var searchable by remember { mutableStateOf(true) }
    var busy by remember { mutableStateOf<String?>(null) }
    var error by remember { mutableStateOf<String?>(null) }
    val pageBytes = remember { mutableStateMapOf<Quality, Long>() }

    val pageCount = docs.sumOf { it.pages.size }
    val title = if (docs.size == 1) "Share ${pagesLabel(pageCount)}" else "Share ${docs.size} documents"

    LaunchedEffect(docs.map { it.pages }) {
        pageBytes[Quality.Original] = docs.sumOf { doc -> DocStore.pageFiles(doc).sumOf { it.length() } }
        listOf(Quality.Standard, Quality.Small).forEach { q ->
            pageBytes[q] = docs.sumOf { doc -> Exporter.pagesFor(context, doc, q).sumOf { it.length() } }
        }
    }

    fun run(save: Boolean) {
        busy = if (save) "Saving…" else "Preparing…"
        error = null
        scope.launch {
            runCatching {
                val mime = if (asPdf) "application/pdf" else "image/jpeg"
                val files = Exporter.export(context, docs, asPdf, quality, searchable)
                if (save) {
                    Exporter.saveToDownloads(context, files, mime)
                } else {
                    context.startActivity(Exporter.shareIntent(context, files, mime))
                }
                docs.forEach { DocStore.markShared(it.id) }
            }.onSuccess {
                busy = null
                state.hide()
                onDone(if (save) "Saved to Downloads" else null)
            }.onFailure {
                busy = null
                error = "Couldn't make the file: ${it.message ?: "unknown error"}. Try again, or pick a smaller size."
            }
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = state,
        containerColor = t.sheet,
        scrimColor = t.scrim,
        shape = RoundedCornerShape(topStart = 26.dp, topEnd = 26.dp),
        dragHandle = {
            Box(Modifier.padding(top = 12.dp, bottom = 4.dp).size(width = 40.dp, height = 4.dp).background(t.rule, CircleShape))
        },
    ) {
        Column(
            Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(start = 22.dp, end = 22.dp, top = 10.dp, bottom = 18.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text(title, style = Type.sheetTitle, color = t.graphite)

            Segmented(listOf("PDF", "Images"), if (asPdf) 0 else 1) { asPdf = it == 0 }

            Column {
                Text("File size", style = Type.label, color = t.pencil)
                Spacer(Modifier.height(4.dp))
                Quality.entries.forEach { q ->
                    val bytes = pageBytes[q]?.let { if (asPdf) Exporter.pdfBytes(it, pageCount) else it }
                    QualityRow(q, selected = q == quality, size = bytes?.let(::formatBytes) ?: "…") { quality = q }
                }
            }

            if (asPdf) {
                HorizontalDivider(color = t.rule)
                Row(
                    Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .clickable { searchable = !searchable },
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(Modifier.weight(1f)) {
                        Text("Searchable text", style = Type.strong, color = t.graphite)
                        Text("Read on your phone, nothing is uploaded", style = Type.small, color = t.pencil)
                    }
                    Switch(searchable)
                }
            }

            error?.let { Text(it, style = Type.small, color = t.graphite) }

            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                PillButton(
                    if (busy == "Saving…") busy!! else "Save to phone",
                    Modifier.weight(1f),
                    solid = false,
                    enabled = busy == null,
                ) { run(save = true) }
                PillButton(
                    if (busy == "Preparing…") busy!! else if (asPdf && docs.size == 1) "Share PDF" else if (asPdf) "Share PDFs" else "Share images",
                    Modifier.weight(1f),
                    enabled = busy == null,
                ) { run(save = false) }
            }
        }
    }
}

@Composable
fun Segmented(options: List<String>, selected: Int, onSelect: (Int) -> Unit) {
    val t = LocalTokens.current
    Row(
        Modifier
            .fillMaxWidth()
            .background(t.paper, CircleShape)
            .border(1.dp, t.rule, CircleShape)
            .padding(3.dp),
    ) {
        options.forEachIndexed { i, label ->
            val on = i == selected
            var m = Modifier.weight(1f).clip(CircleShape)
            if (on) m = m.shadow(2.dp, CircleShape).background(t.sheet, CircleShape)
            Text(
                label,
                style = Type.button.copy(fontSize = Type.meta.fontSize),
                color = if (on) t.graphite else t.pencil,
                textAlign = TextAlign.Center,
                modifier = m.clickable { onSelect(i) }.padding(vertical = 10.dp),
            )
        }
    }
}

@Composable
private fun QualityRow(q: Quality, selected: Boolean, size: String, onClick: () -> Unit) {
    val t = LocalTokens.current
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .clickable(onClick = onClick)
            .padding(vertical = 9.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Radio(selected)
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f)) {
            Text(q.label, style = Type.strong, color = t.graphite)
            Text(q.note, style = Type.small, color = t.pencil)
        }
        Text(size, style = Type.meta, color = t.pencil)
    }
}

@Composable
private fun Radio(on: Boolean) {
    val t = LocalTokens.current
    Box(
        Modifier
            .size(21.dp)
            .border(2.dp, if (on) t.ink else t.pencil, CircleShape),
        contentAlignment = Alignment.Center,
    ) {
        if (on) Box(Modifier.size(11.dp).background(t.ink, CircleShape))
    }
}

@Composable
fun Switch(on: Boolean) {
    val t = LocalTokens.current
    Box(
        Modifier
            .size(width = 46.dp, height = 28.dp)
            .background(if (on) t.ink else t.rule, CircleShape)
            .padding(3.dp),
        contentAlignment = if (on) Alignment.CenterEnd else Alignment.CenterStart,
    ) {
        Box(Modifier.size(22.dp).background(if (on) t.onInk else t.sheet, CircleShape))
    }
}
