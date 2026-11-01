package com.mowtiie.flashback.generator

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class PlainCardsTest {

    @Test
    fun readsTitleAndCards() {
        val parsed = PlainCards.parse(
            "Title: Cells\n\nQ: What makes ATP?\nA: Mitochondria\n\nQ: Where is DNA kept?\nA: The nucleus\n",
            complete = true,
        )

        assertEquals("Cells", parsed.title)
        assertEquals(listOf("What makes ATP?", "Where is DNA kept?"), parsed.cards.map { it.front })
        assertEquals("The nucleus", parsed.cards[1].back)
    }

    @Test
    fun toleratesMarkdownNumberingAndLabels() {
        val parsed = PlainCards.parse(
            "**Title:** Cells\n1. **Q:** What makes ATP?\n   **A:** Mitochondria\n- Question 2: Where is DNA?\n- Answer: Nucleus\n",
            complete = true,
        )

        assertEquals("Cells", parsed.title)
        assertEquals(2, parsed.cards.size)
        assertEquals("Mitochondria", parsed.cards[0].back)
        assertEquals("Nucleus", parsed.cards[1].back)
    }

    @Test
    fun joinsAnAnswerThatContinuesOnTheNextLine() {
        val parsed = PlainCards.parse("Q: Why?\nA: Because it is\nfolded into cristae.\n", complete = true)

        assertEquals("Because it is folded into cristae.", parsed.cards.single().back)
    }

    @Test
    fun skipsACardWithoutAnAnswer() {
        val parsed = PlainCards.parse("Q: One?\nQ: Two?\nA: Yes\n", complete = true)

        assertEquals(listOf("Two?"), parsed.cards.map { it.front })
    }

    @Test
    fun whileStreamingTheCardBeingWrittenIsLeftOut() {
        val text = "Q: One?\nA: Yes\n\nQ: Two?\nA: Ye"

        assertEquals(listOf("One?"), PlainCards.parse(text, complete = false).cards.map { it.front })
    }

    @Test
    fun defaultsTheTitleToNull() {
        assertNull(PlainCards.parse("Q: One?\nA: Yes\n", complete = true).title)
    }

    @Test
    fun stopsWhenAQuestionRepeats() {
        val text = "Q: One?\nA: Yes\n\nQ: Two?\nA: No\n\nQ: one?\nA: Yes\n\nQ: Three?\nA: "

        assertTrue(PlainCards.shouldStop(text, maxCards = 30))
    }

    @Test
    fun stopsAtTheCardLimit() {
        val text = "Q: One?\nA: Yes\n\nQ: Two?\nA: No\n\nQ: Three?\nA: Maybe\n"

        assertFalse(PlainCards.shouldStop(text, maxCards = 3))
        assertTrue(PlainCards.shouldStop(text + "\nQ: Four?\nA: x\n", maxCards = 3))
    }

    @Test
    fun keepsGoingWhileCardsAreNew() {
        assertFalse(PlainCards.shouldStop("Q: One?\nA: Yes\n\nQ: Two?\nA: N", maxCards = 30))
    }

    @Test
    fun namesTheDeckFromTheFirstLineOfTheMaterial() {
        assertEquals("Cell biology notes", PlainCards.titleFrom("\n## Cell biology notes.\nMitochondria..."))
        assertEquals("Mitochondria are organelles found in", PlainCards.titleFrom("Mitochondria are organelles found in most eukaryotic cells."))
        assertEquals(PlainCards.DEFAULT_TITLE, PlainCards.titleFrom("  \n "))
    }

    @Test
    fun buildsArchiveJsonWithoutRepeats() {
        val parsed = PlainCards.parse("Title: T\nQ: One?\nA: Yes\nQ: ONE?\nA: Yes\nQ: Two?\nA: No\n", complete = true)
        val deck = deckFromModelText(PlainCards.toArchiveJson(parsed), maxCards = 30)

        assertEquals("T", deck.name)
        assertEquals(2, deck.notes.size)
    }
}
