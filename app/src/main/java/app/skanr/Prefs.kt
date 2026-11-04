package app.skanr

import android.content.Context
import android.content.SharedPreferences
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.content.edit

object Prefs {
    private lateinit var sp: SharedPreferences

    var theme by mutableStateOf("dark")
        private set

    fun init(context: Context) {
        sp = context.getSharedPreferences("skanr", Context.MODE_PRIVATE)
        theme = sp.getString("theme", "dark") ?: "dark"
    }

    var onboarded: Boolean
        get() = sp.getBoolean("onboarded", false)
        set(value) = sp.edit { putBoolean("onboarded", value) }

    fun changeTheme(value: String) {
        theme = value
        sp.edit { putString("theme", value) }
    }
}
