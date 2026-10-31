package com.mowtiie.flashback.ui.tags

import androidx.compose.runtime.Immutable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mowtiie.flashback.data.entity.Tag
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
data class TagEditorUiState(
    val isEditing: Boolean = false,
    /** False while an existing tag is still loading. */
    val ready: Boolean = false,
    val form: TagEditorForm = TagEditorForm(),
    val nameRequired: Boolean = false,
    val nameTaken: Boolean = false,
    val isDirty: Boolean = false,
    val saving: Boolean = false,
)

sealed interface TagEditorEvent {
    data object Saved : TagEditorEvent

    class Deleted(val undo: FlashbackRepository.UndoAction) : TagEditorEvent

    /** The tag being edited no longer exists. */
    data object NotFound : TagEditorEvent
}

class TagEditorViewModel(
    private val repository: FlashbackRepository,
    private val tagId: Long,
) : ViewModel() {

    private val isEditing = tagId != NEW_ID

    private var loadedTag: Tag? = null

    private val form = MutableStateFlow(TagEditorForm())
    private val nameRequired = MutableStateFlow(false)
    private val nameTaken = MutableStateFlow(false)
    private val saving = MutableStateFlow(false)

    /** What unsaved changes are measured against; null until an existing tag has loaded. */
    private val baseline = MutableStateFlow<TagEditorForm?>(if (isEditing) null else form.value)

    private val events = Channel<TagEditorEvent>(Channel.BUFFERED)
    val eventFlow = events.receiveAsFlow()

    val uiState: StateFlow<TagEditorUiState> = combine(
        form, nameRequired, nameTaken, saving, baseline,
    ) { form, nameRequired, nameTaken, saving, baseline ->
        TagEditorUiState(
            isEditing = isEditing,
            ready = baseline != null,
            form = form,
            nameRequired = nameRequired,
            nameTaken = nameTaken,
            isDirty = baseline != null && baseline.normalized() != form.normalized(),
            saving = saving,
        )
    }.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS),
        TagEditorUiState(isEditing = isEditing, ready = !isEditing),
    )

    init {
        if (isEditing) load()
    }

    private fun load() = viewModelScope.launch {
        val tag = repository.getTag(tagId)
        if (tag == null) {
            events.send(TagEditorEvent.NotFound)
            return@launch
        }
        loadedTag = tag
        val loaded = TagEditorForm.from(tag)
        form.value = loaded
        baseline.value = loaded
    }

    fun onNameChange(value: String) {
        form.update { it.copy(name = value) }
        nameRequired.value = false
        nameTaken.value = false
    }

    fun onDescriptionChange(value: String) = form.update { it.copy(description = value) }

    fun onColorSelected(color: Int) = form.update { it.copy(color = color) }

    fun save() {
        if (saving.value) return
        val current = form.value
        if (!current.isValid) {
            nameRequired.value = true
            return
        }
        saving.value = true
        viewModelScope.launch {
            val savedId = repository.saveTag(current.toTag(loadedTag))
            saving.value = false
            if (savedId == null) nameTaken.value = true else events.send(TagEditorEvent.Saved)
        }
    }

    fun delete() {
        val tag = loadedTag ?: return
        viewModelScope.launch {
            events.send(TagEditorEvent.Deleted(repository.deleteTag(tag)))
        }
    }

    companion object {
        const val NEW_ID = Routes.NEW_ID
        private const val STOP_TIMEOUT_MS = 5_000L
    }
}
