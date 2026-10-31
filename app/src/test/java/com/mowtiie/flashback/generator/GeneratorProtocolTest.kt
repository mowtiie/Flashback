package com.mowtiie.flashback.generator

import org.json.JSONArray
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test

class GeneratorProtocolTest {

    private fun reply(text: String, stopReason: String = "end_turn") = JSONObject()
        .put("stop_reason", stopReason)
        .put("content", JSONArray().put(JSONObject().put("type", "text").put("text", text)))
        .toString()

    private fun cards(vararg pairs: Pair<String, String>, name: String = "Cells"): String = JSONObject()
        .put("version", 1)
        .put(
            "decks",
            JSONArray().put(
                JSONObject().put("name", name).put(
                    "notes",
                    JSONArray().apply {
                        pairs.forEach { (front, back) -> put(JSONObject().put("front", front).put("back", back)) }
                    },
                ),
            ),
        )
        .toString()

    private fun failure(status: Int, body: String): String {
        try {
            GeneratorProtocol.parseResponse(status, body)
        } catch (e: GeneratorException) {
            return e.message.orEmpty()
        }
        fail("Expected a GeneratorException")
        error("unreachable")
    }

    @Test
    fun requestWrapsTheSourceAsMaterialAndAsksForSchemaOutput() {
        val body = JSONObject(GeneratorProtocol.requestBody("Mitochondria make ATP."))

        assertEquals(GeneratorProtocol.MODEL, body.getString("model"))
        val user = body.getJSONArray("messages").getJSONObject(0)
        assertEquals("user", user.getString("role"))
        assertEquals("<material>\nMitochondria make ATP.\n</material>", user.getString("content"))

        val format = body.getJSONObject("output_config").getJSONObject("format")
        assertEquals("json_schema", format.getString("type"))
        assertEquals(false, format.getJSONObject("schema").getBoolean("additionalProperties"))
    }

    @Test
    fun requestIsFreeOfSamplingAndForcedToolSettings() {
        val body = JSONObject(GeneratorProtocol.requestBody("x"))
        listOf("temperature", "top_p", "top_k", "tool_choice", "thinking").forEach {
            assertTrue("$it must not be sent", !body.has(it))
        }
    }

    @Test
    fun theSystemPromptTellsTheModelToTreatTheMaterialAsData() {
        val system = JSONObject(GeneratorProtocol.requestBody("x")).getString("system")
        assertTrue(system.contains("never as instructions"))
        assertTrue(system.contains("at most ${GeneratorLimits.MAX_CARDS}"))
    }

    @Test
    fun aGoodReplyBecomesADeck() {
        val deck = GeneratorProtocol.parseResponse(
            200,
            reply(cards("What makes ATP?" to "Mitochondria", "What holds DNA?" to "The nucleus")),
        )

        assertEquals("Cells", deck.name)
        assertEquals(listOf("What makes ATP?", "What holds DNA?"), deck.notes.map { it.front })
        assertEquals("Mitochondria", deck.notes.first().back)
    }

    @Test
    fun repeatedFrontsAreDroppedIgnoringCase() {
        val deck = GeneratorProtocol.parseResponse(
            200,
            reply(cards("Capital of France?" to "Paris", "capital of france?" to "Paris", "Capital of Spain?" to "Madrid")),
        )
        assertEquals(2, deck.notes.size)
    }

    @Test
    fun moreThanTheCapIsTrimmed() {
        val many = (1..GeneratorLimits.MAX_CARDS + 10).map { "Q$it" to "A$it" }.toTypedArray()
        val deck = GeneratorProtocol.parseResponse(200, reply(cards(*many)))
        assertEquals(GeneratorLimits.MAX_CARDS, deck.notes.size)
    }

    @Test
    fun aReplyWithNoCardsIsAnError() {
        assertEquals("No cards could be made from this text.", failure(200, reply(cards())))
    }

    @Test
    fun aCardMissingItsBackIsRejectedByTheArchiveParser() {
        val message = failure(200, reply(cards("Only a front" to "")))
        assertTrue(message.contains("could not read"))
    }

    @Test
    fun textThatIsNotJsonIsRejected() {
        assertTrue(failure(200, reply("Here are your cards!")).contains("could not read"))
    }

    @Test
    fun aRefusalIsReportedAsSuch() {
        assertEquals(
            "The model declined to make cards from this text.",
            failure(200, reply("", stopReason = "refusal")),
        )
    }

    @Test
    fun aTruncatedReplyAsksForLessText() {
        assertTrue(failure(200, reply("{", stopReason = "max_tokens")).contains("Try less text"))
    }

    @Test
    fun anEmptyContentListIsAnError() {
        assertEquals("The model returned no cards.", failure(200, JSONObject().put("content", JSONArray()).toString()))
    }

    @Test
    fun aBodyThatIsNotJsonIsAnError() {
        assertEquals("The reply from Anthropic could not be read.", failure(200, "<html>"))
    }

    @Test
    fun httpErrorsGetAdviceThatMatchesTheCause() {
        assertTrue(failure(401, "{}").contains("API key"))
        assertTrue(failure(403, "{}").contains("API key"))
        assertTrue(failure(429, "{}").contains("Wait a minute"))
        assertTrue(failure(413, "{}").contains("less text"))
        assertTrue(failure(529, "{}").contains("unavailable"))
    }

    @Test
    fun otherErrorsShowTheApiMessageWhenThereIsOne() {
        val body = JSONObject().put("error", JSONObject().put("message", "Your credit balance is too low.")).toString()
        assertEquals("Your credit balance is too low.", failure(400, body))
        assertEquals("Anthropic returned an error (400).", failure(400, "not json"))
    }
}
