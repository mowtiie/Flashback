package com.mowtiie.flashback.ui.study

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.mowtiie.flashback.theme.FlashbackTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** The study screen in a student's words, with a sense of how much is left. */
@RunWith(AndroidJUnit4::class)
class StudyStudentWordsTest {

    @get:Rule
    val rule = createComposeRule()

    private fun state(revealed: Boolean) = StudyUiState(
        loading = false,
        deckName = "Spanish",
        card = StudyCardUi(question = "el bosque", answer = "the forest"),
        counts = StudyCounts(newCards = 3, learning = 2, due = 7),
        revealed = revealed,
    )

    private fun show(state: StudyUiState) {
        rule.setContent { FlashbackTheme(dynamicColor = false) { StudyScreen(state, StudyActions()) } }
    }

    @Test
    fun theTopShowsHowManyCardsAreLeftInPlainWords() {
        show(state(revealed = false))
        rule.onNodeWithText("12 left").assertIsDisplayed()
        rule.onNodeWithText("New").assertIsDisplayed()
        rule.onNodeWithText("Practice").assertIsDisplayed()
        rule.onNodeWithText("Review").assertIsDisplayed()
    }

    @Test
    fun noRatingHintBeforeTheAnswerIsShown() {
        show(state(revealed = false))
        rule.onNodeWithText("How well did you remember?").assertDoesNotExist()
    }

    @Test
    fun theHintShowsWithTheRatings() {
        show(state(revealed = true))
        rule.onNodeWithText("How well did you remember?").assertIsDisplayed()
    }
}
