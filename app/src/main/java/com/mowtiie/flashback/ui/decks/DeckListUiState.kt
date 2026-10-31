package com.mowtiie.flashback.ui.decks

import androidx.compose.runtime.Immutable
import com.mowtiie.flashback.data.entity.Tag
import com.mowtiie.flashback.data.model.DeckListItem
import com.mowtiie.flashback.data.model.DeckSummary
import com.mowtiie.flashback.data.model.DeckWithTags

@Immutable
data class DeckListUiState(
    /** False until the first query lands, so the empty state doesn't flash. */
    val loaded: Boolean = false,
    val items: List<DeckListItem> = emptyList(),
    val tags: List<Tag> = emptyList(),
    val selectedTagId: Long? = null,
) {
    val isFiltered: Boolean get() = selectedTagId != null
}

/** A selected tag that no longer exists is dropped so the list can't get stuck empty. */
internal fun buildDeckListState(
    summaries: List<DeckSummary>,
    decksWithTags: List<DeckWithTags>,
    allTags: List<Tag>,
    selectedTagId: Long?,
): DeckListUiState {
    val selected = selectedTagId?.takeIf { id -> allTags.any { it.id == id } }
    val tagsByDeck = decksWithTags.associate { it.deck.id to it.tags }

    val items = summaries.mapNotNull { summary ->
        val tags = tagsByDeck[summary.deck.id].orEmpty()
        if (selected != null && tags.none { it.id == selected }) {
            null
        } else {
            DeckListItem(summary, tags)
        }
    }
    return DeckListUiState(
        loaded = true,
        items = items,
        tags = allTags,
        selectedTagId = selected,
    )
}
