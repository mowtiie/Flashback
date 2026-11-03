package com.mowtiie.flashback.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.SemanticsNodeInteraction
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.getBoundsInRoot
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.mowtiie.flashback.data.entity.Deck
import com.mowtiie.flashback.data.entity.Tag
import com.mowtiie.flashback.data.model.TagWithCount
import com.mowtiie.flashback.theme.FlashbackTheme
import com.mowtiie.flashback.ui.decks.DeckDetailScreen
import com.mowtiie.flashback.ui.decks.DeckDetailUiState
import com.mowtiie.flashback.ui.decks.DeckEditorScreen
import com.mowtiie.flashback.ui.decks.DeckEditorUiState
import com.mowtiie.flashback.ui.decks.DeckListScreen
import com.mowtiie.flashback.ui.decks.DeckListUiState
import com.mowtiie.flashback.ui.decks.previewDeckListState
import com.mowtiie.flashback.ui.notes.NoteEditorForm
import com.mowtiie.flashback.ui.notes.NoteEditorScreen
import com.mowtiie.flashback.ui.notes.NoteEditorUiState
import com.mowtiie.flashback.ui.prompt.PromptBuilderActions
import com.mowtiie.flashback.ui.prompt.PromptBuilderScreen
import com.mowtiie.flashback.ui.prompt.PromptBuilderUiState
import com.mowtiie.flashback.ui.study.StudyActions
import com.mowtiie.flashback.ui.study.StudyCardUi
import com.mowtiie.flashback.ui.study.StudyCounts
import com.mowtiie.flashback.ui.study.StudyScreen
import com.mowtiie.flashback.ui.study.StudyUiState
import com.mowtiie.flashback.ui.tags.TagsScreen
import com.mowtiie.flashback.ui.tags.TagsUiState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class LayoutPolishTest {

    @get:Rule
    val rule = createComposeRule()

    private val isHeading = SemanticsMatcher("is a heading") { it.config.getOrNull(SemanticsProperties.Heading) != null }

    private fun styleOf(node: SemanticsNodeInteraction): TextStyle {
        val results = mutableListOf<TextLayoutResult>()
        node.fetchSemanticsNode().config[SemanticsActions.GetTextLayoutResult].action?.invoke(results)
        return results.first().layoutInput.style
    }

    private fun androidx.compose.ui.unit.DpRect.span() = right - left

    // 6. The editors group their study options under a title.
    @Test
    fun theCardEditorGroupsStudyOptionsUnderATitle() {
        rule.setContent {
            FlashbackTheme(dynamicColor = false) {
                NoteEditorScreen(
                    state = NoteEditorUiState(ready = true, form = NoteEditorForm()),
                    frontFocus = remember { FocusRequester() },
                    onBack = {}, onDelete = {}, onFrontChange = {}, onBackChange = {},
                    onReverseChange = {}, onAnswerModeChange = {}, onWrongOptionChange = { _, _ -> },
                    onSuspendedChange = {}, onSave = {}, onSaveAndAdd = {},
                )
            }
        }
        rule.onNode(hasText("Study options") and isHeading).assertExists()
    }

    @Test
    fun theDeckEditorGroupsStudyOptionsUnderATitle() {
        rule.setContent {
            FlashbackTheme(dynamicColor = false) {
                DeckEditorScreen(
                    state = DeckEditorUiState(ready = true),
                    onBack = {}, onNameChange = {}, onDescriptionChange = {}, onReverseChange = {},
                    onNewPerDayChange = {}, onReviewsPerDayChange = {}, onTagToggle = { _, _ -> },
                    onNewTag = {}, onSave = {},
                )
            }
        }
        rule.onNode(hasText("Study") and isHeading).assertExists()
    }

    @Test
    fun theDeckDescriptionSaysItIsOptional() {
        rule.setContent {
            FlashbackTheme(dynamicColor = false) {
                DeckEditorScreen(
                    state = DeckEditorUiState(ready = true),
                    onBack = {}, onNameChange = {}, onDescriptionChange = {}, onReverseChange = {},
                    onNewPerDayChange = {}, onReviewsPerDayChange = {}, onTagToggle = { _, _ -> },
                    onNewTag = {}, onSave = {},
                )
            }
        }
        rule.onNodeWithText("Optional").assertExists()
    }

    // 8. The deck page opens with a summary.
    @Test
    fun theDeckPageShowsADescriptionTagsAndCounts() {
        val state = DeckDetailUiState(
            loaded = true,
            deck = Deck(id = 1, name = "Spanish", description = "Core vocabulary"),
            notes = listOf(com.mowtiie.flashback.data.model.NotePreview(1, "hola", "hello", false)),
            tags = listOf(Tag(id = 1, name = "Languages", color = 0xFF3E63DD.toInt())),
            newCount = 3, learnCount = 1, dueCount = 2,
            studyableCount = 6,
        )
        rule.setContent {
            FlashbackTheme(dynamicColor = false) {
                DeckDetailScreen(state, {}, {}, {}, {}, {}, {}, {})
            }
        }
        rule.onNodeWithText("Core vocabulary").assertIsDisplayed()
        rule.onNodeWithText("Languages").assertIsDisplayed()
        rule.onNodeWithText("3 new · 1 practice · 2 review").assertIsDisplayed()
    }

    // 9. The study counters say what they count.
    @Test
    fun theStudyCountersAreLabelled() {
        val state = StudyUiState(
            loading = false,
            card = StudyCardUi(question = "q", answer = "a"),
            counts = StudyCounts(newCards = 4, learning = 1, due = 2),
        )
        rule.setContent { FlashbackTheme(dynamicColor = false) { StudyScreen(state, StudyActions()) } }
        rule.onNodeWithText("New").assertIsDisplayed()
        rule.onNodeWithText("Practice").assertIsDisplayed()
        rule.onNodeWithText("Review").assertIsDisplayed()
    }

    // 10. A tag's description reads before how many decks use it.
    @Test
    fun aTagDescriptionOutranksItsUsageCount() {
        val tag = Tag(id = 1, name = "Language", description = "Languages", color = 0)
        rule.setContent {
            FlashbackTheme(dynamicColor = false) {
                TagsScreen(TagsUiState(loaded = true, tags = listOf(TagWithCount(tag, 1))), {}, {})
            }
        }
        val description = styleOf(rule.onNodeWithText("Languages", useUnmergedTree = true)).fontSize.value
        val usage = styleOf(rule.onNodeWithText("Used by 1 deck", useUnmergedTree = true)).fontSize.value
        assertTrue("description $description should be larger than usage $usage", description > usage)
    }

    // 11. Copy is reachable without scrolling.
    @Test
    fun copyPromptIsVisibleOnASmallScreenWithoutScrolling() {
        rule.setContent {
            FlashbackTheme(dynamicColor = false) {
                Box(Modifier.requiredSize(width = 360.dp, height = 560.dp)) {
                    PromptBuilderScreen(PromptBuilderUiState(), PromptBuilderActions())
                }
            }
        }
        rule.onNodeWithText("Copy instructions").assertIsDisplayed()
    }

    // 13. The AI ways to start a deck are plain rows after a divider, with no title of their own.
    @Test
    fun theNewDeckSheetListsTheAiWaysWithoutATitle() {
        rule.setContent {
            FlashbackTheme(dynamicColor = false) {
                DeckListScreen(
                    state = previewDeckListState(),
                    onSelectTag = {}, onOpenDeck = {}, onStudyDeck = {}, onAddDeck = {},
                    onGenerate = {}, onPromptHelper = {}, shortWindow = false,
                )
            }
        }
        rule.onNode(androidx.compose.ui.test.hasContentDescription("New deck") and hasClickAction())
            .performClick()
        rule.onNodeWithText("With AI").assertDoesNotExist()
        rule.onNodeWithText("Generate with AI").assertExists()
        rule.onNodeWithText("AI website").assertExists()
    }


    // 14. One long tag cannot push the next filter chip off screen.
    @Test
    fun aLongFilterChipIsCapped() {
        val longName = "Cloud computing certification exam and a few more words"
        val state = DeckListUiState(
            loaded = true,
            items = previewDeckListState().items,
            tags = listOf(Tag(id = 1, name = longName), Tag(id = 2, name = "Language")),
        )
        rule.setContent {
            FlashbackTheme(dynamicColor = false) {
                DeckListScreen(
                    state = state,
                    onSelectTag = {}, onOpenDeck = {}, onStudyDeck = {}, onAddDeck = {},
                    onGenerate = {}, onPromptHelper = {}, shortWindow = false,
                )
            }
        }
        val chip = rule.onNode(hasText(longName, substring = true) and hasClickAction()).getBoundsInRoot().span()
        assertTrue("chip is $chip wide", chip <= 220.dp)
        assertEquals(true, rule.onNode(hasText("Language") and hasClickAction()).getBoundsInRoot().left < 360.dp)
    }
}
