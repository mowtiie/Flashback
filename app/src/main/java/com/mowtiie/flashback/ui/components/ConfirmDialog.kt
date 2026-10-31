package com.mowtiie.flashback.ui.components

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.mowtiie.flashback.R

@Composable
fun ConfirmDialog(
    title: String,
    body: String,
    confirmLabel: String,
    dismissLabel: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = { Text(body) },
        dismissButton = { TextButton(onClick = onDismiss) { Text(dismissLabel) } },
        confirmButton = { TextButton(onClick = onConfirm) { Text(confirmLabel) } },
    )
}

/** Shown before an editor throws away edits; both back and Up land here. */
@Composable
fun DiscardChangesDialog(onDiscard: () -> Unit, onKeepEditing: () -> Unit) {
    ConfirmDialog(
        title = stringResource(R.string.unsaved_title),
        body = stringResource(R.string.unsaved_body),
        confirmLabel = stringResource(R.string.unsaved_discard),
        dismissLabel = stringResource(R.string.unsaved_keep),
        onConfirm = onDiscard,
        onDismiss = onKeepEditing,
    )
}

@Composable
fun DeleteDialog(title: String, body: String, onDelete: () -> Unit, onDismiss: () -> Unit) {
    ConfirmDialog(
        title = title,
        body = body,
        confirmLabel = stringResource(R.string.action_delete),
        dismissLabel = stringResource(R.string.action_cancel),
        onConfirm = onDelete,
        onDismiss = onDismiss,
    )
}
