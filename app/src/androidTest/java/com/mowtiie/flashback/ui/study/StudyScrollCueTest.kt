package com.mowtiie.flashback.ui.study

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipeUp
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.mowtiie.flashback.theme.FlashbackTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class StudyScrollCueTest {

    @get:Rule
    val rule = createComposeRule()

    private fun show(answer: String) {
        rule.setContent {
            FlashbackTheme(dynamicColor = false) {
                Box(Modifier.requiredSize(width = 400.dp, height = 220.dp)) {
                    StudyCardFace(
                        card = StudyCardUi(question = "Explain", answer = answer),
                        revealed = true,
                        rotationY = 0f,
                        onReveal = null,
                    )
                }
            }
        }
    }

    @Test
    fun aLongAnswerShowsAFadeWhileMoreIsBelow() {
        show("a long answer that keeps going. ".repeat(40))
        rule.onNodeWithTag(SCROLL_CUE_TAG).assertExists()
    }

    @Test
    fun theFadeGoesAwayAtTheEnd() {
        show("a long answer that keeps going. ".repeat(40))
        repeat(6) { rule.onNodeWithTag(STUDY_CARD_TAG).performTouchInput { swipeUp() } }
        rule.waitForIdle()
        rule.onNodeWithTag(SCROLL_CUE_TAG).assertDoesNotExist()
    }

    @Test
    fun aShortAnswerShowsNoFade() {
        show("short")
        rule.onNodeWithTag(SCROLL_CUE_TAG).assertDoesNotExist()
    }
}
