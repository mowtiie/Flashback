package com.mowtiie.flashback.generator

import com.mowtiie.flashback.FakePreferenceStore
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

        override suspend fun complete(system: String, user: String): String {
            calls++
            this.system = system
            this.user = user
            return reply
        }
    }

    private val goodReply =
        """{"version":1,"decks":[{"name":"Cells","notes":[{"front":"Powerhouse?","back":"Mitochondria"}]}]}"""

    @Test
    fun parsesTheModelReplyIntoADeck() = runTest {
        val deck = LocalCardGenerator(FakeModel(goodReply)).generate(textRequest("Mitochondria make energy."))

        assertEquals("Cells", deck.name)
        assertEquals("Mitochondria", deck.notes.single().back)
    }

    @Test
    fun acceptsAReplyInACodeFence() = runTest {
        val deck = LocalCardGenerator(FakeModel("```json\n$goodReply\n```")).generate(textRequest("x"))

        assertEquals(1, deck.notes.size)
    }

    @Test
    fun asksForJsonAndWrapsTheMaterial() = runTest {
        val model = FakeModel(goodReply)
        LocalCardGenerator(model).generate(textRequest("Some notes"))

        assertTrue(model.system.contains("Reply with JSON only"))
        assertTrue(model.user.contains("<material>"))
        assertTrue(model.user.contains("Some notes"))
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
    fun reportsAReplyThatIsNotJson() = runTest {
        try {
            LocalCardGenerator(FakeModel("Sure! Here are some cards.")).generate(textRequest("x"))
            fail("expected a GeneratorException")
        } catch (e: GeneratorException) {
            assertEquals(FailureKind.Other, e.kind)
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
