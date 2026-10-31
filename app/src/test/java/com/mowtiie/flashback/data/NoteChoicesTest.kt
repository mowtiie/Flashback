package com.mowtiie.flashback.data

import com.mowtiie.flashback.data.model.NoteChoices
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class NoteChoicesTest {

    @Test
    fun roundTripsTheOptions() {
        val json = NoteChoices.encode(listOf("Rome", "Madrid", "Berlin"))
        assertEquals(listOf("Rome", "Madrid", "Berlin"), NoteChoices.parse(json))
    }

    @Test
    fun cleansDuplicatesBlanksAndTheLimit() {
        val json = NoteChoices.encode(listOf(" Rome ", "Rome", "", "Madrid", "Berlin", "Oslo"))
        assertEquals(listOf("Rome", "Madrid", "Berlin"), NoteChoices.parse(json))
    }

    @Test
    fun nothingToStoreIsNull() {
        assertNull(NoteChoices.encode(emptyList()))
        assertNull(NoteChoices.encode(listOf(" ", "")))
    }

    @Test
    fun badOrMissingJsonIsNoOptions() {
        assertEquals(emptyList<String>(), NoteChoices.parse(null))
        assertEquals(emptyList<String>(), NoteChoices.parse(""))
        assertEquals(emptyList<String>(), NoteChoices.parse("not json"))
        assertEquals(emptyList<String>(), NoteChoices.parse("{\"a\":1}"))
    }
}
