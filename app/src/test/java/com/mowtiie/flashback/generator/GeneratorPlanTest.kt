package com.mowtiie.flashback.generator

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class GeneratorPlanTest {

    private val section = GeneratorLimits.SECTION_CHARS

    @Test
    fun shortTextIsOneRequest() = assertEquals(1, GeneratorPlan.requests(Coverage.Everything, chars = 100, isPdf = false))

    @Test
    fun longTextIsOneRequestPerSectionRoundedUp() {
        assertEquals(1, GeneratorPlan.requests(Coverage.Everything, section, false))
        assertEquals(2, GeneratorPlan.requests(Coverage.Everything, section + 1, false))
        assertEquals(6, GeneratorPlan.requests(Coverage.Everything, section * 5 + 10, false))
    }

    @Test
    fun aPdfAndKeyPointsAreAlwaysOneRequest() {
        assertEquals(1, GeneratorPlan.requests(Coverage.Everything, 0, isPdf = true))
        assertEquals(1, GeneratorPlan.requests(Coverage.KeyPoints, 20_000, false))
    }

    @Test
    fun noTextIsStillCountedAsOneSoTheEstimateNeverReadsZero() =
        assertEquals(1, GeneratorPlan.requests(Coverage.Everything, 0, false))

    @Test
    fun onlyBiggerRunsAskForConfirmation() {
        assertFalse(GeneratorPlan.needsConfirmation(GeneratorLimits.CONFIRM_ABOVE_SECTIONS))
        assertTrue(GeneratorPlan.needsConfirmation(GeneratorLimits.CONFIRM_ABOVE_SECTIONS + 1))
    }

    @Test
    fun theLimitDependsOnTheCoverage() {
        assertEquals(GeneratorLimits.MAX_SOURCE_CHARS, GeneratorPlan.maxChars(Coverage.Everything))
        assertEquals(GeneratorLimits.KEY_POINTS_MAX_CHARS, GeneratorPlan.maxChars(Coverage.KeyPoints))
    }
}

class GeneratorPlanProviderTest {
    @org.junit.Test
    fun onDeviceUsesShorterSectionsThanTheOnlineProviders() {
        val chars = 22_000
        org.junit.Assert.assertEquals(3, GeneratorPlan.requests(Coverage.Everything, chars, false, GeneratorProvider.Anthropic))
        org.junit.Assert.assertEquals(8, GeneratorPlan.requests(Coverage.Everything, chars, false, GeneratorProvider.OnDevice))
    }

    @org.junit.Test
    fun onDeviceKeyPointsTakesLessText() {
        org.junit.Assert.assertEquals(6_000, GeneratorPlan.maxChars(Coverage.KeyPoints, GeneratorProvider.OnDevice))
        org.junit.Assert.assertEquals(GeneratorLimits.KEY_POINTS_MAX_CHARS, GeneratorPlan.maxChars(Coverage.KeyPoints, GeneratorProvider.Gemini))
    }
}
