package com.mowtiie.flashback

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.asCoroutineDispatcher
import java.util.concurrent.Executors

/**
 * [write] is a single thread, so writes keep a predictable order for undo.
 * [io] is for heavier work that must not queue behind answers and undo.
 */
class AppDispatchers(
    val write: CoroutineDispatcher,
    val io: CoroutineDispatcher,
) {
    companion object {
        fun default() = AppDispatchers(
            write = Executors.newSingleThreadExecutor().asCoroutineDispatcher(),
            io = Dispatchers.IO,
        )
    }
}
