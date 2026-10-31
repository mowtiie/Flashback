package com.mowtiie.flashback.ui.export

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import java.io.IOException

/**
 * Holds the export text between building it and the user choosing where to save it.
 * It stays in the ViewModel because it can be megabytes: too big for saved state.
 */
class ExportSession(private val io: CoroutineDispatcher) {
    private var pending: String? = null

    val hasPending: Boolean get() = pending != null

    fun hold(text: String) {
        pending = text
    }

    /** Runs the blocking [writer] off the main thread. False if nothing was held or it failed. */
    suspend fun write(writer: (String) -> Unit): Boolean {
        val text = pending ?: return false
        pending = null
        return withContext(io) {
            try {
                writer(text)
                true
            } catch (e: IOException) {
                false
            }
        }
    }

    fun discard() {
        pending = null
    }
}
