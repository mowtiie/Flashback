package com.mowtiie.flashback.ui

import androidx.annotation.StringRes
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.receiveAsFlow

/** [onAction] runs when the user taps the action, e.g. an undo. */
class AppMessage(
    @param:StringRes val text: Int,
    @param:StringRes val actionLabel: Int? = null,
    val onAction: (suspend () -> Unit)? = null,
)

/** Snackbar messages that outlive the screen that raised them, e.g. after a delete pops it. */
class AppMessenger {
    private val channel = Channel<AppMessage>(Channel.BUFFERED)

    val messages: Flow<AppMessage> = channel.receiveAsFlow()

    fun show(@StringRes text: Int) {
        channel.trySend(AppMessage(text))
    }

    fun showWithAction(@StringRes text: Int, @StringRes actionLabel: Int, onAction: suspend () -> Unit) {
        channel.trySend(AppMessage(text, actionLabel, onAction))
    }
}
