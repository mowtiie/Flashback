package com.mowtiie.flashback.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalContext

enum class ContrastLevel { Standard, Medium, High }

/** Contrast only applies when dynamic colour is off or unavailable. */
@Composable
fun FlashbackTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = true,
    contrast: ContrastLevel = ContrastLevel.Standard,
    content: @Composable () -> Unit,
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }

        else -> staticColorScheme(darkTheme, contrast)
    }

    CompositionLocalProvider(
        LocalCountColors provides if (darkTheme) DarkCountColors else LightCountColors,
    ) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = Typography,
            content = content,
        )
    }
}

internal fun staticColorScheme(darkTheme: Boolean, contrast: ContrastLevel): ColorScheme =
    when (contrast) {
        ContrastLevel.Standard -> if (darkTheme) darkScheme else lightScheme
        ContrastLevel.Medium ->
            if (darkTheme) darkSchemeMediumContrast else lightSchemeMediumContrast
        ContrastLevel.High -> if (darkTheme) darkSchemeHighContrast else lightSchemeHighContrast
    }
