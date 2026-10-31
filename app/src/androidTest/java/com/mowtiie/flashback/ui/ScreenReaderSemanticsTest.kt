package com.mowtiie.flashback.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.mowtiie.flashback.data.entity.Deck
import com.mowtiie.flashback.data.entity.Tag
import com.mowtiie.flashback.data.model.NotePreview
import com.mowtiie.flashback.data.model.TagWithCount
import com.mowtiie.flashback.theme.FlashbackTheme
import com.mowtiie.flashback.ui.decks.DeckDetailScreen
import com.mowtiie.flashback.ui.decks.DeckDetailUiState
import com.mowtiie.flashback.ui.decks.DeckListScreen
import com.mowtiie.flashback.ui.decks.previewDeckListState
import com.mowtiie.flashback.ui.tags.TagsScreen
import com.mowtiie.flashback.ui.tags.TagsUiState
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** What a screen reader is given, read straight from the semantics tree. */
@RunWith(AndroidJUnit4::class)
class ScreenReaderSemanticsTest {

    @get:Rule
    val rule = createComposeRule()

    /** A button a screen reader can name: it is clickable and carries [label] as text or description. */
    private fun namedButton(label: String): SemanticsMatcher =
        (hasText(label) or hasContentDescription(label)) and hasClickAction()

    private fun show(content: @Composable () -> Unit) {
        rule.setContent { FlashbackTheme(dynamicColor = false) { content() } }
    }

    @Test
    fun newDeckButtonIsNamedAndClickable() {
        show {
            DeckListScreen(
                state = previewDeckListState(),
                onSelectTag = {}, onOpenDeck = {}, onStudyDeck = {}, onAddDeck = {}, onGenerate = {},
            )
        }
        rule.onNode(namedButton("New deck")).assertExists()
    }

    @Test
    fun newTagButtonIsNamedAndClickable() {
        show {
            TagsScreen(
                state = TagsUiState(loaded = true, tags = listOf(TagWithCount(Tag(1, "Biology", null, 0), 1))),
                onOpenTag = {}, onAddTag = {},
            )
        }
        rule.onNode(namedButton("New tag")).assertExists()
    }

    @Test
    fun studyButtonOnADeckIsNamedAndClickable() {
        show {
            DeckDetailScreen(
                state = DeckDetailUiState(
                    loaded = true,
                    deck = Deck(id = 1, name = "Spanish"),
                    notes = listOf(NotePreview(1, "hola", "hello", false)),
                    studyableCount = 3,
                ),
                onBack = {}, onEditDeck = {}, onExportDeck = {}, onDeleteDeck = {}, onAddNote = {},
                onOpenNote = {}, onStudy = {},
            )
        }
        rule.onNode(namedButton("Study 3")).assertExists()
    }

    @Test
    fun theVisibleLabelIsStillShown() {
        show {
            DeckListScreen(
                state = previewDeckListState(),
                onSelectTag = {}, onOpenDeck = {}, onStudyDeck = {}, onAddDeck = {}, onGenerate = {},
            )
        }
        rule.onNodeWithText("New deck", useUnmergedTree = true).assertExists()
    }
}
