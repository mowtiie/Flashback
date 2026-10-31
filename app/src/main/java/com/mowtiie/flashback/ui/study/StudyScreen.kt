package com.mowtiie.flashback.ui.study

import android.content.Context
import android.provider.Settings
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutLinearInEasing
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.mowtiie.flashback.R
import com.mowtiie.flashback.scheduler.Rating
import com.mowtiie.flashback.theme.countColors
import com.mowtiie.flashback.ui.components.EmptyState
import com.mowtiie.flashback.ui.components.showReplacing
import kotlinx.coroutines.launch

/** Width at which the card and the controls sit side by side. */
private val TWO_PANE_MIN_WIDTH = 600.dp
private const val FLIP_HALF_MS = 140

@Immutable
class StudyActions(
    val onBack: () -> Unit = {},
    val onUndo: () -> Unit = {},
    val onEditCard: () -> Unit = {},
    val onSuspend: () -> Unit = {},
    val onReveal: () -> Unit = {},
    val onTypedChange: (String) -> Unit = {},
    val onCheck: () -> Unit = {},
    val onChoose: (Int) -> Unit = {},
    val onRate: (Rating) -> Unit = {},
)

@Composable
fun StudyRoute(
    viewModel: StudyViewModel,
    onBack: () -> Unit,
    onEditCard: (noteId: Long) -> Unit,
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val resources = LocalResources.current

    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { viewModel.onResumed() }

    LaunchedEffect(viewModel) {
        viewModel.eventFlow.collect { event ->
            when (event) {
                // Launched, so a snackbar waiting to be dismissed never holds up the next event.
                StudyEvent.Undone -> launch {
                    snackbarHostState.showReplacing(resources.getString(R.string.study_undone))
                }

                StudyEvent.CardSuspended -> launch {
                    val result = snackbarHostState.showReplacing(
                        message = resources.getString(R.string.study_card_suspended),
                        actionLabel = resources.getString(R.string.study_undo),
                        duration = SnackbarDuration.Long,
                    )
                    if (result == SnackbarResult.ActionPerformed) viewModel.undoSuspend()
                }

                is StudyEvent.EditCard -> onEditCard(event.noteId)
            }
        }
    }

    val actions = remember(viewModel, onBack) {
        StudyActions(
            onBack = onBack,
            onUndo = viewModel::undo,
            onEditCard = viewModel::editCard,
            onSuspend = viewModel::suspendCurrent,
            onReveal = viewModel::reveal,
            onTypedChange = viewModel::onTypedChange,
            onCheck = viewModel::check,
            onChoose = viewModel::choose,
            onRate = viewModel::answer,
        )
    }

    StudyScreen(state = state, actions = actions, snackbarHostState = snackbarHostState)
}

@Composable
fun StudyScreen(
    state: StudyUiState,
    actions: StudyActions,
    snackbarHostState: SnackbarHostState = remember { SnackbarHostState() },
) {
    Scaffold(
        topBar = { StudyTopBar(state, actions) },
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { padding ->
        Column(Modifier.padding(padding).fillMaxSize()) {
            val ending = state.ending
            when {
                ending != null -> StudyEndingContent(ending, actions.onBack)
                state.card != null -> StudyContent(state, state.card, actions)
            }
        }
    }
}

@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
private fun StudyTopBar(state: StudyUiState, actions: StudyActions) {
    var menuOpen by rememberSaveable { mutableStateOf(false) }
    val hasCard = state.card != null

    Column {
        TopAppBar(
            title = { Text(state.deckName ?: stringResource(R.string.study_title)) },
            navigationIcon = {
                IconButton(onClick = actions.onBack) {
                    Icon(
                        Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = stringResource(R.string.navigate_up),
                    )
                }
            },
            actions = {
                if (state.canUndo) {
                    IconButton(onClick = actions.onUndo) {
                        Icon(
                            painterResource(R.drawable.ic_undo),
                            contentDescription = stringResource(R.string.study_undo),
                        )
                    }
                }
                if (hasCard) {
                    IconButton(onClick = { menuOpen = true }) {
                        Icon(
                            Icons.Default.MoreVert,
                            contentDescription = stringResource(R.string.more_options),
                        )
                    }
                    DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.study_edit_card)) },
                            onClick = {
                                menuOpen = false
                                actions.onEditCard()
                            },
                        )
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.study_suspend_card)) },
                            onClick = {
                                menuOpen = false
                                actions.onSuspend()
                            },
                        )
                    }
                }
            },
        )
        if (hasCard) {
            CountsRow(state.counts)
        }
    }
}

@Composable
private fun CountsRow(counts: StudyCounts) {
    val colors = MaterialTheme.countColors
    Row(
        horizontalArrangement = Arrangement.Center,
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 8.dp),
    ) {
        Count(counts.newCards, R.string.count_new, colors.new)
        Count(counts.learning, R.string.count_learning, colors.learning)
        Count(counts.due, R.string.count_due, colors.due)
    }
}

@Composable
private fun Count(value: Int, labelRes: Int, color: androidx.compose.ui.graphics.Color) {
    val description = stringResource(R.string.study_count_desc, stringResource(labelRes), value)
    Text(
        text = value.toString(),
        style = MaterialTheme.typography.labelLarge,
        color = color,
        modifier = Modifier
            .padding(horizontal = 8.dp)
            .semantics { contentDescription = description },
    )
}

@Composable
private fun StudyContent(state: StudyUiState, card: StudyCardUi, actions: StudyActions) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val flip = remember { Animatable(0f) }

    val canFlipToReveal = !state.revealed &&
        (state.answerMode == AnswerMode.Flip || (state.answerMode == AnswerMode.Choice && !state.showChoices))

    // Half a flip each way, so the answer text is never mirrored; plain reveal when animations are off.
    val showAnswer: () -> Unit = {
        if (!flip.isRunning && !state.revealed) {
            if (animationsEnabled(context)) {
                scope.launch {
                    flip.animateTo(90f, tween(FLIP_HALF_MS, easing = FastOutLinearInEasing))
                    actions.onReveal()
                    flip.snapTo(-90f)
                    flip.animateTo(0f, tween(FLIP_HALF_MS, easing = LinearOutSlowInEasing))
                }
            } else {
                actions.onReveal()
            }
        }
    }

    BoxWithConstraints(Modifier.fillMaxSize()) {
        if (maxWidth >= TWO_PANE_MIN_WIDTH) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier
                    .fillMaxSize()
                    .imePadding()
                    .padding(16.dp),
            ) {
                StudyCardFace(
                    card = card,
                    revealed = state.revealed,
                    rotationY = flip.value,
                    onReveal = if (canFlipToReveal) showAnswer else null,
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight(),
                )
                Column(
                    verticalArrangement = Arrangement.Center,
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .verticalScroll(rememberScrollState()),
                ) {
                    StudyControls(state, actions, showAnswer)
                }
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .imePadding()
                    .padding(16.dp),
            ) {
                StudyCardFace(
                    card = card,
                    revealed = state.revealed,
                    rotationY = flip.value,
                    onReveal = if (canFlipToReveal) showAnswer else null,
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                )
                StudyControls(
                    state = state,
                    actions = actions,
                    onShowAnswer = showAnswer,
                    modifier = Modifier.padding(top = 16.dp),
                )
            }
        }
    }
}

@Composable
private fun StudyEndingContent(ending: StudyEnding, onBack: () -> Unit) {
    val (title, body) = when (ending) {
        StudyEnding.NothingDue ->
            stringResource(R.string.study_nothing_due_title) to
                stringResource(R.string.study_nothing_due_body)

        is StudyEnding.Finished ->
            stringResource(R.string.study_finished_title) to
                pluralStringResource(R.plurals.study_finished_body, ending.answered, ending.answered)

        is StudyEnding.Waiting ->
            stringResource(R.string.study_waiting_title) to
                stringResource(
                    R.string.study_waiting_body,
                    stringResource(R.string.minutes_short, ending.minutes),
                )
    }
    EmptyState(
        title = title,
        body = body,
        actionLabel = stringResource(R.string.study_back_to_deck),
        onAction = onBack,
    )
}

/** Respects the system animation scale: with animations off the card just turns over. */
private fun animationsEnabled(context: Context): Boolean =
    Settings.Global.getFloat(context.contentResolver, Settings.Global.ANIMATOR_DURATION_SCALE, 1f) != 0f
