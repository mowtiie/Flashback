package com.mowtiie.flashback.ui.notes

import androidx.compose.runtime.Immutable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mowtiie.flashback.data.entity.Note
import com.mowtiie.flashback.repository.FlashbackRepository
import com.mowtiie.flashback.ui.navigation.Routes
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@Immutable
data class NoteEditorUiState(
    val isEditing: Boolean = false,
    /** False while the deck default or an existing card is still loading. */
    val ready: Boolean = false,
    val form: NoteEditorForm = NoteEditorForm(),
    val error: NoteEditorError? = null,
    val isDirty: Boolean = false,
    val saving: Boolean = false,
)

sealed interface NoteEditorEvent {
    data object Saved : NoteEditorEvent

    /** "Save and add another": the form was cleared and the front field should take focus. */
    data object ReadyForNext : NoteEditorEvent

    class Deleted(val undo: FlashbackRepository.UndoAction) : NoteEditorEvent

    /** The deck or card no longer exists. */
    data object NotFound : NoteEditorEvent
}

class NoteEditorViewModel(
    private val repository: FlashbackRepository,
    private val deckId: Long,
    private val noteId: Long,
) : ViewModel() {

    private val isEditing = noteId != NEW_ID

    private var loadedNote: Note? = null

    private val form = MutableStateFlow(NoteEditorForm())
    private val error = MutableStateFlow<NoteEditorError?>(null)
    private val saving = MutableStateFlow(false)

    /** What unsaved changes are measured against; null until the starting values are known. */
    private val baseline = MutableStateFlow<NoteEditorForm?>(null)

    private val events = Channel<NoteEditorEvent>(Channel.BUFFERED)
    val eventFlow = events.receiveAsFlow()

    val uiState: StateFlow<NoteEditorUiState> = combine(
        form, error, saving, baseline,
    ) { form, error, saving, baseline ->
        NoteEditorUiState(
            isEditing = isEditing,
            ready = baseline != null,
            form = form,
            error = error,
            isDirty = baseline != null && baseline.normalized() != form.normalized(),
            saving = saving,
        )
    }.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS),
        NoteEditorUiState(isEditing = isEditing),
    )

    init {
        load()
    }

    private fun load() = viewModelScope.launch {
        val starting = if (isEditing) {
            val note = repository.getNote(noteId)
            if (note == null) {
                events.send(NoteEditorEvent.NotFound)
                return@launch
            }
            loadedNote = note
            NoteEditorForm.from(note, repository.isNoteSuspended(noteId))
        } else {
            val deck = repository.getDeck(deckId)
            if (deck == null) {
                events.send(NoteEditorEvent.NotFound)
                return@launch
            }
            NoteEditorForm(reverseEnabled = deck.reverseByDefault)
        }
        form.value = starting
        baseline.value = starting
    }

    fun onFrontChange(value: String) {
        form.update { it.copy(front = value) }
        error.update { if (it == NoteEditorError.FrontRequired) null else it }
    }

    fun onBackChange(value: String) {
        form.update { it.copy(back = value) }
        error.update { if (it == NoteEditorError.BackRequired) null else it }
    }

    fun onReverseChange(value: Boolean) = form.update { it.copy(reverseEnabled = value) }

    fun onSuspendedChange(value: Boolean) = form.update { it.copy(suspended = value) }

    /** Removing a reverse card deletes its scheduling history. */
    fun save(addAnother: Boolean) {
        if (saving.value) return
        val current = form.value
        val problem = current.validate()
        error.value = problem
        if (problem != null) return

        saving.value = true
        viewModelScope.launch {
            val existing = loadedNote
            if (existing != null) {
                repository.updateNote(current.applyTo(existing), suspended = current.suspended)
            } else {
                repository.addNote(current.toNewNote(deckId))
            }
            saving.value = false

            if (addAnother) {
                val next = current.clearedForNext()
                form.value = next
                baseline.value = next
                events.send(NoteEditorEvent.ReadyForNext)
            } else {
                events.send(NoteEditorEvent.Saved)
            }
        }
    }

    fun delete() {
        val note = loadedNote ?: return
        viewModelScope.launch {
            events.send(NoteEditorEvent.Deleted(repository.deleteNote(note)))
        }
    }

    companion object {
        const val NEW_ID = Routes.NEW_ID
        private const val STOP_TIMEOUT_MS = 5_000L
    }
}
