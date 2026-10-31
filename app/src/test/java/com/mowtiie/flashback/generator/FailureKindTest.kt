package com.mowtiie.flashback.generator

import org.junit.Assert.assertEquals
import org.junit.Assert.fail
import org.junit.Test

class FailureKindTest {

    private fun anthropic(status: Int, body: String = "{}"): FailureKind = try {
        GeneratorProtocol.parseResponse(status, body, 30)
        fail("expected a failure")
        FailureKind.Other
    } catch (e: GeneratorException) {
        e.kind
    }

    private fun gemini(status: Int, body: String = "{}"): FailureKind = try {
        GeminiProtocol.parseResponse(status, body, 30)
        fail("expected a failure")
        FailureKind.Other
    } catch (e: GeneratorException) {
        e.kind
    }

    @Test
    fun anAnthropicKeyRefusalIsABadKey() {
        assertEquals(FailureKind.BadKey, anthropic(401))
        assertEquals(FailureKind.BadKey, anthropic(403))
    }

    @Test
    fun anAnthropicRateLimitIsRetryable() = assertEquals(FailureKind.RateLimited, anthropic(429))

    @Test
    fun otherAnthropicErrorsAreNotSpecial() {
        assertEquals(FailureKind.Other, anthropic(500))
        assertEquals(FailureKind.Other, anthropic(400))
    }

    @Test
    fun aGeminiKeyRefusalIsABadKeyIncludingTheOddFourHundred() {
        assertEquals(FailureKind.BadKey, gemini(401))
        assertEquals(FailureKind.BadKey, gemini(403))
        assertEquals(FailureKind.BadKey, gemini(400, """{"error":{"message":"API key not valid. Please pass a valid API key."}}"""))
    }

    @Test
    fun aGeminiRateLimitIsRetryable() = assertEquals(FailureKind.RateLimited, gemini(429))

    @Test
    fun otherGeminiErrorsAreNotSpecial() {
        assertEquals(FailureKind.Other, gemini(500))
        assertEquals(FailureKind.Other, gemini(400, """{"error":{"message":"Bad request."}}"""))
    }
}
