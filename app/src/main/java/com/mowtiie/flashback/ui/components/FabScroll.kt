package com.mowtiie.flashback.ui.components

import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow

data class ScrollPosition(val index: Int, val offset: Int) {
    operator fun compareTo(other: ScrollPosition): Int =
        if (index != other.index) index.compareTo(other.index) else offset.compareTo(other.offset)
}

/** The floating button shrinks to its icon while the list moves down and returns when it moves up. */
object FabScroll {
    fun expanded(previous: ScrollPosition, current: ScrollPosition, wasExpanded: Boolean): Boolean = when {
        current.index == 0 && current.offset == 0 -> true
        current > previous -> false
        current < previous -> true
        else -> wasExpanded
    }
}

@Composable
fun rememberFabExpanded(state: LazyListState): Boolean {
    var expanded by remember { mutableStateOf(true) }
    LaunchedEffect(state) {
        var previous = ScrollPosition(state.firstVisibleItemIndex, state.firstVisibleItemScrollOffset)
        snapshotFlow { ScrollPosition(state.firstVisibleItemIndex, state.firstVisibleItemScrollOffset) }.collect { current ->
            expanded = FabScroll.expanded(previous, current, expanded)
            previous = current
        }
    }
    return expanded
}
