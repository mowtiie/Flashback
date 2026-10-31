package com.mowtiie.flashback.ui.components

import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics

/**
 * An extended floating button whose name is set explicitly: the label inside it is not merged into the
 * button's semantics, so without this a screen reader finds a button with no name.
 */
@Composable
fun ActionFab(
    label: String,
    onClick: () -> Unit,
    icon: @Composable () -> Unit,
    modifier: Modifier = Modifier,
) {
    ExtendedFloatingActionButton(
        onClick = onClick,
        icon = icon,
        text = { Text(label) },
        modifier = modifier.semantics { contentDescription = label },
    )
}
