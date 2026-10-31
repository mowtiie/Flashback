package com.mowtiie.flashback.ui.decks

import androidx.compose.runtime.Immutable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mowtiie.flashback.data.entity.Deck
import com.mowtiie.flashback.data.model.NotePreview
import com.mowtiie.flashback.repository.FlashbackRepository
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
}

@OptIn(ExperimentalCoroutinesApi::class)
class DeckDetailViewModel(
    private val repository: FlashbackRepository,
    private val deckId: Long,
    private val clock: () -> Long = System::currentTimeMillis,
) : ViewModel() {

    /** Reissued on resume so counts aren't stale after a session. */
    private val now = MutableStateFlow(clock())

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
