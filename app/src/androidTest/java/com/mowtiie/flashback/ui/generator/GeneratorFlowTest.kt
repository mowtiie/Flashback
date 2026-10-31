package com.mowtiie.flashback.ui.generator

import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.mowtiie.flashback.data.io.DeckArchive
import com.mowtiie.flashback.generator.CardGenerator
import com.mowtiie.flashback.generator.CardTypes
import com.mowtiie.flashback.generator.GeneratorException
import com.mowtiie.flashback.generator.GeneratorInput
import com.mowtiie.flashback.generator.GeneratorProvider
import com.mowtiie.flashback.repository.ArchiveDataSource
import com.mowtiie.flashback.data.io.ImportResult
import com.mowtiie.flashback.theme.FlashbackTheme
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** The real view model and screen, with a fake provider, from tapping Generate to a saved deck. */
@RunWith(AndroidJUnit4::class)
class GeneratorFlowTest {

    @get:Rule
    val rule = createComposeRule()

    private val saved = mutableListOf<DeckArchive>()
    private val archive = object : ArchiveDataSource {
        override suspend fun exportAllText(): String? = null
        override suspend fun exportDeckText(deckId: Long): String? = null
        override suspend fun importArchive(archive: DeckArchive): ImportResult? {
            saved += archive
            return ImportResult(decks = 1, notes = archive.decks.sumOf { it.notes.size }, tagsCreated = 0, tagsReused = 0)
        }
    }

    private val sent = mutableListOf<String>()
    private var failSecond = true
    private var gate: CompletableDeferred<Unit>? = null

    private val generator = CardGenerator { request ->
        val text = (request.input as GeneratorInput.Text).text
        val key = text.take(5)
        sent += key
        if (key == "Para2") gate?.await()
        if (key == "Para2" && failSecond) throw GeneratorException("It broke.")
        DeckArchive.Deck(name = "Biology", notes = listOf(DeckArchive.Note("q-$key", "a")))
    }

    private fun waitForDialog() =
        rule.waitUntil(5_000) { rule.onAllNodes(hasText("Send many requests?")).fetchSemanticsNodes().isNotEmpty() }

    private fun paragraph(n: Int) = "Para$n " + "x".repeat(5000)

    private fun start(): GeneratorViewModel {
        val viewModel = GeneratorViewModel(generator, archive, { true }, { GeneratorProvider.Anthropic }, Dispatchers.IO, pause = {})
        viewModel.setCardTypes(CardTypes.Setting)
        viewModel.setSource((1..4).joinToString("\n\n") { paragraph(it) })
        rule.setContent {
            val state by viewModel.uiState.collectAsState()
            FlashbackTheme(dynamicColor = false) {
                GeneratorScreen(
                    state,
                    GeneratorActions(
                        onGenerate = viewModel::generate,
                        onCancel = viewModel::cancelGeneration,
                        onRetry = viewModel::retryFailed,
                        onConfirm = viewModel::confirmGenerate,
                        onDismissConfirm = viewModel::dismissConfirm,
                        onSave = viewModel::save,
                    ),
                )
            }
        }
        return viewModel
    }

    @Test
    fun aFailedSectionIsRetriedAndTheWholeDeckIsSaved() {
        val viewModel = start()
        rule.onNodeWithText("Generate").performScrollTo().performClick()
        waitForDialog()
        waitForDialog()
        rule.onNodeWithText("Send").performClick()

        rule.waitUntil(5_000) { rule.onAllNodes(hasText("1 section made no cards")).fetchSemanticsNodes().isNotEmpty() }
        assertEquals(listOf("Para1", "Para2", "Para3", "Para4"), sent)

        failSecond = false
        sent.clear()
        rule.onNodeWithText("Retry 1 section").performClick()
        rule.waitUntil(5_000) { viewModel.uiState.value.failedSections.isEmpty() && viewModel.uiState.value.stage == GeneratorStage.Review }
        assertEquals(listOf("Para2"), sent)

        rule.onNodeWithText("Save deck").performClick()
        rule.waitUntil(5_000) { saved.isNotEmpty() }
        assertEquals(listOf("q-Para1", "q-Para2", "q-Para3", "q-Para4"), saved.single().decks.single().notes.map { it.front })
    }

    @Test
    fun cancellingKeepsTheFinishedSectionAndOffersRetryForTheRest() {
        val viewModel = start()
        gate = CompletableDeferred()
        rule.onNodeWithText("Generate").performScrollTo().performClick()
        waitForDialog()
        rule.onNodeWithText("Send").performClick()

        rule.waitUntil(5_000) { rule.onAllNodes(hasText("Section 2 of 4")).fetchSemanticsNodes().isNotEmpty() }
        rule.onNodeWithText("Cancel").performClick()

        rule.waitUntil(5_000) { rule.onAllNodes(hasText("3 sections made no cards")).fetchSemanticsNodes().isNotEmpty() }
        assertEquals(listOf("q-Para1"), viewModel.uiState.value.cards.map { it.front })
        assertTrue(sent.none { it == "Para3" || it == "Para4" })
    }

    @Test
    fun decliningTheConfirmationSendsNothing() {
        start()
        rule.onNodeWithText("Generate").performScrollTo().performClick()
        waitForDialog()
        rule.onNodeWithText("Cancel").performClick()
        rule.waitForIdle()
        assertTrue(sent.isEmpty())
        rule.onNodeWithText("Generate").assertExists()
    }
}
