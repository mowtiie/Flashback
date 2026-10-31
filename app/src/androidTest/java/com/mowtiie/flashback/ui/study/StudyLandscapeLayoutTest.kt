package com.mowtiie.flashback.ui.study

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.getBoundsInRoot
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.mowtiie.flashback.scheduler.Rating
import com.mowtiie.flashback.theme.FlashbackTheme
import com.mowtiie.flashback.util.Interval
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.util.Random

/** A landscape phone: wide enough for two panes, and about 387dp tall once the status bar is taken off. */
@RunWith(AndroidJUnit4::class)
class StudyLandscapeLayoutTest {

    @get:Rule
    val rule = createComposeRule()

    private val windowHeight = 387.dp

    private val intervals = mapOf(
        Rating.AGAIN to Interval.Minutes(1),
        Rating.HARD to Interval.Minutes(10),
        Rating.GOOD to Interval.Days(1),
        Rating.EASY to Interval.Days(4),
    )

    private fun answeredChoiceState(): StudyUiState {
        val choices = ChoiceBuilder.build("the forest", listOf("the house", "the dog", "the river"), Random(3))!!
        return StudyUiState(
            loading = false,
            deckName = "Spanish",
            card = StudyCardUi(question = "el bosque", answer = "the forest"),
            answerMode = AnswerMode.Choice,
            choices = choices,
            selectedChoice = choices.correctIndex,
            revealed = true,
            checkResult = AnswerChecker.Result.CORRECT,
            previews = intervals,
            counts = StudyCounts(newCards = 3, learning = 1, due = 0),
        )
    }

    private fun show(state: StudyUiState) {
        rule.setContent {
            FlashbackTheme(dynamicColor = false) {
                Box(Modifier.requiredSize(width = 891.dp, height = windowHeight)) {
                    StudyScreen(state = state, actions = StudyActions())
                }
            }
        }
        rule.waitForIdle()
    }

    /** The interval label sits under each rating button and has no semantics of its own to measure. */
    private val intervalLabelHeight = 28.dp

    @Test
    fun theRatingButtonsAndTheirIntervalsFitOnScreenAfterAnAnswer() {
        show(answeredChoiceState())

        val easy = rule.onNode(hasContentDescription("Easy", substring = true)).getBoundsInRoot()

        assertTrue(
            "Easy ends at ${easy.bottom}; its interval needs room down to ${easy.bottom + intervalLabelHeight}",
            easy.bottom + intervalLabelHeight <= windowHeight,
        )
    }

    @Test
    fun aNewCardStartsWithTheChoicesInView() {
        val answered = answeredChoiceState()
        show(answered.copy(revealed = false, selectedChoice = null, checkResult = null, previews = emptyMap()))

        val first = rule.onNode(hasContentDescription("the house", substring = true).or(hasText("the house"))).getBoundsInRoot()

        assertTrue("The first option starts at ${first.top}", first.top >= 0.dp)
    }
}
