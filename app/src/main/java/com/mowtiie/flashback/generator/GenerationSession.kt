package com.mowtiie.flashback.generator

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

/**
 * What a long generation reports while it runs, so a foreground service can keep the app alive
 * with the screen off and show progress, and its Cancel button can reach the run.
 */
class GenerationSession {
    data class Progress(val finished: Int, val total: Int, val cards: Int)

    private val _progress = MutableStateFlow<Progress?>(null)

    /** Null when nothing is running. */
    val progress: StateFlow<Progress?> = _progress.asStateFlow()

    private var onCancel: (() -> Unit)? = null

    fun begin(total: Int, onCancel: () -> Unit) {
        this.onCancel = onCancel
        _progress.value = Progress(finished = 0, total = total, cards = 0)
    }

    fun update(finished: Int, cards: Int) {
        _progress.update { current -> current?.copy(finished = finished, cards = cards) }
    }

    fun end() {
        onCancel = null
        _progress.value = null
    }

    fun cancel() {
        onCancel?.invoke()
    }
}
