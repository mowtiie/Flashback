package com.mowtiie.flashback.ui.notes

import androidx.compose.runtime.Immutable
import com.mowtiie.flashback.data.entity.Note

@Immutable
data class NoteEditorForm(
    val front: String = "",
    val back: String = "",
    val reverseEnabled: Boolean = false,
    val suspended: Boolean = false,
) {
    /** Surrounding whitespace is not an edit. */
    fun normalized() = copy(front = front.trim(), back = back.trim())

    /** One problem at a time, front first, as in the Java app. */
    fun validate(): NoteEditorError? = when {
        front.isBlank() -> NoteEditorError.FrontRequired
        back.isBlank() -> NoteEditorError.BackRequired
        else -> null
    }

    /** Next card in a run: text cleared, the switches kept since a run usually shares them. */
    fun clearedForNext() = copy(front = "", back = "")

    /** [existing] keeps its id, deck and creation time when editing. */
    fun applyTo(existing: Note): Note {
        val form = normalized()
        return existing.copy(front = form.front, back = form.back, reverseEnabled = reverseEnabled)
    }

    fun toNewNote(deckId: Long): Note {
        val form = normalized()
        return Note(
            deckId = deckId,
            front = form.front,
            back = form.back,
            reverseEnabled = reverseEnabled,
        )
    }

    companion object {
        fun from(note: Note, suspended: Boolean) = NoteEditorForm(
            front = note.front,
            back = note.back,
            reverseEnabled = note.reverseEnabled,
            suspended = suspended,
        )
    }
}

enum class NoteEditorError { FrontRequired, BackRequired }
