package com.mowtiie.flashback.ui.notes

import androidx.compose.runtime.Immutable
import com.mowtiie.flashback.data.entity.Note
import com.mowtiie.flashback.data.model.NoteChoices
import com.mowtiie.flashback.ui.study.AnswerMode

@Immutable
data class NoteEditorForm(
    val front: String = "",
    val back: String = "",
    val reverseEnabled: Boolean = false,
    val suspended: Boolean = false,
    /** Null follows the study setting. */
    val answerMode: AnswerMode? = null,
    /** Wrong options for a multiple choice note; blank slots are ignored. */
    val wrongOptions: List<String> = List(NoteChoices.MAX) { "" },
) {
    /** Surrounding whitespace is not an edit. */
    fun normalized() = copy(
        front = front.trim(),
        back = back.trim(),
        wrongOptions = wrongOptions.map { it.trim() },
    )

    /** One problem at a time, front first, as in the Java app. */
    fun validate(): NoteEditorError? = when {
        front.isBlank() -> NoteEditorError.FrontRequired
        back.isBlank() -> NoteEditorError.BackRequired
        else -> null
    }

    /** Next card in a run: text cleared, the switches and answer type kept since a run usually shares them. */
    fun clearedForNext() = copy(front = "", back = "", wrongOptions = List(NoteChoices.MAX) { "" })

    /** [existing] keeps its id, deck and creation time when editing. */
    fun applyTo(existing: Note): Note {
        val form = normalized()
        return existing.copy(
            front = form.front,
            back = form.back,
            reverseEnabled = reverseEnabled,
            answerMode = answerMode?.value,
            choices = form.storedChoices(),
        )
    }

    fun toNewNote(deckId: Long): Note {
        val form = normalized()
        return Note(
            deckId = deckId,
            front = form.front,
            back = form.back,
            reverseEnabled = reverseEnabled,
            answerMode = answerMode?.value,
            choices = form.storedChoices(),
        )
    }

    /** Wrong options only mean something on a multiple choice note. */
    private fun storedChoices(): String? =
        if (answerMode == AnswerMode.Choice) NoteChoices.encode(wrongOptions) else null

    companion object {
        fun from(note: Note, suspended: Boolean): NoteEditorForm {
            val options = NoteChoices.parse(note.choices)
            return NoteEditorForm(
                front = note.front,
                back = note.back,
                reverseEnabled = note.reverseEnabled,
                suspended = suspended,
                answerMode = note.answerMode?.let(AnswerMode::fromValue),
                wrongOptions = List(NoteChoices.MAX) { options.getOrElse(it) { "" } },
            )
        }
    }
}

enum class NoteEditorError { FrontRequired, BackRequired }
