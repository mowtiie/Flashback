package com.mowtiie.flashback.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

/** Card-count colours that Material roles don't cover. Same values as the Java app. */
@Immutable
class CountColors(
    val new: Color,
    val learning: Color,
    val due: Color,
)

internal val LightCountColors = CountColors(
    new = Color(0xFF3E63DD),
    learning = Color(0xFFA2521A),
    due = Color(0xFF1E7B4F),
)

internal val DarkCountColors = CountColors(
    new = Color(0xFF8AB4F8),
    learning = Color(0xFFFFB77C),
    due = Color(0xFF7DD99F),
)

internal val LocalCountColors = staticCompositionLocalOf { LightCountColors }

val MaterialTheme.countColors: CountColors
    @Composable
    @ReadOnlyComposable
    get() = LocalCountColors.current
