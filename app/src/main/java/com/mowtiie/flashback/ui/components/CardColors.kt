package com.mowtiie.flashback.ui.components

import androidx.compose.material3.CardColors
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable

/** Outlined cards sit a step above the page, so they separate in dark theme too, not by a thin line alone. */
@Composable
fun flashbackCardColors(): CardColors =
    CardDefaults.outlinedCardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)
