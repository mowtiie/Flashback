package com.mowtiie.flashback.ui.notes

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.mowtiie.flashback.R
import com.mowtiie.flashback.repository.FlashbackRepository
import com.mowtiie.flashback.theme.FlashbackTheme
import com.mowtiie.flashback.ui.components.DeleteDialog
import com.mowtiie.flashback.ui.components.DiscardChangesDialog
import com.mowtiie.flashback.ui.components.LightDarkPreviews

@Composable
fun NoteEditorRoute(
    viewModel: NoteEditorViewModel,
    onClose: () -> Unit,
    onSavedAnother: () -> Unit,
    onDeleted: (FlashbackRepository.UndoAction) -> Unit,
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    var confirmDiscard by rememberSaveable { mutableStateOf(false) }
    var confirmDelete by rememberSaveable { mutableStateOf(false) }
    val frontFocus = remember { FocusRequester() }

    LaunchedEffect(viewModel) {
        viewModel.eventFlow.collect { event ->
            when (event) {
                NoteEditorEvent.Saved, NoteEditorEvent.NotFound -> onClose()
                NoteEditorEvent.ReadyForNext -> {
                    frontFocus.requestFocus()
                    onSavedAnother()
                }
                is NoteEditorEvent.Deleted -> onDeleted(event.undo)
            }
        }
    }

    val requestClose = { if (state.isDirty) confirmDiscard = true else onClose() }
    BackHandler(enabled = state.isDirty) { confirmDiscard = true }

    NoteEditorScreen(
        state = state,
        frontFocus = frontFocus,
        onBack = requestClose,
        onDelete = { confirmDelete = true },
        onFrontChange = viewModel::onFrontChange,
        onBackChange = viewModel::onBackChange,
        onReverseChange = viewModel::onReverseChange,
        onSuspendedChange = viewModel::onSuspendedChange,
        onSave = { viewModel.save(addAnother = false) },
        onSaveAndAdd = { viewModel.save(addAnother = true) },
    )

    if (confirmDiscard) {
        DiscardChangesDialog(
            onDiscard = {
                confirmDiscard = false
                onClose()
            },
            onKeepEditing = { confirmDiscard = false },
        )
    }
    if (confirmDelete) {
        DeleteDialog(
            title = stringResource(R.string.note_delete_title),
            body = stringResource(R.string.note_delete_body),
            onDelete = {
                confirmDelete = false
                viewModel.delete()
            },
            onDismiss = { confirmDelete = false },
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NoteEditorScreen(
    state: NoteEditorUiState,
    frontFocus: FocusRequester,
    onBack: () -> Unit,
    onDelete: () -> Unit,
    onFrontChange: (String) -> Unit,
    onBackChange: (String) -> Unit,
    onReverseChange: (Boolean) -> Unit,
    onSuspendedChange: (Boolean) -> Unit,
    onSave: () -> Unit,
    onSaveAndAdd: () -> Unit,
) {
    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior()
    val form = state.form

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        stringResource(
                            if (state.isEditing) R.string.note_editor_edit_title
                            else R.string.note_editor_new_title,
                        ),
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.navigate_up),
                        )
                    }
                },
                actions = {
                    if (state.isEditing) {
                        IconButton(onClick = onDelete) {
                            Icon(
                                painterResource(R.drawable.ic_delete),
                                contentDescription = stringResource(R.string.note_delete),
                            )
                        }
                    }
                },
                scrollBehavior = scrollBehavior,
            )
        },
    ) { padding ->
        if (!state.ready) return@Scaffold

        Column(
            modifier = Modifier
                .padding(padding)
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
        ) {
            OutlinedTextField(
                value = form.front,
                onValueChange = onFrontChange,
                label = { Text(stringResource(R.string.note_front)) },
                isError = state.error == NoteEditorError.FrontRequired,
                supportingText = if (state.error == NoteEditorError.FrontRequired) {
                    { Text(stringResource(R.string.note_front_required)) }
                } else {
                    null
                },
                minLines = 3,
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
                modifier = Modifier
                    .fillMaxWidth()
                    .focusRequester(frontFocus),
            )

            OutlinedTextField(
                value = form.back,
                onValueChange = onBackChange,
                label = { Text(stringResource(R.string.note_back)) },
                isError = state.error == NoteEditorError.BackRequired,
                supportingText = if (state.error == NoteEditorError.BackRequired) {
                    { Text(stringResource(R.string.note_back_required)) }
                } else {
                    null
                },
                minLines = 3,
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 16.dp),
            )

            SwitchRow(
                label = stringResource(R.string.note_reverse),
                help = stringResource(R.string.note_reverse_help),
                checked = form.reverseEnabled,
                onCheckedChange = onReverseChange,
                modifier = Modifier.padding(top = 24.dp),
            )

            if (state.isEditing) {
                SwitchRow(
                    label = stringResource(R.string.note_suspend),
                    help = stringResource(R.string.note_suspend_help),
                    checked = form.suspended,
                    onCheckedChange = onSuspendedChange,
                    modifier = Modifier.padding(top = 24.dp),
                )
            }

            Button(
                onClick = onSave,
                enabled = !state.saving,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 32.dp),
            ) {
                Text(stringResource(R.string.note_save))
            }
            if (!state.isEditing) {
                TextButton(
                    onClick = onSaveAndAdd,
                    enabled = !state.saving,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp),
                ) {
                    Text(stringResource(R.string.note_save_and_add))
                }
            }
        }
    }
}

@Composable
private fun SwitchRow(
    label: String,
    help: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .toggleable(value = checked, role = Role.Switch, onValueChange = onCheckedChange),
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.weight(1f),
            )
            Switch(checked = checked, onCheckedChange = null)
        }
        Text(
            text = help,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 4.dp),
        )
    }
}

@LightDarkPreviews
@Composable
private fun NoteEditorNewPreview() {
    FlashbackTheme(dynamicColor = false) {
        NoteEditorScreen(
            state = NoteEditorUiState(ready = true, form = NoteEditorForm(reverseEnabled = true)),
            frontFocus = FocusRequester(),
            onBack = {}, onDelete = {}, onFrontChange = {}, onBackChange = {},
            onReverseChange = {}, onSuspendedChange = {}, onSave = {}, onSaveAndAdd = {},
        )
    }
}

@LightDarkPreviews
@Composable
private fun NoteEditorEditPreview() {
    FlashbackTheme(dynamicColor = false) {
        NoteEditorScreen(
            state = NoteEditorUiState(
                isEditing = true,
                ready = true,
                form = NoteEditorForm(front = "el bosque", back = "", suspended = true),
                error = NoteEditorError.BackRequired,
            ),
            frontFocus = FocusRequester(),
            onBack = {}, onDelete = {}, onFrontChange = {}, onBackChange = {},
            onReverseChange = {}, onSuspendedChange = {}, onSave = {}, onSaveAndAdd = {},
        )
    }
}
