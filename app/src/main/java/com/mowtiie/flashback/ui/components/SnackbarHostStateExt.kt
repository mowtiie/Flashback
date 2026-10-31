package com.mowtiie.flashback.ui.components

import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult

/**
 * Shows [message] in place of any snackbar still on screen. It suspends until dismissed,
 * so call it from its own `launch` and a waiting snackbar never holds up the next event.
 */
suspend fun SnackbarHostState.showReplacing(
    message: String,
    actionLabel: String? = null,
    duration: SnackbarDuration = SnackbarDuration.Short,
): SnackbarResult {
    currentSnackbarData?.dismiss()
    return showSnackbar(message = message, actionLabel = actionLabel, duration = duration)
}
