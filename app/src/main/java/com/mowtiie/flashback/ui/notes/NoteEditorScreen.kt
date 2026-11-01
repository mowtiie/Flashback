package com.mowtiie.flashback.ui.notes

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.heightIn
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
import com.mowtiie.flashback.ui.components.errorTrailingIcon
import com.mowtiie.flashback.ui.settings.CheckboxRow
import com.mowtiie.flashback.ui.settings.SettingsCard
import com.mowtiie.flashback.ui.settings.RowDivider
import com.mowtiie.flashback.ui.components.SectionTitle
import com.mowtiie.flashback.ui.settings.ChoiceRow
import com.mowtiie.flashback.ui.study.AnswerMode
import com.mowtiie.flashback.repository.FlashbackRepository
import com.mowtiie.flashback.theme.FlashbackTheme
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
        onDelete = viewModel::delete,
        onFrontChange = viewModel::onFrontChange,
        onBackChange = viewModel::onBackChange,
        onReverseChange = viewModel::onReverseChange,
        onAnswerModeChange = viewModel::onAnswerModeChange,
        onWrongOptionChange = viewModel::onWrongOptionChange,
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
    onAnswerModeChange: (AnswerMode?) -> Unit,
    onWrongOptionChange: (Int, String) -> Unit,
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
                trailingIcon = errorTrailingIcon(state.error == NoteEditorError.FrontRequired),
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
                trailingIcon = errorTrailingIcon(state.error == NoteEditorError.BackRequired),
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

            SectionTitle(stringResource(R.string.note_section_study), Modifier.padding(top = 24.dp))
            SettingsCard {
                AnswerTypeSection(form, onAnswerModeChange, onWrongOptionChange)
                RowDivider()
                CheckboxRow(
                    title = stringResource(R.string.note_reverse),
                    body = stringResource(R.string.note_reverse_help),
                    checked = form.reverseEnabled,
                    onCheckedChange = onReverseChange,
                )
                if (state.isEditing) {
                    RowDivider()
                    CheckboxRow(
                        title = stringResource(R.string.note_suspend),
                        body = stringResource(R.string.note_suspend_help),
                        checked = form.suspended,
                        onCheckedChange = onSuspendedChange,
                    )
                }
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
private fun NoteEditorNewPreview() {
    FlashbackTheme(dynamicColor = false) {
        NoteEditorScreen(
            state = NoteEditorUiState(ready = true, form = NoteEditorForm(reverseEnabled = true)),
            frontFocus = remember { FocusRequester() },
            onBack = {}, onDelete = {}, onFrontChange = {}, onBackChange = {},
            onReverseChange = {}, onAnswerModeChange = {}, onWrongOptionChange = { _, _ -> },
            onSuspendedChange = {}, onSave = {}, onSaveAndAdd = {},
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
            frontFocus = remember { FocusRequester() },
            onBack = {}, onDelete = {}, onFrontChange = {}, onBackChange = {},
            onReverseChange = {}, onAnswerModeChange = {}, onWrongOptionChange = { _, _ -> },
            onSuspendedChange = {}, onSave = {}, onSaveAndAdd = {},
        )
    }
}

@Composable
private fun AnswerTypeSection(
    form: NoteEditorForm,
    onAnswerModeChange: (AnswerMode?) -> Unit,
    onWrongOptionChange: (Int, String) -> Unit,
) {
    ChoiceRow(
        title = stringResource(R.string.note_answer_type),
        options = listOf<AnswerMode?>(null) + AnswerMode.entries,
        selected = form.answerMode,
        label = {
            stringResource(
                when (it) {
                    null -> R.string.note_answer_type_default
                    AnswerMode.Flip -> R.string.answer_mode_flip
                    AnswerMode.Type -> R.string.answer_mode_type
                    AnswerMode.Choice -> R.string.answer_mode_choice
                },
            )
        },
        onSelect = onAnswerModeChange,
        hint = stringResource(
            when (form.answerMode) {
                null -> R.string.note_answer_type_default_help
                AnswerMode.Flip -> R.string.note_answer_type_flip_help
                AnswerMode.Type -> R.string.note_answer_type_type_help
                AnswerMode.Choice -> R.string.note_answer_type_choice_help
            },
        ),
    )

    if (form.answerMode == AnswerMode.Choice) {
        form.wrongOptions.forEachIndexed { index, option ->
            OutlinedTextField(
                value = option,
                onValueChange = { onWrongOptionChange(index, it) },
                label = { Text(stringResource(R.string.note_wrong_option, index + 1)) },
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp),
            )
        }
    }
}
