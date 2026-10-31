package com.mowtiie.flashback.ui.generator

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.unit.Density
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.mowtiie.flashback.data.io.DeckArchive
import com.mowtiie.flashback.generator.CardTypes
import com.mowtiie.flashback.generator.Coverage
import com.mowtiie.flashback.theme.FlashbackTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class GeneratorCoverageScreenTest {

    @get:Rule
    val rule = createComposeRule()

    private val liveRegion = SemanticsMatcher("is a live region") {
        it.config.getOrNull(SemanticsProperties.LiveRegion) != null
    }

    private fun show(state: GeneratorUiState, actions: GeneratorActions = GeneratorActions(), fontScale: Float = 1f) {
        rule.setContent {
            val base = LocalDensity.current
            CompositionLocalProvider(LocalDensity provides Density(base.density, fontScale)) {
                FlashbackTheme(dynamicColor = false) { GeneratorScreen(state, actions) }
            }
        }
    }

    private val input = GeneratorUiState(
        hasApiKey = true,
        cardTypes = CardTypes.Setting,
        source = "x".repeat(20_000),
    )

    @Test
    fun everythingShowsTheSizeAndAnEstimateAndHidesTheCardCount() {
        show(input)
        rule.onNodeWithText("Everything").assertIsDisplayed()
        rule.onNode(hasText("About 3 requests", substring = true)).assertExists()
        rule.onNode(hasText("Most cards to make")).assertDoesNotExist()
    }

    @Test
    fun keyPointsShowsTheCardCountAndTheOldCounter() {
        show(input.copy(coverage = Coverage.KeyPoints))
        rule.onNodeWithText("Most cards to make").assertExists()
        rule.onNode(hasText("20,000 / 30,000 characters")).assertExists()
    }

    @Test
    fun theProgressNamesTheSectionAndCancelWorks() {
        var cancelled = 0
        show(
            input.copy(stage = GeneratorStage.Generating, sectionTotal = 6, sectionsFinished = 1),
            GeneratorActions(onCancel = { cancelled++ }),
        )
        rule.onNodeWithText("Section 2 of 6").assertIsDisplayed()
        rule.onAllNodes(liveRegion).assertCountEquals(1)
        rule.onNodeWithText("Cancel").performClick()
        assertEquals(1, cancelled)
    }

    @Test
    fun cardsSoFarAreShownWhileGenerating() {
        show(
            input.copy(
                stage = GeneratorStage.Generating,
                sectionTotal = 3,
                sectionsFinished = 1,
                cards = listOf(DeckArchive.Note("Q", "A"), DeckArchive.Note("Q2", "A2")),
            ),
        )
        rule.onNodeWithText("2 cards so far").assertIsDisplayed()
    }

    private val partial = input.copy(
        stage = GeneratorStage.Review,
        deckName = "Biology",
        cards = listOf(DeckArchive.Note("Q", "A")),
        sectionCounts = listOf(1, 0, 0),
        failedSections = listOf(FailedSection(2, "It broke."), FailedSection(3, "Not sent.")),
    )

    @Test
    fun failedSectionsAreListedWithOneRetry() {
        var retried = 0
        show(partial, GeneratorActions(onRetry = { retried++ }))
        rule.onNodeWithText("2 sections made no cards").assertIsDisplayed()
        rule.onNodeWithText("Section 2: It broke.").assertIsDisplayed()
        rule.onNodeWithText("Retry 2 sections").performClick()
        assertEquals(1, retried)
    }

    @Test
    fun theFailedRowsAreAnnouncedAndTheCountsPerSectionAreShown() {
        show(partial)
        rule.onAllNodes(liveRegion).assertCountEquals(1)
        rule.onNode(hasText("From 3 sections", substring = true)).assertExists()
    }

    @Test
    fun retryIsReachableAtLargeFontSize() {
        var retried = 0
        show(partial, GeneratorActions(onRetry = { retried++ }), fontScale = 2f)
        rule.onNodeWithText("Retry 2 sections").performScrollTo().performClick()
        assertEquals(1, retried)
    }

    @Test
    fun theConfirmationNamesTheCountAndCanBeAnsweredEitherWay() {
        var confirmed = 0
        var dismissed = 0
        show(
            input.copy(confirming = true, sectionTotal = 5),
            GeneratorActions(onConfirm = { confirmed++ }, onDismissConfirm = { dismissed++ }),
        )
        rule.onNodeWithText("Send many requests?").assertIsDisplayed()
        rule.onNode(hasText("split into 5 parts", substring = true)).assertExists()
        rule.onNodeWithText("Send").performClick()
        assertEquals(1, confirmed)
    }
}
