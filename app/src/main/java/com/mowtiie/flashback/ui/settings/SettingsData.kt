package com.mowtiie.flashback.ui.settings

import android.content.res.Resources
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.platform.testTag
import kotlinx.coroutines.launch
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.TextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.mowtiie.flashback.R
import com.mowtiie.flashback.ui.components.errorTrailingIcon
import com.mowtiie.flashback.ui.components.asHeading
import com.mowtiie.flashback.data.io.DeckArchive
import com.mowtiie.flashback.data.io.ImportResult

class DataTransferActions(
    val onExport: () -> Unit = {},
    val onImportFile: () -> Unit = {},
    val onPreviewPaste: (String) -> Unit = {},
    val onPasteTextChange: (String) -> Unit = {},
    val onPasteDismissed: () -> Unit = {},
    val onConfirmImport: () -> Unit = {},
    val onCancelImport: () -> Unit = {},
)

@Composable
internal fun DataSection(state: DataTransferUiState, actions: DataTransferActions) {
    var chooseSource by rememberSaveable { mutableStateOf(false) }

    SectionHeader(R.string.settings_data_header)
    SettingsCard {
        ActionRow(
            title = stringResource(R.string.settings_export_title),
            body = stringResource(R.string.settings_export_body),
            leadingIcon = R.drawable.ic_download,
            enabled = !state.working,
            onClick = actions.onExport,
        )
        RowDivider()
        ActionRow(
            title = stringResource(R.string.settings_import_title),
            body = stringResource(R.string.settings_import_body),
            leadingIcon = R.drawable.ic_upload,
            enabled = !state.working,
            onClick = { chooseSource = true },
        )
    }
    Surface(
        color = MaterialTheme.colorScheme.secondaryContainer,
        shape = MaterialTheme.shapes.medium,
        modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
    ) {
        Text(
            text = stringResource(R.string.settings_format_note),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSecondaryContainer,
            modifier = Modifier.padding(16.dp),
        )
    }

    ImportDialogs(state, actions, chooseSource, onDismissSource = { chooseSource = false })
}

/** The steps of an import: pick a source, paste if needed, then confirm what was found. */
@Composable
internal fun ImportDialogs(
    state: DataTransferUiState,
    actions: DataTransferActions,
    chooseSource: Boolean,
    onDismissSource: () -> Unit,
) {
    var pasting by rememberSaveable { mutableStateOf(false) }

    // A parsed paste moves on to the preview, so the paste dialog gets out of the way.
    LaunchedEffect(state.pendingImport) {
        if (state.pendingImport != null) pasting = false
    }

    if (chooseSource) {
        ImportSourceSheet(
            onFile = {
                onDismissSource()
                actions.onImportFile()
            },
            onPaste = {
                onDismissSource()
                pasting = true
            },
            onDismiss = onDismissSource,
        )
    }

    if (pasting) {
        PasteDialog(
            text = state.pasteText,
            error = state.pasteError,
            working = state.working,
            onTextChange = actions.onPasteTextChange,
            onPreview = { actions.onPreviewPaste(state.pasteText) },
            onDismiss = {
                pasting = false
                actions.onPasteDismissed()
            },
        )
    }

    state.pendingImport?.let { pending ->
        ImportPreviewDialog(pending.counts, actions.onConfirmImport, actions.onCancelImport)
    }
}

/** The ways to import, in a bottom sheet so the choice sits within thumb reach. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ImportSourceSheet(onFile: () -> Unit, onPaste: () -> Unit, onDismiss: () -> Unit) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val scope = rememberCoroutineScope()

    // Slide the sheet away first, then act, so the next dialog does not open over a sheet.
    fun choose(action: () -> Unit) {
        scope.launch { sheetState.hide() }.invokeOnCompletion {
            onDismiss()
            action()
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        modifier = Modifier.testTag(IMPORT_SHEET_TAG),
    ) {
        Text(
            text = stringResource(R.string.import_source_title),
            style = MaterialTheme.typography.titleLarge,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp).asHeading(),
        )
        ActionRow(
            title = stringResource(R.string.import_source_file),
            body = stringResource(R.string.import_source_file_body),
            leadingIcon = R.drawable.ic_file,
            onClick = { choose(onFile) },
        )
        ActionRow(
            title = stringResource(R.string.import_source_paste),
            body = stringResource(R.string.import_source_paste_body),
            leadingIcon = R.drawable.ic_paste,
            onClick = { choose(onPaste) },
        )
        Spacer(Modifier.height(16.dp))
    }
}

@Composable
private fun PasteDialog(
    text: String,
    error: String?,
    working: Boolean,
    onTextChange: (String) -> Unit,
    onPreview: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.import_paste_title)) },
        text = {
            TextField(
                value = text,
                onValueChange = onTextChange,
                label = { Text(stringResource(R.string.import_paste_hint)) },
                isError = error != null,
                trailingIcon = errorTrailingIcon(error != null),
                supportingText = error?.let { { Text(it) } },
                minLines = 5,
                maxLines = 8,
                modifier = Modifier.fillMaxWidth(),
            )
        },
        confirmButton = {
            TextButton(onClick = onPreview, enabled = !working) {
                Text(stringResource(R.string.import_paste_action))
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_cancel)) } },
    )
}

@Composable
private fun ImportPreviewDialog(counts: DeckArchive.Counts, onConfirm: () -> Unit, onCancel: () -> Unit) {
    val decks = pluralStringResource(R.plurals.import_decks, counts.decks, counts.decks)
    val cards = pluralStringResource(R.plurals.import_cards, counts.notes, counts.notes)
    val tags = if (counts.tags > 0) {
        pluralStringResource(R.plurals.import_preview_tags, counts.tags, counts.tags)
    } else {
        ""
    }

    AlertDialog(
        onDismissRequest = onCancel,
        title = { Text(stringResource(R.string.import_preview_title)) },
        text = { Text(stringResource(R.string.import_preview_body, decks, cards, tags)) },
        confirmButton = { TextButton(onClick = onConfirm) { Text(stringResource(R.string.import_confirm)) } },
        dismissButton = { TextButton(onClick = onCancel) { Text(stringResource(R.string.action_cancel)) } },
    )
}

/** The snackbar text for an event; the export-ready hand-off has none. */
internal fun DataTransferEvent.message(resources: Resources): String? = when (this) {
    DataTransferEvent.ExportReady -> null
    DataTransferEvent.ExportNothing -> resources.getString(R.string.export_nothing)
    DataTransferEvent.ExportSaved -> resources.getString(R.string.export_saved)
    DataTransferEvent.ExportWriteFailed -> resources.getString(R.string.export_write_failed)
    is DataTransferEvent.ImportRejected -> message
    is DataTransferEvent.ImportReadFailed -> message ?: resources.getString(R.string.import_read_failed)
    DataTransferEvent.ImportFailed -> resources.getString(R.string.import_failed)
    is DataTransferEvent.ImportDone -> importSummary(resources, result)
}

private fun importSummary(res: Resources, result: ImportResult): String {
    val decks = res.getQuantityString(R.plurals.import_decks, result.decks, result.decks)
    val cards = res.getQuantityString(R.plurals.import_cards, result.notes, result.notes)
    if (result.tagsReused == 0) return res.getString(R.string.import_summary, decks, cards)
    val merged = res.getQuantityString(R.plurals.import_tags_merged, result.tagsReused, result.tagsReused)
    return res.getString(R.string.import_summary_merged, decks, cards, merged)
}
