package com.mowtiie.flashback.ui.prompt

import android.content.ClipData
import android.content.Intent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboard
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.ClipEntry
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.mowtiie.flashback.R
import com.mowtiie.flashback.ui.components.SectionTitle
import com.mowtiie.flashback.ui.components.asHeading
import com.mowtiie.flashback.generator.CardTypes
import com.mowtiie.flashback.generator.PromptCoverage
import com.mowtiie.flashback.theme.FlashbackTheme
import com.mowtiie.flashback.ui.components.CenteredScrollColumn
import com.mowtiie.flashback.ui.components.LightDarkPreviews
import com.mowtiie.flashback.ui.generator.CardTypesChoice
import com.mowtiie.flashback.ui.settings.ChoiceRow
import com.mowtiie.flashback.ui.settings.ChoiceRowStyle
import kotlinx.coroutines.launch

class PromptBuilderActions(
    val onBack: () -> Unit = {},
    val onCoverage: (PromptCoverage) -> Unit = {},
    val onCardTypes: (CardTypes) -> Unit = {},
    val onLanguage: (String) -> Unit = {},
    val onDeckName: (String) -> Unit = {},
    val onCopy: () -> Unit = {},
    val onShare: () -> Unit = {},
    val onImport: () -> Unit = {},
)

@Composable
fun PromptBuilderRoute(viewModel: PromptBuilderViewModel, onBack: () -> Unit, onImport: () -> Unit) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val clipboard = LocalClipboard.current
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    val copied = stringResource(R.string.prompt_copied)
    val chooserTitle = stringResource(R.string.prompt_share_chooser)

    val actions = PromptBuilderActions(
        onBack = onBack,
        onCoverage = viewModel::setCoverage,
        onCardTypes = viewModel::setCardTypes,
        onLanguage = viewModel::setLanguage,
        onDeckName = viewModel::setDeckName,
        onCopy = {
            scope.launch {
                clipboard.setClipEntry(ClipEntry(ClipData.newPlainText("Flashback prompt", state.prompt)))
                snackbarHostState.showSnackbar(copied)
            }
        },
        onShare = {
            val send = Intent(Intent.ACTION_SEND)
                .setType("text/plain")
                .putExtra(Intent.EXTRA_TEXT, state.prompt)
            context.startActivity(Intent.createChooser(send, chooserTitle))
        },
        onImport = onImport,
    )
    PromptBuilderScreen(state, actions, snackbarHostState)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PromptBuilderScreen(
    state: PromptBuilderUiState,
    actions: PromptBuilderActions,
    snackbarHostState: SnackbarHostState = remember { SnackbarHostState() },
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.prompt_title)) },
                navigationIcon = {
                    IconButton(onClick = actions.onBack) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.navigate_up),
                        )
                    }
                },
            )
        },
    ) { padding ->
        Box(Modifier.fillMaxSize()) {
            CenteredScrollColumn(Modifier.padding(padding)) {
                Text(stringResource(R.string.prompt_intro), style = MaterialTheme.typography.bodyMedium)
                ChoiceRow(
                    style = ChoiceRowStyle.Form,
                    title = stringResource(R.string.prompt_coverage_title),
                    options = PromptCoverage.entries,
                    selected = state.options.coverage,
                    label = { stringResource(it.labelRes()) },
                    onSelect = actions.onCoverage,
                    hint = stringResource(state.options.coverage.hintRes()),
                )
                CardTypesChoice(state.options.cardTypes, actions.onCardTypes)
                OutlinedTextField(
                    value = state.options.language,
                    onValueChange = actions.onLanguage,
                    label = { Text(stringResource(R.string.prompt_language_label)) },
                    supportingText = { Text(stringResource(R.string.prompt_language_hint)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(
                    value = state.options.deckName,
                    onValueChange = actions.onDeckName,
                    label = { Text(stringResource(R.string.prompt_name_label)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                SectionTitle(stringResource(R.string.prompt_preview_title))
                OutlinedCard(Modifier.fillMaxWidth()) {
                    Text(
                        text = state.prompt,
                        style = MaterialTheme.typography.bodySmall,
                        maxLines = PREVIEW_LINES,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.padding(12.dp),
                    )
                }
                Button(onClick = actions.onCopy, modifier = Modifier.fillMaxWidth()) {
                    Text(stringResource(R.string.prompt_copy))
                }
                OutlinedButton(onClick = actions.onShare, modifier = Modifier.fillMaxWidth()) {
                    Text(stringResource(R.string.prompt_share))
                }
                Text(
                    text = stringResource(R.string.prompt_import_hint),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                TextButton(onClick = actions.onImport, modifier = Modifier.fillMaxWidth()) {
                    Text(stringResource(R.string.prompt_import))
                }
            }
            // At the top, so it never covers the import button.
            SnackbarHost(snackbarHostState, Modifier.align(Alignment.TopCenter).padding(padding))
        }
    }
}

private const val PREVIEW_LINES = 8

private fun PromptCoverage.labelRes(): Int = when (this) {
    PromptCoverage.Everything -> R.string.prompt_coverage_everything
    PromptCoverage.KeyPoints -> R.string.prompt_coverage_key
}

private fun PromptCoverage.hintRes(): Int = when (this) {
    PromptCoverage.Everything -> R.string.prompt_coverage_hint_everything
    PromptCoverage.KeyPoints -> R.string.prompt_coverage_hint_key
}

@LightDarkPreviews
@Composable
private fun PromptBuilderPreview() {
    FlashbackTheme(dynamicColor = false) {
        PromptBuilderScreen(PromptBuilderUiState(), PromptBuilderActions())
    }
}
