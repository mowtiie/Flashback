package com.mowtiie.flashback.ui.tags

/** Fixed colours that stay legible at chip size on light and dark surfaces. Same as the Java app. */
object TagPalette {
    /** ARGB ints, in the order of the `tag_palette_names` string array. */
    val colors: List<Int> = listOf(
        0xFFE5484D.toInt(), // red
        0xFFF76B15.toInt(), // orange
        0xFFFFB224.toInt(), // amber
        0xFF7CB518.toInt(), // lime
        0xFF30A46C.toInt(), // green
        0xFF12A594.toInt(), // teal
        0xFF00A2C7.toInt(), // cyan
        0xFF3E63DD.toInt(), // blue
        0xFF6E56CF.toInt(), // indigo
        0xFF8E4EC6.toInt(), // purple
        0xFFD6409F.toInt(), // pink
        0xFF8B8D98.toInt(), // slate
    )

    val default: Int = colors.first()
}
