package com.mowtiie.flashback.data.io

import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test
import org.junit.runner.RunWith

/** Android's org.json treats a JSON null differently from the library the JVM tests use. */
@RunWith(AndroidJUnit4::class)
class ArchiveParserAndroidTest {

    private val parser = ArchiveParser()

    private fun rejected(raw: String): String = try {
        parser.parse(raw)
        fail("Expected rejection for: $raw")
        ""
    } catch (e: ArchiveException) {
        e.message.orEmpty()
    }

    @Test
    fun nullBackIsMissingNotTheWordNull() {
        val message = rejected("{\"version\":1,\"decks\":[{\"name\":\"D\",\"notes\":[{\"front\":\"Q\",\"back\":null}]}]}")
        assertTrue(message, message.contains("missing its front or back"))
    }

    @Test
    fun nullFrontIsMissing() {
        val message = rejected("{\"version\":1,\"decks\":[{\"name\":\"D\",\"notes\":[{\"front\":null,\"back\":\"A\"}]}]}")
        assertTrue(message, message.contains("missing its front or back"))
    }

    @Test
    fun nullDeckNameHasNoName() {
        val message = rejected("{\"version\":1,\"decks\":[{\"name\":null,\"notes\":[]}]}")
        assertTrue(message, message.contains("no name"))
    }

    @Test
    fun nullDescriptionsAreAbsent() {
        val deck = parser.parse(
            "{\"version\":1,\"decks\":[{\"name\":\"D\",\"description\":null," +
                "\"tags\":[{\"name\":\"T\",\"description\":null}],\"notes\":[]}]}",
        ).decks.single()

        assertNull(deck.description)
        assertNull(deck.tags.single().description)
    }

    @Test
    fun nullTagNameIsDroppedLikeAMissingOne() {
        val deck = parser.parse(
            "{\"version\":1,\"decks\":[{\"name\":\"D\",\"tags\":[{\"name\":null},{\"name\":\"Keep\"}],\"notes\":[]}]}",
        ).decks.single()

        assertEquals(listOf("Keep"), deck.tags.map { it.name })
    }
}
