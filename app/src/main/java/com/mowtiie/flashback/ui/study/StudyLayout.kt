package com.mowtiie.flashback.ui.study

/** Material's compact window height: below this a software keyboard leaves almost no room. */
internal const val COMPACT_HEIGHT_DP = 480f

/** True when typing on a short window needs the top bar and counters out of the way. */
internal fun isCompactForKeyboard(windowHeightDp: Float, keyboardOpen: Boolean): Boolean =
    keyboardOpen && windowHeightDp < COMPACT_HEIGHT_DP
