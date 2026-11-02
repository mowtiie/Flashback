package com.mowtiie.flashback.ui.generator

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Card
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.mowtiie.flashback.R
import com.mowtiie.flashback.ui.components.ConfirmDialog
import com.mowtiie.flashback.ui.components.asHeading

/** Shown while sections are sent: which one, the cards so far, and a way to stop. */
@Composable
internal fun Working(state: GeneratorUiState, onCancel: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize().padding(horizontal = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        if (state.sectionTotal > 1) {
            val current = (state.sectionsFinished + 1).coerceAtMost(state.sectionTotal)
            LinearProgressIndicator(
                progress = { state.sectionsFinished.toFloat() / state.sectionTotal },
                modifier = Modifier.fillMaxWidth(),
            )
            Text(
                text = stringResource(R.string.generator_progress, current, state.sectionTotal),
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.padding(top = 16.dp).semantics { liveRegion = LiveRegionMode.Polite },
            )
            if (state.cards.isNotEmpty()) {
                Text(
                    text = pluralStringResource(R.plurals.generator_cards_so_far, state.cards.size, state.cards.size),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        } else {
            LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
            Text(
                text = stringResource(R.string.generator_working),
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.padding(top = 16.dp),
            )
        }
        OutlinedButton(onClick = onCancel, modifier = Modifier.padding(top = 24.dp)) {
            Text(stringResource(R.string.action_cancel))
        }
    }
}

/** Before a run of many requests, so nobody spends their own key by accident. */
@Composable
internal fun ConfirmRunDialog(state: GeneratorUiState, onConfirm: () -> Unit, onDismiss: () -> Unit) {
    ConfirmDialog(
        title = stringResource(R.string.generator_confirm_title),
        body = pluralStringResource(
            R.plurals.generator_confirm_body,
            state.sectionTotal,
            state.sectionTotal,
            state.provider.displayName,
        ),
        confirmLabel = stringResource(R.string.generator_confirm_send),
        dismissLabel = stringResource(R.string.action_cancel),
        onConfirm = onConfirm,
        onDismiss = onDismiss,
    )
}

/** Which sections made how many cards, and the ones that failed, with one Retry. */
@Composable
internal fun SectionSummary(state: GeneratorUiState, enabled: Boolean, onRetry: () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        if (state.sectionCounts.size > 1) {
            Text(
                text = stringResource(
                    R.string.generator_from_sections,
                    state.sectionCounts.size,
                    state.sectionCounts.joinToString(" · "),
                ),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        if (state.failedSections.isNotEmpty()) {
            Card(Modifier.fillMaxWidth().semantics { liveRegion = LiveRegionMode.Polite }) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        text = pluralStringResource(
                            R.plurals.generator_failed_title,
                            state.failedSections.size,
                            state.failedSections.size,
                        ),
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier.asHeading(),
                    )
                    state.failedSections.take(MAX_FAILED_ROWS).forEach {
                        Text(
                            text = stringResource(R.string.generator_failed_row, it.number, it.message),
                            style = MaterialTheme.typography.bodySmall,
                        )
                    }
                    Button(onClick = onRetry, enabled = enabled, modifier = Modifier.fillMaxWidth().padding(top = 8.dp)) {
                        Text(
                            pluralStringResource(
                                R.plurals.generator_retry,
                                state.failedSections.size,
                                state.failedSections.size,
                            ),
                        )
                    }
                }
            }
        }
    }
}

private const val MAX_FAILED_ROWS = 5
