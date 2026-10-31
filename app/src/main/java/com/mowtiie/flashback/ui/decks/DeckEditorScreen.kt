package com.mowtiie.flashback.ui.decks

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.mowtiie.flashback.R
import com.mowtiie.flashback.data.entity.Tag
import com.mowtiie.flashback.theme.FlashbackTheme
import com.mowtiie.flashback.ui.components.DiscardChangesDialog
import com.mowtiie.flashback.ui.components.LightDarkPreviews

@Composable
fun DeckEditorRoute(
    viewModel: DeckEditorViewModel,
    onClose: () -> Unit,
    onSaved: () -> Unit,
    onNewTag: () -> Unit,
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    var confirmDiscard by rememberSaveable { mutableStateOf(false) }

    LaunchedEffect(viewModel) {
        viewModel.eventFlow.collect { event ->
            when (event) {
                DeckEditorEvent.Saved -> onSaved()
                DeckEditorEvent.NotFound -> onClose()
            }
        }
    }

    // Back and Up share one path so neither can lose edits silently.
    val requestClose = { if (state.isDirty) confirmDiscard = true else onClose() }
    BackHandler(enabled = state.isDirty) { confirmDiscard = true }

    DeckEditorScreen(
        state = state,
        onBack = requestClose,
        onNameChange = viewModel::onNameChange,
        onDescriptionChange = viewModel::onDescriptionChange,
        onReverseChange = viewModel::onReverseChange,
        onNewPerDayChange = viewModel::onNewPerDayChange,
        onReviewsPerDayChange = viewModel::onReviewsPerDayChange,
        onTagToggle = viewModel::onTagToggle,
        onNewTag = onNewTag,
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
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun DeckEditorScreen(
    state: DeckEditorUiState,
    onBack: () -> Unit,
    onNameChange: (String) -> Unit,
    onDescriptionChange: (String) -> Unit,
    onReverseChange: (Boolean) -> Unit,
    onNewPerDayChange: (String) -> Unit,
    onReviewsPerDayChange: (String) -> Unit,
    onTagToggle: (Long, Boolean) -> Unit,
    onNewTag: () -> Unit,
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
                            if (state.isEditing) R.string.deck_editor_edit_title
                            else R.string.deck_editor_new_title,
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
                label = { Text(stringResource(R.string.deck_name)) },
                isError = state.errors.nameRequired,
                supportingText = if (state.errors.nameRequired) {
                    { Text(stringResource(R.string.deck_name_required)) }
                } else {
                    null
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
                label = { Text(stringResource(R.string.deck_description)) },
                maxLines = 3,
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 16.dp),
            )

            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 24.dp)
                    .toggleable(
                        value = form.reverseByDefault,
                        role = Role.Switch,
                        onValueChange = onReverseChange,
                    ),
            ) {
                Text(
                    text = stringResource(R.string.deck_reverse_default),
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier.weight(1f),
                )
                Switch(checked = form.reverseByDefault, onCheckedChange = null)
            }
            Text(
                text = stringResource(R.string.deck_reverse_default_help),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 4.dp),
            )

            Row(
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 24.dp),
            ) {
                LimitField(
                    value = form.newPerDay,
                    onValueChange = onNewPerDayChange,
                    label = R.string.deck_new_per_day,
                    error = state.errors.newPerDay,
                    imeAction = ImeAction.Next,
                    onDone = {},
                    modifier = Modifier.weight(1f),
                )
                LimitField(
                    value = form.reviewsPerDay,
                    onValueChange = onReviewsPerDayChange,
                    label = R.string.deck_reviews_per_day,
                    error = state.errors.reviewsPerDay,
                    imeAction = ImeAction.Done,
                    onDone = { focusManager.clearFocus() },
                    modifier = Modifier.weight(1f),
                )
            }

            Text(
                text = stringResource(R.string.deck_tags),
                style = MaterialTheme.typography.titleSmall,
                modifier = Modifier.padding(top = 24.dp),
            )
            if (state.tags.isEmpty()) {
                Text(
                    text = stringResource(R.string.deck_tags_none),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 4.dp),
                )
            } else {
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.padding(top = 8.dp),
                ) {
                    state.tags.forEach { tag ->
                        TagToggleChip(tag, tag.id in form.tagIds, onTagToggle)
                    }
                }
            }
            TextButton(onClick = onNewTag, modifier = Modifier.padding(top = 4.dp)) {
                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                Text(
                    text = stringResource(R.string.tag_add),
                    modifier = Modifier.padding(start = 8.dp),
                )
            }

            Button(
                onClick = onSave,
                enabled = !state.saving,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 32.dp),
            ) {
                Text(stringResource(R.string.deck_save))
            }
        }
    }
}

@Composable
private fun LimitField(
    value: String,
    onValueChange: (String) -> Unit,
    label: Int,
    error: LimitError?,
    imeAction: ImeAction,
    onDone: () -> Unit,
    modifier: Modifier = Modifier,
) {
    OutlinedTextField(
        value = value,
        onValueChange = { onValueChange(it.filter(Char::isDigit)) },
        label = { Text(stringResource(label)) },
        isError = error != null,
        supportingText = error?.let {
            {
                Text(
                    stringResource(
                        if (it == LimitError.Required) R.string.deck_limit_required
                        else R.string.deck_limit_too_large,
                    ),
                )
            }
        },
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = imeAction),
        keyboardActions = KeyboardActions(onDone = { onDone() }),
        modifier = modifier,
    )
}

@Composable
private fun TagToggleChip(tag: Tag, selected: Boolean, onToggle: (Long, Boolean) -> Unit) {
    FilterChip(
        selected = selected,
        onClick = { onToggle(tag.id, !selected) },
        label = { Text(tag.name) },
        leadingIcon = if (selected) {
            {
                Icon(
                    Icons.Default.Check,
                    contentDescription = null,
                    modifier = Modifier.size(FilterChipDefaults.IconSize),
                )
            }
        } else {
            null
        },
    )
}

private val previewTags = listOf(Tag(id = 1, name = "Languages"), Tag(id = 2, name = "Science"))

@LightDarkPreviews
@Composable
private fun DeckEditorNewPreview() {
    FlashbackTheme(dynamicColor = false) {
        DeckEditorScreen(
            state = DeckEditorUiState(ready = true, tags = previewTags),
            onBack = {}, onNameChange = {}, onDescriptionChange = {}, onReverseChange = {},
            onNewPerDayChange = {}, onReviewsPerDayChange = {}, onTagToggle = { _, _ -> },
            onNewTag = {}, onSave = {},
        )
    }
}

@LightDarkPreviews
@Composable
private fun DeckEditorErrorsPreview() {
    FlashbackTheme(dynamicColor = false) {
        DeckEditorScreen(
            state = DeckEditorUiState(
                isEditing = true,
                ready = true,
                form = DeckEditorForm(
                    name = "", description = "Core 2000 word list", reverseByDefault = true,
                    newPerDay = "", reviewsPerDay = "20000", tagIds = setOf(1L),
                ),
                errors = DeckEditorErrors(
                    nameRequired = true,
                    newPerDay = LimitError.Required,
                    reviewsPerDay = LimitError.TooLarge,
                ),
                tags = previewTags,
                isDirty = true,
            ),
            onBack = {}, onNameChange = {}, onDescriptionChange = {}, onReverseChange = {},
            onNewPerDayChange = {}, onReviewsPerDayChange = {}, onTagToggle = { _, _ -> },
            onNewTag = {}, onSave = {},
        )
    }
}

@LightDarkPreviews
@Composable
private fun DeckEditorNoTagsPreview() {
    FlashbackTheme(dynamicColor = false) {
        DeckEditorScreen(
            state = DeckEditorUiState(ready = true),
            onBack = {}, onNameChange = {}, onDescriptionChange = {}, onReverseChange = {},
            onNewPerDayChange = {}, onReviewsPerDayChange = {}, onTagToggle = { _, _ -> },
            onNewTag = {}, onSave = {},
        )
    }
}
