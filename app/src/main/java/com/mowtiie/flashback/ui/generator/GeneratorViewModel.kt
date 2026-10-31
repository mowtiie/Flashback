package com.mowtiie.flashback.ui.generator

import androidx.compose.runtime.Immutable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mowtiie.flashback.data.io.DeckArchive
import com.mowtiie.flashback.generator.CardGenerator
import com.mowtiie.flashback.generator.GeneratorException
import com.mowtiie.flashback.generator.GeneratorProtocol
import com.mowtiie.flashback.repository.ArchiveDataSource
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

enum class GeneratorStage { Input, Generating, Review, Saving }

@Immutable
data class GeneratorUiState(
    val hasApiKey: Boolean = false,
    val stage: GeneratorStage = GeneratorStage.Input,
    val source: String = "",
    /** Shown on the input page after a failed attempt. */
    val error: String? = null,
    val deckName: String = "",
    val cards: List<DeckArchive.Note> = emptyList(),
) {
    val sourceTooLong: Boolean get() = source.length > GeneratorProtocol.MAX_SOURCE_CHARS

    val canGenerate: Boolean
        get() = hasApiKey && stage == GeneratorStage.Input && source.isNotBlank() && !sourceTooLong

    val canSave: Boolean
        get() = stage == GeneratorStage.Review && deckName.isNotBlank() && cards.isNotEmpty()
}

sealed interface GeneratorEvent {
    data object Saved : GeneratorEvent

    data object SaveFailed : GeneratorEvent
}

/** Generate, review, then save. Nothing is written until the user saves the reviewed cards. */
class GeneratorViewModel(
    private val generator: CardGenerator,
    private val archive: ArchiveDataSource,
    private val hasApiKey: () -> Boolean,
) : ViewModel() {

    private val _uiState = MutableStateFlow(GeneratorUiState(hasApiKey = hasApiKey()))
    val uiState: StateFlow<GeneratorUiState> = _uiState.asStateFlow()

    private val events = Channel<GeneratorEvent>(Channel.BUFFERED)
    val eventFlow = events.receiveAsFlow()

    /** The key is added in Settings, so the screen re-checks when it comes back. */
    fun refreshKey() {
        _uiState.update { it.copy(hasApiKey = hasApiKey()) }
    }

    fun setSource(text: String) {
        _uiState.update { it.copy(source = text, error = null) }
    }

    fun generate() {
        val state = _uiState.value
        if (!state.canGenerate) return
        _uiState.update { it.copy(stage = GeneratorStage.Generating, error = null) }
        viewModelScope.launch {
            try {
                val deck = generator.generate(state.source.trim())
                _uiState.update {
                    it.copy(stage = GeneratorStage.Review, deckName = deck.name, cards = deck.notes)
                }
            } catch (e: GeneratorException) {
                _uiState.update { it.copy(stage = GeneratorStage.Input, error = e.message) }
            }
        }
    }

    fun setDeckName(name: String) {
        _uiState.update { it.copy(deckName = name.take(MAX_DECK_NAME)) }
    }

    fun removeCard(index: Int) {
        _uiState.update { state ->
            if (index in state.cards.indices) state.copy(cards = state.cards.filterIndexed { i, _ -> i != index }) else state
        }
    }

    /** Back to the input page with the text kept, to adjust it and generate again. */
    fun startOver() {
        if (_uiState.value.stage != GeneratorStage.Review) return
        _uiState.update { it.copy(stage = GeneratorStage.Input, cards = emptyList(), deckName = "") }
    }

    fun save() {
        val state = _uiState.value
        if (!state.canSave) return
        _uiState.update { it.copy(stage = GeneratorStage.Saving) }
        viewModelScope.launch {
            val deck = DeckArchive.Deck(name = state.deckName.trim(), notes = state.cards)
            // The user has confirmed, so leaving the screen must not roll the save back.
            val result = withContext(NonCancellable) { archive.importArchive(DeckArchive(decks = listOf(deck))) }
            if (result == null) {
                _uiState.update { it.copy(stage = GeneratorStage.Review) }
                events.send(GeneratorEvent.SaveFailed)
            } else {
                events.send(GeneratorEvent.Saved)
            }
        }
    }

    private companion object {
        const val MAX_DECK_NAME = 200
    }
}
