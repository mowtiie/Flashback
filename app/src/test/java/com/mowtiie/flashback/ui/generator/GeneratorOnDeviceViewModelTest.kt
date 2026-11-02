package com.mowtiie.flashback.ui.generator

import com.mowtiie.flashback.data.io.DeckArchive
import com.mowtiie.flashback.generator.CardGenerator
import com.mowtiie.flashback.generator.CardCount
import com.mowtiie.flashback.generator.CardTypes
import com.mowtiie.flashback.generator.Coverage
import com.mowtiie.flashback.generator.GeneratorInput
import com.mowtiie.flashback.generator.GeneratorProvider
import com.mowtiie.flashback.generator.GeneratorRequest
import com.mowtiie.flashback.ui.settings.FakeArchive
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class GeneratorOnDeviceViewModelTest {

    private val requests = mutableListOf<GeneratorRequest>()

    private val generator = CardGenerator { request ->
        requests += request
        val text = (request.input as GeneratorInput.Text).text
        DeckArchive.Deck(name = "Deck", notes = listOf(DeckArchive.Note("q-${text.take(5)}", "a")))
    }

    @Before
    fun setUp() = Dispatchers.setMain(UnconfinedTestDispatcher())

    @After
    fun tearDown() = Dispatchers.resetMain()

    private fun viewModel(provider: GeneratorProvider) = GeneratorViewModel(
        generator, FakeArchive(), { true }, { provider }, UnconfinedTestDispatcher(), pause = {},
    ).also {
        it.setCardTypes(CardTypes.Setting)
        it.setCoverage(Coverage.KeyPoints)
    }

    private fun paragraph(n: Int, size: Int) = "Para$n " + "x".repeat(size)

    @Test
    fun aLongOnDeviceKeyPointsTextIsSplitAndEachSectionGetsAShareOfTheCards() {
        val vm = viewModel(GeneratorProvider.OnDevice)
        vm.setCardCount(CardCount.Normal)
        vm.setSource(paragraph(1, 2_000) + "\n\n" + paragraph(2, 2_000))

        assertEquals(2, vm.uiState.value.estimatedRequests)
        vm.generate()

        assertEquals(2, requests.size)
        assertTrue(requests.all { it.coverage == Coverage.KeyPoints })
        assertEquals(listOf(15, 15), requests.map { it.cardLimit })
        assertEquals(GeneratorStage.Review, vm.uiState.value.stage)
        assertEquals(2, vm.uiState.value.cards.size)
    }

    @Test
    fun theShareIsRoundedUpSoNoSectionGetsZero() {
        val vm = viewModel(GeneratorProvider.OnDevice)
        vm.setCardCount(CardCount.Few)
        vm.setSource((1..3).joinToString("\n\n") { paragraph(it, 1_900) })
        vm.generate()

        assertEquals(3, requests.size)
        assertTrue(requests.all { it.cardLimit == 5 })
    }

    @Test
    fun aShortOnDeviceKeyPointsTextStaysOneRequest() {
        val vm = viewModel(GeneratorProvider.OnDevice)
        vm.setSource(paragraph(1, 1_500))
        vm.generate()

        assertEquals(1, requests.size)
        assertNull(requests.single().cardLimit)
    }

    @Test
    fun onlyLongRunsNeedTheProgressNotification() {
        val onDevice = viewModel(GeneratorProvider.OnDevice)
        onDevice.setSource(paragraph(1, 500))
        assertTrue(onDevice.uiState.value.keepsRunning)

        val quickOnline = viewModel(GeneratorProvider.Anthropic)
        quickOnline.setSource(paragraph(1, 500))
        assertFalse(quickOnline.uiState.value.keepsRunning)

        val longOnline = viewModel(GeneratorProvider.Anthropic)
        longOnline.setCoverage(Coverage.Everything)
        longOnline.setSource(paragraph(1, 5_000) + "\n\n" + paragraph(2, 5_000))
        assertTrue(longOnline.uiState.value.keepsRunning)
    }

    @Test
    fun anOnlineProviderKeepsKeyPointsAsOneRequest() {
        val vm = viewModel(GeneratorProvider.Anthropic)
        vm.setSource(paragraph(1, 5_000) + "\n\n" + paragraph(2, 5_000))
        vm.generate()

        assertEquals(1, requests.size)
        assertNull(requests.single().cardLimit)
    }
}
