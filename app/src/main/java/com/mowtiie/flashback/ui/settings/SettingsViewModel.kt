package com.mowtiie.flashback.ui.settings

import androidx.compose.runtime.Immutable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mowtiie.flashback.data.io.ArchiveException
import com.mowtiie.flashback.data.io.ArchiveParser
import com.mowtiie.flashback.data.io.DeckArchive
import com.mowtiie.flashback.data.io.ImportResult
import com.mowtiie.flashback.notifications.ReminderPreferences
import com.mowtiie.flashback.notifications.ReminderScheduling
import com.mowtiie.flashback.repository.ArchiveDataSource
import com.mowtiie.flashback.theme.ContrastLevel
import com.mowtiie.flashback.theme.ThemeMode
import com.mowtiie.flashback.theme.ThemePreferences
import com.mowtiie.flashback.ui.export.ExportSession
import com.mowtiie.flashback.ui.study.AnswerMode
import com.mowtiie.flashback.ui.study.StudyPreferences
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.IOException

/** A parsed file waiting for the user to confirm; nothing is written until they do. */
@Immutable
class PendingImport(val archive: DeckArchive) {
    val counts: DeckArchive.Counts = archive.counts()
}

@Immutable
data class SettingsUiState(
    val answerMode: AnswerMode = AnswerMode.Flip,
    val themeMode: ThemeMode = ThemeMode.System,
    val dynamicColorAvailable: Boolean = false,
    val dynamicColor: Boolean = false,
    val contrast: ContrastLevel = ContrastLevel.Standard,
    val reminderEnabled: Boolean = false,
    val reminderHour: Int = 20,
    val reminderMinute: Int = 0,
    /** An export or import is running. */
    val working: Boolean = false,
    val pendingImport: PendingImport? = null,
    /** Shown inside the paste dialog, which stays open on a parse error. */
    val pasteError: String? = null,
) {
    /** Dynamic colour brings its own palette, so the contrast choice is ignored while it is on. */
    val contrastEnabled: Boolean get() = !dynamicColor
}

sealed interface SettingsEvent {
    data object NotificationsDenied : SettingsEvent

    /** The export text is ready; the screen asks where to save it. */
    data object ExportReady : SettingsEvent

    data object ExportNothing : SettingsEvent

    data object ExportSaved : SettingsEvent

    data object ExportWriteFailed : SettingsEvent

    class ImportRejected(val message: String) : SettingsEvent

    class ImportReadFailed(val message: String?) : SettingsEvent

    class ImportDone(val result: ImportResult) : SettingsEvent

    data object ImportFailed : SettingsEvent
}

class SettingsViewModel(
    private val studyPrefs: StudyPreferences,
    private val themePrefs: ThemePreferences,
    private val reminderPrefs: ReminderPreferences,
    private val scheduler: ReminderScheduling,
    private val archive: ArchiveDataSource,
    private val ioDispatcher: CoroutineDispatcher,
    dynamicColorAvailable: Boolean,
) : ViewModel() {

    private val parser = ArchiveParser()
    private val export = ExportSession(ioDispatcher)

    private val _uiState = MutableStateFlow(
        themePrefs.settings.value.let { theme ->
            SettingsUiState(
                answerMode = studyPrefs.answerMode,
                themeMode = theme.mode,
                dynamicColorAvailable = dynamicColorAvailable,
                dynamicColor = dynamicColorAvailable && theme.dynamicColor,
                contrast = theme.contrast,
                reminderEnabled = reminderPrefs.enabled,
                reminderHour = reminderPrefs.hour,
                reminderMinute = reminderPrefs.minute,
            )
        },
    )
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    private val events = Channel<SettingsEvent>(Channel.BUFFERED)
    val eventFlow = events.receiveAsFlow()

    fun setAnswerMode(mode: AnswerMode) {
        studyPrefs.answerMode = mode
        _uiState.update { it.copy(answerMode = mode) }
    }

    fun setThemeMode(mode: ThemeMode) {
        themePrefs.setMode(mode)
        _uiState.update { it.copy(themeMode = mode) }
    }

    fun setDynamicColor(enabled: Boolean) {
        if (!_uiState.value.dynamicColorAvailable) return
        themePrefs.setDynamicColor(enabled)
        _uiState.update { it.copy(dynamicColor = enabled) }
    }

    fun setContrast(contrast: ContrastLevel) {
        themePrefs.setContrast(contrast)
        _uiState.update { it.copy(contrast = contrast) }
    }

    /** The screen asks for the notification permission first when turning this on. */
    fun setReminderEnabled(enabled: Boolean) {
        reminderPrefs.enabled = enabled
        _uiState.update { it.copy(reminderEnabled = enabled) }
        reschedule()
    }

    fun setReminderTime(hour: Int, minute: Int) {
        reminderPrefs.setTime(hour, minute)
        _uiState.update { it.copy(reminderHour = hour, reminderMinute = minute) }
        if (_uiState.value.reminderEnabled) reschedule()
    }

    fun onNotificationsDenied() {
        events.trySend(SettingsEvent.NotificationsDenied)
    }

    fun prepareExportAll() {
        if (_uiState.value.working) return
        viewModelScope.launch {
            setWorking(true)
            val text = archive.exportAllText()
            setWorking(false)
            if (text == null) {
                events.send(SettingsEvent.ExportNothing)
            } else {
                export.hold(text)
                events.send(SettingsEvent.ExportReady)
            }
        }
    }

    /** [writer] is blocking and runs off the main thread. */
    fun saveExport(writer: (String) -> Unit) {
        viewModelScope.launch {
            setWorking(true)
            val saved = export.write(writer)
            setWorking(false)
            events.send(if (saved) SettingsEvent.ExportSaved else SettingsEvent.ExportWriteFailed)
        }
    }

    /** The user closed the save dialog without choosing a place. */
    fun discardExport() = export.discard()

    /** [reader] is blocking and runs off the main thread. */
    fun previewImportFromFile(reader: () -> String) {
        viewModelScope.launch {
            setWorking(true)
            val raw = try {
                withContext(ioDispatcher) { reader() }
            } catch (e: IOException) {
                setWorking(false)
                events.send(SettingsEvent.ImportReadFailed(e.message))
                return@launch
            }
            stagePreview(raw, inline = false)
            setWorking(false)
        }
    }

    fun previewImportFromText(raw: String) {
        viewModelScope.launch {
            setWorking(true)
            stagePreview(raw, inline = true)
            setWorking(false)
        }
    }

    private suspend fun stagePreview(raw: String, inline: Boolean) {
        try {
            val parsed = withContext(ioDispatcher) { parser.parse(raw) }
            _uiState.update { it.copy(pendingImport = PendingImport(parsed), pasteError = null) }
        } catch (e: ArchiveException) {
            val message = e.message.orEmpty()
            if (inline) {
                _uiState.update { it.copy(pasteError = message) }
            } else {
                events.send(SettingsEvent.ImportRejected(message))
            }
        }
    }

    fun clearPasteError() {
        _uiState.update { it.copy(pasteError = null) }
    }

    fun clearPendingImport() {
        _uiState.update { it.copy(pendingImport = null) }
    }

    fun confirmImport() {
        val pending = _uiState.value.pendingImport ?: return
        _uiState.update { it.copy(pendingImport = null) }
        viewModelScope.launch {
            setWorking(true)
            // The user has confirmed, so leaving the screen must not roll the import back.
            val result = withContext(NonCancellable) { archive.importArchive(pending.archive) }
            setWorking(false)
            events.send(if (result == null) SettingsEvent.ImportFailed else SettingsEvent.ImportDone(result))
        }
    }

    private fun setWorking(working: Boolean) {
        _uiState.update { it.copy(working = working) }
    }

    private fun reschedule() {
        viewModelScope.launch { withContext(ioDispatcher + NonCancellable) { scheduler.reschedule() } }
    }
}
