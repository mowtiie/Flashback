package com.mowtiie.flashback.ui.study

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.TextAutoSize
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mowtiie.flashback.R
import com.mowtiie.flashback.scheduler.Rating
import com.mowtiie.flashback.util.Interval
import java.util.Locale

/** The answer controls for the current card: the mode's input, then the four ratings. */
@Composable
internal fun StudyControls(
    state: StudyUiState,
    actions: StudyActions,
    onShowAnswer: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val typeMode = state.answerMode == AnswerMode.Type

    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        if (state.showChoices) {
            ChoiceButtons(state, actions.onChoose)
        }
        if (typeMode) {
            TypeAnswerField(state, actions)
            if (!state.revealed) {
                Button(onClick = actions.onCheck, modifier = Modifier.fillMaxWidth()) {
                    Text(stringResource(R.string.study_check_answer))
                }
            }
        }
        if (!state.revealed && !typeMode && !state.showChoices) {
            Button(onClick = onShowAnswer, modifier = Modifier.fillMaxWidth()) {
                Text(stringResource(R.string.study_show_answer))
            }
        }
        if (state.revealed) {
            RatingRow(state, actions.onRate, Modifier.padding(top = 8.dp))
        }
    }
}

@Composable
private fun ChoiceButtons(state: StudyUiState, onChoose: (Int) -> Unit) {
    val choices = state.choices ?: return
    val selected = state.selectedChoice
    val colors = MaterialTheme.colorScheme

    choices.options.forEachIndexed { index, option ->
        val correct = selected != null && index == choices.correctIndex
        val wrong = selected != null && index == selected && !correct
        val description = when {
            correct -> stringResource(R.string.choice_correct_desc, option)
            wrong -> stringResource(R.string.choice_wrong_desc, option)
            else -> null
        }
        OutlinedButton(
            onClick = { onChoose(index) },
            colors = when {
                correct -> ButtonDefaults.outlinedButtonColors(
                    containerColor = colors.primaryContainer,
                    contentColor = colors.onPrimaryContainer,
                )
                wrong -> ButtonDefaults.outlinedButtonColors(
                    containerColor = colors.errorContainer,
                    contentColor = colors.onErrorContainer,
                )
                else -> ButtonDefaults.outlinedButtonColors()
            },
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 48.dp)
                .semantics { description?.let { contentDescription = it } },
        ) {
            Text(text = option, maxLines = 3, overflow = TextOverflow.Ellipsis)
        }
    }
}

@Composable
private fun TypeAnswerField(state: StudyUiState, actions: StudyActions) {
    val keyboard = LocalSoftwareKeyboardController.current
    val focus = LocalFocusManager.current
    val submit = {
        actions.onCheck()
        keyboard?.hide()
        focus.clearFocus()
    }
    val result = state.checkResult

    OutlinedTextField(
        value = state.typed,
        onValueChange = actions.onTypedChange,
        label = { Text(stringResource(R.string.study_type_hint)) },
        enabled = result == null,
        isError = result == AnswerChecker.Result.WRONG,
        supportingText = result?.let {
            {
                Text(
                    stringResource(
                        when (it) {
                            AnswerChecker.Result.CORRECT -> R.string.study_result_correct
                            AnswerChecker.Result.CLOSE -> R.string.study_result_close
                            AnswerChecker.Result.WRONG -> R.string.study_result_wrong
                        },
                    ),
                )
            }
        },
        singleLine = true,
        keyboardOptions = KeyboardOptions(
            autoCorrectEnabled = false,
            keyboardType = KeyboardType.Text,
            imeAction = ImeAction.Done,
        ),
        keyboardActions = KeyboardActions(onDone = { submit() }),
        modifier = Modifier
            .fillMaxWidth()
            // Hardware Enter submits as well as the IME Done action.
            .onPreviewKeyEvent { event ->
                val enter = event.key == Key.Enter || event.key == Key.NumPadEnter
                if (enter && event.type == KeyEventType.KeyUp) {
                    submit()
                    true
                } else {
                    enter
                }
            },
    )
}

@Composable
private fun RatingRow(state: StudyUiState, onRate: (Rating) -> Unit, modifier: Modifier = Modifier) {
    Row(modifier = modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Rating.entries.forEach { rating ->
            RatingColumn(
                rating = rating,
                interval = state.previews[rating],
                suggested = rating == state.suggestedRating,
                onClick = { onRate(rating) },
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@Composable
private fun RatingColumn(
    rating: Rating,
    interval: Interval?,
    suggested: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val label = stringResource(rating.labelRes())
    val intervalText = interval?.let { it.text() }.orEmpty()
    val description = stringResource(R.string.rating_with_interval, label, intervalText).let {
        if (suggested) stringResource(R.string.rating_suggested_desc, it) else it
    }
    // The suggestion is drawn with onSurface so it reads in every theme and contrast level.
    val border = if (suggested) BorderStroke(3.dp, MaterialTheme.colorScheme.onSurface) else null
    val shape = RoundedCornerShape(12.dp)
    val padding = PaddingValues(horizontal = 4.dp)
    val buttonModifier = Modifier
        .fillMaxWidth()
        .semantics { contentDescription = description }
    val content: @Composable () -> Unit = {
        Text(
            text = label,
            maxLines = 1,
            style = MaterialTheme.typography.labelLarge,
            autoSize = TextAutoSize.StepBased(minFontSize = 10.sp, maxFontSize = 14.sp),
        )
    }

    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = modifier) {
        when (rating) {
            Rating.AGAIN -> FilledTonalButton(
                onClick = onClick,
                shape = shape,
                border = border,
                contentPadding = padding,
                colors = ButtonDefaults.filledTonalButtonColors(
                    containerColor = MaterialTheme.colorScheme.errorContainer,
                    contentColor = MaterialTheme.colorScheme.onErrorContainer,
                ),
                modifier = buttonModifier,
            ) { content() }

            Rating.GOOD -> Button(
                onClick = onClick,
                shape = shape,
                border = border,
                contentPadding = padding,
                modifier = buttonModifier,
            ) { content() }

            Rating.HARD, Rating.EASY -> FilledTonalButton(
                onClick = onClick,
                shape = shape,
                border = border,
                contentPadding = padding,
                modifier = buttonModifier,
            ) { content() }
        }
        // Folded into the button's description, so it isn't read twice.
        Text(
            text = intervalText,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier
                .padding(top = 4.dp)
                .clearAndSetSemantics {},
        )
    }
}

private fun Rating.labelRes(): Int = when (this) {
    Rating.AGAIN -> R.string.rating_again
    Rating.HARD -> R.string.rating_hard
    Rating.GOOD -> R.string.rating_good
    Rating.EASY -> R.string.rating_easy
}

@Composable
internal fun Interval.text(): String = when (this) {
    is Interval.Minutes -> stringResource(R.string.interval_minutes, value)
    is Interval.Hours -> stringResource(R.string.interval_hours, value)
    is Interval.Days -> stringResource(R.string.interval_days, value)
    is Interval.Months -> stringResource(R.string.interval_months, oneDecimal(value))
    is Interval.Years -> stringResource(R.string.interval_years, oneDecimal(value))
}

/** One decimal, so "1.5mo" rather than "1.4666mo". */
private fun oneDecimal(value: Double): String = String.format(Locale.getDefault(), "%.1f", value)
