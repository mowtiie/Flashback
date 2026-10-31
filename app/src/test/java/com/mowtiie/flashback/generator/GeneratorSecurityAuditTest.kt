package com.mowtiie.flashback.generator

import kotlinx.coroutines.runBlocking
import org.json.JSONArray
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test
import java.net.ServerSocket
import java.net.Socket
import kotlin.concurrent.thread

/**
 * Security audit checks for the AI generator. They feed hostile input to the parsers and a real local
 * socket to the transport. Tests that fail document a finding; tests that pass guard a property.
 */
class GeneratorSecurityAuditTest {

    private fun reply(notesJson: String, version: String = "1", name: String = "\"D\""): String =
        """{"version":$version,"decks":[{"name":$name,"notes":[$notesJson]}]}"""

    private fun parse(text: String, max: Int = 30) = deckFromModelText(text, max)

    private fun failsCleanly(text: String) {
        try {
            parse(text)
            fail("Expected a GeneratorException")
        } catch (_: GeneratorException) {
        }
    }

    // --- hostile replies ----------------------------------------------------------------------

    @Test
    fun deeplyNestedJsonFailsCleanly() {
        val depth = 60_000
        val nested = "[".repeat(depth) + "]".repeat(depth)
        failsCleanly("""{"version":1,"decks":$nested}""")
    }

    @Test
    fun deeplyNestedObjectInsideANoteFailsCleanly() {
        val depth = 40_000
        val nested = "{\"a\":".repeat(depth) + "1" + "}".repeat(depth)
        failsCleanly(reply("""{"front":"q","back":"a","extra":$nested}"""))
    }

    @Test
    fun aHugeNumberOfCardsIsCappedQuickly() {
        val notes = (1..50_000).joinToString(",") { """{"front":"Q$it","back":"A$it"}""" }
        val started = System.nanoTime()
        val deck = parse(reply(notes))
        val millis = (System.nanoTime() - started) / 1_000_000

        assertEquals(30, deck.notes.size)
        assertTrue("took ${millis}ms", millis < 5_000)
    }

    @Test
    fun aFieldOverTheLimitIsRejected() {
        failsCleanly(reply("""{"front":"${"x".repeat(10_001)}","back":"a"}"""))
        failsCleanly(reply("""{"front":"q","back":"${"x".repeat(10_001)}"}"""))
    }

    @Test
    fun nullsAndMissingFieldsAreRejectedNotCrashed() {
        failsCleanly("""{"version":null,"decks":null}""")
        failsCleanly("""{"decks":[]}""")
        failsCleanly("""{"version":1,"decks":[null]}""")
        failsCleanly("""{"version":1,"decks":[{"name":null,"notes":[]}]}""")
        failsCleanly(reply("""{"front":null,"back":null}"""))
        failsCleanly(reply("""null"""))
        failsCleanly(reply(""""just a string""""))
    }

    @Test
    fun aVersionAboveOneIsRejected() {
        failsCleanly(reply("""{"front":"q","back":"a"}""", version = "2"))
        failsCleanly(reply("""{"front":"q","back":"a"}""", version = "99999999999"))
    }

    @Test
    fun anEmptyDeckNameIsRejected() {
        failsCleanly(reply("""{"front":"q","back":"a"}""", name = "\"   \""))
    }

    @Test
    fun aFloodOfChoicesIsCappedAndShort() {
        val choices = JSONArray((1..10_000).map { "wrong-$it" } + "x".repeat(5_000)).toString()
        val note = parse(reply("""{"front":"q","back":"a","answerMode":"choice","choices":$choices}""")).notes.single()

        println("AUDIT: choices kept from a flood of 10001: ${note.choices.size}")
        assertTrue(note.choices.size <= 3)
        assertTrue(note.choices.all { it.length <= 120 })
    }

    @Test
    fun anOddAnswerModeIsIgnored() {
        val note = parse(reply("""{"front":"q","back":"a","answerMode":{"x":1}}""")).notes.single()
        assertEquals(null, note.answerMode)
    }

    @Test
    fun markupPathsAndLinksStayPlainText() {
        val front = "<script>alert(1)</script> [link](https://evil.example) ../../etc/passwd file:///data/data/x"
        val note = parse(reply("""{"front":${JSONObject.quote(front)},"back":"a"}""")).notes.single()
        assertEquals(front, note.front)
    }

    @Test
    fun controlAndBidiCharactersAreStrippedFromEveryField() {
        val dirty = "evil‮fdp.exe\u0000tail​⁦x⁩"
        val clean = "evilfdp.exetailx"
        val choices = JSONArray(listOf(dirty, "ok")).toString()
        val note = deckFromModelText(
            reply("""{"front":${JSONObject.quote(dirty)},"back":"right","answerMode":"choice","choices":$choices}"""),
            30, CardTypes.Choice,
        ).notes.single()

        assertEquals(clean, note.front)
        assertEquals("right", note.back)
        assertEquals(listOf(clean, "ok"), note.choices)
    }

    @Test
    fun newlinesAndTabsSurviveCleaning() {
        val note = parse(reply("""{"front":"line1\nline2\tx","back":"a"}""")).notes.single()
        assertEquals("line1\nline2\tx", note.front)
    }

    @Test
    fun aCardThatIsOnlyInvisibleCharactersIsDropped() {
        val notes = parse(reply("""{"front":"​\u0000","back":"a"},{"front":"real","back":"b"}""")).notes
        assertEquals(listOf("real"), notes.map { it.front })
    }

    // --- the provider's own error text -------------------------------------------------------

    @Test
    fun aProviderErrorMessageShownToTheUserIsShort() {
        val huge = "x".repeat(1_000_000)
        val body = JSONObject().put("error", JSONObject().put("message", huge)).toString()
        val message = try {
            GeneratorProtocol.parseResponse(400, body, 30)
            ""
        } catch (e: GeneratorException) {
            e.message.orEmpty()
        }
        assertTrue("the message was ${message.length} characters", message.length <= 500)
    }

    @Test
    fun anHtmlErrorPageGivesAGenericMessage() {
        val message = try {
            GeminiProtocol.parseResponse(502, "<html><body>Bad gateway</body></html>", 30)
            ""
        } catch (e: GeneratorException) {
            e.message.orEmpty()
        }
        assertFalse(message.contains("<html"))
    }

    // --- what is sent -------------------------------------------------------------------------

    @Test
    fun anthropicRequestHasOnlyTheExpectedFields() {
        val body = JSONObject(GeneratorProtocol.requestBody(textRequest("my notes")))
        assertEquals(setOf("model", "max_tokens", "system", "output_config", "messages"), body.keys().asSequence().toSet())
        val request = AnthropicBackend.request(textRequest("my notes"), "FAKEKEY")
        assertEquals(setOf("content-type", "x-api-key", "anthropic-version"), request.headers.keys)
        assertFalse(request.url.contains("FAKEKEY"))
        assertFalse(request.body.contains("FAKEKEY"))
    }

    @Test
    fun geminiRequestHasOnlyTheExpectedFieldsAndTheKeyIsAHeader() {
        val body = JSONObject(GeminiProtocol.requestBody(textRequest("my notes")))
        assertEquals(setOf("systemInstruction", "contents", "generationConfig"), body.keys().asSequence().toSet())
        val request = GeminiBackend.request(textRequest("my notes"), "FAKEKEY")
        assertEquals(setOf("content-type", "x-goog-api-key"), request.headers.keys)
        assertFalse(request.url.contains("FAKEKEY"))
        assertFalse(request.body.contains("FAKEKEY"))
    }

    @Test
    fun aKeyIsNeverSentToTheOtherProvider() = runBlocking {
        val seen = mutableListOf<Pair<String, Map<String, String>>>()
        val transport = object : HttpTransport {
            override suspend fun post(url: String, headers: Map<String, String>, body: String): HttpResponse {
                seen += url to headers
                return HttpResponse(401, "{}")
            }
        }
        val dispatcher = kotlinx.coroutines.Dispatchers.Unconfined
        val anthropic = AnthropicCardGenerator({ "ANTHROPIC-KEY" }, dispatcher, transport)
        val gemini = GeminiCardGenerator({ "GEMINI-KEY" }, dispatcher, transport)
        runCatching { anthropic.generate(textRequest("x")) }
        runCatching { gemini.generate(textRequest("x")) }

        for ((url, headers) in seen) {
            val values = headers.values.toString()
            if (url.contains("anthropic")) assertFalse(values.contains("GEMINI-KEY"))
            if (url.contains("googleapis")) assertFalse(values.contains("ANTHROPIC-KEY"))
        }
        assertEquals(2, seen.size)
    }

    // --- transport against real local sockets -------------------------------------------------

    private fun readRequest(socket: Socket): String {
        val input = socket.getInputStream()
        val text = StringBuilder()
        var last4 = ""
        while (!last4.endsWith("\r\n\r\n")) {
            val b = input.read()
            if (b < 0) break
            text.append(b.toChar())
            last4 = (last4 + b.toChar()).takeLast(4)
        }
        return text.toString()
    }

    private fun redirectTest(status: String): String? {
        val target = ServerSocket(0)
        val received = java.util.concurrent.atomic.AtomicReference<String?>(null)
        val targetThread = thread(isDaemon = true) {
            runCatching {
                target.accept().use { s ->
                    received.set(readRequest(s))
                    s.getOutputStream().write("HTTP/1.1 200 OK\r\nContent-Length: 2\r\nConnection: close\r\n\r\n{}".toByteArray())
                }
            }
        }
        val first = ServerSocket(0)
        thread(isDaemon = true) {
            runCatching {
                first.accept().use { s ->
                    readRequest(s)
                    s.getOutputStream().write(
                        "HTTP/1.1 $status\r\nLocation: http://127.0.0.1:${target.localPort}/other\r\nContent-Length: 0\r\nConnection: close\r\n\r\n".toByteArray(),
                    )
                }
            }
        }
        runBlocking {
            runCatching {
                UrlConnectionTransport().post("http://127.0.0.1:${first.localPort}/", mapOf("x-api-key" to "FAKEKEY-9999"), """{"material":"my study notes"}""")
            }
        }
        targetThread.join(2_000)
        first.close()
        target.close()
        return received.get()
    }

    @Test
    fun aRedirectDoesNotForwardTheKeyToAnotherHost() {
        for (status in listOf("302 Found", "307 Temporary Redirect", "308 Permanent Redirect")) {
            val received = redirectTest(status)
            println("AUDIT: $status -> second server received request: ${received != null}; key forwarded: ${received?.contains("FAKEKEY-9999") == true}")
            assertFalse("$status forwarded the key", received?.contains("FAKEKEY-9999") == true)
        }
    }

    @Test
    fun anOversizedResponseIsNotReadWhole() {
        val server = ServerSocket(0)
        val size = 40 * 1024 * 1024
        thread(isDaemon = true) {
            runCatching {
                server.accept().use { s ->
                    readRequest(s)
                    s.getInputStream().read(ByteArray(2))
                    val out = s.getOutputStream()
                    out.write("HTTP/1.1 200 OK\r\nContent-Length: $size\r\nConnection: close\r\n\r\n".toByteArray())
                    val chunk = ByteArray(64 * 1024) { 'a'.code.toByte() }
                    var sent = 0
                    while (sent < size) {
                        out.write(chunk)
                        sent += chunk.size
                    }
                }
            }
        }
        val response = runBlocking { UrlConnectionTransport().post("http://127.0.0.1:${server.localPort}/", emptyMap(), "{}") }
        server.close()
        assertTrue("read ${response.body.length} characters", response.body.length <= 2 * 1024 * 1024)
    }
}
