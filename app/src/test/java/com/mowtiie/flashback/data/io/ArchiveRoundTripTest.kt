package com.mowtiie.flashback.data.io

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test

/** Uses the real org.json via testImplementation; Android bundles it at runtime. */
class ArchiveRoundTripTest {

    private val serializer = ArchiveSerializer()
    private val parser = ArchiveParser()

    private fun sample() = DeckArchive(
        exportedAt = 1_700_000_000_000L,
        decks = listOf(
            DeckArchive.Deck(
                name = "Spanish",
                description = "Core vocab",
                reverseByDefault = true,
                newPerDay = 15,
                reviewsPerDay = 150,
                tags = listOf(DeckArchive.Tag(name = "Language", color = -12345)),
                notes = listOf(
                    DeckArchive.Note("el bosque", "the forest", reverseEnabled = true),
                    DeckArchive.Note("la montaña", "the mountain"),
                ),
            ),
        ),
    )

    @Test
    fun roundTripPreservesEveryField() {
        val back = parser.parse(serializer.serialize(sample()))

        assertEquals(1, back.version)
        assertEquals(1_700_000_000_000L, back.exportedAt)
        assertEquals(1, back.decks.size)

        val deck = back.decks[0]
        assertEquals("Spanish", deck.name)
        assertEquals("Core vocab", deck.description)
        assertTrue(deck.reverseByDefault)
        assertEquals(15, deck.newPerDay)
        assertEquals(150, deck.reviewsPerDay)
        assertEquals("Language", deck.tags[0].name)
        assertEquals(-12345, deck.tags[0].color)
        assertEquals(2, deck.notes.size)
        assertTrue(deck.notes[0].reverseEnabled)
        assertFalse(deck.notes[1].reverseEnabled)
    }

    @Test
    fun countsMatchContents() {
        val counts = parser.parse(serializer.serialize(sample())).counts()
        assertEquals(1, counts.decks)
        assertEquals(2, counts.notes)
        assertEquals(1, counts.tags)
    }

    @Test
    fun optionalFieldsFallBackToDefaults() {
        val archive = parser.parse("{\"version\":1,\"decks\":[{\"name\":\"D\",\"notes\":[]}]}")
        val deck = archive.decks[0]
        assertEquals(20, deck.newPerDay)
        assertEquals(200, deck.reviewsPerDay)
        assertNull(deck.description)
    }

    @Test
    fun namelessTagIsDroppedNotFatal() {
        val archive = parser.parse(
            "{\"version\":1,\"decks\":[{\"name\":\"D\"," +
                "\"tags\":[{\"color\":1},{\"name\":\"Keep\"}],\"notes\":[]}]}",
        )
        assertEquals(1, archive.decks[0].tags.size)
        assertEquals("Keep", archive.decks[0].tags[0].name)
    }

    @Test
    fun unicodeAndEscapesSurvive() {
        val deck = DeckArchive.Deck(
            name = "Quotes \" and \\ and \n and 日本語",
            notes = listOf(DeckArchive.Note("emoji 🎴", "tab\there")),
        )

        val back = parser.parse(serializer.serialize(DeckArchive(decks = listOf(deck))))
        assertEquals(deck.name, back.decks[0].name)
        assertEquals("emoji 🎴", back.decks[0].notes[0].front)
        assertEquals("tab\there", back.decks[0].notes[0].back)
    }

    @Test
    fun overlongCardTextRejected() {
        val big = "x".repeat(10_001)
        assertRejected(
            "{\"version\":1,\"decks\":[{\"name\":\"D\",\"notes\":" +
                "[{\"front\":\"a\",\"back\":\"$big\"}]}]}",
            "too long",
        )
    }

    @Test
    fun tagColourIsForcedOpaqueAndZeroKept() {
        val archive = parser.parse(
            "{\"version\":1,\"decks\":[{\"name\":\"D\",\"tags\":[" +
                "{\"name\":\"a\",\"color\":16711680},{\"name\":\"b\",\"color\":0}]}]}",
        )
        assertEquals(0xFFFF0000.toInt(), archive.decks[0].tags[0].color)
        assertEquals(0, archive.decks[0].tags[1].color)
    }

    @Test
    fun overlongTagNameIsTruncated() {
        val name = "t".repeat(150)
        val archive = parser.parse(
            "{\"version\":1,\"decks\":[{\"name\":\"D\",\"tags\":[{\"name\":\"$name\"}]}]}",
        )
        assertEquals(100, archive.decks[0].tags[0].name.length)
    }

    private fun assertRejected(raw: String, fragment: String) {
        try {
            parser.parse(raw)
            fail("Expected rejection for: $raw")
        } catch (e: ArchiveException) {
            assertTrue(
                "message was: ${e.message}",
                e.message!!.lowercase().contains(fragment.lowercase()),
            )
        }
    }

    @Test
    fun emptyInputRejected() = assertRejected("", "empty")

    @Test
    fun nonJsonRejected() = assertRejected("not json at all", "valid json")

    @Test
    fun missingVersionRejected() = assertRejected("{\"decks\":[]}", "version")

    @Test
    fun futureVersionRejected() =
        assertRejected("{\"version\":99,\"decks\":[{\"name\":\"x\"}]}", "newer version")

    @Test
    fun missingDecksRejected() = assertRejected("{\"version\":1}", "no decks")

    @Test
    fun emptyDecksRejected() = assertRejected("{\"version\":1,\"decks\":[]}", "no decks")

    @Test
    fun deckWithoutNameRejected() =
        assertRejected("{\"version\":1,\"decks\":[{\"notes\":[]}]}", "no name")

    @Test
    fun noteMissingBackRejected() = assertRejected(
        "{\"version\":1,\"decks\":[{\"name\":\"D\"," +
            "\"notes\":[{\"front\":\"a\"}]}]}",
        "missing its front or back",
    )

    // Additions beyond the Java tests: a file exactly as the Java app wrote it must still import.

    @Test
    fun javaWrittenFileImports() {
        val javaFile = """
            {
              "version": 1,
              "exportedAt": 1700000000000,
              "decks": [
                {
                  "name": "Spanish",
                  "description": "Core vocab",
                  "reverseByDefault": true,
                  "newPerDay": 15,
                  "reviewsPerDay": 150,
                  "tags": [ { "name": "Language", "color": -12345 } ],
                  "notes": [ { "front": "el bosque", "back": "the forest", "reverseEnabled": true } ]
                }
              ]
            }
        """.trimIndent()
        val deck = parser.parse(javaFile).decks.single()
        assertEquals("Spanish", deck.name)
        assertEquals(-12345, deck.tags.single().color)
        assertTrue(deck.notes.single().reverseEnabled)
    }

    @Test
    fun serializedKeysMatchTheJavaFormat() {
        val text = serializer.serialize(sample())
        listOf(
            "\"version\"", "\"exportedAt\"", "\"decks\"", "\"name\"", "\"description\"",
            "\"reverseByDefault\"", "\"newPerDay\"", "\"reviewsPerDay\"", "\"tags\"",
            "\"color\"", "\"notes\"", "\"front\"", "\"back\"", "\"reverseEnabled\"",
        ).forEach { assertTrue("missing $it", text.contains(it)) }
    }

    @Test
    fun blankDescriptionAndZeroColourAreOmitted() {
        val deck = DeckArchive.Deck(
            name = "D",
            description = "   ",
            tags = listOf(DeckArchive.Tag("t", description = "", color = 0)),
        )
        val text = serializer.serialize(DeckArchive(decks = listOf(deck)))
        assertFalse(text.contains("description"))
        assertFalse(text.contains("color"))
    }

    @Test
    fun nonObjectDeckIsReportedAsMalformed() =
        assertRejected("{\"version\":1,\"decks\":[\"oops\"]}", "malformed")

    @Test
    fun nonPositiveLimitsFallBackToDefaults() {
        val deck = parser.parse(
            "{\"version\":1,\"decks\":[{\"name\":\"D\",\"newPerDay\":0,\"reviewsPerDay\":-5}]}",
        ).decks[0]
        assertEquals(20, deck.newPerDay)
        assertEquals(200, deck.reviewsPerDay)
    }

    @Test
    fun overlongDeckNameIsTruncatedAndNamesAreTrimmed() {
        val deck = parser.parse(
            "{\"version\":1,\"decks\":[{\"name\":\"  ${"n".repeat(250)}  \"}]}",
        ).decks[0]
        assertEquals(200, deck.name.length)
    }

    @Test
    fun deckExportFileNameReplacesUnsafeCharacters() {
        assertEquals("flashback-Spanish_101_.json", deckExportFileName("flashback-", "Spanish 101!"))
        assertEquals("flashback-a-b_c.json", deckExportFileName("flashback-", "a-b_c"))
        assertEquals("flashback-deck.json", deckExportFileName("flashback-", null))
    }
}
