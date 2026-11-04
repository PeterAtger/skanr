package app.skanr.ui

import android.widget.Toast as AndroidToast
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import app.skanr.Prefs
import app.skanr.data.DocStore
import app.skanr.scan.Scanner
import app.skanr.scan.ScannerStatus
import app.skanr.scan.rememberScanLauncher
import app.skanr.ui.theme.LocalTokens
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.io.File

sealed interface Screen {
    data object Welcome : Screen
    data object Home : Screen
    data object NewScan : Screen
    data class Document(val id: String, val justSaved: Boolean = false) : Screen
}

sealed interface ScanTarget {
    data object New : ScanTarget
    data object MorePending : ScanTarget
    data class Append(val id: String) : ScanTarget
    data class Replace(val id: String, val index: Int) : ScanTarget
}

@Composable
fun SkanrApp() {
    val context = LocalContext.current
    val t = LocalTokens.current
    val scope = rememberCoroutineScope()
    val docs by DocStore.docs.collectAsState()

    val stack = remember {
        mutableStateListOf<Screen>(if (Prefs.onboarded || DocStore.docs.value.isNotEmpty()) Screen.Home else Screen.Welcome)
    }
    val pending = remember { mutableStateListOf<File>() }
    var target by remember { mutableStateOf<ScanTarget>(ScanTarget.New) }
    var waiting by remember { mutableStateOf<ScanTarget?>(null) }
    var booting by rememberSaveable { mutableStateOf(true) }

    LaunchedEffect(Unit) {
        delay(1400)
        booting = false
    }

    fun say(text: String) = AndroidToast.makeText(context, text, AndroidToast.LENGTH_SHORT).show()

    val launch = rememberScanLauncher(
        onScanned = { uris ->
            scope.launch {
                val files = DocStore.importScan(context, uris)
                when (val t = target) {
                    ScanTarget.New -> {
                        pending.clear()
                        pending.addAll(files)
                        stack.add(Screen.NewScan)
                    }
                    ScanTarget.MorePending -> pending.addAll(files)
                    is ScanTarget.Append -> DocStore.addPages(t.id, files)
                    is ScanTarget.Replace -> DocStore.replacePage(t.id, t.index, files.first())
                }
            }
        },
        onError = { say("The scanner couldn't open. Check that Google Play services is up to date.") },
    )

    fun scan(next: ScanTarget) {
        target = next
        when (Scanner.status) {
            ScannerStatus.Ready -> launch(if (next is ScanTarget.Replace) 1 else null)
            ScannerStatus.Unavailable -> say("The scanner needs Google Play services, which isn't available on this phone.")
            else -> waiting = next
        }
    }

    LaunchedEffect(Scanner.status) {
        val next = waiting ?: return@LaunchedEffect
        if (Scanner.status == ScannerStatus.Ready) {
            waiting = null
            scan(next)
        }
    }

    fun back() {
        if (stack.size > 1) stack.removeAt(stack.lastIndex)
    }

    val current = stack.last()
    BackHandler(enabled = stack.size > 1 && current != Screen.NewScan) { back() }

    Box(Modifier.fillMaxSize().background(t.paper)) {
        Crossfade(booting, animationSpec = tween(320), label = "boot") { loading ->
        if (loading) {
            LoadingScreen()
            return@Crossfade
        }
        AnimatedContent(
            targetState = current,
            transitionSpec = { fadeIn(tween(180)) togetherWith fadeOut(tween(120)) },
            label = "screen",
        ) { screen ->
            when (screen) {
                Screen.Welcome -> WelcomeScreen(
                    waiting = waiting != null,
                    onScan = { scan(ScanTarget.New) },
                )
                Screen.Home -> HomeScreen(
                    docs = docs,
                    onOpen = { stack.add(Screen.Document(it)) },
                    onScan = { scan(ScanTarget.New) },
                    waiting = waiting != null,
                )
                Screen.NewScan -> NameScreen(
                    pages = pending,
                    onAddPages = { scan(ScanTarget.MorePending) },
                    onDiscard = {
                        pending.forEach { it.delete() }
                        pending.clear()
                        back()
                    },
                    onSave = { name ->
                        scope.launch {
                            val doc = DocStore.create(name, pending.toList())
                            pending.clear()
                            Prefs.onboarded = true
                            stack.clear()
                            stack.add(Screen.Home)
                            stack.add(Screen.Document(doc.id, justSaved = true))
                        }
                    },
                )
                is Screen.Document -> {
                    val doc = docs.find { it.id == screen.id }
                    if (doc == null) {
                        LaunchedEffect(Unit) { if (stack.lastOrNull() == screen) back() }
                    } else {
                        DocumentScreen(
                            doc = doc,
                            justSaved = screen.justSaved,
                            onBack = { back() },
                            onUndoSave = {
                                scope.launch {
                                    DocStore.delete(doc.id)
                                    back()
                                }
                            },
                            onAddPages = { scan(ScanTarget.Append(doc.id)) },
                            onRescan = { index -> scan(ScanTarget.Replace(doc.id, index)) },
                        )
                    }
                }
            }
        }
        }
    }
}
