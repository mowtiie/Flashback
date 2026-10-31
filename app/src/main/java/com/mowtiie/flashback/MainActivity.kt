package com.mowtiie.flashback

import android.content.res.Configuration
import android.graphics.Color
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.core.graphics.drawable.toDrawable
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.mowtiie.flashback.theme.FlashbackTheme
import com.mowtiie.flashback.theme.ThemeMode
import com.mowtiie.flashback.ui.navigation.FlashbackNavHost

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val container = (application as FlashbackApp).container
        paintWindow(isDark(container.themePreferences.settings.value.mode, systemDark()))

        setContent {
            val settings by container.themePreferences.settings.collectAsStateWithLifecycle()
            val darkTheme = isDark(settings.mode, isSystemInDarkTheme())

            // System bar icons follow the chosen theme, not the system one.
            DisposableEffect(darkTheme) {
                enableEdgeToEdge(
                    statusBarStyle = SystemBarStyle.auto(Color.TRANSPARENT, Color.TRANSPARENT) { darkTheme },
                    navigationBarStyle = SystemBarStyle.auto(LIGHT_SCRIM, DARK_SCRIM) { darkTheme },
                )
                paintWindow(darkTheme)
                onDispose {}
            }

            FlashbackTheme(
                darkTheme = darkTheme,
                dynamicColor = settings.dynamicColor,
                contrast = settings.contrast,
            ) {
                FlashbackNavHost(container)
            }
        }
    }

    private fun systemDark() =
        resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK == Configuration.UI_MODE_NIGHT_YES

    private fun isDark(mode: ThemeMode, systemDark: Boolean) = when (mode) {
        ThemeMode.System -> systemDark
        ThemeMode.Light -> false
        ThemeMode.Dark -> true
    }

    /** The theme's window colour follows the system; a forced mode needs it set by hand to avoid a flash. */
    private fun paintWindow(dark: Boolean) {
        val color = getColor(if (dark) R.color.window_background_dark else R.color.window_background_light)
        window.setBackgroundDrawable(color.toDrawable())
    }

    private companion object {
        val LIGHT_SCRIM = Color.argb(0xe6, 0xFF, 0xFF, 0xFF)
        val DARK_SCRIM = Color.argb(0x80, 0x1b, 0x1b, 0x1b)
    }
}
