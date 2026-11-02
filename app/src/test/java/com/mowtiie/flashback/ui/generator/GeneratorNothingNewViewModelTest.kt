package com.mowtiie.flashback.ui.generator

import com.mowtiie.flashback.data.io.DeckArchive
import com.mowtiie.flashback.generator.CardGenerator
import com.mowtiie.flashback.generator.CardTypes
import com.mowtiie.flashback.generator.FailureKind
import com.mowtiie.flashback.generator.GeneratorException
import com.mowtiie.flashback.generator.GeneratorInput
import com.mowtiie.flashback.generator.GeneratorProvider
import com.mowtiie.flashback.ui.settings.FakeArchive
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class GeneratorNothingNewViewModelTest {

    private val asked = mutableListOf<String>()
    private var withCards: Set<String> = setOf("Para1")

    private val generator = CardGenerator { request ->
        val key = (request.input as GeneratorInput.Text).text.take(5)
        asked += key
        if (key in withCards) {
            DeckArchive.Deck(name = "Deck $key", notes = listOf(DeckArchive.Note("q-$key", "a")))
        } else {
            throw GeneratorException("No cards could be made from this text.", kind = FailureKind.NoCards)
        }
    }

    @Before
    fun setUp() = Dispatchers.setMain(UnconfinedTestDispatcher())

    @After
    fun tearDown() = Dispatchers.resetMain()

    private fun viewModel() = GeneratorViewModel(
        generator, FakeArchive(), { true }, { GeneratorProvider.Anthropic }, UnconfinedTestDispatcher(), pause = {},
    ).also { it.setCardTypes(CardTypes.Setting) }

    private fun text(parts: Int) = (1..parts).joinToString("\n\n") { "Para$it " + "x".repeat(5_000) }

    private fun GeneratorViewModel.run(parts: Int) {
        setSource(text(parts))
        generate()
        if (uiState.value.confirming) confirmGenerate()
    }

    @Test
    fun partsWithNothingNewAreCountedAndNotListedAsFailed() {
        val vm = viewModel()
        vm.run(parts = 3)

        val state = vm.uiState.value
        assertEquals(GeneratorStage.Review, state.stage)
        assertEquals(1, state.cards.size)
        assertEquals(listOf(1, 0, 0), state.sectionCounts)
        assertEquals(2, state.nothingNew)
        assertTrue(state.failedSections.isEmpty())
    }

    @Test
    fun nothingNewDoesNotStopTheRun() {
        withCards = setOf("Para1", "Para5")
        val vm = viewModel()
        vm.run(parts = 5)

        assertEquals(listOf("Para1", "Para2", "Para3", "Para4", "Para5"), asked)
        assertEquals(2, vm.uiState.value.cards.size)
        assertEquals(3, vm.uiState.value.nothingNew)
    }

    @Test
    fun aRunWithNoCardsAtAllGoesBackToTheInputPageWithTheReason() {
        withCards = emptySet()
        val vm = viewModel()
        vm.run(parts = 2)

        val state = vm.uiState.value
        assertEquals(GeneratorStage.Input, state.stage)
        assertEquals("No cards could be made from this text.", state.error)
        assertTrue(state.cards.isEmpty())
    }
}
