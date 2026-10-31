package com.mowtiie.flashback.ui.settings

import androidx.compose.runtime.Immutable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mowtiie.flashback.data.io.ArchiveException
import com.mowtiie.flashback.data.io.ArchiveParser
import com.mowtiie.flashback.data.io.DeckArchive
import com.mowtiie.flashback.data.io.ImportResult
import com.mowtiie.flashback.repository.ArchiveDataSource
import com.mowtiie.flashback.ui.export.ExportSession
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
data class DataTransferUiState(
    /** An export or import is running. */
    val working: Boolean = false,
    val pendingImport: PendingImport? = null,
    /** Shown inside the paste dialog, which stays open on a parse error. */
    val pasteError: String? = null,
)

sealed interface DataTransferEvent {
    /** The export text is ready; the screen asks where to save it. */
    data object ExportReady : DataTransferEvent

    data object ExportNothing : DataTransferEvent

    data object ExportSaved : DataTransferEvent

    data object ExportWriteFailed : DataTransferEvent

    class ImportRejected(val message: String) : DataTransferEvent

    class ImportReadFailed(val message: String?) : DataTransferEvent

    class ImportDone(val result: ImportResult) : DataTransferEvent

    data object ImportFailed : DataTransferEvent
}

/** Export and two-stage import: parse and preview first, write only after the user confirms. */
class DataTransferViewModel(
    private val archive: ArchiveDataSource,
    private val ioDispatcher: CoroutineDispatcher,
) : ViewModel() {

    private val parser = ArchiveParser()
    private val export = ExportSession(ioDispatcher)

    private val _uiState = MutableStateFlow(DataTransferUiState())
    val uiState: StateFlow<DataTransferUiState> = _uiState.asStateFlow()

    private val events = Channel<DataTransferEvent>(Channel.BUFFERED)
    val eventFlow = events.receiveAsFlow()

    fun prepareExportAll() {
        if (_uiState.value.working) return
        viewModelScope.launch {
            setWorking(true)
            val text = archive.exportAllText()
            setWorking(false)
            if (text == null) {
                events.send(DataTransferEvent.ExportNothing)
            } else {
                export.hold(text)
                events.send(DataTransferEvent.ExportReady)
            }
        }
    }

    /** [writer] is blocking and runs off the main thread. */
    fun saveExport(writer: (String) -> Unit) {
        viewModelScope.launch {
            setWorking(true)
            val saved = export.write(writer)
            setWorking(false)
            events.send(if (saved) DataTransferEvent.ExportSaved else DataTransferEvent.ExportWriteFailed)
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
                events.send(DataTransferEvent.ImportReadFailed(e.message))
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
                events.send(DataTransferEvent.ImportRejected(message))
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
            events.send(if (result == null) DataTransferEvent.ImportFailed else DataTransferEvent.ImportDone(result))
        }
    }

    private fun setWorking(working: Boolean) {
        _uiState.update { it.copy(working = working) }
    }
}
