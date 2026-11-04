package app.skanr.ui

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.PathParser
import androidx.compose.ui.unit.dp

private fun strokeIcon(name: String, vararg paths: String, width: Float = 1.6f): ImageVector =
    ImageVector.Builder(name, 24.dp, 24.dp, 24f, 24f).apply {
        paths.forEach {
            addPath(
                pathData = PathParser().parsePathString(it).toNodes(),
                stroke = SolidColor(Color.Black),
                strokeLineWidth = width,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round,
            )
        }
    }.build()

private const val CORNERS = "M4 8V5a1 1 0 0 1 1-1h3M16 4h3a1 1 0 0 1 1 1v3M20 16v3a1 1 0 0 1-1 1h-3M8 20H5a1 1 0 0 1-1-1v-3"

object Icons {
    val Close = strokeIcon("close", "M6 6l12 12M18 6 6 18")
    val Back = strokeIcon("back", "M15 6l-6 6 6 6")
    val More = strokeIcon(
        "more",
        "M12 4.5a.5.5 0 1 0 0 1a.5.5 0 1 0 0-1",
        "M12 11.5a.5.5 0 1 0 0 1a.5.5 0 1 0 0-1",
        "M12 18.5a.5.5 0 1 0 0 1a.5.5 0 1 0 0-1",
        width = 2.2f,
    )
    val Settings = strokeIcon(
        "settings",
        "M12 9a3 3 0 1 0 0 6a3 3 0 1 0 0-6",
        "M12 2v3M12 19v3M2 12h3M19 12h3M4.9 4.9l2.1 2.1M17 17l2.1 2.1M4.9 19.1 7 17M17 7l2.1-2.1",
    )
    val Search = strokeIcon("search", "M11 4a7 7 0 1 0 0 14a7 7 0 1 0 0-14", "M20 20l-3.5-3.5")
    val Scan = strokeIcon("scan", CORNERS, "M12 9v6M9 12h6", width = 2f)
    val Rescan = strokeIcon("rescan", CORNERS, "M8 10h8M8 14h5")
    val Plus = strokeIcon("plus", "M12 5v14M5 12h14")
    val Share = strokeIcon("share", "M12 15V3M7 8l5-5 5 5M5 13v6a1 1 0 0 0 1 1h12a1 1 0 0 0 1-1v-6")
    val Rotate = strokeIcon("rotate", "M20 12a8 8 0 1 1-2.3-5.7M20 4v5h-5")
    val Delete = strokeIcon("delete", "M5 7h14M10 7V5h4v2M7 7l1 13h8l1-13")
}
