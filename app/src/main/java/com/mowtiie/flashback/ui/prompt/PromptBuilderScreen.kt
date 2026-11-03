package com.mowtiie.flashback.ui.prompt

import android.content.ClipData
import android.content.Intent
import com.mowtiie.flashback.data.io.ArchiveFiles
import com.mowtiie.flashback.ui.components.showReplacing
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.TextButton
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalResources
import com.mowtiie.flashback.ui.export.JSON_MIME_TYPE
import com.mowtiie.flashback.ui.settings.DataTransferActions
import com.mowtiie.flashback.ui.settings.DataTransferEvent
import com.mowtiie.flashback.ui.settings.DataTransferUiState
import com.mowtiie.flashback.ui.settings.DataTransferViewModel
import com.mowtiie.flashback.ui.settings.ImportDialogs
import com.mowtiie.flashback.ui.settings.message
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Surface
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboard
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.ClipEntry
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.mowtiie.flashback.R
import com.mowtiie.flashback.ui.components.asHeading
import com.mowtiie.flashback.generator.CardCount
import com.mowtiie.flashback.generator.CardTypes
import com.mowtiie.flashback.generator.PromptCoverage
import com.mowtiie.flashback.theme.FlashbackTheme
import com.mowtiie.flashback.ui.components.CenteredScrollColumn
import com.mowtiie.flashback.ui.components.LightDarkPreviews
import com.mowtiie.flashback.ui.generator.CardTypesChoice
import com.mowtiie.flashback.ui.generator.labelRes
import com.mowtiie.flashback.ui.settings.ChoiceRow
import com.mowtiie.flashback.ui.settings.ChoiceRowStyle
import kotlinx.coroutines.launch

class PromptBuilderActions(
    val onBack: () -> Unit = {},
    val onCoverage: (PromptCoverage) -> Unit = {},
    val onCardCount: (CardCount) -> Unit = {},
    val onCardTypes: (CardTypes) -> Unit = {},
    val onLanguage: (String) -> Unit = {},
    val onDeckName: (String) -> Unit = {},
    val onCopy: () -> Unit = {},
    val onShare: () -> Unit = {},
)

@Composable
fun PromptBuilderRoute(
    viewModel: PromptBuilderViewModel,
    transferViewModel: DataTransferViewModel,
    onBack: () -> Unit,
    /** The cards were imported; [summary] says what was added. */
    onImported: (summary: String) -> Unit,
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val transferState by transferViewModel.uiState.collectAsStateWithLifecycle()
    val resources = LocalResources.current
    val context = LocalContext.current

    val openLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) {
            transferViewModel.previewImportFromFile { ArchiveFiles.read(context.contentResolver, uri) }
        }
    }
    val clipboard = LocalClipboard.current
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    val copied = stringResource(R.string.prompt_copied)
    val chooserTitle = stringResource(R.string.prompt_share_chooser)

    val actions = PromptBuilderActions(
        onBack = onBack,
        onCoverage = viewModel::setCoverage,
        onCardCount = viewModel::setCardCount,
        onCardTypes = viewModel::setCardTypes,
        onLanguage = viewModel::setLanguage,
        onDeckName = viewModel::setDeckName,
        onCopy = {
            scope.launch {
                clipboard.setClipEntry(ClipEntry(ClipData.newPlainText("Flashback prompt", state.prompt)))
                snackbarHostState.showSnackbar(copied)
            }
        },
        onShare = {
            val send = Intent(Intent.ACTION_SEND)
                .setType("text/plain")
                .putExtra(Intent.EXTRA_TEXT, state.prompt)
            context.startActivity(Intent.createChooser(send, chooserTitle))
        },
    )

    LaunchedEffect(transferViewModel) {
        transferViewModel.eventFlow.collect { event ->
            val message = event.message(resources) ?: return@collect
            if (event is DataTransferEvent.ImportDone) {
                onImported(message)
            } else {
                launch { snackbarHostState.showReplacing(message = message, duration = SnackbarDuration.Long) }
            }
        }
    }

    val transferActions = remember(transferViewModel) {
        DataTransferActions(
            // A broad filter, because many exporters mislabel JSON as text or octet-stream.
            onImportFile = { openLauncher.launch(arrayOf(JSON_MIME_TYPE, "text/plain", "*/*")) },
            onPreviewPaste = transferViewModel::previewImportFromText,
            onPasteTextChange = transferViewModel::setPasteText,
            onPasteDismissed = transferViewModel::clearPaste,
            onConfirmImport = transferViewModel::confirmImport,
            onCancelImport = transferViewModel::clearPendingImport,
        )
    }

    PromptBuilderScreen(state, actions, transferState, transferActions, snackbarHostState)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PromptBuilderScreen(
    state: PromptBuilderUiState,
    actions: PromptBuilderActions,
    transferState: DataTransferUiState = DataTransferUiState(),
    transferActions: DataTransferActions = DataTransferActions(),
    snackbarHostState: SnackbarHostState = remember { SnackbarHostState() },
) {
    var showInstructions by rememberSaveable { mutableStateOf(false) }
    var choosingImport by rememberSaveable { mutableStateOf(false) }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        // Copy and Share stay in reach however long the options above them get.
        bottomBar = {
            Surface(tonalElevation = 3.dp) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth().navigationBarsPadding().padding(16.dp),
                ) {
                    OutlinedButton(onClick = actions.onShare, modifier = Modifier.weight(1f)) {
                        Text(stringResource(R.string.prompt_share))
                    }
                    Button(onClick = actions.onCopy, modifier = Modifier.weight(1f)) {
                        Text(stringResource(R.string.prompt_copy))
                    }
                }
            }
        },
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.prompt_title)) },
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
    ) { padding ->
        Box(Modifier.fillMaxSize()) {
            CenteredScrollColumn(Modifier.padding(padding)) {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(stringResource(R.string.prompt_step_1), style = MaterialTheme.typography.bodyLarge)
                    Text(stringResource(R.string.prompt_step_2), style = MaterialTheme.typography.bodyLarge)
                    Text(stringResource(R.string.prompt_step_3), style = MaterialTheme.typography.bodyLarge)
                }
                ChoiceRow(
                    style = ChoiceRowStyle.Form,
                    title = stringResource(R.string.prompt_coverage_title),
                    options = PromptCoverage.entries,
                    selected = state.options.coverage,
                    label = { stringResource(it.labelRes()) },
                    onSelect = actions.onCoverage,
                    hint = stringResource(state.options.coverage.hintRes()),
                )
                if (state.options.coverage == PromptCoverage.KeyPoints) {
                    ChoiceRow(
                        style = ChoiceRowStyle.Form,
                        title = stringResource(R.string.generator_count_title),
                        options = CardCount.entries,
                        selected = state.options.cardCount,
                        label = { stringResource(it.labelRes(), it.max) },
                        onSelect = actions.onCardCount,
                    )
                }
                CardTypesChoice(state.options.cardTypes, actions.onCardTypes)
                OutlinedTextField(
                    value = state.options.language,
                    onValueChange = actions.onLanguage,
                    label = { Text(stringResource(R.string.prompt_language_label)) },
                    supportingText = { Text(stringResource(R.string.prompt_language_hint)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(
                    value = state.options.deckName,
                    onValueChange = actions.onDeckName,
                    label = { Text(stringResource(R.string.prompt_name_label)) },
                    supportingText = { Text(stringResource(R.string.field_optional)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                OutlinedButton(onClick = { showInstructions = true }, modifier = Modifier.fillMaxWidth()) {
                    Text(stringResource(R.string.prompt_show))
                }
                OutlinedButton(onClick = { choosingImport = true }, modifier = Modifier.fillMaxWidth()) {
                    Text(stringResource(R.string.prompt_import))
                }
            }
        }
    }

    if (showInstructions) InstructionsDialog(state.prompt) { showInstructions = false }
    ImportDialogs(transferState, transferActions, choosingImport, onDismissSource = { choosingImport = false })
}

@Composable
private fun InstructionsDialog(prompt: String, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.prompt_instructions_title)) },
        text = {
            SelectionContainer {
                Text(
                    text = prompt,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.verticalScroll(rememberScrollState()),
                )
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.settings_license_close)) } },
    )
}

private fun PromptCoverage.labelRes(): Int = when (this) {
    PromptCoverage.Everything -> R.string.prompt_coverage_everything
    PromptCoverage.KeyPoints -> R.string.prompt_coverage_key
}

private fun PromptCoverage.hintRes(): Int = when (this) {
    PromptCoverage.Everything -> R.string.prompt_coverage_hint_everything
    PromptCoverage.KeyPoints -> R.string.prompt_coverage_hint_key
}

@LightDarkPreviews
@Composable
private fun PromptBuilderPreview() {
    FlashbackTheme(dynamicColor = false) {
        PromptBuilderScreen(PromptBuilderUiState(), PromptBuilderActions())
    }
}
