package com.mowtiie.flashback.ui.generator

import com.mowtiie.flashback.data.io.DeckArchive
import com.mowtiie.flashback.generator.CardGenerator
import com.mowtiie.flashback.generator.CardTypes
import com.mowtiie.flashback.generator.Coverage
import com.mowtiie.flashback.generator.FailureKind
import com.mowtiie.flashback.generator.GeneratorException
import com.mowtiie.flashback.generator.GeneratorInput
import com.mowtiie.flashback.generator.GeneratorLimits
import com.mowtiie.flashback.generator.GeneratorProvider
import com.mowtiie.flashback.generator.GeneratorRequest
import com.mowtiie.flashback.ui.settings.FakeArchive
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class GeneratorCoverageViewModelTest {

    private val archive = FakeArchive()
    private val requests = mutableListOf<GeneratorRequest>()
    private var failOn: Set<String> = emptySet()
    private var failure = GeneratorException("It broke.")
    private var gate: Map<String, CompletableDeferred<Unit>> = emptyMap()

    /** Three paragraphs that cannot share a section, so a text of them is three requests. */
    private fun paragraph(n: Int) = "Para$n " + "x".repeat(5000)

    private fun text(count: Int) = (1..count).joinToString("\n\n") { paragraph(it) }

    private fun key(request: GeneratorRequest) = (request.input as GeneratorInput.Text).text.take(5)

    private fun deckFor(request: GeneratorRequest) =
        DeckArchive.Deck(name = "Deck ${key(request)}", notes = listOf(DeckArchive.Note("q-${key(request)}", "a")))

    private val generator = CardGenerator { request ->
        requests += request
        val k = (request.input as? GeneratorInput.Text)?.text?.take(5).orEmpty()
        gate[k]?.await()
        if (k in failOn) throw failure
        if (request.input is GeneratorInput.Text) deckFor(request) else DeckArchive.Deck(name = "Pdf", notes = listOf(DeckArchive.Note("q-pdf", "a")))
    }

    @Before
    fun setUp() = Dispatchers.setMain(UnconfinedTestDispatcher())

    @After
    fun tearDown() = Dispatchers.resetMain()

    private fun viewModel() = GeneratorViewModel(
        generator, archive, { true }, { GeneratorProvider.Anthropic }, UnconfinedTestDispatcher(),
        pause = {},
    ).also { it.setCardTypes(CardTypes.Setting) }

    private fun GeneratorViewModel.generateConfirmed(source: String) {
        setSource(source)
        generate()
        if (uiState.value.confirming) confirmGenerate()
    }

    @Test
    fun everythingIsTheDefaultAndShowsAnEstimate() {
        val vm = viewModel()
        assertEquals(Coverage.Everything, vm.uiState.value.coverage)

        vm.setSource("x".repeat(GeneratorLimits.SECTION_CHARS * 5 + 10))
        assertEquals(6, vm.uiState.value.estimatedRequests)
    }

    @Test
    fun longTextIsSentOneSectionAtATimeAndMergedInOrder() {
        val vm = viewModel()
        vm.generateConfirmed(text(3))

        val state = vm.uiState.value
        assertEquals(GeneratorStage.Review, state.stage)
        assertEquals(listOf("Para1", "Para2", "Para3"), requests.map(::key))
        assertEquals(listOf("q-Para1", "q-Para2", "q-Para3"), state.cards.map { it.front })
        assertEquals(listOf(1, 1, 1), state.sectionCounts)
        assertEquals("Deck Para1", state.deckName)
        assertTrue(state.failedSections.isEmpty())
    }

    @Test
    fun aBiggerRunAsksForConfirmationBeforeSendingAnything() {
        val vm = viewModel()
        vm.setSource(text(GeneratorLimits.CONFIRM_ABOVE_SECTIONS + 1))
        vm.generate()

        assertTrue(vm.uiState.value.confirming)
        assertEquals(GeneratorStage.Input, vm.uiState.value.stage)
        assertTrue(requests.isEmpty())

        vm.confirmGenerate()
        assertFalse(vm.uiState.value.confirming)
        assertEquals(GeneratorStage.Review, vm.uiState.value.stage)
        assertEquals(GeneratorLimits.CONFIRM_ABOVE_SECTIONS + 1, requests.size)
    }

    @Test
    fun decliningTheConfirmationSendsNothing() {
        val vm = viewModel()
        vm.setSource(text(GeneratorLimits.CONFIRM_ABOVE_SECTIONS + 1))
        vm.generate()
        vm.dismissConfirm()

        assertFalse(vm.uiState.value.confirming)
        assertTrue(requests.isEmpty())
    }

    @Test
    fun aSmallRunDoesNotAsk() {
        val vm = viewModel()
        vm.setSource(text(GeneratorLimits.CONFIRM_ABOVE_SECTIONS))
        vm.generate()
        assertFalse(vm.uiState.value.confirming)
        assertEquals(GeneratorStage.Review, vm.uiState.value.stage)
    }

    @Test
    fun progressShowsWhichSectionIsRunningAndKeepsCardsSoFar() {
        val second = CompletableDeferred<Unit>()
        gate = mapOf("Para2" to second)
        val vm = viewModel()
        vm.generateConfirmed(text(3))

        val state = vm.uiState.value
        assertEquals(GeneratorStage.Generating, state.stage)
        assertEquals(3, state.sectionTotal)
        assertEquals(1, state.sectionsFinished)
        assertEquals(listOf("q-Para1"), state.cards.map { it.front })

        second.complete(Unit)
        assertEquals(GeneratorStage.Review, vm.uiState.value.stage)
        assertEquals(3, vm.uiState.value.cards.size)
    }

    @Test
    fun aFailedSectionKeepsTheOthersAndCanBeRetriedAlone() {
        failOn = setOf("Para2")
        val vm = viewModel()
        vm.generateConfirmed(text(3))

        var state = vm.uiState.value
        assertEquals(GeneratorStage.Review, state.stage)
        assertEquals(listOf("q-Para1", "q-Para3"), state.cards.map { it.front })
        assertEquals(listOf(2), state.failedSections.map { it.number })
        assertEquals("It broke.", state.failedSections.single().message)

        failOn = emptySet()
        requests.clear()
        vm.retryFailed()

        state = vm.uiState.value
        assertEquals(listOf("Para2"), requests.map(::key))
        assertEquals(listOf("q-Para1", "q-Para2", "q-Para3"), state.cards.map { it.front })
        assertTrue(state.failedSections.isEmpty())
        assertEquals(GeneratorStage.Review, state.stage)
    }

    @Test
    fun aRetryCarriesTheFrontsAlreadyMade() {
        failOn = setOf("Para2")
        val vm = viewModel()
        vm.generateConfirmed(text(3))
        failOn = emptySet()
        requests.clear()
        vm.retryFailed()

        assertEquals(listOf("q-Para1", "q-Para3"), requests.single().known)
    }

    @Test
    fun aCardTheUserRemovedStaysRemovedAfterARetry() {
        failOn = setOf("Para2")
        val vm = viewModel()
        vm.generateConfirmed(text(3))
        vm.removeCard(0)
        failOn = emptySet()
        vm.retryFailed()

        assertEquals(listOf("q-Para2", "q-Para3"), vm.uiState.value.cards.map { it.front })
    }

    @Test
    fun cancellingKeepsFinishedSectionsAndMarksTheRestForRetry() {
        gate = mapOf("Para2" to CompletableDeferred())
        val vm = viewModel()
        vm.generateConfirmed(text(3))
        vm.cancelGeneration()

        val state = vm.uiState.value
        assertEquals(GeneratorStage.Review, state.stage)
        assertEquals(listOf("q-Para1"), state.cards.map { it.front })
        assertEquals(listOf(2, 3), state.failedSections.map { it.number })
        assertEquals(listOf("Para1", "Para2"), requests.map(::key))
    }

    @Test
    fun cancellingBeforeAnyCardsGoesBackToInput() {
        gate = mapOf("Para1" to CompletableDeferred())
        val vm = viewModel()
        vm.generateConfirmed(text(3))
        vm.cancelGeneration()

        assertEquals(GeneratorStage.Input, vm.uiState.value.stage)
        assertEquals(text(3), vm.uiState.value.source)
        assertTrue(vm.uiState.value.cards.isEmpty())
    }

    @Test
    fun aRefusedKeyStopsTheRunAndReturnsToInputWithTheMessage() {
        failOn = setOf("Para1")
        failure = GeneratorException("Anthropic rejected the API key. Check it in Settings.", kind = FailureKind.BadKey)
        val vm = viewModel()
        vm.generateConfirmed(text(3))

        val state = vm.uiState.value
        assertEquals(1, requests.size)
        assertEquals(GeneratorStage.Input, state.stage)
        assertEquals("Anthropic rejected the API key. Check it in Settings.", state.error)
    }

    @Test
    fun startingOverClearsTheRunButKeepsTheText() {
        failOn = setOf("Para2")
        val vm = viewModel()
        vm.generateConfirmed(text(3))
        vm.startOver()

        val state = vm.uiState.value
        assertEquals(GeneratorStage.Input, state.stage)
        assertTrue(state.cards.isEmpty())
        assertTrue(state.failedSections.isEmpty())
        assertEquals(text(3), state.source)
    }

    @Test
    fun keyPointsIsOneRequestWithTheCardCountAndTheOldSizeLimit() {
        val vm = viewModel()
        vm.setCoverage(Coverage.KeyPoints)
        vm.setSource("Short notes.")
        vm.generate()

        assertEquals(Coverage.KeyPoints, requests.single().coverage)
        assertEquals(GeneratorStage.Review, vm.uiState.value.stage)

        vm.startOver()
        requests.clear()
        vm.setSource("x".repeat(GeneratorLimits.KEY_POINTS_MAX_CHARS + 1))
        assertTrue(vm.uiState.value.sourceTooLong)
        vm.generate()
        assertTrue(requests.isEmpty())
    }

    @Test
    fun longTextIsFineForEverythingButNotAboveTheSanityCeiling() {
        val vm = viewModel()
        vm.setSource("x".repeat(GeneratorLimits.KEY_POINTS_MAX_CHARS + 1))
        assertFalse(vm.uiState.value.sourceTooLong)

        vm.setSource("x".repeat(GeneratorLimits.MAX_SOURCE_CHARS + 1))
        assertTrue(vm.uiState.value.sourceTooLong)
        assertFalse(vm.uiState.value.canGenerate)
    }

    @Test
    fun aPdfIsOneEverythingRequest() {
        val vm = viewModel()
        vm.attachPdf { GeneratorInput.Pdf("handout.pdf", com.mowtiie.flashback.generator.FAKE_PDF) }
        vm.generate()

        assertEquals(1, requests.size)
        assertEquals(Coverage.Everything, requests.single().coverage)
        assertEquals(GeneratorStage.Review, vm.uiState.value.stage)
        assertEquals(1, vm.uiState.value.estimatedRequests)
    }

    @Test
    fun savingStillNeedsReviewAndWritesTheMergedDeck() = runTest {
        val vm = viewModel()
        vm.generateConfirmed(text(2))
        vm.save()

        val saved = archive.imported.single().decks.single()
        assertEquals(listOf("q-Para1", "q-Para2"), saved.notes.map { it.front })
    }
}
