package com.mowtiie.flashback.ui.settings

import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.hasAnyAncestor
import androidx.compose.ui.test.hasAnyDescendant
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.mowtiie.flashback.theme.FlashbackTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class SettingsScreenTest {

    @get:Rule
    val rule = createComposeRule()

    private var filesPicked = 0
    private var exports = 0

    private fun show(
        state: SettingsUiState = SettingsUiState(),
        actions: SettingsActions = SettingsActions(),
    ) {
        val transfer = DataTransferActions(onExport = { exports++ }, onImportFile = { filesPicked++ })
        rule.setContent {
            FlashbackTheme(dynamicColor = false) {
                SettingsScreen(state, DataTransferUiState(), actions, transfer)
            }
        }
    }

    private fun rowWithIcon(title: String) =
        hasTestTag(ROW_ICON_TAG) and hasAnyAncestor(hasClickAction() and hasAnyDescendant(hasText(title)))

    @Test
    fun exportAndImportEachShowAnIcon() {
        show()
        rule.onNodeWithText("Export decks").performScrollTo()

        rule.onNode(rowWithIcon("Export decks"), useUnmergedTree = true).assertExists()
        rule.onNode(rowWithIcon("Import decks"), useUnmergedTree = true).assertExists()
    }

    @Test
    fun importOpensABottomSheetWithBothWaysToImport() {
        show()
        rule.onNodeWithText("Import decks").performScrollTo().performClick()

        rule.onNodeWithTag(IMPORT_SHEET_TAG).assertExists()
        rule.onNode(hasText("From a file") and hasAnyAncestor(hasTestTag(IMPORT_SHEET_TAG))).assertExists()
        rule.onNode(hasText("Paste text") and hasAnyAncestor(hasTestTag(IMPORT_SHEET_TAG))).assertExists()
    }

    @Test
    fun choosingAFileInTheSheetStartsTheFilePicker() {
        show()
        rule.onNodeWithText("Import decks").performScrollTo().performClick()
        rule.onNode(hasText("From a file") and hasAnyAncestor(hasTestTag(IMPORT_SHEET_TAG))).performClick()
        rule.waitForIdle()

        assertEquals(1, filesPicked)
    }

    @Test
    fun exportStillStartsAnExport() {
        show()
        rule.onNodeWithText("Export decks").performScrollTo().performClick()
        assertEquals(1, exports)
    }

    @Test
    fun theKeyRowShowsItsCurrentValueOnTheRight() {
        show(SettingsUiState(generatorKeyHint = null))
        rule.onNodeWithText("Not set").performScrollTo().assertExists()
    }

    @Test
    fun aSavedKeyShowsItsLastFourCharacters() {
        show(SettingsUiState(generatorKeyHint = "…4321"))
        rule.onNodeWithText("…4321").performScrollTo().assertExists()
    }

    @Test
    fun theKeyRowOpensTheKeyDialog() {
        show()
        rule.onNode(hasText("Anthropic API key") and hasClickAction()).performScrollTo().performClick()
        rule.onNodeWithText("Save").assertExists()
    }

    @Test
    fun theReminderTimeRowOnlyWorksWhileTheReminderIsOn() {
        show(SettingsUiState(reminderEnabled = false))
        rule.onNode(hasText("Reminder time") and hasClickAction()).performScrollTo().assertIsNotEnabled()
    }

    @Test
    fun theReminderTimeRowWorksWhenTheReminderIsOn() {
        show(SettingsUiState(reminderEnabled = true))
        rule.onNode(hasText("Reminder time") and hasClickAction()).performScrollTo().assertIsEnabled()
    }

    @Test
    fun thePrivacyPolicyOpensInADialog() {
        val opened = mutableListOf<String>()
        show(actions = SettingsActions(onOpenLink = { opened += it }))
        rule.onNodeWithText("Privacy policy").performScrollTo().performClick()
        rule.onNode(hasText("Flashback privacy policy")).assertExists()
        rule.onNode(hasText("What stays on your device")).assertExists()
        rule.onNode(hasText("2022")).assertDoesNotExist()
        assertEquals(emptyList<String>(), opened)
        rule.onNodeWithText("Close").performClick()
        rule.onNode(hasText("What stays on your device")).assertDoesNotExist()
    }

    @Test
    fun theAboutLinksOpenTheirPages() {
        val opened = mutableListOf<String>()
        show(actions = SettingsActions(onOpenLink = { opened += it }))
        rule.onNodeWithText("Source code").performScrollTo().performClick()
        rule.onNodeWithText("Support").performScrollTo().performClick()
        rule.onNodeWithText("Author").performScrollTo().performClick()
        assertEquals(listOf(AppLinks.SOURCE_CODE, AppLinks.SUPPORT, AppLinks.AUTHOR), opened)
    }

    @Test
    fun theLicenseOpensInADialogTitledLicense() {
        show()
        rule.onNodeWithText("License").performScrollTo().performClick()
        rule.onNode(hasText("GNU GENERAL PUBLIC LICENSE", substring = true)).assertExists()
        rule.onNodeWithText("Close").performClick()
        rule.onNode(hasText("GNU GENERAL PUBLIC LICENSE", substring = true)).assertDoesNotExist()
    }

    @Test
    fun theVersionRowShowsTheVersionAndOpensNothing() {
        val opened = mutableListOf<String>()
        show(actions = SettingsActions(onOpenLink = { opened += it }))
        rule.onNodeWithText("Version").performScrollTo().performClick()
        assertEquals(emptyList<String>(), opened)
    }
}
