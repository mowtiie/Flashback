package com.mowtiie.flashback.generator

import com.mowtiie.flashback.FakePreferenceStore
import com.mowtiie.flashback.ui.study.AnswerMode
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test

class LocalCardGeneratorTest {

    private class FakeModel(private val reply: String) : TextModel {
        var system = ""
        var user = ""
        var calls = 0
        var stopAsked: Boolean? = null

        override suspend fun complete(system: String, user: String, shouldStop: (String) -> Boolean): String {
            calls++
            this.system = system
            this.user = user
            stopAsked = shouldStop(reply)
            return reply
        }
    }

    private val goodReply = "Title: Cells\n\nQ: Powerhouse?\nA: Mitochondria\n"

    @Test
    fun buildsADeckFromTheModelLines() = runTest {
        val deck = LocalCardGenerator(FakeModel(goodReply)).generate(textRequest("Mitochondria make energy."))

        assertEquals("Cells", deck.name)
        assertEquals("Mitochondria", deck.notes.single().back)
    }

    @Test
    fun namesTheDeckFromTheMaterialWhenTheModelGivesNoTitle() = runTest {
        val deck = LocalCardGenerator(FakeModel("Q: One?\nA: Yes\n")).generate(textRequest("Cell biology\nMitochondria make ATP."))

        assertEquals("Cell biology", deck.name)
    }

    @Test
    fun asksForPlainLinesAndWrapsTheMaterial() = runTest {
        val model = FakeModel(goodReply)
        LocalCardGenerator(model).generate(textRequest("Some notes"))

        assertTrue(model.system.contains("\nQ: ") && model.system.contains("\nA: "))
        assertFalse(model.system.contains("JSON"))
        assertTrue(model.user.contains("<material>"))
        assertTrue(model.user.contains("Some notes"))
    }

    @Test
    fun tellsTheModelToStopWhenItLoops() = runTest {
        val model = FakeModel("Q: One?\nA: Yes\n\nQ: One?\nA: Yes\n\nQ: Two?\nA: ")
        LocalCardGenerator(model).generate(textRequest("x"))

        assertEquals(true, model.stopAsked)
    }

    @Test
    fun appliesTheChosenCardTypes() = runTest {
        val request = GeneratorRequest(GeneratorInput.Text("x"), cardTypes = CardTypes.Type)
        val deck = LocalCardGenerator(FakeModel(goodReply)).generate(request)

        assertEquals(AnswerMode.Type.value, deck.notes.single().answerMode)
    }

    @Test
    fun refusesAPdfWithoutCallingTheModel() = runTest {
        val model = FakeModel(goodReply)
        try {
            LocalCardGenerator(model).generate(pdfRequest(FAKE_PDF))
            fail("expected a GeneratorException")
        } catch (e: GeneratorException) {
            assertTrue(e.message!!.contains("text only"))
        }
        assertEquals(0, model.calls)
    }

    @Test
    fun reportsAReplyWithNoCards() = runTest {
        try {
            LocalCardGenerator(FakeModel("Sure! Here are some cards.")).generate(textRequest("x"))
            fail("expected a GeneratorException")
        } catch (e: GeneratorException) {
            assertTrue(e.message!!.contains("usable cards"))
        }
    }

    @Test
    fun reportsAnEmptyReply() = runTest {
        try {
            LocalCardGenerator(FakeModel("  ")).generate(textRequest("x"))
            fail("expected a GeneratorException")
        } catch (e: GeneratorException) {
            assertTrue(e.message!!.contains("no cards"))
        }
    }

    @Test
    fun doesNotShowTheModelTheCardsAlreadyMade() = runTest {
        val model = FakeModel(goodReply)
        val request = GeneratorRequest(
            GeneratorInput.Text("Some notes"),
            coverage = Coverage.Everything,
            known = listOf("What makes ATP?", "Where is DNA kept?"),
        )
        LocalCardGenerator(model).generate(request)

        assertFalse(model.system.contains("What makes ATP?"))
        assertFalse(model.system.contains("already_made"))
    }

    @Test
    fun aSectionCannotAskForMoreCardsThanItsTextAllows() {
        val everything = GeneratorRequest(GeneratorInput.Text("x"), coverage = Coverage.Everything)
        assertEquals(53, LocalCardGenerator.maxCards(everything, 3_000))
        assertEquals(3, LocalCardGenerator.maxCards(everything, 0))

        val keyPoints = GeneratorRequest(GeneratorInput.Text("x"), CardCount.Few, coverage = Coverage.KeyPoints)
        assertEquals(CardCount.Few.max, LocalCardGenerator.maxCards(keyPoints, 3_000))
    }

    @Test
    fun mergingOnDeviceSectionsDropsRewordedRepeats() {
        fun deck(vararg pairs: Pair<String, String>) = com.mowtiie.flashback.data.io.DeckArchive.Deck(
            name = "d",
            notes = pairs.map { com.mowtiie.flashback.data.io.DeckArchive.Note(front = it.first, back = it.second) },
        )
        val first = deck("What is the nickname for mitochondria?" to "Powerhouse of the cell")
        val second = deck(
            "What is the nickname for mitochondria because of their role in energy production?" to "The powerhouse",
            "What is the endosymbiotic theory?" to "Mitochondria were once free-living bacteria",
        )

        assertEquals(3, CoverageMerger.merge(listOf(first, second)).notes.size)
        val merged = CoverageMerger.merge(listOf(first, second), nearDuplicates = true)
        assertEquals(2, merged.notes.size)
        assertEquals(listOf(1, 1), merged.perSection)
    }

    @Test
    fun onDeviceNeedsAModelNotAKey() {
        val prefs = GeneratorPreferences(FakePreferenceStore())
        prefs.provider = GeneratorProvider.OnDevice
        assertFalse(prefs.hasApiKey)

        prefs.localModelName = "gemma.litertlm"
        assertTrue(prefs.hasApiKey)
        assertNull(prefs.apiKey)

        prefs.apiKey = "ignored"
        assertNull(prefs.apiKey)

        prefs.localModelName = null
        assertFalse(prefs.hasApiKey)
    }

    @Test
    fun onDeviceDoesNotAffectTheOnlineKeys() {
        val prefs = GeneratorPreferences(FakePreferenceStore())
        prefs.setApiKey(GeneratorProvider.Anthropic, "anthropic-key")
        prefs.localModelName = "gemma.litertlm"
        prefs.provider = GeneratorProvider.Anthropic

        assertTrue(prefs.hasApiKey)
        assertEquals("anthropic-key", prefs.apiKey)
    }
}
