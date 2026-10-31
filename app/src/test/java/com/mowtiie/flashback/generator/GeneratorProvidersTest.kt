package com.mowtiie.flashback.generator

import com.mowtiie.flashback.FakePreferenceStore
import com.mowtiie.flashback.data.io.DeckArchive
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class GeneratorProvidersTest {

    private val store = FakePreferenceStore()
    private val prefs = GeneratorPreferences(store)

    @Test
    fun eachProviderKeepsItsOwnKey() {
        prefs.setApiKey(GeneratorProvider.Anthropic, "anthropic-key")
        prefs.setApiKey(GeneratorProvider.Gemini, "gemini-key")

        assertEquals("anthropic-key", prefs.apiKey(GeneratorProvider.Anthropic))
        assertEquals("gemini-key", prefs.apiKey(GeneratorProvider.Gemini))
    }

    @Test
    fun theSelectedProviderDecidesWhichKeyApiKeyMeans() {
        prefs.setApiKey(GeneratorProvider.Anthropic, "anthropic-key")
        assertEquals("anthropic-key", prefs.apiKey)

        prefs.provider = GeneratorProvider.Gemini
        assertNull(prefs.apiKey)
        assertFalse(prefs.hasApiKey)

        prefs.apiKey = "gemini-key"
        assertEquals("gemini-key", prefs.apiKey(GeneratorProvider.Gemini))
        assertEquals("anthropic-key", prefs.apiKey(GeneratorProvider.Anthropic))
    }

    @Test
    fun anthropicKeepsItsOriginalStorageName() {
        prefs.setApiKey(GeneratorProvider.Anthropic, "k")
        assertTrue(store.values.containsKey("generator_api_key"))
    }

    @Test
    fun defaultsToAnthropicAndIgnoresAnUnknownSavedProvider() {
        assertEquals(GeneratorProvider.Anthropic, prefs.provider)
        store.values["generator_provider"] = 99
        assertEquals(GeneratorProvider.Anthropic, prefs.provider)
    }

    @Test
    fun selectedGeneratorUsesTheProviderChosenWhenTheRunStarts() = runTest {
        val used = mutableListOf<GeneratorProvider>()
        val deck = DeckArchive.Deck("Deck", notes = listOf(DeckArchive.Note("Q", "A")))
        val generator = SelectedCardGenerator(
            prefs,
            GeneratorProvider.entries.associateWith { provider -> CardGenerator { used += provider; deck } },
        )

        generator.generate(textRequest("one"))
        prefs.provider = GeneratorProvider.Gemini
        generator.generate(textRequest("two"))

        assertEquals(listOf(GeneratorProvider.Anthropic, GeneratorProvider.Gemini), used)
    }

    @Test
    fun geminiGeneratorSendsToGoogleWithItsOwnKeyAndNamesGoogleInErrors() = runTest {
        var seenUrl: String? = null
        var seenKey: String? = null
        val transport = object : HttpTransport {
            override suspend fun post(url: String, headers: Map<String, String>, body: String): HttpResponse {
                seenUrl = url
                seenKey = headers["x-goog-api-key"]
                return HttpResponse(403, "{}")
            }
        }
        val generator = GeminiCardGenerator({ "g-key" }, UnconfinedTestDispatcher(), transport)

        try {
            generator.generate(textRequest("text"))
            org.junit.Assert.fail("Expected a GeneratorException")
        } catch (e: GeneratorException) {
            assertTrue(e.message.orEmpty().contains("Google"))
        }
        assertTrue(seenUrl.orEmpty().startsWith("https://generativelanguage.googleapis.com/"))
        assertEquals("g-key", seenKey)
    }

    @Test
    fun missingKeyMessageNamesTheProvider() = runTest {
        val generator = GeminiCardGenerator({ null }, UnconfinedTestDispatcher())
        try {
            generator.generate(textRequest("text"))
            org.junit.Assert.fail("Expected a GeneratorException")
        } catch (e: GeneratorException) {
            assertTrue(e.message.orEmpty().contains("Google Gemini API key"))
        }
    }

    @Test
    fun codeFencesAreStrippedOnlyWhenPresent() {
        assertEquals("{}", stripCodeFence("```json\n{}\n```"))
        assertEquals("{}", stripCodeFence("```\n{}\n```"))
        assertEquals("{}", stripCodeFence("  {}  "))
    }
}
