package app.skanr.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import app.skanr.R

@Immutable
data class Tokens(
    val desk: Color,
    val paper: Color,
    val sheet: Color,
    val graphite: Color,
    val pencil: Color,
    val rule: Color,
    val ink: Color,
    val onInk: Color,
    val inkSoft: Color,
    val page: Color,
    val pageInk: Color,
    val pageInkSoft: Color,
    val scrim: Color,
    val toast: Color,
    val toastFg: Color,
    val isDark: Boolean,
)

val LightTokens = Tokens(
    desk = Color(0xFFDCD9D5),
    paper = Color(0xFFEFEDEA),
    sheet = Color(0xFFFAF9F8),
    graphite = Color(0xFF26221F),
    pencil = Color(0xFF6E6760),
    rule = Color(0xFFD2CDC7),
    ink = Color(0xFF4B3122),
    onInk = Color(0xFFF7F2ED),
    inkSoft = Color(0xFFE7DED5),
    page = Color(0xFFFFFFFF),
    pageInk = Color(0xFFABA49D),
    pageInkSoft = Color(0xFFD6D1CB),
    scrim = Color(0x731E1610),
    toast = Color(0xFF2E2925),
    toastFg = Color(0xFFF3EFEB),
    isDark = false,
)

val DarkTokens = Tokens(
    desk = Color(0xFF121110),
    paper = Color(0xFF1A1817),
    sheet = Color(0xFF24211F),
    graphite = Color(0xFFECE8E4),
    pencil = Color(0xFFA39C95),
    rule = Color(0xFF36312D),
    ink = Color(0xFFC9A486),
    onInk = Color(0xFF2A1B10),
    inkSoft = Color(0xFF3A2E25),
    page = Color(0xFFDAD6D0),
    pageInk = Color(0xFF948C84),
    pageInkSoft = Color(0xFFBDB6AE),
    scrim = Color(0x99000000),
    toast = Color(0xFFE6E1DC),
    toastFg = Color(0xFF26221F),
    isDark = true,
)

val LocalTokens = staticCompositionLocalOf { LightTokens }

@OptIn(ExperimentalTextApi::class)
private fun geist(weight: Int) = Font(
    R.font.geist,
    weight = FontWeight(weight),
    variationSettings = FontVariation.Settings(FontVariation.weight(weight)),
)

val Geist = FontFamily(geist(400), geist(500), geist(600))
val Serif = FontFamily(Font(R.font.instrument_serif))

object Type {
    val wordmark = TextStyle(fontFamily = Geist, fontWeight = FontWeight(600), fontSize = 28.sp, letterSpacing = (-0.06).em)
    val hero = TextStyle(fontFamily = Serif, fontSize = 46.sp, lineHeight = 46.sp)
    val title = TextStyle(fontFamily = Serif, fontSize = 25.sp, lineHeight = 28.sp)
    val sheetTitle = TextStyle(fontFamily = Serif, fontSize = 28.sp, lineHeight = 31.sp)
    val emptyTitle = TextStyle(fontFamily = Serif, fontSize = 32.sp, lineHeight = 35.sp)
    val body = TextStyle(fontFamily = Geist, fontWeight = FontWeight(400), fontSize = 16.sp, lineHeight = 24.sp)
    val strong = TextStyle(fontFamily = Geist, fontWeight = FontWeight(600), fontSize = 16.sp, lineHeight = 21.sp)
    val meta = TextStyle(fontFamily = Geist, fontWeight = FontWeight(400), fontSize = 13.5.sp, lineHeight = 18.sp, fontFeatureSettings = "tnum")
    val label = TextStyle(fontFamily = Geist, fontWeight = FontWeight(600), fontSize = 13.sp, lineHeight = 17.sp)
    val small = TextStyle(fontFamily = Geist, fontWeight = FontWeight(400), fontSize = 12.5.sp, lineHeight = 16.sp)
    val button = TextStyle(fontFamily = Geist, fontWeight = FontWeight(600), fontSize = 15.5.sp, letterSpacing = (-0.005).em)
    val scan = TextStyle(fontFamily = Geist, fontWeight = FontWeight(500), fontSize = 18.sp, letterSpacing = (-0.01).em)
}

@Composable
fun SkanrTheme(themePref: String, content: @Composable () -> Unit) {
    val dark = when (themePref) {
        "light" -> false
        "dark" -> true
        else -> isSystemInDarkTheme()
    }
    val t = if (dark) DarkTokens else LightTokens
    val base = if (dark) darkColorScheme() else lightColorScheme()
    val scheme = base.copy(
        primary = t.ink,
        onPrimary = t.onInk,
        background = t.paper,
        onBackground = t.graphite,
        surface = t.sheet,
        onSurface = t.graphite,
        surfaceContainer = t.sheet,
        surfaceContainerHigh = t.sheet,
        surfaceContainerLow = t.sheet,
        onSurfaceVariant = t.pencil,
        outline = t.rule,
        scrim = t.scrim,
    )
    CompositionLocalProvider(LocalTokens provides t) {
        MaterialTheme(colorScheme = scheme, content = content)
    }
}
