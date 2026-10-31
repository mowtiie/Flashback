package com.mowtiie.flashback.ui.tags

import com.mowtiie.flashback.data.entity.Tag
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class TagEditorFormTest {

    @Test
    fun newFormStartsOnTheFirstPaletteColour() {
        assertEquals(TagPalette.colors.first(), TagEditorForm().color)
    }

    @Test
    fun blankNameIsInvalid() {
        assertFalse(TagEditorForm(name = "  ").isValid)
        assertTrue(TagEditorForm(name = "Biology").isValid)
    }

    @Test
    fun whitespaceAroundTextIsNotAnEdit() {
        val clean = TagEditorForm(name = "Biology", description = "Year 2")

        assertEquals(clean.normalized(), clean.copy(name = " Biology ", description = "Year 2\n").normalized())
    }

    @Test
    fun colourChangeIsAnEdit() {
        val clean = TagEditorForm(name = "Biology")

        assertFalse(clean.normalized() == clean.copy(color = TagPalette.colors[3]).normalized())
    }

    @Test
    fun blankDescriptionBecomesNull() {
        assertNull(TagEditorForm(name = "Biology", description = "  ").toTag(null).description)
    }

    @Test
    fun editingKeepsTheId() {
        val existing = Tag(id = 5, name = "Old", color = 0)

        val tag = TagEditorForm(name = " New ", color = TagPalette.colors[2]).toTag(existing)

        assertEquals(5L, tag.id)
        assertEquals("New", tag.name)
        assertEquals(TagPalette.colors[2], tag.color)
    }

    @Test
    fun formRoundTripsAnExistingTag() {
        val tag = Tag(id = 2, name = "Bio", description = "Cells", color = TagPalette.colors[4])

        assertEquals(tag, TagEditorForm.from(tag).toTag(tag))
    }

    @Test
    fun paletteMatchesTheJavaApp() {
        assertEquals(12, TagPalette.colors.size)
        assertEquals(0xFFE5484D.toInt(), TagPalette.colors[0])
        assertEquals(0xFF8B8D98.toInt(), TagPalette.colors[11])
        assertEquals(TagPalette.colors.size, TagPalette.colors.toSet().size)
    }
}
