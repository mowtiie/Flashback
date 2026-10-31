package com.mowtiie.flashback.ui.tags

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringArrayResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.mowtiie.flashback.R
import com.mowtiie.flashback.data.entity.Tag
import com.mowtiie.flashback.repository.FlashbackRepository
import com.mowtiie.flashback.theme.FlashbackTheme
import com.mowtiie.flashback.ui.components.DeleteDialog
import com.mowtiie.flashback.ui.components.DiscardChangesDialog
import com.mowtiie.flashback.ui.components.LightDarkPreviews
import com.mowtiie.flashback.ui.components.TagChip

@Composable
fun TagEditorRoute(
    viewModel: TagEditorViewModel,
    onClose: () -> Unit,
    onDeleted: (FlashbackRepository.UndoAction) -> Unit,
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    var confirmDiscard by rememberSaveable { mutableStateOf(false) }
    var confirmDelete by rememberSaveable { mutableStateOf(false) }

    LaunchedEffect(viewModel) {
        viewModel.eventFlow.collect { event ->
            when (event) {
                TagEditorEvent.Saved, TagEditorEvent.NotFound -> onClose()
                is TagEditorEvent.Deleted -> onDeleted(event.undo)
            }
        }
    }

    val requestClose = { if (state.isDirty) confirmDiscard = true else onClose() }
    BackHandler(enabled = state.isDirty) { confirmDiscard = true }

    TagEditorScreen(
        state = state,
        onBack = requestClose,
        onDelete = { confirmDelete = true },
        onNameChange = viewModel::onNameChange,
        onDescriptionChange = viewModel::onDescriptionChange,
        onColorSelected = viewModel::onColorSelected,
        onSave = viewModel::save,
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
            title = stringResource(R.string.tag_delete_title),
            body = stringResource(R.string.tag_delete_body),
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
fun TagEditorScreen(
    state: TagEditorUiState,
    onBack: () -> Unit,
    onDelete: () -> Unit,
    onNameChange: (String) -> Unit,
    onDescriptionChange: (String) -> Unit,
    onColorSelected: (Int) -> Unit,
    onSave: () -> Unit,
) {
    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior()
    val focusManager = LocalFocusManager.current
    val form = state.form

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        stringResource(
                            if (state.isEditing) R.string.tag_edit_title else R.string.tag_new_title,
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
                                contentDescription = stringResource(R.string.tag_delete),
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
                value = form.name,
                onValueChange = onNameChange,
                label = { Text(stringResource(R.string.tag_name)) },
                isError = state.nameRequired || state.nameTaken,
                supportingText = when {
                    state.nameRequired -> {
                        { Text(stringResource(R.string.tag_name_required)) }
                    }
                    state.nameTaken -> {
                        { Text(stringResource(R.string.tag_name_taken)) }
                    }
                    else -> null
                },
                singleLine = true,
                keyboardOptions = KeyboardOptions(
                    capitalization = KeyboardCapitalization.Sentences,
                    imeAction = ImeAction.Next,
                ),
                modifier = Modifier.fillMaxWidth(),
            )

            OutlinedTextField(
                value = form.description,
                onValueChange = onDescriptionChange,
                label = { Text(stringResource(R.string.tag_description)) },
                maxLines = 2,
                keyboardOptions = KeyboardOptions(
                    capitalization = KeyboardCapitalization.Sentences,
                    imeAction = ImeAction.Done,
                ),
                keyboardActions = KeyboardActions(onDone = { focusManager.clearFocus() }),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 16.dp),
            )

            Text(
                text = stringResource(R.string.tag_colour),
                style = MaterialTheme.typography.titleSmall,
                modifier = Modifier.padding(top = 24.dp),
            )
            ColorSwatches(
                selected = form.color,
                onSelected = onColorSelected,
                modifier = Modifier.padding(top = 8.dp),
            )

            Text(
                text = stringResource(R.string.tag_preview),
                style = MaterialTheme.typography.titleSmall,
                modifier = Modifier.padding(top = 16.dp),
            )
            TagChip(
                tag = Tag(
                    name = form.name.trim().ifEmpty { stringResource(R.string.tag_preview_placeholder) },
                    color = form.color,
                ),
                modifier = Modifier.padding(top = 8.dp),
            )

            Button(
                onClick = onSave,
                enabled = !state.saving,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 32.dp),
            ) {
                Text(stringResource(R.string.tag_save))
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ColorSwatches(selected: Int, onSelected: (Int) -> Unit, modifier: Modifier = Modifier) {
    val names = stringArrayResource(R.array.tag_palette_names)
    val selectedLabel = stringResource(R.string.swatch_selected)

    FlowRow(
        modifier = modifier.selectableGroup(),
        maxItemsInEachRow = SWATCH_COLUMNS,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        TagPalette.colors.forEachIndexed { index, argb ->
            val color = Color(argb)
            val isSelected = argb == selected
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(48.dp)
                    .selectable(selected = isSelected, role = Role.RadioButton) {
                        onSelected(argb)
                    }
                    .semantics {
                        names.getOrNull(index)?.let { contentDescription = it }
                        if (isSelected) stateDescription = selectedLabel
                    },
            ) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(40.dp)
                        .background(color, CircleShape),
                ) {
                    if (isSelected) {
                        // The tick contrasts with the swatch it sits on, not with the theme.
                        Icon(
                            Icons.Default.Check,
                            contentDescription = null,
                            tint = if (color.luminance() > 0.5f) Color.Black else Color.White,
                        )
                    }
                }
            }
        }
    }
}

private const val SWATCH_COLUMNS = 6

@LightDarkPreviews
@Composable
private fun TagEditorPreview() {
    FlashbackTheme(dynamicColor = false) {
        TagEditorScreen(
            state = TagEditorUiState(
                isEditing = true,
                ready = true,
                form = TagEditorForm(name = "Biology", description = "Year 2 modules", color = TagPalette.colors[4]),
            ),
            onBack = {}, onDelete = {}, onNameChange = {}, onDescriptionChange = {},
            onColorSelected = {}, onSave = {},
        )
    }
}

@LightDarkPreviews
@Composable
private fun TagEditorNameTakenPreview() {
    FlashbackTheme(dynamicColor = false) {
        TagEditorScreen(
            state = TagEditorUiState(
                ready = true,
                form = TagEditorForm(name = "Biology", color = TagPalette.colors[2]),
                nameTaken = true,
            ),
            onBack = {}, onDelete = {}, onNameChange = {}, onDescriptionChange = {},
            onColorSelected = {}, onSave = {},
        )
    }
}
