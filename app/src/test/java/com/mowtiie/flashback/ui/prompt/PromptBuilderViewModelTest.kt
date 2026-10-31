package com.mowtiie.flashback.ui.prompt

import com.mowtiie.flashback.generator.CardTypes
import com.mowtiie.flashback.generator.PromptCoverage
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PromptBuilderViewModelTest {

    private val viewModel = PromptBuilderViewModel()
    private val state get() = viewModel.uiState.value

    @Test
    fun startsWithFullCoverageAndTheStudySetting() {
        assertEquals(PromptCoverage.Everything, state.options.coverage)
        assertEquals(CardTypes.Setting, state.options.cardTypes)
        assertTrue(state.prompt.contains("flashback-cards.json"))
    }

    @Test
    fun changingCoverageRebuildsThePrompt() {
        viewModel.setCoverage(PromptCoverage.KeyPoints)

        assertEquals(PromptCoverage.KeyPoints, state.options.coverage)
        assertTrue(state.prompt.contains("main ideas"))
    }

    @Test
    fun changingCardTypesRebuildsThePrompt() {
        viewModel.setCardTypes(CardTypes.Choice)
        assertTrue(state.prompt.contains("\"choices\""))
    }

    @Test
    fun languageAndDeckNameReachThePrompt() {
        viewModel.setLanguage("Filipino")
        viewModel.setDeckName("Cell Biology")

        assertTrue(state.prompt.contains("Filipino"))
        assertTrue(state.prompt.contains("Cell Biology"))
    }

    @Test
    fun clearingTheNameGoesBackToTheDefault() {
        viewModel.setDeckName("Cell Biology")
        viewModel.setDeckName("")
        assertTrue(state.prompt.contains("Short deck title"))
    }
}
