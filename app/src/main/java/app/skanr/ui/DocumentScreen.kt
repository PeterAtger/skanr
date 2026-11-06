package app.skanr.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import app.skanr.data.Doc
import app.skanr.data.DocStore
import app.skanr.ui.theme.LocalTokens
import app.skanr.ui.theme.Type
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.absoluteValue
import kotlin.math.roundToInt

private val ThumbWidth = 40.dp
private val ThumbGap = 10.dp

@Composable
fun DocumentScreen(
    doc: Doc,
    justSaved: Boolean,
    onBack: () -> Unit,
    onUndoSave: () -> Unit,
    onAddPages: () -> Unit,
    onRescan: (Int) -> Unit,
) {
    val t = LocalTokens.current
    val scope = rememberCoroutineScope()
    val pager = rememberPagerState { doc.pages.size }
    var menu by remember { mutableStateOf(false) }
    var renaming by remember { mutableStateOf(false) }
    var confirmDelete by remember { mutableStateOf<Int?>(null) }
    var sharing by remember { mutableStateOf(false) }
    var viewing by remember { mutableStateOf<Int?>(null) }
    var toast by rememberSaveable { mutableStateOf(if (justSaved) "saved" else null) }
    var lastPageCount by remember { mutableIntStateOf(doc.pages.size) }

    LaunchedEffect(toast) {
        if (toast != null) {
            delay(4500)
            toast = null
        }
    }
    LaunchedEffect(doc.pages.size) {
        if (doc.pages.size > lastPageCount) pager.animateScrollToPage(lastPageCount)
        lastPageCount = doc.pages.size
    }

    val current = pager.currentPage.coerceIn(0, (doc.pages.size - 1).coerceAtLeast(0))

    Box(Modifier.fillMaxSize()) {
        Column(Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding()) {
            TopBar(
                title = doc.name,
                subtitle = "${pagesLabel(doc.pages.size)}, ${formatBytes(doc.bytes)}",
                navIcon = Icons.Back,
                onNav = onBack,
            ) {
                Box {
                    IconTap(Icons.More, "More") { menu = true }
                    DropdownMenu(
                        expanded = menu,
                        onDismissRequest = { menu = false },
                        containerColor = t.sheet,
                        shape = RoundedCornerShape(14.dp),
                        border = BorderStroke(1.dp, t.rule),
                    ) {
                        DropdownMenuItem(
                            text = { Text("Rename", style = Type.body, color = t.graphite) },
                            onClick = { menu = false; renaming = true },
                        )
                        DropdownMenuItem(
                            text = { Text("Delete document", style = Type.body, color = t.graphite) },
                            onClick = { menu = false; confirmDelete = -1 },
                        )
                    }
                }
            }

            BoxWithConstraints(Modifier.weight(1f).fillMaxWidth()) {
                val maxPageHeight = maxHeight - 28.dp
                val pageWidth = minOf(maxWidth - 112.dp, maxPageHeight * A4)
                val side = (maxWidth - pageWidth) / 2
                HorizontalPager(
                    state = pager,
                    contentPadding = PaddingValues(horizontal = side),
                    pageSpacing = 16.dp,
                    key = { doc.pages.getOrElse(it) { "$it" } },
                    modifier = Modifier.fillMaxSize(),
                ) { index ->
                    val page = doc.pages.getOrNull(index) ?: return@HorizontalPager
                    val file = DocStore.pageFile(doc, page)
                    val offset = ((pager.currentPage - index) + pager.currentPageOffsetFraction).absoluteValue.coerceIn(0f, 1f)
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        PageImage(
                            file,
                            Modifier
                                .fillMaxWidth()
                                .aspectRatio(A4)
                                .graphicsLayer { alpha = 1f - offset * 0.5f }
                                .clickable(interactionSource = null, indication = null) { viewing = index },
                            maxSide = 1600,
                            raised = true,
                        )
                    }
                }
            }

            Text(
                "Page ${current + 1} of ${doc.pages.size}",
                style = Type.meta,
                color = t.pencil,
                modifier = Modifier.align(Alignment.CenterHorizontally).padding(top = 4.dp),
            )

            Row(
                Modifier.fillMaxWidth().padding(top = 14.dp, bottom = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(40.dp, Alignment.CenterHorizontally),
            ) {
                Tool(Icons.Rotate, "Rotate") { scope.launch { DocStore.rotatePage(doc.id, current) } }
                Tool(Icons.Rescan, "Rescan") { onRescan(current) }
                Tool(Icons.Delete, "Delete") { confirmDelete = current }
            }

            Filmstrip(
                doc = doc,
                current = current,
                onSelect = { scope.launch { pager.animateScrollToPage(it) } },
                onMove = { from, to ->
                    scope.launch {
                        DocStore.movePage(doc.id, from, to)
                        pager.scrollToPage(to)
                    }
                },
                onAdd = onAddPages,
            )

            PillButton(
                "Share",
                Modifier.fillMaxWidth().padding(start = 18.dp, end = 18.dp, top = 10.dp, bottom = 16.dp),
                icon = Icons.Share,
            ) { sharing = true }
        }

        toast?.let { kind ->
            Toast(
                text = if (kind == "saved") "Saved" else kind,
                action = if (kind == "saved") "Undo" else null,
                onAction = { toast = null; onUndoSave() },
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .navigationBarsPadding()
                    .padding(start = 16.dp, end = 16.dp, bottom = 84.dp),
            )
        }
    }

    viewing?.let { PageViewer(DocStore.pageFiles(doc), it) { viewing = null } }

    if (renaming) RenameDialog(doc.name, onDismiss = { renaming = false }) { name ->
        renaming = false
        scope.launch { DocStore.rename(doc.id, name) }
    }

    confirmDelete?.let { index ->
        val wholeDoc = index < 0 || doc.pages.size == 1
        ConfirmDialog(
            title = if (wholeDoc) "Delete this document?" else "Delete page ${index + 1}?",
            text = if (wholeDoc) "${doc.name} and its ${pagesLabel(doc.pages.size)} will be removed from this phone." else "The other pages stay as they are.",
            confirm = "Delete",
            dismiss = "Cancel",
            onConfirm = {
                confirmDelete = null
                scope.launch {
                    if (wholeDoc) {
                        DocStore.delete(doc.id)
                        onBack()
                    } else {
                        DocStore.deletePage(doc.id, index)
                    }
                }
            },
            onDismiss = { confirmDelete = null },
        )
    }

    if (sharing) ShareSheet(listOf(doc), onDismiss = { sharing = false }) { message ->
        sharing = false
        message?.let { toast = it }
    }
}

@Composable
private fun Tool(icon: androidx.compose.ui.graphics.vector.ImageVector, label: String, onClick: () -> Unit) {
    val t = LocalTokens.current
    Column(
        Modifier
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 6.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Icon(icon, contentDescription = null, tint = t.graphite, modifier = Modifier.size(23.dp))
        Spacer(Modifier.height(4.dp))
        Text(label, style = Type.small, color = t.graphite)
    }
}

@Composable
private fun Filmstrip(doc: Doc, current: Int, onSelect: (Int) -> Unit, onMove: (Int, Int) -> Unit, onAdd: () -> Unit) {
    val t = LocalTokens.current
    val step = with(LocalDensity.current) { (ThumbWidth + ThumbGap).toPx() }
    var dragging by remember { mutableStateOf<Int?>(null) }
    var dragX by remember { mutableFloatStateOf(0f) }
    val slots = (dragX / step).roundToInt()

    Row(
        Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = 18.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.spacedBy(ThumbGap, Alignment.CenterHorizontally),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        doc.pages.forEachIndexed { index, page ->
            val file = DocStore.pageFile(doc, page)
            val from = dragging
            val shift = when {
                from == null || index == from -> 0f
                slots > 0 && index in (from + 1)..(from + slots) -> -step
                slots < 0 && index in (from + slots) until from -> step
                else -> 0f
            }
            val isDragged = from == index
            Box(
                Modifier
                    .width(ThumbWidth)
                    .zIndex(if (isDragged) 1f else 0f)
                    .graphicsLayer {
                        translationX = if (isDragged) dragX else shift
                        scaleX = if (isDragged) 1.12f else 1f
                        scaleY = if (isDragged) 1.12f else 1f
                    }
                    .pointerInput(doc.pages) {
                        detectDragGesturesAfterLongPress(
                            onDragStart = { dragging = index; dragX = 0f },
                            onDrag = { change, amount -> change.consume(); dragX += amount.x },
                            onDragEnd = {
                                val to = (index + (dragX / step).roundToInt()).coerceIn(0, doc.pages.lastIndex)
                                dragging = null
                                dragX = 0f
                                if (to != index) onMove(index, to)
                            },
                            onDragCancel = { dragging = null; dragX = 0f },
                        )
                    }
                    .clickable { onSelect(index) }
                    .then(if (index == current) Modifier.brackets(t.ink, 9.dp, 2.dp) else Modifier)
                    .padding(5.dp),
                contentAlignment = Alignment.Center,
            ) {
                PageImage(file, Modifier.fillMaxWidth().aspectRatio(A4), maxSide = 200)
            }
        }
        Box(
            Modifier
                .width(ThumbWidth)
                .padding(5.dp)
                .aspectRatio(A4)
                .clip(RoundedCornerShape(3.dp))
                .clickable(onClick = onAdd)
                .drawBehind {
                    drawRoundRect(
                        color = t.pencil,
                        cornerRadius = CornerRadius(3.dp.toPx()),
                        style = Stroke(1.5.dp.toPx(), pathEffect = PathEffect.dashPathEffect(floatArrayOf(4.dp.toPx(), 3.dp.toPx()))),
                    )
                },
            contentAlignment = Alignment.Center,
        ) {
            Icon(Icons.Plus, contentDescription = "Add pages", tint = t.pencil, modifier = Modifier.size(16.dp))
        }
    }
}

@Composable
private fun RenameDialog(current: String, onDismiss: () -> Unit, onSave: (String) -> Unit) {
    val t = LocalTokens.current
    var value by remember { mutableStateOf(selectAll(current)) }
    androidx.compose.material3.AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = t.sheet,
        shape = RoundedCornerShape(22.dp),
        title = { Text("Rename", style = Type.sheetTitle, color = t.graphite) },
        text = { NameField(value, { value = it }, Modifier.fillMaxWidth()) },
        confirmButton = { DialogAction("Save") { onSave(value.text.trim().ifEmpty { current }) } },
        dismissButton = { DialogAction("Cancel", onDismiss) },
    )
}
