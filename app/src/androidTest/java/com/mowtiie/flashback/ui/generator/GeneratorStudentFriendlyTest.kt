package com.mowtiie.flashback.ui.generator

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.test.espresso.Espresso
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.mowtiie.flashback.data.io.DeckArchive
import com.mowtiie.flashback.generator.Coverage
import com.mowtiie.flashback.theme.FlashbackTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** The generate cards screen as a student meets it: no forced choices, no lost work, no jargon. */
@RunWith(AndroidJUnit4::class)
class GeneratorStudentFriendlyTest {

    @get:Rule
    val rule = createComposeRule()

    private fun show(state: GeneratorUiState, actions: GeneratorActions = GeneratorActions()) {
        rule.setContent { FlashbackTheme(dynamicColor = false) { GeneratorScreen(state, actions) } }
    }

    private val empty = GeneratorUiState(hasApiKey = true)
    private val cards = listOf(DeckArchive.Note("What is a cell?", "The unit of life"), DeckArchive.Note("What is DNA?", "Genetic code"))
    private val review = GeneratorUiState(hasApiKey = true, stage = GeneratorStage.Review, deckName = "Biology", cards = cards)
    private val running = GeneratorUiState(hasApiKey = true, stage = GeneratorStage.Generating, sectionTotal = 3, sectionsFinished = 1)

    private fun openOptions() { rule.onNodeWithText("Options").performClick() }

    private fun navigateUp() = rule.onNode(hasContentDescription("Navigate up")).performClick()

    @Test
    fun theHintIsVisibleBeforeAnythingIsTyped() {
        show(empty)
        rule.onNode(hasText("Paste your notes, a chapter or an exam reviewer here")).assertExists()
    }

    @Test
    fun noHugeCharacterCounterIsShown() {
        show(empty)
        rule.onNode(hasText("500,000", substring = true)).assertDoesNotExist()
    }

    @Test
    fun plainCardsAreAlreadyChosenAndGenerateWorksWithJustText() {
        var generated = 0
        show(empty.copy(source = "The cell is the unit of life."), GeneratorActions(onGenerate = { generated++ }))

        openOptions()
        rule.onNodeWithText("Match my settings").assertIsSelected()
        rule.onNodeWithText("Generate").performScrollTo().assertIsEnabled().performClick()
        assertEquals(1, generated)
    }

    @Test
    fun theOptionsStartClosedWithASummary() {
        show(empty)
        rule.onNodeWithText("Options").assertExists()
        rule.onNodeWithText("Every fact · Match my settings").assertExists()
        rule.onNode(hasText("Big ideas")).assertDoesNotExist()
        rule.onNode(hasText("Pick from choices")).assertDoesNotExist()
    }

    @Test
    fun openingTheOptionsShowsThemAndTheChoiceIsKept() {
        var coverage: Coverage? = null
        show(empty, GeneratorActions(onCoverage = { coverage = it }))
        openOptions()
        rule.onNodeWithText("Big ideas").performScrollTo().performClick()
        assertEquals(Coverage.KeyPoints, coverage)
    }

    @Test
    fun theOptionsUseStudentWords() {
        show(empty)
        openOptions()
        listOf("What to include", "Every fact", "Big ideas", "Card style", "Match my settings", "A mix", "Flip the card", "Type the answer", "Pick from choices")
            .forEach { rule.onNodeWithText(it).performScrollTo().assertExists() }
        listOf("Study style", "Flip only", "Type only", "Multiple choice only", "Choose one to continue.")
            .forEach { rule.onNode(hasText(it)).assertDoesNotExist() }
    }

    @Test
    fun leavingAnEmptyInputPageDoesNotAsk() {
        var left = 0
        show(empty.copy(source = "some notes"), GeneratorActions(onBack = { left++ }))
        navigateUp()
        assertEquals(1, left)
    }

    @Test
    fun leavingARunAsksFirstAndKeepGoingStaysPut() {
        var left = 0
        show(running, GeneratorActions(onBack = { left++ }))

        navigateUp()
        rule.onNodeWithText("Stop making cards?").assertExists()
        assertEquals(0, left)

        rule.onNodeWithText("Keep going").performClick()
        rule.onNode(hasText("Stop making cards?")).assertDoesNotExist()
        assertEquals(0, left)

        navigateUp()
        rule.onNodeWithText("Stop").performClick()
        assertEquals(1, left)
    }

    @Test
    fun theBackGestureAsksToo() {
        var left = 0
        show(running, GeneratorActions(onBack = { left++ }))

        Espresso.pressBack()
        rule.onNodeWithText("Stop making cards?").assertExists()
        assertEquals(0, left)
    }

    @Test
    fun leavingUnsavedCardsAsksHowManyWouldBeLost() {
        var left = 0
        show(review, GeneratorActions(onBack = { left++ }))

        navigateUp()
        rule.onNodeWithText("Discard these cards?").assertExists()
        rule.onNode(hasText("These 2 cards have not been saved yet.")).assertExists()
        assertEquals(0, left)

        rule.onNodeWithText("Keep reviewing").performClick()
        assertEquals(0, left)

        navigateUp()
        rule.onNodeWithText("Discard").performClick()
        assertEquals(1, left)
    }

    @Test
    fun leavingAReviewWithNoCardsLeftDoesNotAsk() {
        var left = 0
        show(review.copy(cards = emptyList()), GeneratorActions(onBack = { left++ }))
        navigateUp()
        assertEquals(1, left)
    }

    @Test
    fun removingACardOffersUndoAndUndoPutsItBack() {
        var shown by mutableStateOf(cards)
        rule.setContent {
            FlashbackTheme(dynamicColor = false) {
                GeneratorScreen(
                    review.copy(cards = shown),
                    GeneratorActions(
                        onRemoveCard = { index -> shown = shown.filterIndexed { i, _ -> i != index } },
                        onRestoreCard = { front -> shown = cards.filter { it in shown || it.front == front } },
                    ),
                )
            }
        }

        rule.onNode(hasContentDescription("Remove card What is a cell?")).performClick()
        rule.onNodeWithText("Card removed").assertExists()
        rule.onNode(hasText("What is a cell?")).assertDoesNotExist()

        rule.onNodeWithText("Undo").performClick()
        rule.onNode(hasText("What is a cell?")).assertExists()
    }
}
