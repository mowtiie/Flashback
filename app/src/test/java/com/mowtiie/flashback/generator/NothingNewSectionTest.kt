package com.mowtiie.flashback.generator

import com.mowtiie.flashback.data.io.DeckArchive
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class NothingNewSectionTest {

    private val sections = listOf("one", "two", "three", "four", "five")
    private val calls = mutableListOf<String>()

    private fun nothingNew() = GeneratorException(NO_CARDS_MESSAGE, kind = FailureKind.NoCards)

    private fun deck(front: String) = DeckArchive.Deck(name = "Deck", notes = listOf(DeckArchive.Note(front, "answer")))

    private fun runner(script: (String) -> DeckArchive.Deck) = SectionedGenerator(
        generator = { request ->
            val text = (request.input as GeneratorInput.Text).text
            calls += text
            script(text)
        },
        pause = {},
    )

    private suspend fun SectionedGenerator.runAll(known: List<String>): List<SectionOutcome> {
        val outcomes = mutableListOf<SectionOutcome>()
        run(sections, sections.indices.toList(), CardTypes.Setting, known = { known }) { outcomes += it }
        return outcomes
    }

    @Test
    fun anEmptyPartAfterEarlierCardsIsNotAFailureAndDoesNotStopTheRun() = runTest {
        val outcomes = runner { text ->
            when (text) {
                "one", "five" -> deck("q-$text")
                else -> throw nothingNew()
            }
        }.runAll(known = listOf("q-earlier"))

        assertEquals(sections, calls)
        assertTrue(outcomes.all { it is SectionOutcome.Done })
        assertTrue((outcomes[1] as SectionOutcome.Done).deck.notes.isEmpty())
    }

    @Test
    fun anEmptyFirstPartIsStillAFailure() = runTest {
        val outcomes = runner { throw nothingNew() }.runAll(known = emptyList())

        val first = outcomes.first() as SectionOutcome.Failed
        assertEquals(FailureKind.NoCards, first.kind)
        assertEquals(NO_CARDS_MESSAGE, first.message)
    }

    @Test
    fun otherFailuresStillStopTheRunAfterThreeInARow() = runTest {
        val outcomes = runner { throw GeneratorException("It broke.") }.runAll(known = listOf("q-earlier"))

        assertEquals(3, calls.size)
        assertEquals(5, outcomes.size)
        assertTrue(outcomes.all { it is SectionOutcome.Failed })
    }

    @Test
    fun anEmptyReplyIsTaggedSoItCanBeToldApartFromAnError() {
        try {
            deckFromModelText("""{"version":1,"decks":[{"name":"x","notes":[]}]}""", maxCards = 10)
            org.junit.Assert.fail("expected a GeneratorException")
        } catch (e: GeneratorException) {
            assertEquals(FailureKind.NoCards, e.kind)
            assertEquals(NO_CARDS_MESSAGE, e.message)
        }
    }

    @Test
    fun theMergedDeckTakesTheFirstNonBlankName() {
        val merged = CoverageMerger.merge(
            listOf(
                DeckArchive.Deck(name = "", notes = emptyList()),
                null,
                DeckArchive.Deck(name = "Cells", notes = listOf(DeckArchive.Note("q", "a"))),
            ),
        )

        assertEquals("Cells", merged.name)
    }
}
