package com.mowtiie.flashback.data.io

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.mowtiie.flashback.data.FlashbackDatabase
import com.mowtiie.flashback.data.entity.Card
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ArchiveRepositoryTest {

    private lateinit var db: FlashbackDatabase
    private lateinit var repository: ArchiveRepository

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, FlashbackDatabase::class.java).build()
        repository = ArchiveRepository(db)
    }

    @After
    fun tearDown() = db.close()

    private fun archive(tagName: String = "Language") = DeckArchive(
        decks = listOf(
            DeckArchive.Deck(
                name = "Spanish",
                description = "Core vocab",
                reverseByDefault = true,
                newPerDay = 15,
                reviewsPerDay = 150,
                tags = listOf(DeckArchive.Tag(tagName, color = 0xFF3E63DD.toInt())),
                notes = listOf(
                    DeckArchive.Note("el bosque", "the forest", reverseEnabled = true),
                    DeckArchive.Note("la montaña", "the mountain"),
                ),
            ),
        ),
    )

    @Test
    fun importCreatesDeckNotesAndOneOrTwoCardsPerNote() = runBlocking {
        val result = repository.importArchive(archive())

        assertEquals(1, result.decks)
        assertEquals(2, result.notes)
        val deck = db.deckDao().getAll().single()
        assertEquals(15, deck.newPerDay)
        val notes = db.noteDao().getByDeck(deck.id)
        assertEquals(2, notes.size)
        val cards = db.cardDao().getByDeck(deck.id)
        assertEquals(3, cards.size)
        val reversed = notes.first { it.reverseEnabled }
        assertEquals(
            listOf(Card.ORDINAL_FORWARD, Card.ORDINAL_REVERSE),
            cards.filter { it.noteId == reversed.id }.map { it.ordinal }.sorted(),
        )
    }

    @Test
    fun importedCardsStartNew() = runBlocking {
        repository.importArchive(archive())
        val deck = db.deckDao().getAll().single()
        assertTrue(db.cardDao().getByDeck(deck.id).all { it.reps == 0 && it.state == 0 })
    }

    @Test
    fun tagsMergeByNameAcrossImports() = runBlocking {
        val first = repository.importArchive(archive())
        val second = repository.importArchive(archive())

        assertEquals(1, first.tagsCreated)
        assertEquals(0, first.tagsReused)
        assertEquals(0, second.tagsCreated)
        assertEquals(1, second.tagsReused)

        val decks = db.deckDao().getAll()
        assertEquals(2, decks.size)
        assertTrue(decks.all { db.tagDao().getTagsForDeck(it.id).single().name == "Language" })
    }

    @Test
    fun exportRoundTripsAnImport() = runBlocking {
        repository.importArchive(archive())

        val exported = repository.exportAll(now = 5L)

        assertEquals(5L, exported.exportedAt)
        val deck = exported.decks.single()
        assertEquals("Spanish", deck.name)
        assertEquals(150, deck.reviewsPerDay)
        assertEquals("Language", deck.tags.single().name)
        assertEquals(listOf("el bosque", "la montaña"), deck.notes.map { it.front }.sorted())
    }

    @Test
    fun exportingAMissingDeckGivesAnEmptyArchive() = runBlocking {
        assertTrue(repository.exportSingle(deckId = 999, now = 1L).decks.isEmpty())
    }
}
