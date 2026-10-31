package com.mowtiie.flashback.ui

import androidx.compose.material3.Text
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.getBoundsInRoot
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.mowtiie.flashback.R
import com.mowtiie.flashback.theme.FlashbackTheme
import com.mowtiie.flashback.ui.components.asHeading
import com.mowtiie.flashback.ui.settings.ChoiceRow
import com.mowtiie.flashback.ui.settings.SectionHeader
import com.mowtiie.flashback.ui.study.AnswerChecker
import com.mowtiie.flashback.ui.study.AnswerMode
import com.mowtiie.flashback.ui.study.StudyActions
import com.mowtiie.flashback.ui.study.StudyCardUi
import com.mowtiie.flashback.ui.study.StudyScreen
import com.mowtiie.flashback.ui.study.StudyUiState
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class AuditFixesTest {

    @get:Rule
    val rule = createComposeRule()

    private val isHeading = SemanticsMatcher("is a heading") {
        it.config.getOrNull(SemanticsProperties.Heading) != null
    }
    private val isRadio = SemanticsMatcher("is a radio button") {
        it.config.getOrNull(SemanticsProperties.Role) == androidx.compose.ui.semantics.Role.RadioButton
    }
    private val isLiveRegion = SemanticsMatcher("is a live region") {
        it.config.getOrNull(SemanticsProperties.LiveRegion) != null
    }

    private fun typeResult(result: AnswerChecker.Result) = StudyUiState(
        loading = false,
        card = StudyCardUi(question = "el bosque", answer = "the forest"),
        answerMode = AnswerMode.Type,
        revealed = true,
        typed = "the forst",
        checkResult = result,
    )

    @Test
    fun theTypedAnswerStaysInNormalColoursAfterChecking() {
        rule.setContent { FlashbackTheme(dynamicColor = false) { StudyScreen(typeResult(AnswerChecker.Result.WRONG), StudyActions()) } }
        // A disabled field is drawn faint; a read-only one is still enabled.
        rule.onNodeWithText("the forst").assertIsEnabled()
    }

    @Test
    fun theTypeResultIsAnnouncedWhenItAppears() {
        rule.setContent { FlashbackTheme(dynamicColor = false) { StudyScreen(typeResult(AnswerChecker.Result.CLOSE), StudyActions()) } }
        rule.onAllNodes(isLiveRegion).assertCountEquals(1)
    }

    @Test
    fun sectionTitlesAreHeadings() {
        rule.setContent { FlashbackTheme(dynamicColor = false) { SectionHeader(R.string.settings_reminders_header) } }
        rule.onAllNodes(isHeading).assertCountEquals(1)
    }

    @Test
    fun asHeadingMarksAnyText() {
        rule.setContent { Text("Title", Modifier.asHeading()) }
        rule.onNodeWithText("Title").assert(isHeading)
    }

    private fun choices(fontScale: Float) {
        rule.setContent {
            val base = LocalDensity.current
            CompositionLocalProvider(LocalDensity provides Density(base.density, fontScale)) {
                FlashbackTheme(dynamicColor = false) {
                    ChoiceRow(
                        title = "Mode",
                        options = listOf("Default", "Choice", "Type"),
                        selected = "Default",
                        label = { it },
                        onSelect = {},
                    )
                }
            }
        }
    }

    private fun androidx.compose.ui.unit.DpRect.span() = right - left

    private fun firstOptionWidth() = rule.onAllNodes(isRadio)[0].getBoundsInRoot().span()

    @Test
    fun choiceRowsStayAsSegmentsAtNormalFontSize() {
        choices(1.0f)
        rule.onAllNodes(isRadio).assertCountEquals(3)
        // Segments share the row, so each is well under its full width.
        assertTrue(firstOptionWidth() < rule.onRoot().getBoundsInRoot().span())
    }

    @Test
    fun choiceRowsBecomeAListAtLargeFontSize() {
        choices(2.0f)
        rule.onAllNodes(isRadio).assertCountEquals(3)
        rule.onNodeWithText("Choice").assertExists()
        // A list option spans the row; a segment would be about a third of it.
        assertTrue(firstOptionWidth() > 300.dp)
    }
}
