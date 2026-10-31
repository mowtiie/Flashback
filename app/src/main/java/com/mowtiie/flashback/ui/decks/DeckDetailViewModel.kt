package com.mowtiie.flashback.ui.decks

import androidx.compose.runtime.Immutable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mowtiie.flashback.data.entity.Deck
import com.mowtiie.flashback.data.model.NotePreview
import com.mowtiie.flashback.repository.FlashbackRepository
import com.mowtiie.flashback.ui.export.ExportSession
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@Immutable
data class DeckDetailUiState(
    val loaded: Boolean = false,
    /** Null once the deck has been deleted. */
    val deck: Deck? = null,
    val notes: List<NotePreview> = emptyList(),
    val suspendedNoteIds: Set<Long> = emptySet(),
    val studyableCount: Int = 0,
)

sealed interface DeckDetailEvent {
    /** [undo] puts the deck, its cards and review history back. */
    class Deleted(val deckName: String, val undo: FlashbackRepository.UndoAction) : DeckDetailEvent

    /** The export text is ready; the screen asks where to save it. */
    class ExportReady(val deckName: String) : DeckDetailEvent

    data object ExportNothing : DeckDetailEvent

    data object ExportSaved : DeckDetailEvent

    data object ExportWriteFailed : DeckDetailEvent
}

@OptIn(ExperimentalCoroutinesApi::class)
class DeckDetailViewModel(
    private val repository: FlashbackRepository,
    private val deckId: Long,
    ioDispatcher: CoroutineDispatcher,
    private val clock: () -> Long = System::currentTimeMillis,
) : ViewModel() {

    /** Reissued on resume so counts aren't stale after a session. */
    private val now = MutableStateFlow(clock())

    private val export = ExportSession(ioDispatcher)

    private val events = Channel<DeckDetailEvent>(Channel.BUFFERED)
    val eventFlow = events.receiveAsFlow()

    val uiState: StateFlow<DeckDetailUiState> = combine(
        repository.observeDeck(deckId),
        repository.observeNotes(deckId),
        repository.observeSuspendedNoteIds(deckId),
        now.flatMapLatest { repository.observeDeckSummary(deckId, it) },
    ) { deck, notes, suspendedIds, summary ->
        DeckDetailUiState(
            loaded = true,
            deck = deck,
            notes = notes,
            suspendedNoteIds = suspendedIds.toSet(),
            studyableCount = summary?.studyableCount() ?: 0,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), DeckDetailUiState())

    fun refresh() {
        now.value = clock()
    }

    fun exportDeck() {
        val deck = uiState.value.deck ?: return
        viewModelScope.launch {
            val text = repository.exportDeckText(deckId)
            if (text == null) {
                events.send(DeckDetailEvent.ExportNothing)
            } else {
                export.hold(text)
                events.send(DeckDetailEvent.ExportReady(deck.name))
            }
        }
    }

    /** [writer] is blocking and runs off the main thread. */
    fun saveExport(writer: (String) -> Unit) {
        viewModelScope.launch {
            val saved = export.write(writer)
            events.send(if (saved) DeckDetailEvent.ExportSaved else DeckDetailEvent.ExportWriteFailed)
        }
    }

    /** The user closed the save dialog without choosing a place. */
    fun discardExport() = export.discard()

    fun deleteDeck() {
        val deck = uiState.value.deck ?: return
        viewModelScope.launch {
            val undo = repository.deleteDeck(deck)
            events.send(DeckDetailEvent.Deleted(deck.name, undo))
        }
    }

    private companion object {
        const val STOP_TIMEOUT_MS = 5_000L
    }
}
