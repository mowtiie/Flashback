package com.mowtiie.flashback.ui.settings

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.unit.dp
import com.mowtiie.flashback.R

@Composable
internal fun GeneratorSection(state: SettingsUiState, actions: SettingsActions) {
    var editing by rememberSaveable { mutableStateOf(false) }

    SectionHeader(R.string.settings_generator_header)
    SettingsCard {
        ActionRow(
            title = stringResource(R.string.settings_generator_key_title),
            body = state.generatorKeyHint?.let { stringResource(R.string.settings_generator_key_saved, it) }
                ?: stringResource(R.string.settings_generator_key_unset),
            enabled = true,
            onClick = { editing = true },
        )
        Text(
            text = stringResource(R.string.settings_generator_note),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
        )
    }

    if (editing) {
        ApiKeyDialog(
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

@Composable
private fun ApiKeyDialog(hasKey: Boolean, onSave: (String) -> Unit, onRemove: () -> Unit, onDismiss: () -> Unit) {
    // Not rememberSaveable: the secret should not land in saved instance state.
    var key by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.settings_generator_key_title)) },
        text = {
            OutlinedTextField(
                value = key,
                onValueChange = { key = it },
                label = { Text(stringResource(R.string.settings_generator_key_hint)) },
                singleLine = true,
                visualTransformation = PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                modifier = Modifier.fillMaxWidth(),
            )
        },
        confirmButton = {
            TextButton(onClick = { onSave(key) }, enabled = key.isNotBlank()) {
                Text(stringResource(R.string.action_save))
            }
        },
        dismissButton = {
            if (hasKey) {
                TextButton(onClick = onRemove) { Text(stringResource(R.string.settings_generator_key_remove)) }
            } else {
                TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_cancel)) }
            }
        },
    )
}
