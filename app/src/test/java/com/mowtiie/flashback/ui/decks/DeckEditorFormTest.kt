package com.mowtiie.flashback.ui.decks

import com.mowtiie.flashback.data.entity.Deck
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class DeckEditorFormTest {

    private val filled = DeckEditorForm(name = "Spanish")

    private fun valid(form: DeckEditorForm) =
        form.validate() as DeckEditorValidation.Valid

    private fun errors(form: DeckEditorForm) =
        (form.validate() as DeckEditorValidation.Invalid).errors

    @Test
    fun defaultsMatchTheJavaApp() {
        val result = valid(filled)

        assertEquals(20, result.newPerDay)
        assertEquals(200, result.reviewsPerDay)
    }

    @Test
    fun blankNameIsRequired() {
        assertTrue(errors(filled.copy(name = "   ")).nameRequired)
    }

    @Test
    fun emptyLimitIsRequired() {
        val e = errors(filled.copy(newPerDay = "", reviewsPerDay = " "))

        assertEquals(LimitError.Required, e.newPerDay)
        assertEquals(LimitError.Required, e.reviewsPerDay)
    }

    @Test
    fun limitAboveMaximumIsTooLarge() {
        val e = errors(filled.copy(newPerDay = "10000", reviewsPerDay = "99999999999"))

        assertEquals(LimitError.TooLarge, e.newPerDay)
        assertEquals(LimitError.TooLarge, e.reviewsPerDay)
    }

    @Test
    fun limitAtMaximumAndZeroAreAccepted() {
        val result = valid(filled.copy(newPerDay = "9999", reviewsPerDay = "0"))

        assertEquals(9999, result.newPerDay)
        assertEquals(0, result.reviewsPerDay)
    }

    @Test
    fun allProblemsAreReportedTogether() {
        val e = errors(DeckEditorForm(name = "", newPerDay = "", reviewsPerDay = "x"))

        assertTrue(e.nameRequired)
        assertEquals(LimitError.Required, e.newPerDay)
        assertEquals(LimitError.TooLarge, e.reviewsPerDay)
    }

    @Test
    fun whitespaceAroundValuesIsNotAnEdit() {
        val clean = DeckEditorForm(name = "Spanish", description = "Words")
        val padded = clean.copy(name = " Spanish ", description = "Words\n", newPerDay = "20 ")

        assertEquals(clean.normalized(), padded.normalized())
    }

    @Test
    fun blankDescriptionBecomesNull() {
        val deck = filled.copy(description = "  ").toDeck(valid(filled), null)

        assertNull(deck.description)
    }

    @Test
    fun editingKeepsIdAndCreationTime() {
        val existing = Deck(id = 7, name = "Old", createdAt = 1234L)
        val form = DeckEditorForm.from(existing, setOf(1L)).copy(name = " New ", newPerDay = "5")

        val deck = form.toDeck(valid(form), existing)

        assertEquals(7L, deck.id)
        assertEquals(1234L, deck.createdAt)
        assertEquals("New", deck.name)
        assertEquals(5, deck.newPerDay)
    }

    @Test
    fun formRoundTripsAnExistingDeck() {
        val existing = Deck(
            id = 3, name = "Bio", description = "Cells", reverseByDefault = true,
            newPerDay = 10, reviewsPerDay = 100,
        )
        val form = DeckEditorForm.from(existing, setOf(2L, 5L))

        assertEquals(existing, form.toDeck(valid(form), existing))
        assertEquals(setOf(2L, 5L), form.tagIds)
    }
}
