package com.mowtiie.flashback.ui.generator

import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import android.Manifest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.layout.heightIn
import androidx.compose.material3.RadioButton
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import com.mowtiie.flashback.ui.study.AnswerMode
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Card
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.mowtiie.flashback.R
import com.mowtiie.flashback.notifications.canPostNotifications
import com.mowtiie.flashback.ui.components.errorTrailingIcon
import com.mowtiie.flashback.ui.components.asHeading
import com.mowtiie.flashback.data.io.DeckArchive
import com.mowtiie.flashback.generator.CardCount
import com.mowtiie.flashback.generator.CardTypes
import com.mowtiie.flashback.generator.Coverage
import com.mowtiie.flashback.generator.GeneratorPlan
import com.mowtiie.flashback.generator.GeneratorProvider
import com.mowtiie.flashback.theme.FlashbackTheme
import com.mowtiie.flashback.ui.components.CenteredScrollColumn
import com.mowtiie.flashback.ui.components.ConfirmDialog
import com.mowtiie.flashback.ui.components.EmptyState
import com.mowtiie.flashback.ui.components.LightDarkPreviews
import com.mowtiie.flashback.ui.components.showReplacing
import com.mowtiie.flashback.ui.settings.ChoiceRow
import com.mowtiie.flashback.ui.settings.ChoiceRowStyle
import android.text.format.Formatter
import java.text.NumberFormat
import kotlinx.coroutines.launch

class GeneratorActions(
    val onBack: () -> Unit = {},
    val onOpenSettings: () -> Unit = {},
    val onSourceChange: (String) -> Unit = {},
    val onChoosePdf: () -> Unit = {},
    val onRemovePdf: () -> Unit = {},
    val onCoverage: (Coverage) -> Unit = {},
    val onCardCount: (CardCount) -> Unit = {},
    val onCardTypes: (CardTypes) -> Unit = {},
    val onGenerate: () -> Unit = {},
    val onCancel: () -> Unit = {},
    val onRetry: () -> Unit = {},
    val onConfirm: () -> Unit = {},
    val onDismissConfirm: () -> Unit = {},
    val onDeckNameChange: (String) -> Unit = {},
    val onRemoveCard: (Int) -> Unit = {},
    val onRestoreCard: (String) -> Unit = {},
    val onStartOver: () -> Unit = {},
    val onSave: () -> Unit = {},
)

@Composable
fun GeneratorRoute(
    viewModel: GeneratorViewModel,
    onBack: () -> Unit,
    onOpenSettings: () -> Unit,
    onSaved: () -> Unit,
    notificationAsked: () -> Boolean = { true },
    onNotificationAsked: () -> Unit = {},
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val resources = LocalResources.current
    val snackbarHostState = remember { SnackbarHostState() }
    val context = LocalContext.current

    // Asked straight away by Android; the run starts whatever the answer is, since the notification only shows progress.
    val notificationLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {
        viewModel.generate()
    }

    val pdfLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) viewModel.attachPdf { PdfFiles.read(context.contentResolver, uri) }
    }

    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { viewModel.refreshKey() }

    LaunchedEffect(viewModel) {
        viewModel.eventFlow.collect { event ->
            when (event) {
                GeneratorEvent.Saved -> onSaved()
                GeneratorEvent.SaveFailed -> snackbarHostState.showReplacing(
                    message = resources.getString(R.string.generator_save_failed),
                    duration = SnackbarDuration.Long,
                )
            }
        }
    }

    val actions = remember(viewModel, onBack, onOpenSettings, pdfLauncher) {
        GeneratorActions(
            onBack = onBack,
            onOpenSettings = onOpenSettings,
            onSourceChange = viewModel::setSource,
            onChoosePdf = { pdfLauncher.launch(arrayOf("application/pdf")) },
            onRemovePdf = viewModel::removePdf,
            onCoverage = viewModel::setCoverage,
            onCardCount = viewModel::setCardCount,
            onCardTypes = viewModel::setCardTypes,
            onGenerate = {
                // A long run shows its progress in a notification, which needs the permission on Android 13 and up.
                if (viewModel.uiState.value.keepsRunning && !context.canPostNotifications() && !notificationAsked()) {
                    onNotificationAsked()
                    notificationLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                } else {
                    viewModel.generate()
                }
            },
            onCancel = viewModel::cancelGeneration,
            onRetry = viewModel::retryFailed,
            onConfirm = viewModel::confirmGenerate,
            onDismissConfirm = viewModel::dismissConfirm,
            onDeckNameChange = viewModel::setDeckName,
            onRemoveCard = viewModel::removeCard,
            onRestoreCard = viewModel::restoreCard,
            onStartOver = viewModel::startOver,
            onSave = viewModel::save,
        )
    }

    GeneratorScreen(state, actions, snackbarHostState)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GeneratorScreen(
    state: GeneratorUiState,
    actions: GeneratorActions,
    snackbarHostState: SnackbarHostState = remember { SnackbarHostState() },
) {
    val scope = rememberCoroutineScope()
    val resources = LocalResources.current
    var confirmLeave by rememberSaveable { mutableStateOf(false) }

    // Leaving while cards are being made, or before saving them, would throw the work away.
    val leave = { if (state.hasWorkToLose) confirmLeave = true else actions.onBack() }
    BackHandler(enabled = state.hasWorkToLose) { confirmLeave = true }

    val removeCard = { index: Int, note: DeckArchive.Note ->
        actions.onRemoveCard(index)
        scope.launch {
            snackbarHostState.currentSnackbarData?.dismiss()
            val result = snackbarHostState.showSnackbar(
                message = resources.getString(R.string.generator_card_removed),
                actionLabel = resources.getString(R.string.study_undo),
                duration = SnackbarDuration.Short,
            )
            if (result == SnackbarResult.ActionPerformed) actions.onRestoreCard(note.front)
        }
        Unit
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.generator_title)) },
                navigationIcon = {
                    IconButton(onClick = leave) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.navigate_up),
                        )
                    }
                },
            )
        },
        bottomBar = {
            if (state.hasApiKey && (state.stage == GeneratorStage.Review || state.stage == GeneratorStage.Saving)) {
                ReviewButtons(state, actions)
            }
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { padding ->
        Box(Modifier.padding(padding).fillMaxSize()) {
            when {
                !state.hasApiKey -> EmptyState(
                    title = stringResource(if (state.provider.usesNetwork) R.string.generator_no_key_title else R.string.generator_no_model_title),
                    body = stringResource(if (state.provider.usesNetwork) R.string.generator_no_key_body else R.string.generator_no_model_body),
                    actionLabel = stringResource(R.string.generator_no_key_action),
                    onAction = actions.onOpenSettings,
                )
                state.stage == GeneratorStage.Generating -> Working(state, actions.onCancel)
                state.stage == GeneratorStage.Input -> InputPage(state, actions)
                else -> ReviewPage(state, actions, removeCard)
            }
        }
    }

    if (confirmLeave && state.hasWorkToLose) {
        val generating = state.stage == GeneratorStage.Generating
        ConfirmDialog(
            title = stringResource(if (generating) R.string.generator_leave_run_title else R.string.generator_leave_review_title),
            body = if (generating) {
                stringResource(R.string.generator_leave_run_body)
            } else {
                pluralStringResource(R.plurals.generator_leave_review_body, state.cards.size, state.cards.size)
            },
            confirmLabel = stringResource(if (generating) R.string.generator_leave_run_stop else R.string.generator_leave_review_discard),
            dismissLabel = stringResource(if (generating) R.string.generator_leave_run_keep else R.string.generator_leave_review_keep),
            onConfirm = {
                confirmLeave = false
                actions.onBack()
            },
            onDismiss = { confirmLeave = false },
        )
    }
}

@Composable
private fun InputPage(state: GeneratorUiState, actions: GeneratorActions) {
    CenteredScrollColumn {
        if (state.hasPdf) {
            PdfCard(state, actions.onRemovePdf)
        } else {
            OutlinedTextField(
                value = state.source,
                onValueChange = actions.onSourceChange,
                label = { Text(stringResource(R.string.generator_source_label)) },
                isError = state.sourceTooLong,
                trailingIcon = errorTrailingIcon(state.sourceTooLong),
                supportingText = sourceSupport(state)?.let { text -> { Text(text) } },
                minLines = 8,
                maxLines = 16,
                modifier = Modifier.fillMaxWidth(),
            )
            // The on-device model reads text only.
            if (state.provider.usesNetwork) {
                OutlinedButton(onClick = actions.onChoosePdf, modifier = Modifier.fillMaxWidth()) {
                    Text(stringResource(R.string.generator_pdf_choose))
                }
            }
        }
        ChoiceRow(
            style = ChoiceRowStyle.Form,
            title = stringResource(R.string.prompt_coverage_title),
            options = Coverage.entries,
            selected = state.coverage,
            label = { stringResource(it.labelRes()) },
            onSelect = actions.onCoverage,
            hint = stringResource(state.coverage.hintRes()),
        )
        if (state.coverage == Coverage.KeyPoints) {
            ChoiceRow(
                style = ChoiceRowStyle.Form,
                title = stringResource(R.string.generator_count_title),
                options = CardCount.entries,
                selected = state.cardCount,
                label = { stringResource(it.labelRes(), it.max) },
                onSelect = actions.onCardCount,
            )
        }
        CardTypesChoice(state.cardTypes, actions.onCardTypes)
        state.error?.let {
            Text(
                it,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite },
            )
        }
        Text(
            text = stringResource(state.provider.privacyRes()),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Button(onClick = actions.onGenerate, enabled = state.canGenerate, modifier = Modifier.fillMaxWidth()) {
            Text(stringResource(R.string.generator_action))
        }
    }
    if (state.confirming) ConfirmRunDialog(state, actions.onConfirm, actions.onDismissConfirm)
}

/** The hint while the box is empty, then only what a student needs: a problem, or how many parts it will take. */
@Composable
private fun sourceSupport(state: GeneratorUiState): String? {
    val limit = GeneratorPlan.maxChars(state.coverage, state.provider)
    return when {
        state.sourceTooLong -> stringResource(R.string.generator_source_too_long, grouped(limit))
        state.source.isBlank() -> stringResource(R.string.generator_source_hint)
        state.estimatedRequests > 1 ->
            pluralStringResource(R.plurals.generator_parts, state.estimatedRequests, state.estimatedRequests)
        state.source.length > limit * NEAR_LIMIT -> stringResource(R.string.generator_source_count, grouped(state.source.length), grouped(limit))
        else -> null
    }
}

private const val NEAR_LIMIT = 0.8

@Composable
private fun ReviewPage(
    state: GeneratorUiState,
    actions: GeneratorActions,
    onRemoveCard: (Int, DeckArchive.Note) -> Unit,
) {
    val saving = state.stage == GeneratorStage.Saving
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        item(key = "name") {
            OutlinedTextField(
                value = state.deckName,
                onValueChange = actions.onDeckNameChange,
                label = { Text(stringResource(R.string.generator_deck_name)) },
                singleLine = true,
                enabled = !saving,
                modifier = Modifier.fillMaxWidth(),
            )
        }
        item(key = "count") {
            Text(
                text = pluralStringResource(R.plurals.generator_review_count, state.cards.size, state.cards.size),
                style = MaterialTheme.typography.titleSmall,
            )
        }
        if (state.sectionCounts.size > 1 || state.failedSections.isNotEmpty()) {
            item(key = "sections") { SectionSummary(state, enabled = !saving, onRetry = actions.onRetry) }
        }
        if (state.cards.isEmpty()) {
            item(key = "empty") {
                Text(
                    text = stringResource(R.string.generator_all_removed),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        itemsIndexed(state.cards, key = { index, note -> "$index:${note.front}" }) { index, note ->
            DraftCard(note, enabled = !saving, onRemove = { onRemoveCard(index, note) })
        }
    }
}

/** In the bottom bar, so the Undo message for a removed card appears above it and not over it. */
@Composable
private fun ReviewButtons(state: GeneratorUiState, actions: GeneratorActions) {
    val saving = state.stage == GeneratorStage.Saving
    Row(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.fillMaxWidth().padding(16.dp),
    ) {
        OutlinedButton(onClick = actions.onStartOver, enabled = !saving, modifier = Modifier.weight(1f)) {
            Text(stringResource(R.string.generator_start_over))
        }
        Button(onClick = actions.onSave, enabled = state.canSave, modifier = Modifier.weight(1f)) {
            Text(stringResource(R.string.generator_save))
        }
    }
}

@Composable
private fun DraftCard(note: DeckArchive.Note, enabled: Boolean, onRemove: () -> Unit) {
    Card(Modifier.fillMaxWidth()) {
        Row(
            verticalAlignment = Alignment.Top,
            modifier = Modifier.padding(start = 16.dp, top = 8.dp, bottom = 8.dp),
        ) {
            Column(Modifier.weight(1f).padding(top = 8.dp)) {
                note.answerMode?.let {
                    Text(
                        text = stringResource(AnswerMode.fromValue(it).labelRes()),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary,
                    )
                }
                Text(note.front, style = MaterialTheme.typography.titleSmall)
                Text(
                    text = note.back,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 4.dp),
                )
                if (note.choices.isNotEmpty()) {
                    Text(
                        text = stringResource(R.string.generator_wrong_answers, note.choices.joinToString(" · ")),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 4.dp),
                    )
                }
            }
            IconButton(onClick = onRemove, enabled = enabled) {
                Icon(
                    Icons.Default.Close,
                    contentDescription = stringResource(R.string.generator_remove_card, note.front),
                )
            }
        }
    }
}

private val previewCards = listOf(
    DeckArchive.Note("Which organelle makes most of a cell's ATP?", "The mitochondrion"),
    DeckArchive.Note("What does the nucleus contain?", "The cell's DNA"),
    DeckArchive.Note("What is the role of ribosomes?", "They build proteins"),
)

@LightDarkPreviews
@Composable
private fun GeneratorInputPreview() {
    FlashbackTheme(dynamicColor = false) {
        GeneratorScreen(
            GeneratorUiState(hasApiKey = true, source = "Mitochondria are the powerhouse of the cell."),
            GeneratorActions(),
        )
    }
}

@LightDarkPreviews
@Composable
private fun GeneratorReviewPreview() {
    FlashbackTheme(dynamicColor = false) {
        GeneratorScreen(
            GeneratorUiState(
                hasApiKey = true,
                stage = GeneratorStage.Review,
                deckName = "Cell biology",
                cards = previewCards,
            ),
            GeneratorActions(),
        )
    }
}

@LightDarkPreviews
@Composable
private fun GeneratorNoKeyPreview() {
    FlashbackTheme(dynamicColor = false) {
        GeneratorScreen(GeneratorUiState(), GeneratorActions())
    }
}

private fun GeneratorProvider.privacyRes(): Int = when (this) {
    GeneratorProvider.Anthropic -> R.string.generator_privacy_anthropic
    GeneratorProvider.Gemini -> R.string.generator_privacy_gemini
    GeneratorProvider.OnDevice -> R.string.generator_privacy_on_device
}

private fun grouped(count: Int): String = NumberFormat.getIntegerInstance().format(count)

@Composable
private fun PdfCard(state: GeneratorUiState, onRemove: () -> Unit) {
    Card(Modifier.fillMaxWidth()) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(start = 16.dp, top = 8.dp, bottom = 8.dp),
        ) {
            Column(Modifier.weight(1f)) {
                Text(state.pdfName.orEmpty(), style = MaterialTheme.typography.titleSmall)
                Text(
                    text = Formatter.formatShortFileSize(LocalContext.current, state.pdfSize.toLong()),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            IconButton(onClick = onRemove) {
                Icon(
                    Icons.Default.Close,
                    contentDescription = stringResource(R.string.generator_pdf_remove, state.pdfName.orEmpty()),
                )
            }
        }
    }
}

private fun Coverage.labelRes(): Int = when (this) {
    Coverage.Everything -> R.string.prompt_coverage_everything
    Coverage.KeyPoints -> R.string.prompt_coverage_key
}

private fun Coverage.hintRes(): Int = when (this) {
    Coverage.Everything -> R.string.prompt_coverage_hint_everything
    Coverage.KeyPoints -> R.string.prompt_coverage_hint_key
}

internal fun CardCount.labelRes(): Int = when (this) {
    CardCount.Few -> R.string.generator_count_few
    CardCount.Normal -> R.string.generator_count_normal
    CardCount.Many -> R.string.generator_count_many
}

/** Nothing is preselected: the user decides how the cards will be studied. */
@Composable
internal fun CardTypesChoice(selected: CardTypes?, onSelect: (CardTypes) -> Unit) {
    Column(Modifier.selectableGroup()) {
        Text(
            text = stringResource(R.string.generator_types_title),
            style = MaterialTheme.typography.titleSmall,
            modifier = Modifier.padding(top = 8.dp, bottom = 4.dp).asHeading(),
        )
        CardTypes.entries.forEach { types ->
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 56.dp)
                    .selectable(selected = types == selected, role = Role.RadioButton, onClick = { onSelect(types) }),
            ) {
                RadioButton(selected = types == selected, onClick = null)
                Column(Modifier.padding(start = 16.dp, top = 8.dp, bottom = 8.dp)) {
                    Text(stringResource(types.labelRes()), style = MaterialTheme.typography.bodyLarge)
                    Text(
                        text = stringResource(types.helpRes()),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}

internal fun CardTypes.labelRes(): Int = when (this) {
    CardTypes.Setting -> R.string.generator_types_setting
    CardTypes.Mixed -> R.string.generator_types_mixed
    CardTypes.Flip -> R.string.generator_types_flip
    CardTypes.Type -> R.string.generator_types_type
    CardTypes.Choice -> R.string.generator_types_choice
}

private fun CardTypes.helpRes(): Int = when (this) {
    CardTypes.Setting -> R.string.generator_types_setting_help
    CardTypes.Mixed -> R.string.generator_types_mixed_help
    CardTypes.Flip -> R.string.generator_types_flip_help
    CardTypes.Type -> R.string.generator_types_type_help
    CardTypes.Choice -> R.string.generator_types_choice_help
}

private fun AnswerMode.labelRes(): Int = when (this) {
    AnswerMode.Flip -> R.string.answer_mode_flip
    AnswerMode.Type -> R.string.answer_mode_type
    AnswerMode.Choice -> R.string.answer_mode_choice
}

@LightDarkPreviews
@Composable
private fun GeneratorProgressPreview() {
    FlashbackTheme(dynamicColor = false) {
        GeneratorScreen(
            GeneratorUiState(
                hasApiKey = true,
                stage = GeneratorStage.Generating,
                sectionTotal = 6,
                sectionsFinished = 2,
                cards = previewCards,
            ),
            GeneratorActions(),
        )
    }
}

@LightDarkPreviews
@Composable
private fun GeneratorPartialReviewPreview() {
    FlashbackTheme(dynamicColor = false) {
        GeneratorScreen(
            GeneratorUiState(
                hasApiKey = true,
                stage = GeneratorStage.Review,
                deckName = "Cell biology",
                cards = previewCards,
                sectionCounts = listOf(2, 0, 1),
                failedSections = listOf(FailedSection(2, "Anthropic is unavailable at the moment. Try again later.")),
            ),
            GeneratorActions(),
        )
    }
}
