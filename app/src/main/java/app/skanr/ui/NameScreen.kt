package app.skanr.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import app.skanr.data.Ocr
import app.skanr.ui.theme.LocalTokens
import app.skanr.ui.theme.Type
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun NameScreen(pages: List<File>, onAddPages: () -> Unit, onDiscard: () -> Unit, onSave: (String) -> Unit) {
    val t = LocalTokens.current
    val defaultName = remember { "Scan " + SimpleDateFormat("d MMM, HH:mm", Locale.getDefault()).format(Date()) }
    var name by remember { mutableStateOf(cursorAtEnd(defaultName)) }
    var suggestions by remember { mutableStateOf<List<String>?>(null) }
    var confirmDiscard by remember { mutableStateOf(false) }
    var saving by remember { mutableStateOf(false) }
    var viewing by remember { mutableStateOf<Int?>(null) }
    val first = pages.firstOrNull()

    LaunchedEffect(first?.path) {
        suggestions = first?.let { Ocr.read(it) }?.let(Ocr::suggestions).orEmpty()
    }
    BackHandler { confirmDiscard = true }

    Column(Modifier.fillMaxSize().statusBarsPadding().imePadding()) {
        TopBar(
            title = "New scan",
            subtitle = "${pagesLabel(pages.size)}, about ${formatBytes(pages.sumOf { it.length() })}",
            navIcon = Icons.Close,
            onNav = { confirmDiscard = true },
        )
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState())) {
            LazyRow(
                contentPadding = PaddingValues(horizontal = 22.dp, vertical = 18.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                itemsIndexed(pages, key = { _, page -> page.path }) { index, page ->
                    PageImage(
                        page,
                        Modifier
                            .height(124.dp)
                            .aspectRatio(A4)
                            .clickable { viewing = index },
                        maxSide = 500,
                    )
                }
            }
            Column(Modifier.padding(horizontal = 22.dp, vertical = 8.dp)) {
                Text("Name", style = Type.label, color = t.pencil)
                Spacer(Modifier.height(8.dp))
                NameField(name, { name = it }, Modifier.fillMaxWidth())
                Spacer(Modifier.height(14.dp))
                when {
                    suggestions == null -> Text("Reading page 1…", style = Type.small, color = t.pencil)
                    suggestions!!.isNotEmpty() -> {
                        Text("Found on page 1, tap to use", style = Type.small, color = t.pencil)
                        Spacer(Modifier.height(10.dp))
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            suggestions!!.forEach { s ->
                                Text(
                                    s,
                                    style = Type.body.copy(fontSize = Type.meta.fontSize),
                                    color = t.graphite,
                                    modifier = Modifier
                                        .clip(CircleShape)
                                        .background(t.inkSoft)
                                        .clickable { name = cursorAtEnd(s) }
                                        .padding(horizontal = 14.dp, vertical = 8.dp),
                                )
                            }
                        }
                    }
                }
            }
        }
        Row(
            Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(start = 18.dp, end = 18.dp, top = 12.dp, bottom = 18.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            PillButton("Add pages", Modifier.weight(1f), solid = false, icon = Icons.Plus, onClick = onAddPages)
            PillButton("Save", Modifier.weight(1f), enabled = !saving && pages.isNotEmpty()) {
                saving = true
                onSave(name.text.trim().ifEmpty { defaultName })
            }
        }
    }

    viewing?.let { PageViewer(pages, it) { viewing = null } }

    if (confirmDiscard) {
        ConfirmDialog(
            title = "Discard ${pagesLabel(pages.size)}?",
            text = "They haven't been saved yet.",
            confirm = "Discard",
            dismiss = "Keep editing",
            onConfirm = {
                confirmDiscard = false
                onDiscard()
            },
            onDismiss = { confirmDiscard = false },
        )
    }
}
