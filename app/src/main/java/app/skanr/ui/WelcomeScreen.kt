package app.skanr.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.unit.dp
import app.skanr.scan.Scanner
import app.skanr.scan.ScannerStatus
import app.skanr.ui.theme.LocalTokens
import app.skanr.ui.theme.Type
import kotlin.math.roundToInt

@Composable
private fun WelcomeFrame(bottom: @Composable ColumnScope.() -> Unit) {
    val t = LocalTokens.current
    Column(
        Modifier
            .fillMaxSize()
            .systemBarsPadding()
            .padding(horizontal = 28.dp, vertical = 24.dp),
    ) {
        Box(
            Modifier
                .fillMaxWidth()
                .height(270.dp)
                .padding(top = 16.dp),
            contentAlignment = Alignment.TopCenter,
        ) {
            PaperSketch(
                Modifier
                    .width(124.dp)
                    .offset(x = (-58).dp, y = 22.dp)
                    .rotate(-11f),
                variant = 1,
            )
            PaperSketch(
                Modifier
                    .width(124.dp)
                    .offset(x = 58.dp, y = 22.dp)
                    .rotate(9f),
                variant = 2,
            )
            Box(
                Modifier
                    .width(150.dp)
                    .brackets(t.ink, 20.dp, 3.dp)
                    .padding(10.dp),
            ) {
                PaperSketch(Modifier.fillMaxWidth())
            }
        }
        Spacer(Modifier.height(24.dp))
        Text("Paper in,\nclean PDF out.", style = Type.hero, color = t.graphite)
        Spacer(Modifier.height(14.dp))
        Text(
            "Scans stay on this phone until you share them. No ads and no account, now or later.",
            style = Type.body,
            color = t.pencil,
        )
        Spacer(Modifier.weight(1f))
        bottom()
    }
}

@Composable
fun LoadingScreen() {
    WelcomeFrame {}
}

@Composable
fun WelcomeScreen(waiting: Boolean, onScan: () -> Unit) {
    val t = LocalTokens.current
    WelcomeFrame {
        if (Scanner.status == ScannerStatus.Downloading || Scanner.status == ScannerStatus.Checking) {
            Row(Modifier.fillMaxWidth()) {
                Text("Getting the scanner ready", style = Type.label, color = t.graphite, modifier = Modifier.weight(1f))
                if (Scanner.status == ScannerStatus.Downloading) {
                    Text("${(Scanner.progress * 100).roundToInt()}%", style = Type.meta, color = t.pencil)
                }
            }
            Spacer(Modifier.height(8.dp))
            Box(
                Modifier
                    .fillMaxWidth()
                    .height(4.dp)
                    .background(t.rule, RoundedCornerShape(2.dp)),
            ) {
                Box(
                    Modifier
                        .fillMaxHeight()
                        .fillMaxWidth(Scanner.progress.coerceIn(0.02f, 1f))
                        .background(t.ink, RoundedCornerShape(2.dp)),
                )
            }
            Spacer(Modifier.height(8.dp))
            Text(
                if (waiting) "The scanner opens as soon as the download finishes." else "A one-time download from Google Play services.",
                style = Type.small,
                color = t.pencil,
            )
            Spacer(Modifier.height(20.dp))
        }
        PillButton("Scan a document", Modifier.fillMaxWidth().height(56.dp), onClick = onScan)
    }
}

