package com.mowtiie.flashback.ui.decks

import com.mowtiie.flashback.data.entity.Deck
import com.mowtiie.flashback.data.entity.Tag
import com.mowtiie.flashback.data.model.DeckSummary
import com.mowtiie.flashback.data.model.DeckWithTags
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class DeckListUiStateTest {

    private val languages = Tag(id = 1, name = "Languages")
    private val science = Tag(id = 2, name = "Science")

    private fun summary(id: Long) = DeckSummary(
        deck = Deck(id = id, name = "Deck $id"),
        newCount = 0,
        learnCount = 0,
        dueCount = 0,
        totalCount = 0,
        newToday = 0,
        reviewsToday = 0,
    )

    private val summaries = listOf(summary(1), summary(2), summary(3))

    private val withTags = listOf(
        DeckWithTags(summaries[0].deck, listOf(languages)),
        DeckWithTags(summaries[1].deck, listOf(languages, science)),
        DeckWithTags(summaries[2].deck, emptyList()),
    )

    private val allTags = listOf(languages, science)

    @Test
    fun noFilterKeepsEveryDeckInOrderWithItsTags() {
        val state = buildDeckListState(summaries, withTags, allTags, null)

        assertTrue(state.loaded)
        assertEquals(listOf(1L, 2L, 3L), state.items.map { it.summary.deck.id })
        assertEquals(listOf(languages, science), state.items[1].tags)
        assertEquals(emptyList<Tag>(), state.items[2].tags)
        assertFalse(state.isFiltered)
    }

    @Test
    fun filterKeepsOnlyDecksWithThatTag() {
        val state = buildDeckListState(summaries, withTags, allTags, science.id)

        assertEquals(listOf(2L), state.items.map { it.summary.deck.id })
        assertEquals(science.id, state.selectedTagId)
        assertTrue(state.isFiltered)
    }

    @Test
    fun deletedSelectedTagFallsBackToAllDecks() {
        val state = buildDeckListState(summaries, withTags, listOf(languages), science.id)

        assertNull(state.selectedTagId)
        assertEquals(3, state.items.size)
    }

    @Test
    fun deckMissingFromTagQueryHasNoTags() {
        val state = buildDeckListState(summaries, emptyList(), allTags, null)

        assertEquals(3, state.items.size)
        assertTrue(state.items.all { it.tags.isEmpty() })
    }

    @Test
    fun filterWithNoMatchesGivesLoadedEmptyList() {
        val state = buildDeckListState(listOf(summaries[2]), withTags, allTags, languages.id)

        assertTrue(state.loaded)
        assertTrue(state.items.isEmpty())
        assertTrue(state.isFiltered)
    }
}
