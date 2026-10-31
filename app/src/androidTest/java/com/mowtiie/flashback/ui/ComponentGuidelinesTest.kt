package com.mowtiie.flashback.ui

import androidx.compose.runtime.remember
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.getBoundsInRoot
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.mowtiie.flashback.R
import com.mowtiie.flashback.theme.FlashbackTheme
import com.mowtiie.flashback.ui.decks.DeckEditorErrors
import com.mowtiie.flashback.ui.decks.DeckEditorScreen
import com.mowtiie.flashback.ui.decks.DeckEditorUiState
import com.mowtiie.flashback.ui.generator.GeneratorActions
import com.mowtiie.flashback.ui.generator.GeneratorScreen
import com.mowtiie.flashback.ui.generator.GeneratorStage
import com.mowtiie.flashback.ui.generator.GeneratorUiState
import com.mowtiie.flashback.ui.notes.NoteEditorForm
import com.mowtiie.flashback.ui.notes.NoteEditorScreen
import com.mowtiie.flashback.ui.notes.NoteEditorUiState
import com.mowtiie.flashback.ui.settings.ReminderTimeDialog
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** Material 3 usage rules the app is held to: switches act at once, errors carry an icon, one progress style, short labels. */
@RunWith(AndroidJUnit4::class)
class ComponentGuidelinesTest {

    @get:Rule
    val rule = createComposeRule()

    private fun role(role: Role) = SemanticsMatcher("has role $role") { it.config.getOrNull(SemanticsProperties.Role) == role }

    private fun deckEditor() = rule.setContent {
        FlashbackTheme(dynamicColor = false) {
            DeckEditorScreen(
                state = DeckEditorUiState(ready = true, errors = DeckEditorErrors(nameRequired = true)),
                onBack = {}, onNameChange = {}, onDescriptionChange = {}, onReverseChange = {},
                onNewPerDayChange = {}, onReviewsPerDayChange = {}, onTagToggle = { _, _ -> },
                onNewTag = {}, onSave = {},
            )
        }
    }

    private fun cardEditor(editing: Boolean) = rule.setContent {
        FlashbackTheme(dynamicColor = false) {
            NoteEditorScreen(
                state = NoteEditorUiState(ready = true, isEditing = editing, form = NoteEditorForm()),
                frontFocus = remember { FocusRequester() },
                onBack = {}, onDelete = {}, onFrontChange = {}, onBackChange = {},
                onReverseChange = {}, onAnswerModeChange = {}, onWrongOptionChange = { _, _ -> },
                onSuspendedChange = {}, onSave = {}, onSaveAndAdd = {},
            )
        }
    }

    // 1. Options that are saved with the form are checkboxes; only settings that apply at once are switches.
    @Test
    fun theDeckEditorUsesACheckboxForTheReverseOption() {
        deckEditor()
        rule.onAllNodes(role(Role.Checkbox)).assertCountEquals(1)
        rule.onAllNodes(role(Role.Switch)).assertCountEquals(0)
    }

    @Test
    fun theNewCardEditorUsesACheckboxForTheReverseOption() {
        cardEditor(editing = false)
        rule.onAllNodes(role(Role.Checkbox)).assertCountEquals(1)
        rule.onAllNodes(role(Role.Switch)).assertCountEquals(0)
    }

    @Test
    fun theEditCardEditorAlsoUsesACheckboxForSuspend() {
        cardEditor(editing = true)
        rule.onAllNodes(role(Role.Checkbox)).assertCountEquals(2)
        rule.onAllNodes(role(Role.Switch)).assertCountEquals(0)
    }

    // 4. An error is not shown by colour alone.
    @Test
    fun aFieldInErrorShowsAnErrorIcon() {
        deckEditor()
        rule.onAllNodes(hasContentDescription("Error")).assertCountEquals(1)
    }

    // 6. One progress style for generating: a linear bar, whether or not the number of sections is known.
    @Test
    fun aSingleRequestShowsTheSameLinearBarAsASectionedRun() {
        rule.setContent {
            FlashbackTheme(dynamicColor = false) {
                GeneratorScreen(
                    GeneratorUiState(hasApiKey = true, stage = GeneratorStage.Generating, sectionTotal = 1),
                    GeneratorActions(),
                )
            }
        }
        val bar = rule.onAllNodes(SemanticsMatcher.keyIsDefined(SemanticsProperties.ProgressBarRangeInfo))[0].getBoundsInRoot()
        assertTrue("indicator is ${bar.right - bar.left} wide, so it is the circular one", bar.right - bar.left > 200.dp)
    }

    // 7. The time picker falls back to typing when the window is short.
    private val editable = SemanticsMatcher.keyIsDefined(SemanticsProperties.EditableText)

    @Test
    fun theTimePickerShowsTheDialOnATallWindow() {
        rule.setContent { FlashbackTheme(dynamicColor = false) { ReminderTimeDialog(20, 0, false, { _, _ -> }, {}, shortWindow = false) } }
        rule.onAllNodes(editable).assertCountEquals(0)
    }

    @Test
    fun theTimePickerShowsTypingFieldsOnAShortWindow() {
        rule.setContent { FlashbackTheme(dynamicColor = false) { ReminderTimeDialog(20, 0, false, { _, _ -> }, {}, shortWindow = true) } }
        rule.onAllNodes(editable).assertCountEquals(2)
    }

    // 8. Button labels are one to three words.
    @Test
    fun buttonLabelsAreAtMostThreeWords() {
        val resources = InstrumentationRegistry.getInstrumentation().targetContext.resources
        listOf(R.string.prompt_import, R.string.note_save_and_add, R.string.generator_pdf_choose).forEach {
            val label = resources.getString(it)
            assertTrue("\"$label\" has more than three words", label.trim().split(" ").size <= 3)
        }
    }
}
