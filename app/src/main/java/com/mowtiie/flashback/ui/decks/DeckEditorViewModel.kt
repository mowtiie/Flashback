package com.mowtiie.flashback.ui.decks

import androidx.compose.runtime.Immutable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mowtiie.flashback.data.entity.Deck
import com.mowtiie.flashback.data.entity.Tag
import com.mowtiie.flashback.repository.FlashbackRepository
import com.mowtiie.flashback.ui.navigation.Routes
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@Immutable
data class DeckEditorUiState(
    val isEditing: Boolean = false,
    /** False while an existing deck is still loading. */
    val ready: Boolean = false,
    val form: DeckEditorForm = DeckEditorForm(),
    val errors: DeckEditorErrors = DeckEditorErrors(),
    val tags: List<Tag> = emptyList(),
    val isDirty: Boolean = false,
    val saving: Boolean = false,
)

sealed interface DeckEditorEvent {
    data object Saved : DeckEditorEvent

    /** The deck being edited no longer exists. */
    data object NotFound : DeckEditorEvent
}

class DeckEditorViewModel(
    private val repository: FlashbackRepository,
    private val deckId: Long,
) : ViewModel() {

    private val isEditing = deckId != NEW_ID

    private var loadedDeck: Deck? = null

    private val form = MutableStateFlow(DeckEditorForm())
    private val errors = MutableStateFlow(DeckEditorErrors())
    private val saving = MutableStateFlow(false)

    /** What unsaved changes are measured against; null until an existing deck has loaded. */
    private val baseline = MutableStateFlow<DeckEditorForm?>(if (isEditing) null else form.value)

    private val events = Channel<DeckEditorEvent>(Channel.BUFFERED)
    val eventFlow = events.receiveAsFlow()

    val uiState: StateFlow<DeckEditorUiState> = combine(
        form, errors, saving, baseline, repository.observeAllTags(),
    ) { form, errors, saving, baseline, tags ->
        DeckEditorUiState(
            isEditing = isEditing,
            ready = baseline != null,
            form = form,
            errors = errors,
            tags = tags,
            isDirty = baseline != null && baseline.normalized() != form.normalized(),
            saving = saving,
        )
    }.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS),
        DeckEditorUiState(isEditing = isEditing, ready = !isEditing),
    )

    init {
        if (isEditing) load()
    }

    private fun load() = viewModelScope.launch {
        val deck = repository.getDeck(deckId)
        if (deck == null) {
            events.send(DeckEditorEvent.NotFound)
            return@launch
        }
        // Seeded once: the tag query would otherwise re-emit over the user's changes.
        val tagIds = repository.observeTagsForDeck(deckId).first().map { it.id }.toSet()
        loadedDeck = deck
        val loaded = DeckEditorForm.from(deck, tagIds)
        form.value = loaded
        baseline.value = loaded
    }

    fun onNameChange(value: String) {
        form.update { it.copy(name = value) }
        errors.update { it.copy(nameRequired = false) }
    }

    fun onDescriptionChange(value: String) = form.update { it.copy(description = value) }

    fun onReverseChange(value: Boolean) = form.update { it.copy(reverseByDefault = value) }

    fun onNewPerDayChange(value: String) {
        form.update { it.copy(newPerDay = value) }
        errors.update { it.copy(newPerDay = null) }
    }

    fun onReviewsPerDayChange(value: String) {
        form.update { it.copy(reviewsPerDay = value) }
        errors.update { it.copy(reviewsPerDay = null) }
    }

    fun onTagToggle(tagId: Long, checked: Boolean) = form.update {
        it.copy(tagIds = if (checked) it.tagIds + tagId else it.tagIds - tagId)
    }

    fun save() {
        if (saving.value) return
        val current = form.value
        when (val result = current.validate()) {
            is DeckEditorValidation.Invalid -> errors.value = result.errors
            is DeckEditorValidation.Valid -> {
                errors.value = DeckEditorErrors()
                saving.value = true
                viewModelScope.launch {
                    repository.saveDeck(current.toDeck(result, loadedDeck), current.tagIds)
                    events.send(DeckEditorEvent.Saved)
                }
            }
        }
    }

    companion object {
        const val NEW_ID = Routes.NEW_ID
        private const val STOP_TIMEOUT_MS = 5_000L
    }
}
