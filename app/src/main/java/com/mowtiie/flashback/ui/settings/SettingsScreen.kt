package com.mowtiie.flashback.ui.settings

import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.mowtiie.flashback.R
import com.mowtiie.flashback.data.io.ArchiveFiles
import com.mowtiie.flashback.theme.ContrastLevel
import com.mowtiie.flashback.theme.ThemeMode
import com.mowtiie.flashback.ui.components.CenteredScrollColumn
import com.mowtiie.flashback.ui.components.showReplacing
import com.mowtiie.flashback.ui.export.JSON_MIME_TYPE
import com.mowtiie.flashback.ui.export.rememberSaveDocumentLauncher
import com.mowtiie.flashback.ui.study.AnswerMode
import kotlinx.coroutines.launch

class SettingsActions(
    val onAnswerMode: (AnswerMode) -> Unit = {},
    val onThemeMode: (ThemeMode) -> Unit = {},
    val onDynamicColor: (Boolean) -> Unit = {},
    val onContrast: (ContrastLevel) -> Unit = {},
    val onReminderEnabled: (Boolean) -> Unit = {},
    val onReminderTime: (hour: Int, minute: Int) -> Unit = { _, _ -> },
)

@Composable
fun SettingsRoute(viewModel: SettingsViewModel, transferViewModel: DataTransferViewModel) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val transferState by transferViewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { granted ->
        if (granted) viewModel.setReminderEnabled(true) else viewModel.onNotificationsDenied()
    }

    val saveLauncher = rememberSaveDocumentLauncher(
        onChosen = transferViewModel::saveExport,
        onCancelled = transferViewModel::discardExport,
    )

    val openLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument(),
    ) { uri ->
        if (uri != null) {
            transferViewModel.previewImportFromFile { ArchiveFiles.read(context.contentResolver, uri) }
        }
    }

    LaunchedEffect(viewModel) {
        viewModel.eventFlow.collect { event ->
            when (event) {
                SettingsEvent.NotificationsDenied -> launch {
                    snackbarHostState.showReplacing(
                        message = context.getString(R.string.reminder_permission_denied),
                        duration = SnackbarDuration.Long,
                    )
                }
            }
        }
    }

    LaunchedEffect(transferViewModel) {
        transferViewModel.eventFlow.collect { event ->
            val message = event.message(context)
            if (message == null) {
                saveLauncher.launch(context.getString(R.string.export_all_filename))
            } else {
                launch {
                    snackbarHostState.showReplacing(
                        message = message,
                        duration = if (event is DataTransferEvent.ExportSaved) SnackbarDuration.Short else SnackbarDuration.Long,
                    )
                }
            }
        }
    }

    val actions = remember(viewModel, context) {
        SettingsActions(
            onAnswerMode = viewModel::setAnswerMode,
            onThemeMode = viewModel::setThemeMode,
            onDynamicColor = viewModel::setDynamicColor,
            onContrast = viewModel::setContrast,
            // Asked on opt-in rather than at first launch.
            onReminderEnabled = { enabled ->
                if (enabled && !hasNotificationPermission(context)) {
                    permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                } else {
                    viewModel.setReminderEnabled(enabled)
                }
            },
            onReminderTime = viewModel::setReminderTime,
        )
    }

    val transferActions = remember(transferViewModel) {
        DataTransferActions(
            onExport = transferViewModel::prepareExportAll,
            // A broad filter, because many exporters mislabel JSON as text or octet-stream.
            onImportFile = { openLauncher.launch(arrayOf(JSON_MIME_TYPE, "text/plain", "*/*")) },
            onPreviewPaste = transferViewModel::previewImportFromText,
            onPasteEdited = transferViewModel::clearPasteError,
            onConfirmImport = transferViewModel::confirmImport,
            onCancelImport = transferViewModel::clearPendingImport,
        )
    }

    SettingsScreen(state, transferState, actions, transferActions, snackbarHostState)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    state: SettingsUiState,
    transferState: DataTransferUiState,
    actions: SettingsActions,
    transferActions: DataTransferActions,
    snackbarHostState: SnackbarHostState = remember { SnackbarHostState() },
) {
    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior()

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            Column {
                TopAppBar(
                    title = { Text(stringResource(R.string.settings_title)) },
                    scrollBehavior = scrollBehavior,
                )
                if (transferState.working) LinearProgressIndicator(Modifier.fillMaxWidth())
            }
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { padding ->
        CenteredScrollColumn(Modifier.padding(padding)) {
            StudySection(state, actions)
            AppearanceSection(state, actions)
            RemindersSection(state, actions)
            DataSection(transferState, transferActions)
        }
    }
}

@Composable
private fun StudySection(state: SettingsUiState, actions: SettingsActions) {
    SectionHeader(R.string.settings_study_header)
    SettingsCard {
        ChoiceRow(
            title = stringResource(R.string.settings_answer_mode),
            options = AnswerMode.entries,
            selected = state.answerMode,
            label = { stringResource(it.labelRes()) },
            onSelect = actions.onAnswerMode,
        )
    }
}

@Composable
private fun AppearanceSection(state: SettingsUiState, actions: SettingsActions) {
    SectionHeader(R.string.settings_appearance_header)
    SettingsCard {
        ChoiceRow(
            title = stringResource(R.string.settings_theme_mode),
            options = ThemeMode.entries,
            selected = state.themeMode,
            label = { stringResource(it.labelRes()) },
            onSelect = actions.onThemeMode,
        )
        if (state.dynamicColorAvailable) {
            SwitchRow(
                title = stringResource(R.string.settings_dynamic_color),
                body = stringResource(R.string.settings_dynamic_color_body),
                checked = state.dynamicColor,
                onCheckedChange = actions.onDynamicColor,
            )
        }
        ChoiceRow(
            title = stringResource(R.string.settings_contrast),
            options = ContrastLevel.entries,
            selected = state.contrast,
            label = { stringResource(it.labelRes()) },
            onSelect = actions.onContrast,
            enabled = state.contrastEnabled,
            hint = if (state.contrastEnabled) null else stringResource(R.string.settings_contrast_dynamic_hint),
        )
    }
}

private fun AnswerMode.labelRes(): Int = when (this) {
    AnswerMode.Flip -> R.string.answer_mode_flip
    AnswerMode.Type -> R.string.answer_mode_type
    AnswerMode.Choice -> R.string.answer_mode_choice
}

private fun ThemeMode.labelRes(): Int = when (this) {
    ThemeMode.System -> R.string.theme_mode_system
    ThemeMode.Light -> R.string.theme_mode_light
    ThemeMode.Dark -> R.string.theme_mode_dark
}

private fun ContrastLevel.labelRes(): Int = when (this) {
    ContrastLevel.Standard -> R.string.contrast_standard
    ContrastLevel.Medium -> R.string.contrast_medium
    ContrastLevel.High -> R.string.contrast_high
}
