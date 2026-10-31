package com.mowtiie.flashback.generator

import org.json.JSONObject
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class CoveragePromptTest {

    private fun request(coverage: Coverage, known: List<String> = emptyList()) = GeneratorRequest(
        input = GeneratorInput.Text("Some material."),
        cardCount = CardCount.Few,
        cardTypes = CardTypes.Setting,
        coverage = coverage,
        known = known,
    )

    @Test
    fun everythingAsksForEveryFactAndSetsNoCardLimit() {
        val prompt = CardPrompt.system(request(Coverage.Everything))

        assertTrue(prompt.contains("every", ignoreCase = true))
        assertFalse(prompt.contains("at most", ignoreCase = true))
        assertFalse(prompt.contains("fewer for short material", ignoreCase = true))
    }

    @Test
    fun keyPointsKeepsTheCardLimit() {
        val prompt = CardPrompt.system(request(Coverage.KeyPoints))
        assertTrue(prompt.contains("at most ${CardCount.Few.max} cards"))
    }

    @Test
    fun knownFrontsAreListedSoTheyAreNotRepeated() {
        val prompt = CardPrompt.system(request(Coverage.Everything, listOf("What is DNA?", "Define mitosis")))

        assertTrue(prompt.contains("What is DNA?"))
        assertTrue(prompt.contains("Define mitosis"))
        assertTrue(prompt.contains("<already_made>"))
    }

    @Test
    fun noKnownFrontsMeansNoAlreadyMadeSection() {
        assertFalse(CardPrompt.system(request(Coverage.Everything)).contains("<already_made>"))
    }

    @Test
    fun theKnownListIsBoundedInSizeAndKeepsTheNewestFronts() {
        val known = (1..5000).map { "Front number $it with some extra words to make it long" }
        val prompt = CardPrompt.system(request(Coverage.Everything, known))
        val base = CardPrompt.system(request(Coverage.Everything))

        assertTrue(prompt.length - base.length <= CardPrompt.MAX_KNOWN_CHARS + 500)
        assertTrue(prompt.contains("Front number 5000"))
        assertFalse(prompt.contains("Front number 1 "))
    }

    @Test
    fun aVeryLongKnownFrontIsShortened() {
        val prompt = CardPrompt.system(request(Coverage.Everything, listOf("x".repeat(5000))))
        assertTrue(prompt.length < 5000)
    }

    @Test
    fun theKnownFrontsReachBothProviders() {
        val req = request(Coverage.Everything, listOf("What is DNA?"))
        assertTrue(GeneratorProtocol.requestBody(req).contains("What is DNA?"))
        assertTrue(GeminiProtocol.requestBody(req).contains("What is DNA?"))
    }

    @Test
    fun knownFrontsCannotBreakOutOfTheRequestBody() {
        val req = request(Coverage.Everything, listOf("quote \" and \\ and \n newline"))
        JSONObject(GeneratorProtocol.requestBody(req))
        JSONObject(GeminiProtocol.requestBody(req))
    }
}
