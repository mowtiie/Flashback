package com.mowtiie.flashback.ui.generator

import com.mowtiie.flashback.data.io.DeckArchive
import com.mowtiie.flashback.generator.CardGenerator
import com.mowtiie.flashback.generator.GeneratorException
import com.mowtiie.flashback.generator.CardCount
import com.mowtiie.flashback.generator.GeneratorInput
import com.mowtiie.flashback.generator.GeneratorLimits
import com.mowtiie.flashback.generator.GeneratorRequest
import com.mowtiie.flashback.generator.pdfRequest
import com.mowtiie.flashback.generator.GeneratorProvider
import com.mowtiie.flashback.ui.settings.FakeArchive
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class GeneratorViewModelTest {

    private val archive = FakeArchive()
    private var hasKey = true
    private var provider = GeneratorProvider.Anthropic
    private var generated: DeckArchive.Deck? = DeckArchive.Deck(
        name = "Cells",
        notes = listOf(DeckArchive.Note("Q1", "A1"), DeckArchive.Note("Q2", "A2"), DeckArchive.Note("Q3", "A3")),
    )
    private var failure: GeneratorException? = null
    private val sources = mutableListOf<String>()

    private fun textOf(request: GeneratorRequest) = (request.input as? GeneratorInput.Text)?.text ?: "<pdf>"

    private val generator = CardGenerator { request ->
        sources += textOf(request)
        failure?.let { throw it }
        generated!!
    }

    @Before
    fun setUp() = Dispatchers.setMain(UnconfinedTestDispatcher())

    @After
    fun tearDown() = Dispatchers.resetMain()

    private fun viewModel() = GeneratorViewModel(generator, archive, { hasKey }, { provider }, UnconfinedTestDispatcher())

    @Test
    fun cannotGenerateWithoutAKeyOrText() {
        hasKey = false
        val viewModel = viewModel()
        viewModel.setSource("Some notes")
        assertFalse(viewModel.uiState.value.canGenerate)

        hasKey = true
        viewModel.refreshKey()
        assertTrue(viewModel.uiState.value.canGenerate)

        viewModel.setSource("   ")
        assertFalse(viewModel.uiState.value.canGenerate)
    }

    @Test
    fun overlongSourceIsRefusedRatherThanTrimmed() {
        val viewModel = viewModel()
        viewModel.setSource("x".repeat(GeneratorLimits.MAX_SOURCE_CHARS + 1))
        assertTrue(viewModel.uiState.value.sourceTooLong)
        viewModel.generate()
        assertTrue(sources.isEmpty())
    }

    @Test
    fun generatingMovesToReviewWithTheModelsDeck() {
        val viewModel = viewModel()
        viewModel.setSource("  Cells have a nucleus.  ")
        viewModel.generate()

        val state = viewModel.uiState.value
        assertEquals(GeneratorStage.Review, state.stage)
        assertEquals("Cells", state.deckName)
        assertEquals(3, state.cards.size)
        assertEquals(listOf("Cells have a nucleus."), sources)
    }

    @Test
    fun aFailureReturnsToInputWithTheTextAndMessageKept() {
        failure = GeneratorException("Anthropic rejected the API key. Check it in Settings.")
        val viewModel = viewModel()
        viewModel.setSource("notes")
        viewModel.generate()

        val state = viewModel.uiState.value
        assertEquals(GeneratorStage.Input, state.stage)
        assertEquals("notes", state.source)
        assertEquals("Anthropic rejected the API key. Check it in Settings.", state.error)

        viewModel.setSource("notes!")
        assertNull(viewModel.uiState.value.error)
    }

    @Test
    fun aSecondTapWhileGeneratingDoesNotSendAgain() = runTest {
        val gate = CompletableDeferred<DeckArchive.Deck>()
        val slow = GeneratorViewModel({ request -> sources += textOf(request); gate.await() }, archive, { true }, { provider }, UnconfinedTestDispatcher())
        slow.setSource("notes")
        slow.generate()
        slow.generate()

        assertEquals(GeneratorStage.Generating, slow.uiState.value.stage)
        assertEquals(1, sources.size)
        gate.complete(generated!!)
        assertEquals(GeneratorStage.Review, slow.uiState.value.stage)
    }

    @Test
    fun removedCardsAreNotSaved() = runTest {
        val viewModel = viewModel()
        viewModel.setSource("notes")
        viewModel.generate()
        viewModel.removeCard(1)
        viewModel.setDeckName("  Biology  ")
        viewModel.save()

        assertEquals(GeneratorEvent.Saved, viewModel.eventFlow.first())
        val saved = archive.imported.single().decks.single()
        assertEquals("Biology", saved.name)
        assertEquals(listOf("Q1", "Q3"), saved.notes.map { it.front })
    }

    @Test
    fun cannotSaveWithoutAnameOrCards() {
        val viewModel = viewModel()
        viewModel.setSource("notes")
        viewModel.generate()

        viewModel.setDeckName(" ")
        viewModel.save()
        assertTrue(archive.imported.isEmpty())

        viewModel.setDeckName("Cells")
        (0 until 3).forEach { viewModel.removeCard(0) }
        viewModel.save()
        assertTrue(archive.imported.isEmpty())
    }

    @Test
    fun aFailedSaveStaysOnReviewAndTellsTheScreen() = runTest {
        archive.importResult = null
        val viewModel = viewModel()
        viewModel.setSource("notes")
        viewModel.generate()
        viewModel.save()

        assertEquals(GeneratorEvent.SaveFailed, viewModel.eventFlow.first())
        assertEquals(GeneratorStage.Review, viewModel.uiState.value.stage)
        assertEquals(3, viewModel.uiState.value.cards.size)
    }

    @Test
    fun startingOverKeepsTheSourceText() {
        val viewModel = viewModel()
        viewModel.setSource("notes")
        viewModel.generate()
        viewModel.startOver()

        val state = viewModel.uiState.value
        assertEquals(GeneratorStage.Input, state.stage)
        assertEquals("notes", state.source)
        assertTrue(state.cards.isEmpty())
    }

    private val pdf = com.mowtiie.flashback.generator.GeneratorInput.Pdf("handout.pdf", com.mowtiie.flashback.generator.FAKE_PDF)

    @Test
    fun aPickedPdfCanBeGeneratedWithoutAnyText() {
        val viewModel = viewModel()
        viewModel.attachPdf { pdf }

        val state = viewModel.uiState.value
        assertEquals("handout.pdf", state.pdfName)
        assertEquals(com.mowtiie.flashback.generator.FAKE_PDF.size, state.pdfSize)
        assertTrue(state.canGenerate)

        viewModel.generate()
        assertEquals(GeneratorStage.Review, viewModel.uiState.value.stage)
        assertEquals(listOf("<pdf>"), sources)
    }

    @Test
    fun theCardCountReachesTheGenerator() {
        var asked: CardCount? = null
        val viewModel = GeneratorViewModel(
            { request -> asked = request.cardCount; generated!! },
            archive,
            { true },
            { provider },
            UnconfinedTestDispatcher(),
        )
        viewModel.setSource("notes")
        viewModel.setCardCount(CardCount.Many)
        viewModel.generate()
        assertEquals(CardCount.Many, asked)
    }

    @Test
    fun aBadFileShowsAMessageAndKeepsAnyEarlierPdf() {
        val viewModel = viewModel()
        viewModel.attachPdf { pdf }
        viewModel.attachPdf { throw GeneratorException("That file does not look like a PDF.") }

        val state = viewModel.uiState.value
        assertEquals("That file does not look like a PDF.", state.error)
        assertEquals("handout.pdf", state.pdfName)
    }

    @Test
    fun removingThePdfGoesBackToText() {
        val viewModel = viewModel()
        viewModel.attachPdf { pdf }
        viewModel.removePdf()

        assertFalse(viewModel.uiState.value.hasPdf)
        assertFalse(viewModel.uiState.value.canGenerate)
        viewModel.setSource("notes")
        viewModel.generate()
        assertEquals(listOf("notes"), sources)
    }

    @Test
    fun anAttachedPdfWinsOverTypedText() {
        val viewModel = viewModel()
        viewModel.setSource("notes")
        viewModel.attachPdf { pdf }
        viewModel.generate()
        assertEquals(listOf("<pdf>"), sources)
    }
}
