package com.mowtiie.flashback.ui.settings

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.text.format.DateFormat
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.mowtiie.flashback.data.io.ArchiveFiles
import com.mowtiie.flashback.data.io.DeckArchive
import com.mowtiie.flashback.data.io.ImportResult
import kotlinx.coroutines.launch
import com.mowtiie.flashback.R
import com.mowtiie.flashback.theme.ContrastLevel
import com.mowtiie.flashback.theme.ThemeMode
import com.mowtiie.flashback.ui.study.AnswerMode
import java.util.Calendar

private val CONTENT_MAX_WIDTH = 720.dp
private const val JSON_MIME = "application/json"

@Composable
fun SettingsRoute(viewModel: SettingsViewModel) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { granted ->
        if (granted) viewModel.setReminderEnabled(true) else viewModel.onNotificationsDenied()
    }

    val saveLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument(JSON_MIME),
    ) { uri ->
        if (uri == null) {
            viewModel.discardExport()
        } else {
            viewModel.saveExport { text -> ArchiveFiles.write(context.contentResolver, uri, text) }
        }
    }

    val openLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument(),
    ) { uri ->
        if (uri != null) {
            viewModel.previewImportFromFile { ArchiveFiles.read(context.contentResolver, uri) }
        }
    }

    LaunchedEffect(viewModel) {
        viewModel.eventFlow.collect { event ->
            if (event is SettingsEvent.ExportReady) {
                saveLauncher.launch(context.getString(R.string.export_all_filename))
            } else {
                // Launched, so a snackbar waiting to be dismissed never holds up the next event.
                launch {
                    snackbarHostState.currentSnackbarData?.dismiss()
                    snackbarHostState.showSnackbar(
                        message = event.message(context),
                        duration = if (event is SettingsEvent.ExportSaved) SnackbarDuration.Short else SnackbarDuration.Long,
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
            onExport = viewModel::prepareExportAll,
            // A broad filter, because many exporters mislabel JSON as text or octet-stream.
            onImportFile = { openLauncher.launch(arrayOf(JSON_MIME, "text/plain", "*/*")) },
            onPreviewPaste = viewModel::previewImportFromText,
            onPasteEdited = viewModel::clearPasteError,
            onConfirmImport = viewModel::confirmImport,
            onCancelImport = viewModel::clearPendingImport,
        )
    }

    SettingsScreen(state, actions, snackbarHostState)
}

private fun hasNotificationPermission(context: Context): Boolean =
    Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
        ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) ==
        PackageManager.PERMISSION_GRANTED

class SettingsActions(
    val onAnswerMode: (AnswerMode) -> Unit = {},
    val onThemeMode: (ThemeMode) -> Unit = {},
    val onDynamicColor: (Boolean) -> Unit = {},
    val onContrast: (ContrastLevel) -> Unit = {},
    val onReminderEnabled: (Boolean) -> Unit = {},
    val onReminderTime: (hour: Int, minute: Int) -> Unit = { _, _ -> },
    val onExport: () -> Unit = {},
    val onImportFile: () -> Unit = {},
    val onPreviewPaste: (String) -> Unit = {},
    val onPasteEdited: () -> Unit = {},
    val onConfirmImport: () -> Unit = {},
    val onCancelImport: () -> Unit = {},
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    state: SettingsUiState,
    actions: SettingsActions,
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
                if (state.working) LinearProgressIndicator(Modifier.fillMaxWidth())
            }
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { padding ->
        Box(
            contentAlignment = Alignment.TopCenter,
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState()),
        ) {
            Column(
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier
                    .widthIn(max = CONTENT_MAX_WIDTH)
                    .padding(16.dp),
            ) {
                StudySection(state, actions)
                AppearanceSection(state, actions)
                RemindersSection(state, actions)
                DataSection(state, actions)
            }
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

@Composable
private fun RemindersSection(state: SettingsUiState, actions: SettingsActions) {
    var picking by rememberSaveable { mutableStateOf(false) }
    val context = LocalContext.current

    SectionHeader(R.string.settings_reminders_header)
    SettingsCard {
        SwitchRow(
            title = stringResource(R.string.reminder_toggle_title),
            body = stringResource(R.string.reminder_toggle_body),
            checked = state.reminderEnabled,
            onCheckedChange = actions.onReminderEnabled,
        )
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier
                .fillMaxWidth()
                .alpha(if (state.reminderEnabled) 1f else DISABLED_ALPHA)
                .clickable(enabled = state.reminderEnabled, role = Role.Button) { picking = true }
                .heightIn(min = 56.dp)
                .padding(horizontal = 16.dp),
        ) {
            Text(stringResource(R.string.reminder_time_title), style = MaterialTheme.typography.bodyLarge)
            Text(
                text = formatTime(context, state.reminderHour, state.reminderMinute),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.primary,
            )
        }
    }

    if (picking) {
        ReminderTimeDialog(
            hour = state.reminderHour,
            minute = state.reminderMinute,
            is24Hour = DateFormat.is24HourFormat(context),
            onConfirm = { hour, minute ->
                picking = false
                actions.onReminderTime(hour, minute)
            },
            onDismiss = { picking = false },
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ReminderTimeDialog(
    hour: Int,
    minute: Int,
    is24Hour: Boolean,
    onConfirm: (hour: Int, minute: Int) -> Unit,
    onDismiss: () -> Unit,
) {
    val pickerState = rememberTimePickerState(hour, minute, is24Hour)

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.reminder_time_title)) },
        text = { TimePicker(pickerState) },
        confirmButton = {
            TextButton(onClick = { onConfirm(pickerState.hour, pickerState.minute) }) {
                Text(stringResource(android.R.string.ok))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_cancel)) }
        },
    )
}

@Composable
private fun SectionHeader(titleRes: Int) {
    Text(
        text = stringResource(titleRes),
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(top = 8.dp),
    )
}

@Composable
private fun SettingsCard(content: @Composable () -> Unit) {
    OutlinedCard(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(vertical = 8.dp)) { content() }
    }
}

@Composable
private fun SwitchRow(
    title: String,
    body: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .toggleable(value = checked, role = Role.Switch, onValueChange = onCheckedChange)
            .heightIn(min = 56.dp)
            .padding(horizontal = 16.dp, vertical = 8.dp),
    ) {
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyLarge)
            Text(
                text = body,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Switch(checked = checked, onCheckedChange = null, modifier = Modifier.padding(start = 16.dp))
    }
}

@Composable
private fun <T> ChoiceRow(
    title: String,
    options: List<T>,
    selected: T,
    label: @Composable (T) -> String,
    onSelect: (T) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    hint: String? = null,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.alpha(if (enabled) 1f else DISABLED_ALPHA),
        )
        SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth().padding(top = 8.dp)) {
            options.forEachIndexed { index, option ->
                SegmentedButton(
                    selected = option == selected,
                    onClick = { onSelect(option) },
                    enabled = enabled,
                    shape = SegmentedButtonDefaults.itemShape(index, options.size),
                    modifier = Modifier.heightIn(min = 48.dp),
                ) {
                    Text(label(option))
                }
            }
        }
        if (hint != null) {
            Text(
                text = hint,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 8.dp),
            )
        }
    }
}

private const val DISABLED_ALPHA = 0.5f

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

private fun formatTime(context: Context, hour: Int, minute: Int): String {
    val cal = Calendar.getInstance().apply {
        set(Calendar.HOUR_OF_DAY, hour)
        set(Calendar.MINUTE, minute)
    }
    return DateFormat.getTimeFormat(context).format(cal.time)
}

@Composable
private fun DataSection(state: SettingsUiState, actions: SettingsActions) {
    var chooseSource by rememberSaveable { mutableStateOf(false) }
    var pasting by rememberSaveable { mutableStateOf(false) }
    var pasted by rememberSaveable { mutableStateOf("") }

    // A parsed paste moves on to the preview, so the paste dialog gets out of the way.
    LaunchedEffect(state.pendingImport) {
        if (state.pendingImport != null) {
            pasting = false
            pasted = ""
        }
    }

    SectionHeader(R.string.settings_data_header)
    SettingsCard {
        ActionRow(
            title = stringResource(R.string.settings_export_title),
            body = stringResource(R.string.settings_export_body),
            enabled = !state.working,
            onClick = actions.onExport,
        )
        ActionRow(
            title = stringResource(R.string.settings_import_title),
            body = stringResource(R.string.settings_import_body),
            enabled = !state.working,
            onClick = { chooseSource = true },
        )
        Text(
            text = stringResource(R.string.settings_format_note),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
        )
    }

    if (chooseSource) {
        ImportSourceDialog(
            onFile = {
                chooseSource = false
                actions.onImportFile()
            },
            onPaste = {
                chooseSource = false
                pasting = true
            },
            onDismiss = { chooseSource = false },
        )
    }

    if (pasting) {
        PasteDialog(
            text = pasted,
            error = state.pasteError,
            working = state.working,
            onTextChange = {
                pasted = it
                actions.onPasteEdited()
            },
            onPreview = { actions.onPreviewPaste(pasted) },
            onDismiss = {
                pasting = false
                pasted = ""
                actions.onPasteEdited()
            },
        )
    }

    state.pendingImport?.let { pending ->
        ImportPreviewDialog(pending.counts, actions.onConfirmImport, actions.onCancelImport)
    }
}

@Composable
private fun ActionRow(title: String, body: String, enabled: Boolean, onClick: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .alpha(if (enabled) 1f else DISABLED_ALPHA)
            .clickable(enabled = enabled, role = Role.Button, onClick = onClick)
            .heightIn(min = 56.dp)
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.Center,
    ) {
        Text(title, style = MaterialTheme.typography.bodyLarge)
        Text(
            text = body,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun ImportSourceDialog(onFile: () -> Unit, onPaste: () -> Unit, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.import_source_title)) },
        text = {
            Column {
                SourceOption(stringResource(R.string.import_source_file), onFile)
                SourceOption(stringResource(R.string.import_source_paste), onPaste)
            }
        },
        confirmButton = {},
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_cancel)) } },
    )
}

@Composable
private fun SourceOption(label: String, onClick: () -> Unit) {
    Text(
        text = label,
        style = MaterialTheme.typography.bodyLarge,
        modifier = Modifier
            .fillMaxWidth()
            .clickable(role = Role.Button, onClick = onClick)
            .heightIn(min = 48.dp)
            .padding(vertical = 12.dp),
    )
}

@Composable
private fun PasteDialog(
    text: String,
    error: String?,
    working: Boolean,
    onTextChange: (String) -> Unit,
    onPreview: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.import_paste_title)) },
        text = {
            OutlinedTextField(
                value = text,
                onValueChange = onTextChange,
                label = { Text(stringResource(R.string.import_paste_hint)) },
                isError = error != null,
                supportingText = error?.let { { Text(it) } },
                minLines = 5,
                maxLines = 8,
                modifier = Modifier.fillMaxWidth(),
            )
        },
        confirmButton = {
            TextButton(onClick = onPreview, enabled = !working) {
                Text(stringResource(R.string.import_paste_action))
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_cancel)) } },
    )
}

@Composable
private fun ImportPreviewDialog(counts: DeckArchive.Counts, onConfirm: () -> Unit, onCancel: () -> Unit) {
    val decks = pluralStringResource(R.plurals.import_decks, counts.decks, counts.decks)
    val cards = pluralStringResource(R.plurals.import_cards, counts.notes, counts.notes)
    val tags = if (counts.tags > 0) {
        pluralStringResource(R.plurals.import_preview_tags, counts.tags, counts.tags)
    } else {
        ""
    }

    AlertDialog(
        onDismissRequest = onCancel,
        title = { Text(stringResource(R.string.import_preview_title)) },
        text = { Text(stringResource(R.string.import_preview_body, decks, cards, tags)) },
        confirmButton = { TextButton(onClick = onConfirm) { Text(stringResource(R.string.import_confirm)) } },
        dismissButton = { TextButton(onClick = onCancel) { Text(stringResource(R.string.action_cancel)) } },
    )
}

private fun SettingsEvent.message(context: Context): String = when (this) {
    SettingsEvent.NotificationsDenied -> context.getString(R.string.reminder_permission_denied)
    SettingsEvent.ExportReady -> ""
    SettingsEvent.ExportNothing -> context.getString(R.string.export_nothing)
    SettingsEvent.ExportSaved -> context.getString(R.string.export_saved)
    SettingsEvent.ExportWriteFailed -> context.getString(R.string.export_write_failed)
    is SettingsEvent.ImportRejected -> message
    is SettingsEvent.ImportReadFailed -> message ?: context.getString(R.string.import_read_failed)
    SettingsEvent.ImportFailed -> context.getString(R.string.import_failed)
    is SettingsEvent.ImportDone -> importSummary(context, result)
}

private fun importSummary(context: Context, result: ImportResult): String {
    val res = context.resources
    val decks = res.getQuantityString(R.plurals.import_decks, result.decks, result.decks)
    val cards = res.getQuantityString(R.plurals.import_cards, result.notes, result.notes)
    if (result.tagsReused == 0) return res.getString(R.string.import_summary, decks, cards)
    val merged = res.getQuantityString(R.plurals.import_tags_merged, result.tagsReused, result.tagsReused)
    return res.getString(R.string.import_summary_merged, decks, cards, merged)
}
