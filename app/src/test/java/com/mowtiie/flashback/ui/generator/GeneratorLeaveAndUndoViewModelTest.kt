package com.mowtiie.flashback.ui.generator

import com.mowtiie.flashback.data.io.DeckArchive
import com.mowtiie.flashback.generator.CardGenerator
import com.mowtiie.flashback.generator.GeneratorInput
import com.mowtiie.flashback.generator.GeneratorProvider
import com.mowtiie.flashback.ui.settings.FakeArchive
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class GeneratorLeaveAndUndoViewModelTest {

    private var gate: CompletableDeferred<Unit>? = null

    private val generator = CardGenerator { request ->
        val text = (request.input as GeneratorInput.Text).text
        gate?.await()
        DeckArchive.Deck(
            name = "Deck",
            notes = listOf(DeckArchive.Note("first ${text.take(3)}", "a"), DeckArchive.Note("second ${text.take(3)}", "b")),
        )
    }

    @Before
    fun setUp() = Dispatchers.setMain(UnconfinedTestDispatcher())

    @After
    fun tearDown() = Dispatchers.resetMain()

    private fun viewModel() = GeneratorViewModel(
        generator, FakeArchive(), { true }, { GeneratorProvider.Anthropic }, UnconfinedTestDispatcher(), pause = {},
    )

    @Test
    fun anEmptyInputPageHasNothingToLose() {
        val vm = viewModel()
        vm.setSource("some notes")

        assertFalse(vm.uiState.value.hasWorkToLose)
    }

    @Test
    fun aRunInProgressHasWorkToLose() {
        gate = CompletableDeferred()
        val vm = viewModel()
        vm.setSource("notes")
        vm.generate()

        assertEquals(GeneratorStage.Generating, vm.uiState.value.stage)
        assertTrue(vm.uiState.value.hasWorkToLose)
        gate?.complete(Unit)
    }

    @Test
    fun reviewedCardsHaveWorkToLoseUntilTheyAreAllRemoved() {
        gate = null
        val vm = viewModel()
        vm.setSource("notes")
        vm.generate()

        assertEquals(GeneratorStage.Review, vm.uiState.value.stage)
        assertTrue(vm.uiState.value.hasWorkToLose)

        vm.removeCard(0)
        vm.removeCard(0)
        assertTrue(vm.uiState.value.cards.isEmpty())
        assertFalse(vm.uiState.value.hasWorkToLose)
    }

    @Test
    fun aRemovedCardComesBackInTheSamePlace() {
        gate = null
        val vm = viewModel()
        vm.setSource("notes")
        vm.generate()
        val fronts = vm.uiState.value.cards.map { it.front }

        vm.removeCard(0)
        assertEquals(fronts.drop(1), vm.uiState.value.cards.map { it.front })

        vm.restoreCard(fronts[0])
        assertEquals(fronts, vm.uiState.value.cards.map { it.front })
    }
}
