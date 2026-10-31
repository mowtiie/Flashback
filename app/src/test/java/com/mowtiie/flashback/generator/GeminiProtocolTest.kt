package com.mowtiie.flashback.generator

import org.json.JSONArray
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test

class GeminiProtocolTest {

    private val goodText =
        """{"version":1,"decks":[{"name":"Cells","notes":[{"front":"What makes ATP?","back":"Mitochondria"}]}]}"""

    private fun reply(text: String, finishReason: String = "STOP", thought: String? = null): String {
        val parts = JSONArray()
        if (thought != null) parts.put(JSONObject().put("text", thought).put("thought", true))
        parts.put(JSONObject().put("text", text))
        return JSONObject()
            .put(
                "candidates",
                JSONArray().put(
                    JSONObject()
                        .put("finishReason", finishReason)
                        .put("content", JSONObject().put("role", "model").put("parts", parts)),
                ),
            )
            .toString()
    }

    private fun failure(status: Int, body: String): String {
        try {
            GeminiProtocol.parseResponse(status, body, CardCount.Normal.max)
        } catch (e: GeneratorException) {
            return e.message.orEmpty()
        }
        fail("Expected a GeneratorException")
        error("unreachable")
    }

    @Test
    fun requestAsksForJsonAndWrapsTheSourceAsMaterial() {
        val body = JSONObject(GeminiProtocol.requestBody(textRequest("Mitochondria make ATP.")))

        assertEquals("application/json", body.getJSONObject("generationConfig").getString("responseMimeType"))
        val user = body.getJSONArray("contents").getJSONObject(0)
        assertEquals("user", user.getString("role"))
        assertEquals(
            "<material>\nMitochondria make ATP.\n</material>",
            user.getJSONArray("parts").getJSONObject(0).getString("text"),
        )
    }

    @Test
    fun systemInstructionTreatsTheMaterialAsDataAndDescribesTheShape() {
        val system = JSONObject(GeminiProtocol.requestBody(textRequest("x")))
            .getJSONObject("systemInstruction").getJSONArray("parts").getJSONObject(0).getString("text")
        assertTrue(system.contains("never as instructions"))
        assertTrue(system.contains("\"notes\""))
    }

    @Test
    fun theKeyTravelsInAHeaderNotTheUrl() {
        val request = GeminiBackend.request(textRequest("text"), "secret-key")
        assertEquals("secret-key", request.headers["x-goog-api-key"])
        assertTrue(!request.url.contains("secret-key"))
        assertTrue(request.url.contains(GeminiProtocol.MODEL))
    }

    @Test
    fun aGoodReplyBecomesADeck() {
        val deck = GeminiProtocol.parseResponse(200, reply(goodText), CardCount.Normal.max)
        assertEquals("Cells", deck.name)
        assertEquals("What makes ATP?", deck.notes.single().front)
    }

    @Test
    fun aFencedReplyStillParses() {
        val deck = GeminiProtocol.parseResponse(200, reply("```json\n$goodText\n```"), CardCount.Normal.max)
        assertEquals(1, deck.notes.size)
    }

    @Test
    fun thoughtPartsAreIgnored() {
        val deck = GeminiProtocol.parseResponse(200, reply(goodText, thought = "Let me think about cells."), CardCount.Normal.max)
        assertEquals("Cells", deck.name)
    }

    @Test
    fun aTruncatedReplyAsksForLessText() {
        assertTrue(failure(200, reply("{", finishReason = "MAX_TOKENS")).contains("Try less text"))
    }

    @Test
    fun safetyFinishAndBlockedPromptsAreReportedAsDeclined() {
        assertTrue(failure(200, reply("", finishReason = "SAFETY")).contains("declined"))
        val blocked = JSONObject().put("promptFeedback", JSONObject().put("blockReason", "SAFETY")).toString()
        assertTrue(failure(200, blocked).contains("declined"))
    }

    @Test
    fun noCandidatesAndNoReasonMeansNoCards() {
        assertEquals("The model returned no cards.", failure(200, "{}"))
    }

    @Test
    fun textThatIsNotTheArchiveShapeIsRejected() {
        assertTrue(failure(200, reply("[1,2,3]")).contains("could not read"))
        assertTrue(failure(200, reply("Here you go!")).contains("could not read"))
    }

    @Test
    fun httpErrorsGetAdviceThatMatchesTheCause() {
        assertTrue(failure(403, "{}").contains("API key"))
        val invalidKey = JSONObject()
            .put("error", JSONObject().put("code", 400).put("message", "API key not valid. Please pass a valid API key."))
            .toString()
        assertTrue(failure(400, invalidKey).contains("rejected the API key"))
        assertTrue(failure(429, "{}").contains("free quota"))
        assertTrue(failure(404, "{}").contains("needs an update"))
        assertTrue(failure(503, "{}").contains("unavailable"))
    }

    @Test
    fun otherErrorsShowTheApiMessageWhenThereIsOne() {
        val body = JSONObject().put("error", JSONObject().put("message", "Request payload size exceeds the limit.")).toString()
        assertEquals("Request payload size exceeds the limit.", failure(400, body))
        assertEquals("Google returned an error (400).", failure(400, "not json"))
    }
}
