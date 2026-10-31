package com.mowtiie.flashback.ui.generator

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.layout.heightIn
import androidx.compose.material3.RadioButton
import androidx.compose.ui.semantics.Role
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
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
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
import com.mowtiie.flashback.data.io.DeckArchive
import com.mowtiie.flashback.generator.CardCount
import com.mowtiie.flashback.generator.CardTypes
import com.mowtiie.flashback.generator.GeneratorLimits
import com.mowtiie.flashback.generator.GeneratorProvider
import com.mowtiie.flashback.theme.FlashbackTheme
import com.mowtiie.flashback.ui.components.CenteredScrollColumn
import com.mowtiie.flashback.ui.components.EmptyState
import com.mowtiie.flashback.ui.components.LightDarkPreviews
import com.mowtiie.flashback.ui.components.showReplacing
import com.mowtiie.flashback.ui.settings.ChoiceRow
import android.text.format.Formatter
import java.text.NumberFormat

class GeneratorActions(
    val onBack: () -> Unit = {},
    val onOpenSettings: () -> Unit = {},
    val onSourceChange: (String) -> Unit = {},
    val onChoosePdf: () -> Unit = {},
    val onRemovePdf: () -> Unit = {},
    val onCardCount: (CardCount) -> Unit = {},
    val onCardTypes: (CardTypes) -> Unit = {},
    val onGenerate: () -> Unit = {},
    val onDeckNameChange: (String) -> Unit = {},
    val onRemoveCard: (Int) -> Unit = {},
    val onStartOver: () -> Unit = {},
    val onSave: () -> Unit = {},
)

@Composable
fun GeneratorRoute(
    viewModel: GeneratorViewModel,
    onBack: () -> Unit,
    onOpenSettings: () -> Unit,
    onSaved: () -> Unit,
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val resources = LocalResources.current
    val snackbarHostState = remember { SnackbarHostState() }
    val context = LocalContext.current

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
            onCardCount = viewModel::setCardCount,
            onCardTypes = viewModel::setCardTypes,
            onGenerate = viewModel::generate,
            onDeckNameChange = viewModel::setDeckName,
            onRemoveCard = viewModel::removeCard,
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
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.generator_title)) },
                navigationIcon = {
                    IconButton(onClick = actions.onBack) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.navigate_up),
                        )
                    }
                },
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { padding ->
        Box(Modifier.padding(padding).fillMaxSize()) {
            when {
                !state.hasApiKey -> EmptyState(
                    title = stringResource(R.string.generator_no_key_title),
                    body = stringResource(R.string.generator_no_key_body),
                    actionLabel = stringResource(R.string.generator_no_key_action),
                    onAction = actions.onOpenSettings,
                )
                state.stage == GeneratorStage.Generating -> Working(R.string.generator_working)
                state.stage == GeneratorStage.Input -> InputPage(state, actions)
                else -> ReviewPage(state, actions)
            }
        }
    }
}

@Composable
private fun Working(messageRes: Int) {
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        CircularProgressIndicator()
        Text(
            text = stringResource(messageRes),
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.padding(top = 16.dp),
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
                placeholder = { Text(stringResource(R.string.generator_source_hint)) },
                isError = state.sourceTooLong,
                supportingText = {
                    Text(
                        if (state.sourceTooLong) {
                            stringResource(R.string.generator_source_too_long, grouped(GeneratorLimits.MAX_SOURCE_CHARS))
                        } else {
                            stringResource(
                                R.string.generator_source_count,
                                grouped(state.source.length),
                                grouped(GeneratorLimits.MAX_SOURCE_CHARS),
                            )
                        },
                    )
                },
                minLines = 8,
                maxLines = 16,
                modifier = Modifier.fillMaxWidth(),
            )
            OutlinedButton(onClick = actions.onChoosePdf, modifier = Modifier.fillMaxWidth()) {
                Text(stringResource(R.string.generator_pdf_choose))
            }
        }
        ChoiceRow(
            title = stringResource(R.string.generator_count_title),
            options = CardCount.entries,
            selected = state.cardCount,
            label = { stringResource(it.labelRes(), it.max) },
            onSelect = actions.onCardCount,
            modifier = Modifier.padding(horizontal = 0.dp),
        )
        CardTypesChoice(state.cardTypes, actions.onCardTypes)
        state.error?.let {
            Text(it, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.error)
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
}

@Composable
private fun ReviewPage(state: GeneratorUiState, actions: GeneratorActions) {
    val saving = state.stage == GeneratorStage.Saving
    Column(Modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier.weight(1f),
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
                DraftCard(note, enabled = !saving, onRemove = { actions.onRemoveCard(index) })
            }
        }
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
}

@Composable
private fun DraftCard(note: DeckArchive.Note, enabled: Boolean, onRemove: () -> Unit) {
    OutlinedCard(Modifier.fillMaxWidth()) {
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
}

private fun grouped(count: Int): String = NumberFormat.getIntegerInstance().format(count)

@Composable
private fun PdfCard(state: GeneratorUiState, onRemove: () -> Unit) {
    OutlinedCard(Modifier.fillMaxWidth()) {
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

private fun CardCount.labelRes(): Int = when (this) {
    CardCount.Few -> R.string.generator_count_few
    CardCount.Normal -> R.string.generator_count_normal
    CardCount.Many -> R.string.generator_count_many
}

/** Nothing is preselected: the user decides how the cards will be studied. */
@Composable
private fun CardTypesChoice(selected: CardTypes?, onSelect: (CardTypes) -> Unit) {
    Column(Modifier.selectableGroup()) {
        Text(
            text = stringResource(R.string.generator_types_title),
            style = MaterialTheme.typography.titleSmall,
            modifier = Modifier.padding(top = 8.dp, bottom = 4.dp),
        )
        if (selected == null) {
            Text(
                text = stringResource(R.string.generator_types_required),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
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

private fun CardTypes.labelRes(): Int = when (this) {
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
