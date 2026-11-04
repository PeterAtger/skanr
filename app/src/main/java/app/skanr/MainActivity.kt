package app.skanr

import android.graphics.Color
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.LaunchedEffect
import app.skanr.data.DocStore
import app.skanr.scan.Scanner
import app.skanr.ui.SkanrApp
import app.skanr.ui.theme.LocalTokens
import app.skanr.ui.theme.SkanrTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Prefs.init(this)
        DocStore.init(this)
        Scanner.prepare(applicationContext)
        setContent {
            SkanrTheme(Prefs.theme) {
                val dark = LocalTokens.current.isDark
                LaunchedEffect(dark) {
                    val style = if (dark) {
                        SystemBarStyle.dark(Color.TRANSPARENT)
                    } else {
                        SystemBarStyle.light(Color.TRANSPARENT, Color.TRANSPARENT)
                    }
                    enableEdgeToEdge(statusBarStyle = style, navigationBarStyle = style)
                }
                SkanrApp()
            }
        }
    }
}
