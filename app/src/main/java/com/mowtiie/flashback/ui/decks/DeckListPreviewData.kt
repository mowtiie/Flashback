package com.mowtiie.flashback.ui.decks

import com.mowtiie.flashback.data.entity.Deck
import com.mowtiie.flashback.data.entity.Tag
import com.mowtiie.flashback.data.model.DeckListItem
import com.mowtiie.flashback.data.model.DeckSummary

internal fun previewDeckListState(): DeckListUiState {
    val language = Tag(id = 1, name = "Languages", color = 0xFF3E63DD.toInt())
    val science = Tag(id = 2, name = "Science", color = 0xFF1E7B4F.toInt())
    return DeckListUiState(
        loaded = true,
        tags = listOf(language, science),
        items = listOf(
            DeckListItem(
                summary = DeckSummary(
                    deck = Deck(
                        id = 1,
                        name = "Spanish vocabulary",
                        description = "Core 2000 word list, chapters 1 to 4",
                    ),
                    newCount = 12,
                    learnCount = 3,
                    dueCount = 47,
                    totalCount = 212,
                    newToday = 0,
                    reviewsToday = 0,
                ),
                tags = listOf(language),
            ),
            DeckListItem(
                summary = DeckSummary(
                    deck = Deck(id = 2, name = "Cell biology"),
                    newCount = 0,
                    learnCount = 0,
                    dueCount = 0,
                    totalCount = 1,
                    newToday = 0,
                    reviewsToday = 0,
                ),
                tags = listOf(science, language),
            ),
        ),
    )
}
