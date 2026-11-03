package com.mowtiie.flashback.ui.prompt

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.mowtiie.flashback.generator.ChatPromptOptions
import com.mowtiie.flashback.generator.PromptCoverage
import com.mowtiie.flashback.theme.FlashbackTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class PromptBuilderCardCountTest {

    @get:Rule
    val rule = createComposeRule()

    private fun show(coverage: PromptCoverage) {
        val options = ChatPromptOptions(coverage)
        rule.setContent {
            FlashbackTheme(dynamicColor = false) {
                PromptBuilderScreen(PromptBuilderUiState(options), PromptBuilderActions())
            }
        }
    }

    @Test
    fun everyFactHasNoCardCount() {
        show(PromptCoverage.Everything)
        rule.onNode(hasText("How many cards?")).assertDoesNotExist()
    }

    @Test
    fun theInstructionsOpenInADialog() {
        show(PromptCoverage.Everything)
        rule.onNode(hasText("Turn it into flashcards for the Flashback app", substring = true)).assertDoesNotExist()

        rule.onNodeWithText("See the instructions").performScrollTo().performClick()
        rule.onNodeWithText("The instructions").assertIsDisplayed()
        rule.onNode(hasText("Turn it into flashcards for the Flashback app", substring = true)).assertIsDisplayed()

        rule.onNodeWithText("Close").performClick()
        rule.onNode(hasText("The instructions")).assertDoesNotExist()
    }

    @Test
    fun optionalFieldsSayOptionalInTheirHelperText() {
        show(PromptCoverage.Everything)
        rule.onNodeWithText("Language").performScrollTo().assertIsDisplayed()
        rule.onNode(hasText("Optional. Leave empty", substring = true)).performScrollTo().assertIsDisplayed()
        rule.onNode(hasText("(optional)", substring = true)).assertDoesNotExist()
    }

    @Test
    fun importOffersAFileOrPastedTextWithoutLeavingThePage() {
        show(PromptCoverage.Everything)
        rule.onNodeWithText("Import the cards").performScrollTo().performClick()
        rule.onNodeWithText("From a file").assertExists()
        rule.onNodeWithText("Paste text").assertExists()
    }

    @Test
    fun bigIdeasAsksHowManyCards() {
        show(PromptCoverage.KeyPoints)
        rule.onNodeWithText("How many cards?").performScrollTo().assertIsDisplayed()
    }
}
