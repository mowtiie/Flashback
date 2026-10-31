package com.mowtiie.flashback.generator

import org.json.JSONArray
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class CardTypesTest {

    private fun reply(vararg notes: String): String =
        """{"version":1,"decks":[{"name":"D","notes":[${notes.joinToString(",")}]}]}"""

    private val flip = """{"front":"q1","back":"a1","answerMode":"flip"}"""
    private val type = """{"front":"q2","back":"a2","answerMode":"type","choices":["junk"]}"""
    private val choice = """{"front":"q3","back":"a3","answerMode":"choice","choices":["x","y","z"]}"""
    private val choiceWithout = """{"front":"q4","back":"a4","answerMode":"choice"}"""
    private val untyped = """{"front":"q5","back":"a5"}"""

    private fun modes(types: CardTypes, vararg notes: String) =
        deckFromModelText(reply(*notes), 30, types).notes.map { it.answerMode }

    @Test
    fun settingTypesStripsAnythingTheModelAdded() {
        val notes = deckFromModelText(reply(flip, choice), 30, CardTypes.Setting).notes
        assertTrue(notes.all { it.answerMode == null && it.choices.isEmpty() })
    }

    @Test
    fun flipOnlyForcesFlipAndDropsOptions() {
        val notes = deckFromModelText(reply(type, choice, untyped), 30, CardTypes.Flip).notes
        assertTrue(notes.all { it.answerMode == 0 && it.choices.isEmpty() })
    }

    @Test
    fun typeOnlyForcesTypeAndDropsOptions() {
        val notes = deckFromModelText(reply(flip, choice), 30, CardTypes.Type).notes
        assertTrue(notes.all { it.answerMode == 1 && it.choices.isEmpty() })
    }

    @Test
    fun choiceOnlyForcesChoiceAndKeepsTheOptions() {
        val notes = deckFromModelText(reply(flip, choice, choiceWithout), 30, CardTypes.Choice).notes
        assertTrue(notes.all { it.answerMode == 2 })
        assertEquals(listOf("x", "y", "z"), notes[1].choices)
        assertTrue(notes[2].choices.isEmpty())
    }

    @Test
    fun mixedKeepsTheModelsPicksButRepairsOddOnes() {
        val result = deckFromModelText(reply(flip, type, choice, choiceWithout, untyped), 30, CardTypes.Mixed).notes

        assertEquals(listOf(0, 1, 2, 0, 0), result.map { it.answerMode })
        assertTrue("a type card carries no options", result[1].choices.isEmpty())
        assertEquals(listOf("x", "y", "z"), result[2].choices)
    }

    @Test
    fun theOptionLimitStillAppliesToWhatTheModelReturns() {
        val many = """{"front":"q","back":"a","answerMode":"choice","choices":["1","2","3","4","5"]}"""
        assertEquals(3, deckFromModelText(reply(many), 30, CardTypes.Choice).notes.single().choices.size)
    }

    @Test
    fun anUnknownTypeFromTheModelCountsAsMissing() {
        val odd = """{"front":"q","back":"a","answerMode":"hologram"}"""
        assertEquals(listOf(0), modes(CardTypes.Mixed, odd))
    }

    @Test
    fun thePromptOnlyMentionsTypesWhenTheUserChoseSome() {
        assertFalse(CardPrompt.system(30, CardTypes.Setting).contains("answerMode"))
        CardTypes.entries.filter { it != CardTypes.Setting }.forEach {
            assertTrue("$it", CardPrompt.system(30, it).contains("answerMode"))
        }
    }

    @Test
    fun theMixedPromptExplainsWhenToUseEachType() {
        val prompt = CardPrompt.system(30, CardTypes.Mixed)
        assertTrue(prompt.contains("\"flip\""))
        assertTrue(prompt.contains("\"type\""))
        assertTrue(prompt.contains("\"choice\""))
        assertTrue(prompt.contains("exactly 3 plausible wrong answers"))
    }

    @Test
    fun theJsonShapeForGeminiNamesTheNewFieldsOnlyWhenNeeded() {
        assertFalse(CardPrompt.jsonShape(CardTypes.Setting).contains("answerMode"))
        assertTrue(CardPrompt.jsonShape(CardTypes.Mixed).contains("answerMode"))
    }

    @Test
    fun theAnthropicSchemaAllowsATypeAndOptionsOnEachNote() {
        val schema = JSONObject(GeneratorProtocol.requestBody(textRequest("x")))
            .getJSONObject("output_config").getJSONObject("format").getJSONObject("schema")
        val note = schema.getJSONObject("properties").getJSONObject("decks").getJSONObject("items")
            .getJSONObject("properties").getJSONObject("notes").getJSONObject("items")

        assertEquals(
            listOf("flip", "type", "choice"),
            note.getJSONObject("properties").getJSONObject("answerMode").getJSONArray("enum").let { array ->
                List(array.length()) { array.getString(it) }
            },
        )
        assertEquals("array", note.getJSONObject("properties").getJSONObject("choices").getString("type"))
        assertEquals(listOf("front", "back"), note.getJSONArray("required").let { array -> List(array.length()) { array.getString(it) } })
    }

    @Test
    fun theTypesChoiceReachesBothProviderPrompts() {
        val request = GeneratorRequest(GeneratorInput.Text("x"), CardCount.Normal, CardTypes.Choice)

        val anthropic = JSONObject(GeneratorProtocol.requestBody(request)).getString("system")
        val gemini = JSONObject(GeminiProtocol.requestBody(request))
            .getJSONObject("systemInstruction").getJSONArray("parts").getJSONObject(0).getString("text")

        assertTrue(anthropic.contains("Set answerMode to \"choice\""))
        assertTrue(gemini.contains("Set answerMode to \"choice\""))
    }

    @Test
    fun aGeminiReplyIsTypedAccordingToTheChoice() {
        val body = JSONObject().put(
            "candidates",
            JSONArray().put(
                JSONObject().put("finishReason", "STOP").put(
                    "content",
                    JSONObject().put("parts", JSONArray().put(JSONObject().put("text", reply(flip, choice)))),
                ),
            ),
        ).toString()

        val deck = GeminiProtocol.parseResponse(200, body, 30, CardTypes.Mixed)

        assertEquals(listOf(0, 2), deck.notes.map { it.answerMode })
    }
}
