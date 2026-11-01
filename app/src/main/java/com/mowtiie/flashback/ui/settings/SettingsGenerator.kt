package com.mowtiie.flashback.ui.settings

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.TextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import com.mowtiie.flashback.R
import com.mowtiie.flashback.generator.GeneratorProvider

@Composable
internal fun GeneratorSection(state: SettingsUiState, actions: SettingsActions) {
    var editing by rememberSaveable { mutableStateOf(false) }
    val provider = state.generatorProvider

    SectionHeader(R.string.settings_generator_header)
    SettingsCard {
        ChoiceRow(
            title = stringResource(R.string.settings_generator_provider),
            options = GeneratorProvider.entries,
            selected = provider,
            label = { it.displayName },
            onSelect = actions.onGeneratorProvider,
        )
        RowDivider()
        if (provider.usesNetwork) {
            ActionRow(
                title = stringResource(R.string.settings_generator_key_title, provider.displayName),
                value = state.generatorKeyHint ?: stringResource(R.string.settings_generator_key_value_unset),
                valueHighlighted = state.generatorKeyHint != null,
                onClick = { editing = true },
            )
        } else {
            ActionRow(
                title = stringResource(R.string.settings_generator_model_title),
                value = when {
                    state.localModelBusy -> stringResource(R.string.settings_generator_model_copying)
                    else -> state.localModelName ?: stringResource(R.string.settings_generator_model_unset)
                },
                valueHighlighted = state.localModelName != null,
                enabled = !state.localModelBusy,
                onClick = actions.onChooseModel,
            )
            if (state.localModelName != null) {
                RowDivider()
                ActionRow(
                    title = stringResource(R.string.settings_generator_model_remove),
                    onClick = actions.onClearModel,
                    enabled = !state.localModelBusy,
                    destructive = true,
                )
            }
        }
    }
    Footnote(stringResource(provider.noteRes()))

    if (editing) {
        ApiKeyDialog(
            provider = provider,
            hasKey = state.generatorKeyHint != null,
            onSave = {
                editing = false
                actions.onGeneratorKey(it)
            },
            onRemove = {
                editing = false
                actions.onClearGeneratorKey()
            },
            onDismiss = { editing = false },
        )
    }
}

private fun GeneratorProvider.noteRes(): Int = when (this) {
    GeneratorProvider.Anthropic -> R.string.settings_generator_note_anthropic
    GeneratorProvider.Gemini -> R.string.settings_generator_note_gemini
    GeneratorProvider.OnDevice -> R.string.settings_generator_note_on_device
}

@Composable
private fun ApiKeyDialog(
    provider: GeneratorProvider,
    hasKey: Boolean,
    onSave: (String) -> Unit,
    onRemove: () -> Unit,
    onDismiss: () -> Unit,
) {
    // Not rememberSaveable: the secret should not land in saved instance state.
    var key by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.settings_generator_key_title, provider.displayName)) },
        text = {
            TextField(
                value = key,
                onValueChange = { key = it },
                label = { Text(stringResource(R.string.settings_generator_key_hint)) },
                singleLine = true,
                visualTransformation = PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(onDone = { if (key.isNotBlank()) onSave(key) }),
                modifier = Modifier.fillMaxWidth(),
            )
        },
        confirmButton = {
            TextButton(onClick = { onSave(key) }, enabled = key.isNotBlank()) {
                Text(stringResource(R.string.action_save))
            }
        },
        dismissButton = {
            Row {
                if (hasKey) {
                    TextButton(onClick = onRemove) { Text(stringResource(R.string.settings_generator_key_remove)) }
                }
                TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_cancel)) }
            }
        },
    )
}
