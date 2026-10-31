package com.mowtiie.flashback.ui.tags

import androidx.compose.runtime.Immutable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mowtiie.flashback.data.model.TagWithCount
import com.mowtiie.flashback.repository.FlashbackRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

@Immutable
data class TagsUiState(
    /** False until the first query lands, so the empty state doesn't flash. */
    val loaded: Boolean = false,
    val tags: List<TagWithCount> = emptyList(),
)

class TagsViewModel(repository: FlashbackRepository) : ViewModel() {

    val uiState: StateFlow<TagsUiState> = repository.observeTagsWithCounts()
        .map { TagsUiState(loaded = true, tags = it) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), TagsUiState())

    private companion object {
        const val STOP_TIMEOUT_MS = 5_000L
    }
}
