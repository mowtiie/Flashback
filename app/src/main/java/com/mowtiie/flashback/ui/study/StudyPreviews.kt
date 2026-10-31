package com.mowtiie.flashback.ui.study

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.mowtiie.flashback.scheduler.Rating
import com.mowtiie.flashback.theme.FlashbackTheme
import com.mowtiie.flashback.ui.components.LightDarkPreviews
import com.mowtiie.flashback.util.Interval

private val previewIntervals = mapOf(
    Rating.AGAIN to Interval.Minutes(1),
    Rating.HARD to Interval.Minutes(10),
    Rating.GOOD to Interval.Days(1),
    Rating.EASY to Interval.Days(4),
)

private val previewCard = StudyCardUi(question = "el bosque", answer = "the forest")

private val previewState = StudyUiState(
    loading = false,
    deckName = "Spanish vocabulary",
    card = previewCard,
    counts = StudyCounts(newCards = 12, learning = 3, due = 47),
)

private val previewChoices = ChoiceBuilderPreview.choices

/** Choices has an internal constructor, so previews go through the builder. */
private object ChoiceBuilderPreview {
    val choices: ChoiceBuilder.Choices = ChoiceBuilder.build(
        correct = "the forest",
        candidates = listOf("the house", "the dog", "the river"),
        random = java.util.Random(3),
    )!!
}

@Composable
private fun Preview(state: StudyUiState) {
    FlashbackTheme(dynamicColor = false) {
        StudyScreen(state = state, actions = StudyActions())
    }
}

@LightDarkPreviews
@Composable
private fun StudyQuestionPreview() = Preview(previewState)

@LightDarkPreviews
@Composable
private fun StudyRevealedPreview() = Preview(
    previewState.copy(revealed = true, previews = previewIntervals, canUndo = true),
)

@LightDarkPreviews
@Composable
private fun StudyTypeCloseFeedbackPreview() = Preview(
    previewState.copy(
        answerMode = AnswerMode.Type,
        revealed = true,
        typed = "the forst",
        checkResult = AnswerChecker.Result.CLOSE,
        previews = previewIntervals,
    ),
)

@LightDarkPreviews
@Composable
private fun StudyTypeWrongFeedbackPreview() = Preview(
    previewState.copy(
        answerMode = AnswerMode.Type,
        revealed = true,
        typed = "the house",
        checkResult = AnswerChecker.Result.WRONG,
        previews = previewIntervals,
    ),
)

@LightDarkPreviews
@Composable
private fun StudyChoicePreview() = Preview(
    previewState.copy(answerMode = AnswerMode.Choice, choices = previewChoices),
)

@LightDarkPreviews
@Composable
private fun StudyChoiceWrongPickedPreview() {
    val wrong = previewChoices.options.indices.first { it != previewChoices.correctIndex }
    Preview(
        previewState.copy(
            answerMode = AnswerMode.Choice,
            choices = previewChoices,
            selectedChoice = wrong,
            revealed = true,
            checkResult = AnswerChecker.Result.WRONG,
            previews = previewIntervals,
        ),
    )
}

@LightDarkPreviews
@Composable
private fun StudyLongTextPreview() = Preview(
    previewState.copy(
        card = StudyCardUi(
            question = "Explain how the sodium-potassium pump maintains the resting membrane potential. ".repeat(4),
            answer = "It moves three sodium ions out for every two potassium ions in. ".repeat(4),
        ),
        revealed = true,
        previews = previewIntervals,
    ),
)

@Preview(name = "Landscape two-pane", widthDp = 840, heightDp = 400, showBackground = true)
@Composable
private fun StudyTwoPanePreview() = Preview(
    previewState.copy(revealed = true, previews = previewIntervals, checkResult = AnswerChecker.Result.CORRECT),
)

@LightDarkPreviews
@Composable
private fun StudyFinishedPreview() = Preview(
    StudyUiState(loading = false, deckName = "Spanish vocabulary", ending = StudyEnding.Finished(23)),
)

@LightDarkPreviews
@Composable
private fun StudyWaitingPreview() = Preview(
    StudyUiState(loading = false, deckName = "Spanish vocabulary", ending = StudyEnding.Waiting(7)),
)

@LightDarkPreviews
@Composable
private fun StudyNothingDuePreview() = Preview(
    StudyUiState(loading = false, deckName = "Spanish vocabulary", ending = StudyEnding.NothingDue),
)
