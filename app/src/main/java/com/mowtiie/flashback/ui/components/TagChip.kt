package com.mowtiie.flashback.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.mowtiie.flashback.data.entity.Tag

private const val FILL_ALPHA_LIGHT = 0.14f
private const val FILL_ALPHA_DARK = 0.26f
private const val STROKE_ALPHA = 140 / 255f
private const val NEUTRAL_FILL_ALPHA = 0.06f

/** Longer names are cut with an ellipsis, so one tag cannot fill a whole row. */
private val MAX_CHIP_WIDTH = 160.dp

/** Read-only chip tinted from the tag colour, with the label kept on the surface colour. */
@Composable
fun TagChip(tag: Tag, modifier: Modifier = Modifier) {
    val scheme = MaterialTheme.colorScheme
    val neutral = tag.color == 0
    val accent = if (neutral) scheme.onSurfaceVariant else Color(tag.color)
    val fillAlpha = when {
        neutral -> NEUTRAL_FILL_ALPHA
        scheme.surface.luminance() < 0.5f -> FILL_ALPHA_DARK
        else -> FILL_ALPHA_LIGHT
    }
    // A tag with no colour stays quiet: a hairline border, so it never outweighs the deck name.
    val border = if (neutral) scheme.outlineVariant else accent.copy(alpha = STROKE_ALPHA)

    Surface(
        modifier = modifier.widthIn(max = MAX_CHIP_WIDTH),
        shape = MaterialTheme.shapes.small,
        color = lerp(scheme.surface, accent, fillAlpha),
        contentColor = scheme.onSurface,
        border = BorderStroke(1.dp, border),
    ) {
        Box(Modifier.heightIn(min = 26.dp), contentAlignment = Alignment.Center) {
            Text(
                text = tag.name,
                style = MaterialTheme.typography.labelLarge,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(horizontal = 8.dp),
            )
        }
    }
}
