package com.mowtiie.flashback.ui.generator

import com.mowtiie.flashback.data.io.DeckArchive
import com.mowtiie.flashback.generator.CardGenerator
import com.mowtiie.flashback.generator.CardTypes
import com.mowtiie.flashback.generator.Coverage
import com.mowtiie.flashback.generator.GenerationSession
import com.mowtiie.flashback.generator.GeneratorException
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
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class GeneratorSessionViewModelTest {

    private val session = GenerationSession()
    private var gate: Map<String, CompletableDeferred<Unit>> = emptyMap()
    private var failure: GeneratorException? = null

    private fun key(text: String) = text.take(5)

    private val generator = CardGenerator { request ->
        val text = (request.input as GeneratorInput.Text).text
        gate[key(text)]?.await()
        failure?.let { throw it }
        DeckArchive.Deck(name = "Deck", notes = listOf(DeckArchive.Note("q-${key(text)}", "a")))
    }

    @Before
    fun setUp() = Dispatchers.setMain(UnconfinedTestDispatcher())

    @After
    fun tearDown() = Dispatchers.resetMain()

    private fun viewModel(provider: GeneratorProvider) = GeneratorViewModel(
        generator, FakeArchive(), { true }, { provider }, UnconfinedTestDispatcher(),
        pause = {}, session = session,
    ).also { it.setCardTypes(CardTypes.Setting) }

    private fun paragraph(n: Int, size: Int) = "Para$n " + "x".repeat(size)

    private fun twoSections(size: Int) = paragraph(1, size) + "\n\n" + paragraph(2, size)

    @Test
    fun anOnDeviceRunReportsProgressAndEndsWhenFinished() {
        gate = mapOf("Para2" to CompletableDeferred())
        val vm = viewModel(GeneratorProvider.OnDevice)
        vm.setSource(twoSections(2_000))
        vm.generate()

        assertEquals(GenerationSession.Progress(finished = 1, total = 2, cards = 1), session.progress.value)

        gate.getValue("Para2").complete(Unit)
        assertNull(session.progress.value)
        assertEquals(GeneratorStage.Review, vm.uiState.value.stage)
    }

    @Test
    fun aShortOnDeviceRunIsKeptAliveToo() {
        gate = mapOf("Para1" to CompletableDeferred())
        val vm = viewModel(GeneratorProvider.OnDevice)
        vm.setCoverage(Coverage.KeyPoints)
        vm.setSource(paragraph(1, 500))
        vm.generate()

        assertEquals(1, session.progress.value?.total)

        gate.getValue("Para1").complete(Unit)
        assertNull(session.progress.value)
    }

    @Test
    fun aQuickOnlineRequestDoesNotStartASession() {
        gate = mapOf("Para1" to CompletableDeferred())
        val vm = viewModel(GeneratorProvider.Anthropic)
        vm.setCoverage(Coverage.KeyPoints)
        vm.setSource(paragraph(1, 500))
        vm.generate()

        assertNull(session.progress.value)
        gate.getValue("Para1").complete(Unit)
    }

    @Test
    fun aLongOnlineRunStartsASession() {
        gate = mapOf("Para2" to CompletableDeferred())
        val vm = viewModel(GeneratorProvider.Anthropic)
        vm.setSource(twoSections(5_000))
        vm.generate()

        assertNotNull(session.progress.value)
        assertEquals(2, session.progress.value?.total)

        gate.getValue("Para2").complete(Unit)
        assertNull(session.progress.value)
    }

    @Test
    fun cancelFromTheNotificationStopsTheRunAndKeepsFinishedSections() {
        gate = mapOf("Para2" to CompletableDeferred())
        val vm = viewModel(GeneratorProvider.OnDevice)
        vm.setSource(twoSections(2_000))
        vm.generate()

        session.cancel()

        assertNull(session.progress.value)
        assertEquals(GeneratorStage.Review, vm.uiState.value.stage)
        assertEquals(1, vm.uiState.value.cards.size)
    }

    @Test
    fun aFailedOnDeviceRunEndsTheSession() {
        failure = GeneratorException("It broke.")
        val vm = viewModel(GeneratorProvider.OnDevice)
        vm.setCoverage(Coverage.KeyPoints)
        vm.setSource(paragraph(1, 500))
        vm.generate()

        assertNull(session.progress.value)
        assertEquals("It broke.", vm.uiState.value.error)
        assertEquals(GeneratorStage.Input, vm.uiState.value.stage)
    }
}
