package com.mowtiie.flashback.ui.decks

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mowtiie.flashback.repository.FlashbackRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn

@OptIn(ExperimentalCoroutinesApi::class)
class DeckListViewModel(
    repository: FlashbackRepository,
    private val clock: () -> Long = System::currentTimeMillis,
) : ViewModel() {

    /** Reissued on resume so due counts follow the clock. */
    private val now = MutableStateFlow(clock())
    private val selectedTagId = MutableStateFlow<Long?>(null)

    val uiState: StateFlow<DeckListUiState> = combine(
        now.flatMapLatest { repository.observeDeckSummaries(it) },
        repository.observeDecksWithTags(),
        repository.observeAllTags(),
        selectedTagId,
        ::buildDeckListState,
    ).stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), DeckListUiState())

    fun selectTag(tagId: Long?) {
        selectedTagId.value = tagId
    }

    fun refresh() {
        now.value = clock()
    }

    private companion object {
        const val STOP_TIMEOUT_MS = 5_000L
    }
}
