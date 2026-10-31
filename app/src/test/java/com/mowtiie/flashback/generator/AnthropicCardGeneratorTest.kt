package com.mowtiie.flashback.generator

import com.mowtiie.flashback.FakePreferenceStore
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.json.JSONArray
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test
import java.io.IOException

@OptIn(ExperimentalCoroutinesApi::class)
class AnthropicCardGeneratorTest {

    private class RecordingTransport(private val respond: () -> HttpResponse) : HttpTransport {
        var calls = 0
        var url: String? = null
        var headers: Map<String, String> = emptyMap()
        var body: String? = null

        override fun post(url: String, headers: Map<String, String>, body: String): HttpResponse {
            calls++
            this.url = url
            this.headers = headers
            this.body = body
            return respond()
        }
    }

    private val okBody = JSONObject()
        .put("stop_reason", "end_turn")
        .put(
            "content",
            JSONArray().put(
                JSONObject().put("type", "text").put(
                    "text",
                    """{"version":1,"decks":[{"name":"Bio","notes":[{"front":"Q","back":"A"}]}]}""",
                ),
            ),
        )
        .toString()

    private fun generator(key: String?, transport: HttpTransport) =
        AnthropicCardGenerator({ key }, UnconfinedTestDispatcher(), transport)

    @Test
    fun sendsTheKeyAndSourceToTheMessagesEndpoint() = runTest {
        val transport = RecordingTransport { HttpResponse(200, okBody) }
        val deck = generator("sk-ant-test", transport).generate("Cells have a nucleus.")

        assertEquals("https://api.anthropic.com/v1/messages", transport.url)
        assertEquals("sk-ant-test", transport.headers["x-api-key"])
        assertTrue(transport.body.orEmpty().contains("Cells have a nucleus."))
        assertEquals("Bio", deck.name)
    }

    @Test
    fun withoutAKeyNothingIsSent() = runTest {
        val transport = RecordingTransport { HttpResponse(200, okBody) }
        try {
            generator(null, transport).generate("text")
            fail("Expected a GeneratorException")
        } catch (e: GeneratorException) {
            assertTrue(e.message.orEmpty().contains("API key"))
        }
        assertEquals(0, transport.calls)
    }

    @Test
    fun aNetworkFailureBecomesAFriendlyError() = runTest {
        val transport = RecordingTransport { throw IOException("timeout") }
        try {
            generator("k", transport).generate("text")
            fail("Expected a GeneratorException")
        } catch (e: GeneratorException) {
            assertTrue(e.message.orEmpty().contains("Could not reach"))
        }
    }

    @Test
    fun anHttpErrorIsMapped() = runTest {
        val transport = RecordingTransport { HttpResponse(401, "{}") }
        try {
            generator("k", transport).generate("text")
            fail("Expected a GeneratorException")
        } catch (e: GeneratorException) {
            assertTrue(e.message.orEmpty().contains("rejected"))
        }
    }

    @Test
    fun theApiKeyIsStoredTrimmedAndCanBeRemoved() {
        val store = FakePreferenceStore()
        val prefs = GeneratorPreferences(store)
        assertFalse(prefs.hasApiKey)

        prefs.apiKey = "  sk-ant-abc1234 "
        assertEquals("sk-ant-abc1234", prefs.apiKey)
        assertTrue(prefs.hasApiKey)
        assertEquals("…1234", prefs.apiKey!!.keyHint())

        prefs.apiKey = "   "
        assertNull(prefs.apiKey)
        assertFalse(store.values.containsKey("generator_api_key"))
    }
}
