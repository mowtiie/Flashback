package com.mowtiie.flashback.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalWindowInfo

/** Below this height, such as a phone in landscape, floating buttons cover too much of the list. */
private const val SHORT_WINDOW_DP = 480

@Composable
fun isShortWindow(): Boolean {
    val heightDp = with(LocalDensity.current) { LocalWindowInfo.current.containerSize.height.toDp().value }
    return heightDp < SHORT_WINDOW_DP
}
