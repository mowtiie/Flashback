package com.mowtiie.flashback.ui

import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.getBoundsInRoot
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.mowtiie.flashback.data.entity.Deck
import com.mowtiie.flashback.data.model.NotePreview
import com.mowtiie.flashback.theme.FlashbackTheme
import com.mowtiie.flashback.ui.components.ActionFab
import com.mowtiie.flashback.ui.decks.DeckDetailScreen
import com.mowtiie.flashback.ui.decks.DeckDetailUiState
import com.mowtiie.flashback.ui.prompt.PromptBuilderActions
import com.mowtiie.flashback.ui.prompt.PromptBuilderScreen
import com.mowtiie.flashback.ui.prompt.PromptBuilderUiState
import com.mowtiie.flashback.ui.study.StudyActions
import com.mowtiie.flashback.ui.study.StudyCardUi
import com.mowtiie.flashback.ui.study.StudyScreen
import com.mowtiie.flashback.ui.study.StudyUiState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** Follow-ups to the Material 3 audit: top bar actions, snackbar placement, card variant, FAB collapse, delete without a dialog. */
@RunWith(AndroidJUnit4::class)
class GuidelinesFollowUpTest {

    @get:Rule
    val rule = createComposeRule()

    private fun deckPage(
        notes: List<NotePreview>,
        onDelete: () -> Unit = {},
        onExport: () -> Unit = {},
    ) = rule.setContent {
        FlashbackTheme(dynamicColor = false) {
            DeckDetailScreen(
                state = DeckDetailUiState(loaded = true, deck = Deck(id = 1, name = "Spanish"), notes = notes),
                onBack = {}, onEditDeck = {}, onExportDeck = onExport, onDeleteDeck = onDelete,
                onAddNote = {}, onOpenNote = {}, onStudy = {}, shortWindow = true,
            )
        }
    }

    private val oneNote = listOf(NotePreview(1, "hola", "hello", false))

    // 2. Two actions in the bar and no overflow menu.
    @Test
    fun theDeckPageBarHasTwoActionsAndNoOverflowMenu() {
        deckPage(oneNote)
        rule.onAllNodes(hasContentDescription("More options")).assertCountEquals(0)
        rule.onNode(hasContentDescription("Add card")).assertExists()
        rule.onNode(hasContentDescription("Edit deck")).assertExists()
    }

    @Test
    fun exportAndDeleteSitAtTheEndOfTheDeckPage() {
        var exported = 0
        var deleted = 0
        deckPage(oneNote, onDelete = { deleted++ }, onExport = { exported++ })
        rule.onNodeWithText("Export deck").performScrollTo().performClick()
        rule.onNodeWithText("Delete deck").performScrollTo().performClick()
        assertEquals(1, exported)
        // 13. Deleting acts at once; the Undo message that follows is the safety net.
        assertEquals(1, deleted)
    }

    @Test
    fun anEmptyDeckCanStillBeExportedAndDeleted() {
        deckPage(emptyList())
        rule.onNodeWithText("Export deck").assertExists()
        rule.onNodeWithText("Delete deck").assertExists()
    }

    // 5. Snackbars sit at the bottom, above the controls, not at the top.
    @Test
    fun theStudySnackbarIsAtTheBottomAndLeavesShowAnswerClear() {
        val host = SnackbarHostState()
        rule.setContent {
            LaunchedEffect(Unit) { host.showSnackbar("Last answer undone") }
            FlashbackTheme(dynamicColor = false) {
                StudyScreen(
                    StudyUiState(loading = false, card = StudyCardUi("el bosque", "the forest")),
                    StudyActions(),
                    host,
                )
            }
        }
        rule.waitForIdle()
        val snackbar = rule.onNodeWithText("Last answer undone").getBoundsInRoot()
        val button = rule.onNodeWithText("Show answer").getBoundsInRoot()
        val height = rule.onRoot().getBoundsInRoot().bottom
        assertTrue("snackbar should be in the lower half", snackbar.top > height / 2)
        assertTrue("snackbar ${snackbar.bottom} should end above the button at ${button.top}", snackbar.bottom <= button.top)
    }

    @Test
    fun thePromptSnackbarIsAtTheBottomAndLeavesCopyClear() {
        val host = SnackbarHostState()
        rule.setContent {
            LaunchedEffect(Unit) { host.showSnackbar("Prompt copied") }
            FlashbackTheme(dynamicColor = false) {
                PromptBuilderScreen(PromptBuilderUiState(), PromptBuilderActions(), host)
            }
        }
        rule.waitForIdle()
        val snackbar = rule.onNodeWithText("Prompt copied").getBoundsInRoot()
        val copy = rule.onNodeWithText("Copy prompt").getBoundsInRoot()
        val height = rule.onRoot().getBoundsInRoot().bottom
        assertTrue("snackbar should be in the lower half", snackbar.top > height / 2)
        assertTrue("snackbar ${snackbar.bottom} should end above Copy at ${copy.top}", snackbar.bottom <= copy.top)
    }

    // 10. Cards are the filled variant, which already sits a step above the page.
    @Test
    fun filledCardsAreDistinctFromThePageInDarkTheme() {
        var container = Color.Unspecified
        var page = Color.Unspecified
        rule.setContent {
            FlashbackTheme(darkTheme = true, dynamicColor = false) {
                container = CardDefaults.cardColors().containerColor
                page = MaterialTheme.colorScheme.background
            }
        }
        rule.waitForIdle()
        assertNotEquals(page, container)
    }

    // 11. The floating button shrinks to its icon but keeps its name.
    @Test
    fun aCollapsedFabShowsOnlyTheIconAndKeepsItsName() {
        rule.setContent { FlashbackTheme(dynamicColor = false) { ActionFab("New deck", {}, {}, expanded = false) } }
        rule.onAllNodes(hasText("New deck"), useUnmergedTree = true).assertCountEquals(0)
        rule.onAllNodes(hasContentDescription("New deck")).assertCountEquals(1)
    }

    @Test
    fun anExpandedFabShowsItsLabel() {
        rule.setContent { FlashbackTheme(dynamicColor = false) { ActionFab("New deck", {}, {}, expanded = true) } }
        rule.onAllNodes(hasText("New deck"), useUnmergedTree = true).assertCountEquals(1)
    }
}
