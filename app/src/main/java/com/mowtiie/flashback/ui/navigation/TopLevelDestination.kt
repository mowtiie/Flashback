package com.mowtiie.flashback.ui.navigation

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import com.mowtiie.flashback.R

/** The four screens in the bottom bar (or rail). Anything else hides it. */
enum class TopLevelDestination(
    val route: String,
    @param:StringRes val label: Int,
    /** Shown while the destination is not selected. */
    @param:DrawableRes val icon: Int,
    /** Shown while the destination is selected. */
    @param:DrawableRes val selectedIcon: Int,
) {
    Decks(Routes.DECKS, R.string.nav_decks, R.drawable.ic_decks_outlined, R.drawable.ic_decks),
    Tags(Routes.TAGS, R.string.nav_tags, R.drawable.ic_tags_outlined, R.drawable.ic_tags),
    Stats(Routes.STATS, R.string.nav_stats, R.drawable.ic_stats_outlined, R.drawable.ic_stats),
    Settings(Routes.SETTINGS, R.string.nav_settings, R.drawable.ic_settings_outlined, R.drawable.ic_settings),
}
