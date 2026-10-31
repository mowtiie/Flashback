package com.mowtiie.flashback.generator

import com.mowtiie.flashback.data.io.ArchiveParser
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ChatPromptTest {

    private fun prompt(
        coverage: PromptCoverage = PromptCoverage.Everything,
        types: CardTypes = CardTypes.Setting,
        language: String = "",
        deckName: String = "",
    ) = ChatPrompt.build(ChatPromptOptions(coverage, types, language, deckName))

    /** The first JSON object in the prompt is the example the model is asked to copy. */
    private fun example(text: String): String {
        val start = text.indexOf("{\"version\"")
        var depth = 0
        for (i in start until text.length) {
            if (text[i] == '{') depth++
            if (text[i] == '}' && --depth == 0) return text.substring(start, i + 1)
        }
        error("no example in prompt")
    }

    @Test
    fun theExampleInEveryPromptIsAcceptedByTheParser() {
        for (coverage in PromptCoverage.entries) {
            for (types in CardTypes.entries) {
                val archive = ArchiveParser().parse(example(prompt(coverage, types)))
                assertEquals(1, archive.decks.size)
                assertTrue("$types has no notes", archive.decks.single().notes.isNotEmpty())
            }
        }
    }

    @Test
    fun everythingAsksForNoCardLimitAndFullCoverage() {
        val text = prompt(PromptCoverage.Everything)
        assertTrue(text.contains("every fact", ignoreCase = true))
        assertFalse(text.contains("at most", ignoreCase = true))
    }

    @Test
    fun keyPointsAsksForTheMainIdeasOnly() {
        val text = prompt(PromptCoverage.KeyPoints)
        assertTrue(text.contains("main ideas", ignoreCase = true))
        assertFalse(text.contains("every fact", ignoreCase = true))
    }

    @Test
    fun cardTypesAreNamedInThePrompt() {
        assertFalse(prompt(types = CardTypes.Setting).contains("answerMode"))
        assertTrue(prompt(types = CardTypes.Flip).contains("\"flip\""))
        assertTrue(prompt(types = CardTypes.Type).contains("\"type\""))
        assertTrue(prompt(types = CardTypes.Choice).contains("\"choices\""))
        val mixed = prompt(types = CardTypes.Mixed)
        assertTrue(mixed.contains("\"flip\"") && mixed.contains("\"type\"") && mixed.contains("\"choice\""))
    }

    @Test
    fun theLimitsTheParserEnforcesAreStated() {
        val text = prompt(types = CardTypes.Choice)
        assertTrue(text.contains("120"))
        assertTrue(text.contains("3 wrong"))
    }

    @Test
    fun languageAndDeckNameAreUsedWhenGiven() {
        val text = prompt(language = "Filipino", deckName = "Cell Biology")
        assertTrue(text.contains("Filipino"))
        assertTrue(text.contains("Cell Biology"))
        assertTrue(ArchiveParser().parse(example(text)).decks.single().name == "Cell Biology")
    }

    @Test
    fun withoutALanguageTheMaterialsLanguageIsUsed() {
        assertTrue(prompt().contains("language of the material", ignoreCase = true))
    }

    @Test
    fun aQuoteInTheDeckNameStillGivesValidJson() {
        val archive = ArchiveParser().parse(example(prompt(deckName = "The \"Big\" Test")))
        assertEquals("The \"Big\" Test", archive.decks.single().name)
    }

    @Test
    fun theReplyFormatAndLongOutputAreExplained() {
        val text = prompt()
        assertTrue(text.contains("flashback-cards.json"))
        assertTrue(text.contains("part 1"))
        assertTrue(text.contains("material", ignoreCase = true))
    }
}
