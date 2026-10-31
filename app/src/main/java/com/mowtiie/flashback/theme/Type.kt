package com.mowtiie.flashback.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.font.FontFamily

/**
 * Material defaults, except that titleLarge, which the top app bar uses for its title,
 * is set in a serif face. Nothing else in the app uses that style.
 */
val Typography = Typography().let { defaults ->
    defaults.copy(titleLarge = defaults.titleLarge.copy(fontFamily = FontFamily.Serif))
}
