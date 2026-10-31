package com.mowtiie.flashback.ui.decks

import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.hasAnyAncestor
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.mowtiie.flashback.theme.FlashbackTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class NewDeckSheetTest {

    @get:Rule
    val rule = createComposeRule()

    private var manual = 0
    private var generate = 0
    private var prompt = 0

    private fun show(withDecks: Boolean = true, shortWindow: Boolean = false) {
        rule.setContent {
            FlashbackTheme(dynamicColor = false) {
                DeckListScreen(
                    state = if (withDecks) previewDeckListState() else DeckListUiState(loaded = true),
                    onSelectTag = {}, onOpenDeck = {}, onStudyDeck = {},
                    onAddDeck = { manual++ },
                    onGenerate = { generate++ },
                    onPromptHelper = { prompt++ },
                    shortWindow = shortWindow,
                )
            }
        }
    }

    private fun openSheet() {
        rule.onNode(hasContentDescription("New deck") and hasClickAction()).performClick()
    }

    private fun inSheet(text: String) = hasText(text) and hasAnyAncestor(hasTestTag(NEW_DECK_SHEET_TAG))

    @Test
    fun theAppBarNoLongerHasAGenerateButton() {
        show()
        rule.onAllNodesWithContentDescription("Generate cards with AI").assertCountEquals(0)
    }

    @Test
    fun newDeckOpensASheetWithThreeWays() {
        show()
        openSheet()

        rule.onNodeWithTag(NEW_DECK_SHEET_TAG).assertExists()
        rule.onNode(inSheet("Make it myself")).assertExists()
        rule.onNode(inSheet("Generate with AI")).assertExists()
        rule.onNode(inSheet("Use an AI website")).assertExists()
        assertEquals(0, manual + generate + prompt)
    }

    @Test
    fun theEmptyListButtonOpensTheSameSheet() {
        show(withDecks = false)
        rule.onNodeWithText("Create a deck").performClick()
        rule.onNodeWithTag(NEW_DECK_SHEET_TAG).assertExists()
    }

    @Test
    fun eachRowStartsItsOwnPath() {
        show()
        openSheet()
        rule.onNode(inSheet("Make it myself")).performClick()
        rule.waitForIdle()
        assertEquals(listOf(1, 0, 0), listOf(manual, generate, prompt))

        openSheet()
        rule.onNode(inSheet("Generate with AI")).performClick()
        rule.waitForIdle()
        assertEquals(listOf(1, 1, 0), listOf(manual, generate, prompt))

        openSheet()
        rule.onNode(inSheet("Use an AI website")).performClick()
        rule.waitForIdle()
        assertEquals(listOf(1, 1, 1), listOf(manual, generate, prompt))
    }

    @Test
    fun theFabIsStillNamedForScreenReaders() {
        show()
        rule.onNode(hasContentDescription("New deck") or hasText("New deck")).assertExists()
    }

    @Test
    fun onAShortWindowTheActionMovesToTheTopBarAndNothingFloatsOverTheList() {
        show(shortWindow = true)
        // The floating button's visible label is gone, but the action is still named and opens the sheet.
        rule.onAllNodes(hasText("New deck"), useUnmergedTree = true).assertCountEquals(0)
        openSheet()
        rule.onNodeWithTag(NEW_DECK_SHEET_TAG).assertExists()
    }

    @Test
    fun onATallWindowTheFloatingButtonStays() {
        show(shortWindow = false)
        rule.onAllNodes(hasText("New deck"), useUnmergedTree = true).assertCountEquals(1)
    }
}
