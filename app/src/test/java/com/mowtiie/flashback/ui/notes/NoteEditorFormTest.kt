package com.mowtiie.flashback.ui.notes

import com.mowtiie.flashback.data.entity.Note
import com.mowtiie.flashback.data.model.NoteChoices
import com.mowtiie.flashback.ui.study.AnswerMode
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class NoteEditorFormTest {

    @Test
    fun blankFrontIsReportedBeforeBlankBack() {
        assertEquals(NoteEditorError.FrontRequired, NoteEditorForm(front = " ", back = "").validate())
    }

    @Test
    fun blankBackIsReportedWhenFrontIsFilled() {
        assertEquals(NoteEditorError.BackRequired, NoteEditorForm(front = "a", back = "  ").validate())
    }

    @Test
    fun filledFormIsValid() {
        assertNull(NoteEditorForm(front = "a", back = "b").validate())
    }

    @Test
    fun whitespaceAroundTextIsNotAnEdit() {
        val clean = NoteEditorForm(front = "el bosque", back = "the forest")
        val padded = clean.copy(front = " el bosque\n", back = "the forest ")

        assertEquals(clean.normalized(), padded.normalized())
    }

    @Test
    fun switchChangesAreEdits() {
        val clean = NoteEditorForm(front = "a", back = "b")

        assertFalse(clean.normalized() == clean.copy(reverseEnabled = true).normalized())
        assertFalse(clean.normalized() == clean.copy(suspended = true).normalized())
    }

    @Test
    fun clearingForTheNextCardKeepsTheSwitches() {
        val next = NoteEditorForm("a", "b", reverseEnabled = true, suspended = false).clearedForNext()

        assertEquals("", next.front)
        assertEquals("", next.back)
        assertTrue(next.reverseEnabled)
    }

    @Test
    fun newNoteIsTrimmedAndBelongsToTheDeck() {
        val note = NoteEditorForm(front = " a ", back = "b\n", reverseEnabled = true).toNewNote(deckId = 4)

        assertEquals(4L, note.deckId)
        assertEquals("a", note.front)
        assertEquals("b", note.back)
        assertTrue(note.reverseEnabled)
        assertEquals(0L, note.id)
    }

    @Test
    fun editingKeepsIdDeckAndCreationTime() {
        val existing = Note(id = 9, deckId = 2, front = "old", back = "old", createdAt = 100L, modifiedAt = 200L)

        val updated = NoteEditorForm(front = " new ", back = "new", reverseEnabled = true).applyTo(existing)

        assertEquals(9L, updated.id)
        assertEquals(2L, updated.deckId)
        assertEquals(100L, updated.createdAt)
        assertEquals("new", updated.front)
        assertTrue(updated.reverseEnabled)
    }

    @Test
    fun formRoundTripsAnExistingNote() {
        val note = Note(id = 1, deckId = 1, front = "f", back = "b", reverseEnabled = true)

        val form = NoteEditorForm.from(note, suspended = true)

        assertEquals(note, form.applyTo(note))
        assertTrue(form.suspended)
    }

    @Test
    fun aNoteWithoutATypeStaysWithoutOne() {
        val note = NoteEditorForm(front = "a", back = "b").toNewNote(deckId = 1)
        assertNull(note.answerMode)
        assertNull(note.choices)
    }

    @Test
    fun aChoiceNoteStoresItsWrongOptionsTrimmedAndWithoutBlanks() {
        val form = NoteEditorForm(
            front = "Capital of France?",
            back = "Paris",
            answerMode = AnswerMode.Choice,
            wrongOptions = listOf(" Rome ", "", "Madrid"),
        )

        val note = form.toNewNote(deckId = 1)

        assertEquals(AnswerMode.Choice.value, note.answerMode)
        assertEquals(listOf("Rome", "Madrid"), NoteChoices.parse(note.choices))
    }

    @Test
    fun wrongOptionsAreDroppedWhenTheTypeIsNotChoice() {
        val form = NoteEditorForm(
            front = "a",
            back = "b",
            answerMode = AnswerMode.Type,
            wrongOptions = listOf("x", "y", "z"),
        )

        val note = form.toNewNote(deckId = 1)

        assertEquals(AnswerMode.Type.value, note.answerMode)
        assertNull(note.choices)
    }

    @Test
    fun editingKeepsTheNoteAndLoadsItsTypeAndOptions() {
        val existing = Note(
            id = 5, deckId = 2, front = "q", back = "a",
            answerMode = AnswerMode.Choice.value, choices = NoteChoices.encode(listOf("x", "y")),
        )

        val form = NoteEditorForm.from(existing, suspended = false)

        assertEquals(AnswerMode.Choice, form.answerMode)
        assertEquals(listOf("x", "y", ""), form.wrongOptions)
        val saved = form.copy(back = "a2").applyTo(existing)
        assertEquals(5L, saved.id)
        assertEquals(listOf("x", "y"), NoteChoices.parse(saved.choices))
    }

    @Test
    fun typeAndOptionChangesAreEdits() {
        val clean = NoteEditorForm(front = "a", back = "b")

        assertFalse(clean.normalized() == clean.copy(answerMode = AnswerMode.Type).normalized())
        val choice = clean.copy(answerMode = AnswerMode.Choice)
        assertFalse(choice.normalized() == choice.copy(wrongOptions = listOf("x", "", "")).normalized())
    }

    @Test
    fun nextCardKeepsTheTypeButNotTheOptions() {
        val form = NoteEditorForm(
            front = "a", back = "b", answerMode = AnswerMode.Choice, wrongOptions = listOf("x", "y", "z"),
        ).clearedForNext()

        assertEquals(AnswerMode.Choice, form.answerMode)
        assertEquals(listOf("", "", ""), form.wrongOptions)
        assertEquals("", form.front)
    }
}
