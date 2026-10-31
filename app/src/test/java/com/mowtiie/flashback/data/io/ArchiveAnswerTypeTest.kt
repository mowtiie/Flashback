package com.mowtiie.flashback.data.io

import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ArchiveAnswerTypeTest {

    private val serializer = ArchiveSerializer()
    private val parser = ArchiveParser()

    private fun archive(vararg notes: DeckArchive.Note) =
        DeckArchive(decks = listOf(DeckArchive.Deck(name = "D", notes = notes.toList())))

    private fun parseNotes(notesJson: String) = parser.parse(
        """{"version":1,"decks":[{"name":"D","notes":[$notesJson]}]}""",
    ).decks.single().notes

    @Test
    fun answerTypeAndChoicesSurviveARoundTrip() {
        val note = DeckArchive.Note(
            front = "Capital of France?", back = "Paris", answerMode = 2, choices = listOf("Rome", "Madrid", "Berlin"),
        )

        val back = parser.parse(serializer.serialize(archive(note))).decks.single().notes.single()

        assertEquals(2, back.answerMode)
        assertEquals(listOf("Rome", "Madrid", "Berlin"), back.choices)
    }

    @Test
    fun theTypeIsWrittenAsAReadableName() {
        val json = JSONObject(serializer.serialize(archive(DeckArchive.Note("a", "b", answerMode = 1))))
        val note = json.getJSONArray("decks").getJSONObject(0).getJSONArray("notes").getJSONObject(0)
        assertEquals("type", note.getString("answerMode"))
    }

    @Test
    fun plainNotesAreWrittenExactlyAsBefore() {
        val json = JSONObject(serializer.serialize(archive(DeckArchive.Note("a", "b"))))
        val note = json.getJSONArray("decks").getJSONObject(0).getJSONArray("notes").getJSONObject(0)
        assertEquals(setOf("front", "back", "reverseEnabled"), note.keys().asSequence().toSet())
    }

    @Test
    fun filesFromTheJavaAppStillImport() {
        val note = parseNotes("""{"front":"a","back":"b","reverseEnabled":true}""").single()
        assertNull(note.answerMode)
        assertTrue(note.choices.isEmpty())
        assertTrue(note.reverseEnabled)
    }

    @Test
    fun answerTypeNamesAreMatchedIgnoringCase() {
        val notes = parseNotes(
            """{"front":"a","back":"b","answerMode":"Flip"},
               {"front":"c","back":"d","answerMode":" CHOICE "},
               {"front":"e","back":"f","answerMode":"type"}""",
        )
        assertEquals(listOf(0, 2, 1), notes.map { it.answerMode })
    }

    @Test
    fun anUnknownTypeIsIgnoredRatherThanFailingTheImport() {
        val notes = parseNotes(
            """{"front":"a","back":"b","answerMode":"hologram"},
               {"front":"c","back":"d","answerMode":null},
               {"front":"e","back":"f","answerMode":7}""",
        )
        assertEquals(3, notes.size)
        assertTrue(notes.all { it.answerMode == null })
    }

    @Test
    fun choicesAreCleanedAndCapped() {
        val note = parseNotes(
            """{"front":"a","back":"b","answerMode":"choice",
               "choices":[" Rome ","",null,"Rome","Madrid","Berlin","Oslo","${"x".repeat(121)}"]}""",
        ).single()
        assertEquals(listOf("Rome", "Madrid", "Berlin"), note.choices)
    }

    @Test
    fun choicesThatAreNotAnArrayAreIgnored() {
        val note = parseNotes("""{"front":"a","back":"b","choices":"Rome"}""").single()
        assertFalse(note.choices.isNotEmpty())
    }

    @Test
    fun theVersionStaysOneSoOlderAppsKeepReadingNewFiles() {
        val json = JSONObject(serializer.serialize(archive(DeckArchive.Note("a", "b", answerMode = 2))))
        assertEquals(1, json.getInt("version"))
    }
}
