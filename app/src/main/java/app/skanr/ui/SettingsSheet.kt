package app.skanr.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import app.skanr.Prefs
import app.skanr.data.Doc
import app.skanr.ui.theme.LocalTokens
import app.skanr.ui.theme.Type

private val themes = listOf("system" to "System", "light" to "Light", "dark" to "Dark")

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsSheet(docs: List<Doc>, onDismiss: () -> Unit) {
    val t = LocalTokens.current
    val context = LocalContext.current
    val version = context.packageManager.getPackageInfo(context.packageName, 0).versionName
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
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
                .padding(start = 22.dp, end = 22.dp, top = 10.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text("Settings", style = Type.sheetTitle, color = t.graphite)
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Appearance", style = Type.label, color = t.pencil)
                Segmented(themes.map { it.second }, themes.indexOfFirst { it.first == Prefs.theme }.coerceAtLeast(0)) {
                    Prefs.changeTheme(themes[it].first)
                }
            }
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text("On this phone", style = Type.label, color = t.pencil)
                Text(
                    "${if (docs.size == 1) "1 document" else "${docs.size} documents"}, ${formatBytes(docs.sumOf { it.bytes })}",
                    style = Type.body,
                    color = t.graphite,
                )
            }
            Text(
                "skanr $version. No ads, no account, no tracking.",
                style = Type.small,
                color = t.pencil,
            )
        }
    }
}
